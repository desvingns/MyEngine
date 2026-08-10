[CmdletBinding()]
param(
    [ValidateSet("validate", "gate1", "bridge")]
    [string]$Mode = "gate1",
    [Parameter(Mandatory = $true)]
    [string]$EvidenceRoot,
    [string]$RepoRoot = "",
    [string]$OpenQuestionsPath = "",
    [string]$EngineGapId = "",
    [string]$DemandTag = "",
    [switch]$Apply
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = Split-Path -Parent $PSScriptRoot
}
$RepoRoot = [System.IO.Path]::GetFullPath($RepoRoot)
$EvidenceRoot = [System.IO.Path]::GetFullPath($EvidenceRoot)

$errors = [System.Collections.Generic.List[string]]::new()
$warnings = [System.Collections.Generic.List[string]]::new()

function Add-Error([string]$Message) { [void]$errors.Add($Message) }
function Add-Warning([string]$Message) { [void]$warnings.Add($Message) }

function Has-Property {
    param([object]$Value, [string]$Name)
    return $null -ne $Value -and $null -ne $Value.PSObject.Properties[$Name]
}

function Require-Properties {
    param([object]$Value, [string]$Context, [string[]]$Names)
    foreach ($name in $Names) {
        if (-not (Has-Property $Value $name)) {
            Add-Error "$Context is missing '$name'"
        }
    }
}

function Get-Array([object]$Value) {
    if ($null -eq $Value) { return @() }
    return @($Value)
}

function Test-UniqueIds {
    param([object[]]$Values, [string]$Context, [string]$IdProperty = "id")
    $ids = @($Values | ForEach-Object { [string]$_.$IdProperty })
    $duplicates = @($ids | Group-Object | Where-Object { $_.Count -gt 1 } | ForEach-Object { $_.Name })
    foreach ($duplicate in $duplicates) { Add-Error "$Context contains duplicate id '$duplicate'" }
    return $ids
}

function Test-Confidence {
    param([object]$Value, [string]$Context)
    $parsed = 0.0
    if (-not [double]::TryParse(
        [string]$Value,
        [Globalization.NumberStyles]::Float,
        [Globalization.CultureInfo]::InvariantCulture,
        [ref]$parsed
    ) -or $parsed -lt 0 -or $parsed -gt 1) {
        Add-Error "$Context confidence must be a number in [0,1]"
    }
    return $parsed
}

function Test-IdList {
    param([object]$Values, [string]$Pattern, [string]$Context)
    foreach ($value in (Get-Array $Values)) {
        if ([string]$value -notmatch $Pattern) { Add-Error "$Context contains invalid id '$value'" }
    }
}

function Read-CsvFile {
    param([string]$Path, [string[]]$ExpectedHeaders, [string]$Context)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        Add-Error "$Context is missing: $Path"
        return @()
    }
    $header = (Get-Content -LiteralPath $Path -TotalCount 1)
    $expected = $ExpectedHeaders -join ","
    if ($header -ne $expected) { Add-Error "$Context header must be '$expected'" }
    return @(Import-Csv -LiteralPath $Path)
}

function Read-EvidenceBundle {
    if (-not (Test-Path -LiteralPath $EvidenceRoot -PathType Container)) {
        Add-Error "Evidence root does not exist: $EvidenceRoot"
        return $null
    }

    $graphPath = Join-Path $EvidenceRoot "state-graph.v1.json"
    $claimsPath = Join-Path $EvidenceRoot "mechanic-claims.csv"
    $evidenceIndexPath = Join-Path $EvidenceRoot "evidence-index.csv"
    $graph = $null
    if (-not (Test-Path -LiteralPath $graphPath -PathType Leaf)) {
        Add-Error "Missing state graph: $graphPath"
    } else {
        try { $graph = Get-Content -Raw -LiteralPath $graphPath | ConvertFrom-Json -ErrorAction Stop }
        catch { Add-Error "state-graph.v1.json is not valid JSON: $($_.Exception.Message)" }
    }

    $claims = Read-CsvFile -Path $claimsPath -ExpectedHeaders @(
        "claim_id", "claim", "hypothesis", "controlled_variables", "sample_count",
        "supporting_evidence_ids", "contradicting_evidence_ids", "confidence", "status",
        "future_fr_links", "future_eng_links", "notes"
    ) -Context "mechanic-claims.csv"
    $evidence = Read-CsvFile -Path $evidenceIndexPath -ExpectedHeaders @(
        "evidence_id", "source_type", "local_relative_path", "sha256", "captured_at_utc",
        "sanitized_summary", "ip_privacy_review", "status"
    ) -Context "evidence-index.csv"

    return [pscustomobject][ordered]@{
        graph = $graph
        claims = @($claims)
        evidence = @($evidence)
        paths = [ordered]@{
            graph = $graphPath
            claims = $claimsPath
            evidence = $evidenceIndexPath
        }
    }
}

function Test-PublicSafety {
    $forbidden = @(".apk", ".aab", ".png", ".jpg", ".jpeg", ".webp", ".gif", ".mp4", ".webm", ".xml", ".wav", ".mp3", ".zip")
    if (Test-Path -LiteralPath $EvidenceRoot -PathType Container) {
        foreach ($file in @(Get-ChildItem -LiteralPath $EvidenceRoot -Recurse -File)) {
            if ($forbidden -contains $file.Extension.ToLowerInvariant()) {
                Add-Error "Public evidence root contains forbidden raw artifact: $($file.FullName)"
            }
        }
    }
}

function Test-EvidenceIndex {
    param([object[]]$Evidence)
    $ids = Test-UniqueIds -Values $Evidence -Context "evidence-index" -IdProperty "evidence_id"
    foreach ($row in $Evidence) {
        if ([string]$row.evidence_id -notmatch '^EV-[0-9]{4}$') { Add-Error "Invalid evidence id '$($row.evidence_id)'" }
        if ([string]$row.sha256 -notmatch '^[a-fA-F0-9]{64}$') { Add-Error "Evidence $($row.evidence_id) has invalid sha256" }
        if ([string]$row.local_relative_path -notmatch '^\.reference-local[\\/]') {
            Add-Error "Evidence $($row.evidence_id) must point inside .reference-local"
        }
        if ([System.IO.Path]::IsPathRooted([string]$row.local_relative_path)) {
            Add-Error "Evidence $($row.evidence_id) uses an absolute path"
        }
        if (@("pass", "pending", "fail") -notcontains [string]$row.ip_privacy_review) {
            Add-Error "Evidence $($row.evidence_id) has invalid ip_privacy_review"
        }
    }
    return $ids
}

function Test-EvidenceRef {
    param([string]$Id, [string]$Context, [string[]]$EvidenceIds, [hashtable]$EvidenceById)
    if ([string]::IsNullOrWhiteSpace($Id)) { Add-Error "$Context has a blank evidence id"; return }
    if ($EvidenceIds -notcontains $Id) { Add-Error "$Context references unknown evidence '$Id'"; return }
    if ([string]$EvidenceById[$Id].ip_privacy_review -ne "pass") {
        Add-Error "$Context references evidence without passed IP/privacy review '$Id'"
    }
}

function Test-Graph {
    param([object]$Graph, [object[]]$Evidence)
    if ($null -eq $Graph) { return $null }
    Require-Properties $Graph "state graph" @("schema_version", "reference", "nodes", "edges", "observations", "coverage")
    if ([string]$Graph.schema_version -ne "state-graph.v1") { Add-Error "schema_version must equal state-graph.v1" }
    Require-Properties $Graph.reference "reference" @("package", "capture_meta_evidence_id")
    $evidenceIds = Test-EvidenceIndex -Evidence $Evidence
    $evidenceById = @{}
    foreach ($row in $Evidence) { $evidenceById[[string]$row.evidence_id] = $row }

    $nodes = @(Get-Array $Graph.nodes)
    $edges = @(Get-Array $Graph.edges)
    $observations = @(Get-Array $Graph.observations)
    $nodeIds = Test-UniqueIds -Values $nodes -Context "nodes"
    $edgeIds = Test-UniqueIds -Values $edges -Context "edges"
    $observationIds = Test-UniqueIds -Values $observations -Context "observations"
    $affordances = [System.Collections.Generic.List[object]]::new()

    foreach ($node in $nodes) {
        Require-Properties $node "node $($node.id)" @(
            "id", "kind", "parent", "route", "phase", "semantic_flags", "visible_affordances",
            "signatures", "evidence_tier", "visual_status", "visual_evidence_ids", "evidence_ids", "source", "confidence"
        )
        if ([string]$node.id -notmatch '^ST-[0-9]{4}$') { Add-Error "Invalid node id '$($node.id)'" }
        if (@("screen", "overlay", "battle_phase", "meta_state") -notcontains [string]$node.kind) { Add-Error "Invalid node kind at $($node.id)" }
        if ($null -ne $node.parent -and $nodeIds -notcontains [string]$node.parent) { Add-Error "Node $($node.id) references unknown parent $($node.parent)" }
        Test-Confidence $node.confidence "node $($node.id)" | Out-Null
        if (@("observed", "inferred") -notcontains [string]$node.source) { Add-Error "Invalid node source at $($node.id)" }
        if (@("behavioral", "behavioral_and_visual") -notcontains [string]$node.evidence_tier) { Add-Error "Invalid evidence_tier at $($node.id)" }
        if (@("usable", "deferred_missing_signature", "preserved_unusable") -notcontains [string]$node.visual_status) { Add-Error "Invalid visual_status at $($node.id)" }
        Require-Properties $node.signatures "node $($node.id).signatures" @("structural", "visual", "semantic")
        Require-Properties $node.signatures.structural "node $($node.id).signatures.structural" @("activity", "normalized_affordances", "digest")
        Require-Properties $node.signatures.visual "node $($node.id).signatures.visual" @("algorithm", "perceptual_hash", "masked_regions")
        Require-Properties $node.signatures.semantic "node $($node.id).signatures.semantic" @("route", "overlay", "battle_phase", "flags", "digest")
        Test-IdList $node.evidence_ids '^EV-[0-9]{4}$' "node $($node.id).evidence_ids"
        Test-IdList $node.visual_evidence_ids '^EV-[0-9]{4}$' "node $($node.id).visual_evidence_ids"
        foreach ($evidenceId in (Get-Array $node.evidence_ids)) { Test-EvidenceRef ([string]$evidenceId) "node $($node.id)" $evidenceIds $evidenceById }
        foreach ($evidenceId in (Get-Array $node.visual_evidence_ids)) { Test-EvidenceRef ([string]$evidenceId) "node $($node.id) visual" $evidenceIds $evidenceById }
        if ([string]$node.visual_status -eq "usable" -and [string]::IsNullOrWhiteSpace([string]$node.signatures.visual.perceptual_hash)) {
            Add-Error "Usable node $($node.id) requires a visual perceptual hash"
        }
        foreach ($affordance in (Get-Array $node.visible_affordances)) {
            [void]$affordances.Add($affordance)
            Require-Properties $affordance "affordance $($affordance.id)" @("id", "role", "action", "bounds_dp", "bounds_unavailable_reason", "coverage_status")
            if ([string]$affordance.id -notmatch '^AF-[0-9]{4}$') { Add-Error "Invalid affordance id '$($affordance.id)'" }
            if (@("edge", "deviation", "blocker", "unmatched") -notcontains [string]$affordance.coverage_status) { Add-Error "Invalid affordance coverage at $($affordance.id)" }
            if ([string]$affordance.coverage_status -ne "unmatched" -and [string]::IsNullOrWhiteSpace([string]$affordance.coverage_ref)) {
                Add-Error "Affordance $($affordance.id) requires coverage_ref"
            }
        }
    }
    $affordanceIds = @($affordances | ForEach-Object { [string]$_.id })
    $duplicateAffordances = @($affordanceIds | Group-Object | Where-Object { $_.Count -gt 1 } | ForEach-Object { $_.Name })
    foreach ($duplicate in $duplicateAffordances) { Add-Error "Affordances contains duplicate id '$duplicate'" }

    foreach ($edge in $edges) {
        Require-Properties $edge "edge $($edge.id)" @(
            "id", "from", "to", "action", "preconditions", "cost_observation_ids", "observed_effect", "wait_ms",
            "before_evidence_ids", "after_evidence_ids", "classification", "source", "confidence"
        )
        if ([string]$edge.id -notmatch '^ED-[0-9]{4}$') { Add-Error "Invalid edge id '$($edge.id)'" }
        if ($nodeIds -notcontains [string]$edge.from) { Add-Error "Edge $($edge.id) references unknown from node $($edge.from)" }
        if ($null -ne $edge.to -and $nodeIds -notcontains [string]$edge.to) { Add-Error "Edge $($edge.id) references unknown to node $($edge.to)" }
        if (@("navigation", "phase", "meta", "service_adapter", "blocked") -notcontains [string]$edge.classification) { Add-Error "Invalid edge classification at $($edge.id)" }
        if (@("observed", "inferred") -notcontains [string]$edge.source) { Add-Error "Invalid edge source at $($edge.id)" }
        Test-Confidence $edge.confidence "edge $($edge.id)" | Out-Null
        Require-Properties $edge.wait_ms "edge $($edge.id).wait_ms" @("min", "max", "samples")
        if ([int]$edge.wait_ms.samples -gt 0 -and ($null -eq $edge.wait_ms.min -or $null -eq $edge.wait_ms.max -or [int]$edge.wait_ms.min -gt [int]$edge.wait_ms.max)) {
            Add-Error "Edge $($edge.id) has invalid wait_ms range"
        }
        foreach ($observationId in (Get-Array $edge.cost_observation_ids)) {
            if ($observationIds -notcontains [string]$observationId) { Add-Error "Edge $($edge.id) references unknown cost observation $observationId" }
        }
        foreach ($evidenceId in (Get-Array $edge.before_evidence_ids)) { Test-EvidenceRef ([string]$evidenceId) "edge $($edge.id) before" $evidenceIds $evidenceById }
        foreach ($evidenceId in (Get-Array $edge.after_evidence_ids)) { Test-EvidenceRef ([string]$evidenceId) "edge $($edge.id) after" $evidenceIds $evidenceById }
        if ([string]$edge.source -eq "observed" -and ((Get-Array $edge.before_evidence_ids).Count -eq 0 -or (Get-Array $edge.after_evidence_ids).Count -eq 0)) {
            Add-Error "Observed edge $($edge.id) requires before and after evidence"
        }
    }
    foreach ($affordance in $affordances) {
        if ([string]$affordance.coverage_status -eq "edge" -and $edgeIds -notcontains [string]$affordance.coverage_ref) {
            Add-Error "Affordance $($affordance.id) references unknown edge $($affordance.coverage_ref)"
        }
    }
    foreach ($observation in $observations) {
        Require-Properties $observation "observation $($observation.id)" @("id", "node_id", "metric", "value", "unit", "evidence_id", "captured_at_utc")
        if ([string]$observation.id -notmatch '^OB-[0-9]{4}$') { Add-Error "Invalid observation id '$($observation.id)'" }
        if ($nodeIds -notcontains [string]$observation.node_id) { Add-Error "Observation $($observation.id) references unknown node $($observation.node_id)" }
        Test-EvidenceRef ([string]$observation.evidence_id) "observation $($observation.id)" $evidenceIds $evidenceById
    }

    $claimIds = Test-UniqueIds -Values $script:bundle.claims -Context "mechanic claims" -IdProperty "claim_id"
    foreach ($claim in $script:bundle.claims) {
        if ([string]$claim.claim_id -notmatch '^CL-[0-9]{4}$') { Add-Error "Invalid mechanic claim id '$($claim.claim_id)'" }
        $confidence = Test-Confidence $claim.confidence "claim $($claim.claim_id)"
        $sampleCount = 0
        if (-not [int]::TryParse([string]$claim.sample_count, [ref]$sampleCount) -or $sampleCount -lt 0) { Add-Error "Claim $($claim.claim_id) has invalid sample_count" }
        if (@("candidate", "testing", "open_question", "supported", "contradicted", "accepted") -notcontains [string]$claim.status) { Add-Error "Claim $($claim.claim_id) has invalid status" }
        $supporting = @([string]$claim.supporting_evidence_ids -split ';' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
        $contradicting = @([string]$claim.contradicting_evidence_ids -split ';' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
        foreach ($evidenceId in $supporting + $contradicting) { Test-EvidenceRef $evidenceId "claim $($claim.claim_id)" $evidenceIds $evidenceById }
        if ($confidence -ge 0.8 -and ($sampleCount -lt 1 -or [string]::IsNullOrWhiteSpace([string]$claim.controlled_variables) -or $supporting.Count -eq 0)) {
            Add-Error "High-confidence claim $($claim.claim_id) requires controlled_variables, sample_count >= 1, and supporting evidence"
        }
    }
    return [pscustomobject][ordered]@{
        node_ids = @($nodeIds)
        edge_ids = @($edgeIds)
        observation_ids = @($observationIds)
        affordance_ids = @($affordanceIds)
        claim_ids = @($claimIds)
        evidence_ids = @($evidenceIds)
        affordances = @($affordances)
    }
}

function Get-HexHammingDistance {
    param([string]$Left, [string]$Right)
    if ([string]::IsNullOrWhiteSpace($Left) -or [string]::IsNullOrWhiteSpace($Right) -or $Left.Length -ne $Right.Length) { return $null }
    $distance = 0
    $bitCounts = @(0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4)
    for ($index = 0; $index -lt $Left.Length; $index++) {
        $a = [Convert]::ToInt32($Left[$index].ToString(), 16)
        $b = [Convert]::ToInt32($Right[$index].ToString(), 16)
        $nibble = $a -bxor $b
        $distance += $bitCounts[$nibble]
    }
    return $distance
}

function Test-Dedup {
    param([object]$Graph)
    $nodes = @(Get-Array $Graph.nodes)
    $candidates = [System.Collections.Generic.List[object]]::new()
    for ($left = 0; $left -lt $nodes.Count; $left++) {
        for ($right = $left + 1; $right -lt $nodes.Count; $right++) {
            $a = $nodes[$left]
            $b = $nodes[$right]
            $sameStructural = [string]$a.signatures.structural.digest -eq [string]$b.signatures.structural.digest -and
                [string]$a.signatures.semantic.digest -eq [string]$b.signatures.semantic.digest
            if (-not $sameStructural) { continue }
            $distance = Get-HexHammingDistance ([string]$a.signatures.visual.perceptual_hash) ([string]$b.signatures.visual.perceptual_hash)
            $threshold = 0
            if ($null -ne $a.signatures.visual.distance_threshold) { $threshold = [int]$a.signatures.visual.distance_threshold }
            if ($null -ne $b.signatures.visual.distance_threshold) { $threshold = [Math]::Max($threshold, [int]$b.signatures.visual.distance_threshold) }
            if ($null -eq $distance) {
                $candidates.Add([pscustomobject][ordered]@{ left = $a.id; right = $b.id; result = "visual_review_required"; distance = $null; threshold = $threshold })
            } elseif ($distance -le $threshold) {
                $candidates.Add([pscustomobject][ordered]@{ left = $a.id; right = $b.id; result = "duplicate_candidate"; distance = $distance; threshold = $threshold })
            }
        }
    }
    return @($candidates)
}

function Test-Gate1 {
    param([object]$Graph, [object]$Validation)
    if ($null -eq $Graph -or $null -eq $Validation) { return }
    if (@("ready", "accepted") -notcontains [string]$Graph.coverage.gate1_status) { Add-Error "Gate 1 status must be ready or accepted" }
    $missingRoots = @(Get-Array $Graph.coverage.root_routes_expected | Where-Object { (Get-Array $Graph.coverage.root_routes_seen) -notcontains [string]$_ })
    if ($missingRoots.Count -gt 0) { Add-Error "Gate 1 missing root routes: $($missingRoots -join ',')" }
    $terminalStates = @(Get-Array $Graph.coverage.terminal_states_seen)
    $terminalBlockers = @(Get-Array $Graph.coverage.terminal_state_blockers)
    foreach ($terminal in @("victory", "defeat")) {
        $observed = $terminalStates -contains $terminal
        $blocked = @($terminalBlockers | Where-Object { [string]$_.terminal -eq $terminal })
        if (-not $observed -and $blocked.Count -eq 0) { Add-Error "Gate 1 terminal '$terminal' requires an observed state or structured blocker" }
        foreach ($blocker in $blocked) {
            if ([string]$blocker.edge_id -notin $Validation.edge_ids) { Add-Error "Terminal blocker references unknown edge $($blocker.edge_id)" }
            if ([string]$blocker.reason_code -ne "safe_boundary_unreachable" -or [string]::IsNullOrWhiteSpace([string]$blocker.reason)) { Add-Error "Terminal blocker $($blocker.edge_id) needs a safe-boundary reason" }
        }
    }
    if (@("observed", "observed_zero_plus_structured_blocker") -notcontains [string]$Graph.coverage.negative_access_coverage.status) { Add-Error "Gate 1 requires negative access coverage" }
    if ([string]$Graph.coverage.negative_access_coverage.status -eq "observed_zero_plus_structured_blocker") {
        $negativeIds = Get-Array $Graph.coverage.negative_access_coverage.observation_ids
        $hasZero = @($Graph.observations | Where-Object { $negativeIds -contains [string]$_.id -and [string]$_.value -match '^0(?:\.0+)?$' }).Count -gt 0
        if (-not $hasZero -or (Get-Array $Graph.coverage.negative_access_coverage.blockers).Count -eq 0) { Add-Error "Zero-resource fallback needs an observed zero and blocker" }
    }
    $scopeDecisions = @(Get-Array $Graph.coverage.scope_decisions)
    $scopeIds = @($scopeDecisions | ForEach-Object { [string]$_.inventory_id })
    foreach ($decision in $scopeDecisions) { if ([string]$decision.human_lock -ne "accepted") { Add-Error "Scope $($decision.inventory_id) is not human-accepted" } }
    if ($scopeIds.Count -eq 0) { Add-Error "Gate 1 requires scope decisions" }
    if (([int]$Graph.coverage.plateau_iterations) -lt 6) { Add-Error "Gate 1 requires six plateau iterations" }
    if ((Get-Array $Graph.coverage.unmatched_affordance_ids).Count -gt 0) { Add-Error "Gate 1 has unmatched affordances" }
    foreach ($affordance in $Validation.affordances) { if ([string]$affordance.coverage_status -eq "unmatched") { Add-Error "Affordance $($affordance.id) is unmatched" } }
    $lowConfidence = @($script:bundle.claims | Where-Object {
        $value = 0.0
        [double]::TryParse([string]$_.confidence, [Globalization.NumberStyles]::Float, [Globalization.CultureInfo]::InvariantCulture, [ref]$value) -and $value -lt 0.8
    })
    foreach ($claim in $lowConfidence) {
        if ([string]$claim.status -ne "open_question") { Add-Error "Low-confidence claim $($claim.claim_id) must remain open_question" }
        if (-not [string]::IsNullOrWhiteSpace([string]$claim.future_fr_links) -or -not [string]::IsNullOrWhiteSpace([string]$claim.future_eng_links)) { Add-Error "Low-confidence claim $($claim.claim_id) cannot link to FR or ENG" }
    }
    if ([int]$Graph.coverage.low_confidence_open_questions -lt $lowConfidence.Count) { Add-Error "Coverage undercounts low-confidence open questions" }
    $openQuestions = $OpenQuestionsPath
    if ([string]::IsNullOrWhiteSpace($openQuestions)) { $openQuestions = Join-Path $EvidenceRoot "open-questions.md" }
    if (Test-Path -LiteralPath $openQuestions -PathType Leaf) {
        $text = Get-Content -Raw -LiteralPath $openQuestions
        if ([string]::IsNullOrWhiteSpace($text)) { Add-Error "Open questions file is empty" }
    } else {
        Add-Warning "Open questions file was not supplied; claim status is used as the machine-readable fallback"
    }
}

function Find-ExistingCard {
    param([string]$Id)
    if ([string]::IsNullOrWhiteSpace($Id)) { return @() }
    $foundPaths = [System.Collections.Generic.List[string]]::new()
    foreach ($folder in @("backlog", "active", "done")) {
        $path = Join-Path $RepoRoot ".claude\specs\$folder"
        if (Test-Path -LiteralPath $path -PathType Container) {
            foreach ($file in @(Get-ChildItem -LiteralPath $path -Filter "*.md" -File)) {
                if ((Get-Content -Raw -LiteralPath $file.FullName) -match "(?m)^id:\s*$([regex]::Escape($Id))\s*$|(?m)^#\s*$([regex]::Escape($Id))\b") { [void]$foundPaths.Add($file.FullName) }
            }
        }
    }
    return @($foundPaths)
}

function Test-Bridge {
    param([object]$Gate1Report)
    if ($null -eq $Gate1Report -or $Gate1Report.verdict -ne "pass") { return $null }
    $existing = Find-ExistingCard $EngineGapId
    $roadmap = Join-Path $RepoRoot ".claude\specs\ENGINE_ROADMAP.md"
    $apiStability = Join-Path $RepoRoot "docs\API_STABILITY.md"
    $roadmapText = if (Test-Path -LiteralPath $roadmap -PathType Leaf) { Get-Content -Raw -LiteralPath $roadmap } else { "" }
    $roadmapHasId = -not [string]::IsNullOrWhiteSpace($EngineGapId) -and $roadmapText -match [regex]::Escape($EngineGapId)
    if ($existing.Count -gt 0) {
        if ($Apply) { Add-Warning "Apply is report-only for existing cards; no backlog file was changed" }
        return [ordered]@{
            action = "reference_existing_card"
            duplicate_card = $true
            card_paths = @($existing)
            roadmap_contains_id = $roadmapHasId
            api_stability_scanned = Test-Path -LiteralPath $apiStability -PathType Leaf
            demand_tag = $DemandTag
            writes = @()
        }
    }
    return [ordered]@{
        action = "new_card_requires_human_gate"
        duplicate_card = $false
        card_paths = @()
        roadmap_contains_id = $roadmapHasId
        api_stability_scanned = Test-Path -LiteralPath $apiStability -PathType Leaf
        demand_tag = $DemandTag
        writes = @()
    }
}

$bundle = Read-EvidenceBundle
$script:bundle = $bundle
Test-PublicSafety
$validation = $null
if ($null -ne $bundle) { $validation = Test-Graph -Graph $bundle.graph -Evidence $bundle.evidence }
$validationErrorsBeforeGate = @($errors)
if ($Mode -ne "validate" -and $null -ne $bundle -and $null -ne $validation -and $validationErrorsBeforeGate.Count -eq 0) {
    Test-Gate1 -Graph $bundle.graph -Validation $validation
}

$dedup = if ($null -ne $bundle -and $null -ne $bundle.graph -and $errors.Count -eq 0) { @(Test-Dedup -Graph $bundle.graph) } else { @() }
foreach ($candidate in $dedup | Where-Object { $_.result -eq "duplicate_candidate" }) {
    Add-Error "Duplicate state candidate: $($candidate.left) and $($candidate.right)"
}

$bridge = $null
if ($Mode -eq "bridge" -and $errors.Count -eq 0) {
    $gateReport = [pscustomobject]@{ verdict = "pass" }
    $bridge = Test-Bridge -Gate1Report $gateReport
}

$needsHumanBridge = $Mode -eq "bridge" -and $null -ne $bridge -and -not $bridge.duplicate_card
$verdict = if ($errors.Count -gt 0) { "fail" } elseif ($needsHumanBridge) { "needs_human" } else { "pass" }
$result = [ordered]@{
    agent = "me-reference-evidence"
    mode = $Mode
    verdict = $verdict
    summary = if ($verdict -eq "pass") { "Sanitized reference evidence passed the requested evidence bridge checks." } elseif ($verdict -eq "needs_human") { "A new reusable gap needs human approval before a backlog card may be created." } else { "Reference evidence bridge checks failed." }
    evidence_root = $EvidenceRoot
    counts = if ($null -ne $bundle -and $null -ne $bundle.graph) {
        [ordered]@{
            nodes = @($bundle.graph.nodes).Count
            edges = @($bundle.graph.edges).Count
            observations = @($bundle.graph.observations).Count
            claims = @($bundle.claims).Count
            evidence = @($bundle.evidence).Count
        }
    } else { $null }
    checks = [ordered]@{
        schema_import = ($validationErrorsBeforeGate.Count -eq 0)
        clone_strict_gate1 = ($Mode -eq "validate" -or ($errors.Count -eq 0))
        game_aware_dedup = ($dedup | Where-Object { $_.result -eq "duplicate_candidate" }).Count -eq 0
        public_safety = ($errors | Where-Object { $_ -match "raw artifact|absolute path|inside .reference-local" }).Count -eq 0
        volatile_observations_separate = if ($null -ne $bundle -and $null -ne $bundle.graph) {
            (Has-Property $bundle.graph "observations") -and @($bundle.graph.nodes | Where-Object { Has-Property $_ "observations" }).Count -eq 0
        } else { $false }
        traceability = ($validationErrorsBeforeGate.Count -eq 0)
        gap_dedup = ($null -ne $bridge -and $bridge.duplicate_card) -or $Mode -ne "bridge"
    }
    dedup = @($dedup)
    bridge = $bridge
    errors = @($errors)
    warnings = @($warnings)
}
$result | ConvertTo-Json -Compress -Depth 12
if ($verdict -eq "pass") { exit 0 } else { exit 1 }
