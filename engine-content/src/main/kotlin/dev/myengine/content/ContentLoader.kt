package dev.myengine.content

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import java.nio.file.Files
import java.nio.file.Path
import java.math.BigDecimal
import dev.myengine.core.GameplayEventType
import dev.myengine.core.command.TargetingMode
import java.util.ArrayDeque
import java.util.Properties

data class ContentValidationError(
    val file: String,
    val id: String,
    val field: String,
    val message: String,
) {
    override fun toString(): String = "$file:$id:$field: $message"
}

data class ContentLoadResult(
    val registry: ContentRegistry?,
    val errors: List<ContentValidationError>,
) {
    val isValid: Boolean = errors.isEmpty() && registry != null
}

object ContentPackLoader {
    const val SUPPORTED_SCHEMA_VERSION: Int = 1

    fun load(root: Path): ContentLoadResult {
        val errors = mutableListOf<ContentValidationError>()
        val manifestProps = readProperties(root.resolve("manifest.properties"), errors)
        val manifest = manifestProps?.let { parseManifest(it, errors) }

        val tiles = parseDefinitions(root, "tiles.properties", errors, ::parseTile)
        val resources = parseDefinitions(root, "resources.properties", errors, ::parseResource)
        val damageTypesDeclared = Files.exists(root.resolve("damage-types.properties"))
        val damageTypes = parseOptionalDefinitions(root, "damage-types.properties", errors, ::parseDamageType)
        val towers = parseDefinitions(root, "towers.properties", errors, ::parseTower)
        val enemies = parseDefinitions(root, "enemies.properties", errors, ::parseEnemy)
        val workers = parseOptionalDefinitions(root, "workers.properties", errors, ::parseWorker)
        val needs = parseOptionalDefinitions(root, "needs.properties", errors, ::parseNeed)
        val buildings = parseOptionalDefinitions(root, "buildings.properties", errors, ::parseBuilding)
        val recipes = parseDefinitions(root, "recipes.properties", errors, ::parseRecipe)
        val waves = parseDefinitions(root, "waves.properties", errors, ::parseWave)
        val endlessWave = parseEndlessWave(root, errors)
        val incidents = parseOptionalDefinitions(root, "incidents.properties", errors, ::parseIncident)
        val difficulties = parseOptionalDefinitions(root, "difficulties.properties", errors, ::parseDifficulty)
        val effects = parseOptionalDefinitions(root, "effects.properties", errors, ::parseStatusEffect)
        val sounds = parseSounds(root, manifest?.id ?: "unknown-pack", errors)
        val strings = readProperties(root.resolve("strings.properties"), errors)?.entries
            ?.associate { it.key.toString() to it.value.toString() }
            ?: emptyMap()
        val maps = parseMaps(root, tiles, resources, errors)
        val techNodes = parseTechTree(root, errors)

        if (manifest != null && manifest.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            errors += ContentValidationError("manifest.properties", manifest.id, "schemaVersion", "Unsupported schema version ${manifest.schemaVersion}.")
        }

        validateReferences(
            root = root,
            packId = manifest?.id ?: "unknown-pack",
            tiles = tiles,
            damageTypes = damageTypes,
            damageTypesDeclared = damageTypesDeclared,
            towers = towers,
            enemies = enemies,
            buildings = buildings,
            recipes = recipes,
            waves = waves,
            incidents = incidents,
            resources = resources,
            effects = effects,
            sounds = sounds,
            maps = maps,
            endlessWave = endlessWave,
            errors = errors,
        )
        validateTerminalRules(maps, waves, endlessWave, errors)
        validateLocalization(damageTypes, resources, towers, buildings, strings, errors)
        validateTechTree(techNodes, resources, towers, buildings, recipes, errors)

        if (errors.isNotEmpty() || manifest == null) {
            return ContentLoadResult(null, errors)
        }

        return ContentLoadResult(
            registry = ContentRegistry(
                manifest = manifest,
                tiles = tiles,
                resources = resources,
                towers = towers,
                enemies = enemies,
                buildings = buildings,
                recipes = recipes,
                waves = waves,
                incidents = incidents,
                strings = strings,
                difficulties = difficulties,
                maps = maps,
                effects = effects,
                sounds = sounds,
                endlessWave = endlessWave,
                damageTypes = damageTypes,
                workers = workers,
                needs = needs,
                techNodes = techNodes,
            ),
            errors = emptyList(),
        )
    }

    private fun parseManifest(props: Properties, errors: MutableList<ContentValidationError>): ContentPackManifest? {
        fun required(field: String): String? = props.getProperty(field)
            ?: run {
                errors += ContentValidationError("manifest.properties", "pack", field, "Required field is missing.")
                null
            }
        val id = required("id") ?: return null
        return ContentPackManifest(
            id = id,
            version = required("version") ?: return null,
            schemaVersion = required("schemaVersion")?.toIntOrNull()
                ?: return errors.addAndNull("manifest.properties", id, "schemaVersion", "Expected integer."),
            engineMin = required("engineMin") ?: return null,
            engineMax = required("engineMax") ?: return null,
            locales = props.getProperty("locales", "en").splitCsv(),
            dependencies = props.getProperty("dependencies", "").splitCsv(),
        )
    }

    private fun <T : ContentDefinition> parseDefinitions(
        root: Path,
        fileName: String,
        errors: MutableList<ContentValidationError>,
        parser: (String, Map<String, String>, MutableList<ContentValidationError>, String) -> T?,
    ): Map<String, T> {
        val props = readProperties(root.resolve(fileName), errors) ?: return emptyMap()
        val byId = props.entries
            .map { it.key.toString() to it.value.toString() }
            .groupBy(
                keySelector = { it.first.substringBefore('.') },
                valueTransform = { it.first.substringAfter('.', "") to it.second },
            )
        val parsed = linkedMapOf<String, T>()
        byId.toSortedMap().forEach { (id, pairs) ->
            if (id.isBlank()) {
                errors += ContentValidationError(fileName, id, "id", "Definition id is blank.")
                return@forEach
            }
            val fields = pairs.toMap()
            val definition = parser(id, fields, errors, fileName) ?: return@forEach
            if (parsed.containsKey(id)) {
                errors += ContentValidationError(fileName, id, "id", "Duplicate definition id.")
            } else {
                parsed[id] = definition
            }
        }
        return parsed
    }

    private fun <T : ContentDefinition> parseOptionalDefinitions(
        root: Path,
        fileName: String,
        errors: MutableList<ContentValidationError>,
        parser: (String, Map<String, String>, MutableList<ContentValidationError>, String) -> T?,
    ): Map<String, T> =
        if (Files.exists(root.resolve(fileName))) {
            parseDefinitions(root, fileName, errors, parser)
        } else {
            emptyMap()
        }

    private fun parseTile(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): TileContent? =
        TileContent(
            id = id,
            buildable = fields.requiredBool(file, id, "buildable", errors) ?: return null,
            blocksMovement = fields.requiredBool(file, id, "blocksMovement", errors) ?: return null,
            isCore = fields["isCore"]?.toBooleanStrictOrNull() ?: false,
            assetRef = parseVisualAssetRef(id, fields, errors, file),
        )

    private fun parseResource(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): ResourceContent? =
        ResourceContent(
            id = id,
            displayKey = fields.required(file, id, "displayKey", errors) ?: return null,
        )

    private fun parseDamageType(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): DamageTypeContent? =
        DamageTypeContent(
            id = id,
            displayKey = fields.required(file, id, "displayKey", errors) ?: return null,
        )

    private fun parseTower(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): TowerContent? =
        TowerContent(
            id = id,
            displayKey = fields.required(file, id, "displayKey", errors) ?: return null,
            range = fields.requiredPositiveInt(file, id, "range", errors) ?: return null,
            damage = fields.requiredPositiveInt(file, id, "damage", errors) ?: return null,
            cooldownTicks = fields.requiredPositiveInt(file, id, "cooldownTicks", errors) ?: return null,
            costResource = fields.required(file, id, "costResource", errors) ?: return null,
            costAmount = fields.requiredNonNegativeInt(file, id, "costAmount", errors) ?: return null,
            sellRefundRatio = fields.requiredDecimalInRange(file, id, "sellRefundRatio", BigDecimal.ZERO, BigDecimal.ONE, errors)
                ?: return null,
            splashRadius = fields.optionalPositiveInt(file, id, "splashRadius", errors),
            falloffPercent = fields.optionalFalloffPercent(file, id, errors) ?: return null,
            upgradeTiers = parseTowerUpgradeTiers(id, fields, errors, file),
            targetingMode = fields.targetingModeOrDefault(file, id, errors) ?: return null,
            assetRef = parseVisualAssetRef(id, fields, errors, file),
            effectId = fields["effectId"]?.let { value ->
                if (value.isBlank()) {
                    errors += ContentValidationError(file, id, "effectId", "Expected a non-blank status effect id.")
                    null
                } else value
            },
            damageTypeId = fields.optionalNonBlank(file, id, "damageTypeId", errors),
            maxHealth = fields.optionalPositiveInt(file, id, "maxHealth", errors) ?: 10,
        )

    private fun parseTowerUpgradeTiers(
        id: String,
        fields: Map<String, String>,
        errors: MutableList<ContentValidationError>,
        file: String,
    ): Map<String, TowerUpgradeTier> {
        val groups = linkedMapOf<Pair<String, Int>, MutableMap<String, String>>()
        fields.filterKeys { it.startsWith("upgrade.") }.forEach { (field, value) ->
            val parts = field.split('.')
            if (parts.size != 4) {
                errors += ContentValidationError(file, id, field, "Expected upgrade.<branch>.<tier>.<field>.")
                return@forEach
            }
            val branch = parts[1]
            val tier = parts[2].toIntOrNull()
            val upgradeField = parts[3]
            if (!branch.matches(TowerUpgradeTier.BRANCH_ID_REGEX)) {
                errors += ContentValidationError(file, id, field, "Upgrade branch must match ${TowerUpgradeTier.BRANCH_ID_REGEX.pattern}.")
                return@forEach
            }
            if (tier == null || tier <= 0) {
                errors += ContentValidationError(file, id, field, "Upgrade tier must be a positive integer.")
                return@forEach
            }
            groups.getOrPut(branch to tier) { linkedMapOf() }[upgradeField] = value
        }

        return groups.entries
            .sortedWith(compareBy<Map.Entry<Pair<String, Int>, MutableMap<String, String>>> { it.key.first }.thenBy { it.key.second })
            .mapNotNull { (key, upgradeFields) ->
                val (branch, tier) = key
                val prefix = "upgrade.$branch.$tier"
                val scopedFields = upgradeFields.mapKeys { "$prefix.${it.key}" }
                TowerUpgradeTier(
                    branch = branch,
                    tier = tier,
                    displayKey = scopedFields.required(file, id, "$prefix.displayKey", errors) ?: return@mapNotNull null,
                    range = scopedFields.requiredPositiveInt(file, id, "$prefix.range", errors) ?: return@mapNotNull null,
                    damage = scopedFields.requiredPositiveInt(file, id, "$prefix.damage", errors) ?: return@mapNotNull null,
                    cooldownTicks = scopedFields.requiredPositiveInt(file, id, "$prefix.cooldownTicks", errors) ?: return@mapNotNull null,
                    costResource = scopedFields.required(file, id, "$prefix.costResource", errors) ?: return@mapNotNull null,
                    costAmount = scopedFields.requiredNonNegativeInt(file, id, "$prefix.costAmount", errors) ?: return@mapNotNull null,
                    assetRef = parseVisualAssetRef(id, scopedFields, errors, file, "$prefix."),
                )
            }
            .associateBy { it.key }
    }

    private fun parseEnemy(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): EnemyContent? {
        val isElite = fields.optionalBool(file, id, "isElite", errors) ?: false
        val isBoss = fields.optionalBool(file, id, "isBoss", errors) ?: false
        if (isElite && isBoss) {
            errors += ContentValidationError(file, id, "isBoss", "An enemy cannot be both elite and boss.")
        }
        return EnemyContent(
            id = id,
            health = fields.requiredPositiveInt(file, id, "health", errors) ?: return null,
            speedTilesPerTick = fields.requiredPositiveInt(file, id, "speedTilesPerTick", errors) ?: return null,
            rewardResource = fields.required(file, id, "rewardResource", errors) ?: return null,
            rewardAmount = fields.requiredNonNegativeInt(file, id, "rewardAmount", errors) ?: return null,
            coreDamage = fields.requiredPositiveInt(file, id, "coreDamage", errors) ?: return null,
            attacksStructures = fields.optionalBool(file, id, "attacksStructures", errors) ?: false,
            assetRef = parseVisualAssetRef(id, fields, errors, file),
            isElite = isElite,
            isBoss = isBoss && !isElite,
            healthScalePercent = fields.optionalPositivePercent(file, id, "healthScalePercent", errors) ?: 100,
            speedScalePercent = fields.optionalPositivePercent(file, id, "speedScalePercent", errors) ?: 100,
            rewardScalePercent = fields.optionalPositivePercent(file, id, "rewardScalePercent", errors) ?: 100,
            resists = parseEnemyResists(id, fields, errors, file),
        )
    }

    private fun parseWorker(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): WorkerContent? =
        WorkerContent(
            id = id,
            speedTilesPerTick = fields.requiredPositiveInt(file, id, "speedTilesPerTick", errors) ?: return null,
            capacity = fields.requiredPositiveInt(file, id, "capacity", errors) ?: return null,
        )

    private fun parseNeed(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): NeedContent? {
        val threshold = fields.requiredNonNegativeInt(file, id, "threshold", errors) ?: return null
        if (threshold !in 0..100) {
            errors += ContentValidationError(file, id, "threshold", "Expected an integer from 0 to 100.")
            return null
        }
        return NeedContent(
            id = id,
            decayPerTick = fields.requiredPositiveInt(file, id, "decayPerTick", errors) ?: return null,
            threshold = threshold,
            recoveryAmount = fields.requiredPositiveInt(file, id, "recoveryAmount", errors) ?: return null,
            jobType = fields.required(file, id, "jobType", errors) ?: return null,
            priority = fields.requiredNonNegativeInt(file, id, "priority", errors) ?: return null,
            displayKey = fields.optionalNonBlank(file, id, "displayKey", errors) ?: "need.$id",
        )
    }

    private fun parseTechTree(root: Path, errors: MutableList<ContentValidationError>): Map<String, TechNodeContent> {
        val file = "tech-tree.json"
        val path = root.resolve(file)
        if (!Files.exists(path)) return emptyMap()
        val document = try {
            Files.newBufferedReader(path).use { reader -> Json.parseToJsonElement(reader.readText()) as? JsonObject }
        } catch (error: Exception) {
            errors += ContentValidationError(file, "file", "json", "Invalid JSON: ${error.message ?: error::class.simpleName}.")
            return emptyMap()
        }
        if (document == null) {
            errors += ContentValidationError(file, "file", "json", "Expected a top-level JSON object.")
            return emptyMap()
        }
        val definitions = document["nodes"] as? JsonArray
        if (definitions == null) {
            errors += ContentValidationError(file, "file", "nodes", "Expected a 'nodes' array.")
            return emptyMap()
        }
        val parsed = linkedMapOf<String, TechNodeContent>()
        definitions.forEachIndexed { index, value ->
            val objectValue = value as? JsonObject
            if (objectValue == null) {
                errors += ContentValidationError(file, "nodes[$index]", "entry", "Expected a tech node object.")
                return@forEachIndexed
            }
            val fallbackId = "nodes[$index]"
            val id = objectValue.requiredString("id", file, fallbackId, "id", errors) ?: return@forEachIndexed
            val cost = objectValue["cost"] as? JsonObject
            if (cost == null) {
                errors += ContentValidationError(file, id, "cost", "Expected an object with resource and amount.")
                return@forEachIndexed
            }
            val costResource = cost.requiredString("resource", file, id, "cost.resource", errors) ?: return@forEachIndexed
            val costAmount = cost.requiredInt("amount", file, id, "cost.amount", errors)
            if (costAmount == null || costAmount <= 0) {
                if (costAmount != null) errors += ContentValidationError(file, id, "cost.amount", "Expected a positive integer.")
                return@forEachIndexed
            }
            val prerequisites = objectValue.optionalStringArray("prerequisites", file, id, errors)
            val unlocks = parseTechUnlocks(objectValue["unlocks"], file, id, errors)
            val node = TechNodeContent(id, costResource, costAmount, prerequisites, unlocks)
            if (parsed.containsKey(id)) {
                errors += ContentValidationError(file, id, "id", "Duplicate tech node id.")
            } else {
                parsed[id] = node
            }
        }
        return parsed.toSortedMap()
    }

    private fun parseTechUnlocks(
        value: kotlinx.serialization.json.JsonElement?,
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): List<TechUnlockRef> {
        if (value == null) return emptyList()
        val array = value as? JsonArray
        if (array == null) {
            errors += ContentValidationError(file, id, "unlocks", "Expected an array of typed unlock references.")
            return emptyList()
        }
        return array.mapIndexedNotNull { index, item ->
            val objectValue = item as? JsonObject
            if (objectValue == null) {
                errors += ContentValidationError(file, id, "unlocks[$index]", "Expected an unlock object.")
                return@mapIndexedNotNull null
            }
            val typeId = objectValue.requiredString("type", file, id, "unlocks[$index].type", errors)
                ?: return@mapIndexedNotNull null
            val type = TechUnlockType.fromId(typeId)
            if (type == null) {
                errors += ContentValidationError(file, id, "unlocks[$index].type", "Expected tower, building, or recipe.")
                return@mapIndexedNotNull null
            }
            val targetId = objectValue.requiredString("id", file, id, "unlocks[$index].id", errors)
                ?: return@mapIndexedNotNull null
            TechUnlockRef(type, targetId)
        }
    }

    private fun parseEnemyResists(
        id: String,
        fields: Map<String, String>,
        errors: MutableList<ContentValidationError>,
        file: String,
    ): Map<String, Int> = fields.keys
        .filter { it.startsWith("resist.") }
        .sorted()
        .mapNotNull { field ->
            val damageTypeId = field.removePrefix("resist.")
            if (damageTypeId.isBlank()) {
                errors += ContentValidationError(file, id, field, "Resistance damage type id must be non-blank.")
                return@mapNotNull null
            }
            val value = fields[field]?.toIntOrNull()
            if (value == null || value !in 0..100) {
                errors += ContentValidationError(file, id, field, "Expected an integer resistance from 0 to 100.")
                return@mapNotNull null
            }
            damageTypeId to value
        }
        .toMap()

    private fun parseStatusEffect(
        id: String,
        fields: Map<String, String>,
        errors: MutableList<ContentValidationError>,
        file: String,
    ): StatusEffectContent? {
        val kind = when {
            fields.containsKey("type") -> StatusEffectKind.fromId(fields["type"].orEmpty())
            fields.containsKey("kind") -> StatusEffectKind.fromId(fields["kind"].orEmpty())
            id.equals("slow", ignoreCase = true) -> StatusEffectKind.SLOW
            id.equals("dot", ignoreCase = true) -> StatusEffectKind.DOT
            else -> null
        }
        if (kind == null) {
            val field = if (fields.containsKey("type")) "type" else "kind"
            errors += ContentValidationError(file, id, field, "Expected one of slow or dot.")
            return null
        }
        val magnitude = if (kind == StatusEffectKind.SLOW) {
            fields.requiredNonNegativeInt(file, id, "magnitude", errors)?.takeIf { value ->
                if (value > 100) {
                    errors += ContentValidationError(file, id, "magnitude", "Slow magnitude must be between 0 and 100 percent.")
                    false
                } else true
            }
        } else {
            fields.requiredPositiveInt(file, id, "magnitude", errors)
        } ?: return null
        val durationTicks = fields.requiredPositiveInt(file, id, "durationTicks", errors) ?: return null
        val stackingRule = fields.required(file, id, "stackingRule", errors)?.let { raw ->
            StatusEffectStackingRule.fromId(raw) ?: run {
                errors += ContentValidationError(file, id, "stackingRule", "Expected one of refresh, stack, or ignore.")
                null
            }
        } ?: return null
        return StatusEffectContent(id, kind, magnitude, durationTicks, stackingRule)
    }

    private fun parseBuilding(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): BuildingContent? {
        val width = fields.requiredPositiveInt(file, id, "footprintWidth", errors) ?: return null
        val height = fields.requiredPositiveInt(file, id, "footprintHeight", errors) ?: return null
        if (width != 1 || height != 1) {
            errors += ContentValidationError(file, id, "footprint", "Only 1x1 wall footprints are supported.")
            return null
        }
        return BuildingContent(
            id = id,
            costResource = fields.required(file, id, "costResource", errors) ?: return null,
            costAmount = fields.requiredPositiveInt(file, id, "costAmount", errors) ?: return null,
            maxHealth = fields.requiredPositiveInt(file, id, "maxHealth", errors) ?: return null,
            footprintWidth = width,
            footprintHeight = height,
            sellRefundRatio = fields.requiredDecimalInRange(file, id, "sellRefundRatio", BigDecimal.ZERO, BigDecimal.ONE, errors)
                ?: return null,
            displayKey = fields.required(file, id, "displayKey", errors) ?: return null,
            assetRef = parseVisualAssetRef(id, fields, errors, file),
            buildWorkTicks = fields.optionalPositiveInt(file, id, "buildWorkTicks", errors) ?: 1,
            producerRecipeId = fields.optionalNonBlank(file, id, "producerRecipeId", errors),
            beltGeometry = fields.optionalNonBlank(file, id, "beltGeometry", errors)?.let { value ->
                BeltGeometryContent.fromId(value) ?: run {
                    errors += ContentValidationError(file, id, "beltGeometry", "Expected straight or corner.")
                    null
                }
            },
            beltDirection = fields.optionalNonBlank(file, id, "beltDirection", errors)?.let { value ->
                BeltDirectionContent.fromId(value) ?: run {
                    errors += ContentValidationError(file, id, "beltDirection", "Expected north, east, south, or west.")
                    null
                }
            },
            beltTicksPerCell = fields.optionalPositiveInt(file, id, "beltTicksPerCell", errors),
        )
    }

    /** Parses one optional sprite path or an atlas path/key pair under [fieldPrefix]. */
    private fun parseVisualAssetRef(
        id: String,
        fields: Map<String, String>,
        errors: MutableList<ContentValidationError>,
        file: String,
        fieldPrefix: String = "",
    ): VisualAssetRef? {
        val spritePathField = "${fieldPrefix}spritePath"
        val atlasPathField = "${fieldPrefix}atlasPath"
        val atlasKeyField = "${fieldPrefix}atlasKey"
        val hasSprite = fields.containsKey(spritePathField)
        val hasAtlasPath = fields.containsKey(atlasPathField)
        val hasAtlasKey = fields.containsKey(atlasKeyField)
        if (!hasSprite && !hasAtlasPath && !hasAtlasKey) return null
        if (hasSprite && (hasAtlasPath || hasAtlasKey)) {
            errors += ContentValidationError(
                file,
                id,
                "${fieldPrefix}visual",
                "Declare either ${spritePathField} or the ${atlasPathField}/${atlasKeyField} pair, not both.",
            )
            return null
        }
        if (hasAtlasPath != hasAtlasKey) {
            if (!hasAtlasPath) errors += ContentValidationError(file, id, atlasPathField, "Atlas path/key fields must be declared as a pair.")
            if (!hasAtlasKey) errors += ContentValidationError(file, id, atlasKeyField, "Atlas path/key fields must be declared as a pair.")
            return null
        }
        return if (hasSprite) {
            VisualAssetRef(fields.required(file, id, spritePathField, errors) ?: return null)
        } else {
            VisualAssetRef(
                path = fields.required(file, id, atlasPathField, errors) ?: return null,
                atlasKey = fields.required(file, id, atlasKeyField, errors) ?: return null,
            )
        }
    }

    private fun parseRecipe(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): RecipeContent? =
        RecipeContent(
            id = id,
            inputResource = fields["inputResource"]?.takeIf { it.isNotBlank() },
            inputAmount = fields["inputAmount"]?.toIntOrNull() ?: 0,
            outputResource = fields.required(file, id, "outputResource", errors) ?: return null,
            outputAmount = fields.requiredPositiveInt(file, id, "outputAmount", errors) ?: return null,
            durationTicks = fields.requiredPositiveInt(file, id, "durationTicks", errors) ?: return null,
        )

    private fun parseWave(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): WaveContent? {
        val spawns = fields.required(file, id, "spawns", errors)
            ?.splitCsv()
            ?.mapNotNull { token ->
                val parts = token.split(":")
                if (parts.size != 2 || parts[1].toIntOrNull() == null) {
                    errors += ContentValidationError(file, id, "spawns", "Expected enemyId:count list.")
                    null
                } else {
                    WaveSpawn(parts[0], parts[1].toInt())
                }
            }
            ?: return null
        val spawnSelection = parseWaveSpawnSelection(id, fields, errors, file)
        if (!spawnSelection.valid) return null
        return WaveContent(
            id = id,
            startTick = fields.requiredNonNegativeLong(file, id, "startTick", errors) ?: return null,
            spawns = spawns,
            earlyCallBonus = parseWaveEarlyCallBonus(id, fields, errors, file),
            modifiers = parseWaveModifiers(id, fields, errors, file),
            spawnSelection = spawnSelection.ids,
        )
    }

    private fun parseEndlessWave(root: Path, errors: MutableList<ContentValidationError>): EndlessWaveContent? {
        val path = root.resolve("endless.properties")
        if (!Files.exists(path)) return null
        val props = readProperties(path, errors) ?: return null
        val file = path.fileName.toString()
        val fields = props.entries.associate { it.key.toString() to it.value.toString() }
        val spawnSelection = parseWaveSpawnSelection("endless", fields, errors, file)
        if (!spawnSelection.valid) return null
        val compositions = parseEndlessCompositions(fields, errors, file) ?: return null
        val startTick = fields.requiredNonNegativeLong(file, "endless", "startTick", errors) ?: return null
        val intervalTicks = fields.requiredPositiveLong(file, "endless", "intervalTicks", errors) ?: return null
        val countGrowth = fields.requiredPositivePercent(file, "endless", "countGrowthPercent", errors) ?: return null
        val healthGrowth = fields.requiredPositivePercent(file, "endless", "healthGrowthPercent", errors) ?: return null
        val rewardGrowth = fields.requiredPositivePercent(file, "endless", "rewardGrowthPercent", errors) ?: return null
        return EndlessWaveContent(
            startTick = startTick,
            intervalTicks = intervalTicks,
            compositionCycle = compositions,
            countGrowthPercent = countGrowth,
            healthGrowthPercent = healthGrowth,
            rewardGrowthPercent = rewardGrowth,
            spawnSelection = spawnSelection.ids,
        )
    }

    private fun parseEndlessCompositions(
        fields: Map<String, String>,
        errors: MutableList<ContentValidationError>,
        file: String,
    ): List<EndlessWaveComposition>? {
        val rawCycle = fields["compositionCycle"]?.takeIf { it.isNotBlank() }
        val values = if (rawCycle != null) {
            rawCycle.split(';').map(String::trim).filter(String::isNotBlank)
        } else {
            val cycleFields = fields.keys.filter { it.startsWith("cycle.") }
            val indexes = cycleFields.mapNotNull { it.substringAfter("cycle.").toIntOrNull() }.distinct().sorted()
            if (indexes.isEmpty()) {
                errors += ContentValidationError(file, "endless", "compositionCycle", "Required field is missing.")
                return null
            }
            if (indexes != (0 until indexes.size).toList()) {
                errors += ContentValidationError(file, "endless", "cycle", "Cycle indexes must be contiguous from 0.")
                return null
            }
            indexes.map { index -> fields["cycle.$index"].orEmpty() }
        }
        if (values.isEmpty()) {
            errors += ContentValidationError(file, "endless", "compositionCycle", "Composition cycle cannot be empty.")
            return null
        }
        val parsed = values.mapIndexedNotNull { index, value ->
            val tokens = value.split('|', ',').map(String::trim).filter(String::isNotBlank)
            val spawns = tokens.mapNotNull { token ->
                val parts = token.split(":")
                val count = parts.getOrNull(1)?.toIntOrNull()
                if (parts.size != 2 || parts[0].isBlank() || count == null || count <= 0) {
                    errors += ContentValidationError(file, "endless", "compositionCycle[$index]", "Expected enemyId:positiveCount entries.")
                    null
                } else {
                    WaveSpawn(parts[0], count)
                }
            }
            if (spawns.isEmpty()) null else EndlessWaveComposition(spawns)
        }
        return parsed.takeIf { it.size == values.size }
    }

    private data class ParsedWaveSpawnSelection(
        val ids: List<String>?,
        val valid: Boolean,
    )

    /** Parses the optional `all` or pipe-delimited named spawn list. */
    private fun parseWaveSpawnSelection(
        id: String,
        fields: Map<String, String>,
        errors: MutableList<ContentValidationError>,
        file: String,
    ): ParsedWaveSpawnSelection {
        val raw = fields["spawnSelection"] ?: return ParsedWaveSpawnSelection(null, true)
        val value = raw.trim()
        if (value == "all") return ParsedWaveSpawnSelection(null, true)

        val ids = value.split("|", ignoreCase = false, limit = Int.MAX_VALUE).map(String::trim)
        if (ids.any(String::isBlank)) {
            errors += ContentValidationError(file, id, "spawnSelection", "Spawn selection ids cannot be blank.")
            return ParsedWaveSpawnSelection(null, false)
        }
        if (ids.any { it == "all" }) {
            errors += ContentValidationError(file, id, "spawnSelection", "Use either 'all' or a named spawn id list, not both.")
            return ParsedWaveSpawnSelection(null, false)
        }
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) {
            errors += ContentValidationError(
                file,
                id,
                "spawnSelection",
                "Spawn selection contains duplicate ids: ${duplicates.sorted().joinToString(", ")}.",
            )
            return ParsedWaveSpawnSelection(null, false)
        }
        return ParsedWaveSpawnSelection(ids, true)
    }

    private fun parseWaveModifiers(
        id: String,
        fields: Map<String, String>,
        errors: MutableList<ContentValidationError>,
        file: String,
    ): List<WaveModifier> {
        val modifierFields = fields.keys.filter { it.startsWith("modifier.") }
        modifierFields.forEach { field ->
            val parts = field.split('.')
            if (parts.size != 3 || parts[1].toIntOrNull() == null || parts[2] !in setOf("healthPercent", "speedPercent", "count")) {
                errors += ContentValidationError(file, id, field, "Expected modifier.<index>.healthPercent, speedPercent, or count.")
            }
        }
        val indexes = modifierFields
            .mapNotNull { key -> key.split('.').getOrNull(1)?.toIntOrNull() }
            .distinct()
            .sorted()
        if (indexes.isEmpty()) return emptyList()
        if (indexes != (0 until indexes.size).toList()) {
            errors += ContentValidationError(file, id, "modifier", "Modifier indexes must be contiguous from 0.")
            return emptyList()
        }
        return indexes.mapNotNull { index ->
            val prefix = "modifier.$index."
            val health = fields.requiredPositivePercent(file, id, "${prefix}healthPercent", errors) ?: return@mapNotNull null
            val speed = fields.requiredPositivePercent(file, id, "${prefix}speedPercent", errors) ?: return@mapNotNull null
            val count = fields.requiredPositiveInt(file, id, "${prefix}count", errors) ?: return@mapNotNull null
            WaveModifier(health, speed, count)
        }
    }

    /**
     * Parses the optional paired bonus fields. Keeping the pair optional preserves v1 pack
     * compatibility while making a half-declared bonus a validation error instead of silently
     * changing the early-call economy.
     */
    private fun parseWaveEarlyCallBonus(
        id: String,
        fields: Map<String, String>,
        errors: MutableList<ContentValidationError>,
        file: String,
    ): WaveEarlyCallBonus? {
        val resourceField = "earlyCallBonusResourceId"
        val amountField = "earlyCallBonusAmount"
        val hasResource = fields.containsKey(resourceField)
        val hasAmount = fields.containsKey(amountField)
        if (!hasResource && !hasAmount) return null
        if (hasResource != hasAmount) {
            if (!hasResource) errors += ContentValidationError(file, id, resourceField, "Early-call bonus fields must be declared as a pair.")
            if (!hasAmount) errors += ContentValidationError(file, id, amountField, "Early-call bonus fields must be declared as a pair.")
            return null
        }
        val resourceId = fields.required(file, id, resourceField, errors) ?: return null
        val amount = fields.requiredPositiveInt(file, id, amountField, errors) ?: return null
        return WaveEarlyCallBonus(resourceId, amount)
    }

    private fun parseIncident(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): IncidentContent? {
        val minThreat = fields.requiredNonNegativeInt(file, id, "minThreat", errors) ?: return null
        val maxThreat = fields.requiredNonNegativeInt(file, id, "maxThreat", errors) ?: return null
        val pacingMinThreat = fields.optionalNonNegativeInt(file, id, "pacingMinThreat", errors) ?: minThreat
        val pacingMaxThreat = fields.optionalNonNegativeInt(file, id, "pacingMaxThreat", errors) ?: maxThreat
        val cadenceInterval = fields.optionalNonNegativeInt(file, id, "cadenceIntervalTicks", errors)
            ?: fields.optionalNonNegativeInt(file, id, "cadenceTicks", errors)
            ?: 0
        val cadenceStart = fields.optionalNonNegativeLong(file, id, "cadenceStartTick", errors) ?: 0
        val cadenceEnd = fields.optionalNonNegativeLong(file, id, "cadenceEndTick", errors)
        if (maxThreat < minThreat) {
            errors += ContentValidationError(file, id, "maxThreat", "Must be greater than or equal to minThreat.")
        }
        if (pacingMaxThreat < pacingMinThreat) {
            errors += ContentValidationError(file, id, "pacingMaxThreat", "Must be greater than or equal to pacingMinThreat.")
        }
        if (cadenceEnd != null && cadenceEnd < cadenceStart) {
            errors += ContentValidationError(file, id, "cadenceEndTick", "Must be greater than or equal to cadenceStartTick.")
        }
        if (maxThreat < minThreat || pacingMaxThreat < pacingMinThreat || (cadenceEnd != null && cadenceEnd < cadenceStart)) {
            return null
        }
        return IncidentContent(
            id = id,
            minThreat = minThreat,
            maxThreat = maxThreat,
            weight = fields.requiredPositiveInt(file, id, "weight", errors) ?: return null,
            cadenceStartTick = cadenceStart,
            cadenceIntervalTicks = cadenceInterval,
            cadenceEndTick = cadenceEnd,
            pacingMinThreat = pacingMinThreat,
            pacingMaxThreat = pacingMaxThreat,
            cooldownTicks = fields.optionalNonNegativeInt(file, id, "cooldownTicks", errors) ?: 0,
            effects = parseIncidentEffects(id, fields, errors, file),
        )
    }

    private fun parseIncidentEffects(
        id: String,
        fields: Map<String, String>,
        errors: MutableList<ContentValidationError>,
        file: String,
    ): List<IncidentEffectDescriptor> {
        val direct = fields["effects"]?.split(',')?.map(String::trim)?.filter(String::isNotEmpty).orEmpty()
        val indexed = fields.entries
            .mapNotNull { entry ->
                val match = Regex("(?:effect|effects)\\.(\\d+)").find(entry.key) ?: return@mapNotNull null
                if (match.value == entry.key) match.groupValues[1].toInt() to entry.value else null
            }
            .sortedBy { it.first }
            .map { it.second }
        if (direct.isNotEmpty() && indexed.isNotEmpty()) {
            errors += ContentValidationError(file, id, "effects", "Use either effects=... or indexed effect.N fields, not both.")
            return emptyList()
        }
        if (indexed.isNotEmpty()) {
            return indexed.mapIndexedNotNull { index, value ->
                parseIncidentEffect(id, "effect.$index", value, errors, file)
            }
        }
        return direct.mapIndexedNotNull { index, value ->
            parseIncidentEffect(id, "effects[$index]", value, errors, file)
        }
    }

    private fun parseIncidentEffect(
        id: String,
        field: String,
        raw: String,
        errors: MutableList<ContentValidationError>,
        file: String,
    ): IncidentEffectDescriptor? {
        val parts = raw.split(':').map(String::trim)
        val type = IncidentEffectType.fromId(parts.firstOrNull().orEmpty())
        if (type == null) {
            errors += ContentValidationError(file, id, field, "Expected spawn_wave, resource_event, or modifier.")
            return null
        }
        return when (type) {
            IncidentEffectType.SPAWN_WAVE -> if (parts.size == 2 && parts[1].isNotBlank()) {
                IncidentEffectDescriptor.SpawnWave(parts[1])
            } else {
                errors += ContentValidationError(file, id, field, "Expected spawn_wave:waveId.")
                null
            }
            IncidentEffectType.RESOURCE_EVENT -> if (parts.size == 3 && parts[1].isNotBlank()) {
                val amount = parts[2].toIntOrNull()
                if (amount == null || amount <= 0) {
                    errors += ContentValidationError(file, id, field, "Resource event amount must be a positive integer.")
                    null
                } else IncidentEffectDescriptor.ResourceEvent(parts[1], amount)
            } else {
                errors += ContentValidationError(file, id, field, "Expected resource_event:resourceId:amount.")
                null
            }
            IncidentEffectType.MODIFIER -> if (parts.size == 4 && parts[1].isNotBlank()) {
                val amount = parts[2].toIntOrNull()
                val duration = parts[3].toIntOrNull()
                if (amount == null || amount <= 0 || duration == null || duration <= 0) {
                    errors += ContentValidationError(file, id, field, "Modifier amount and duration must be positive integers.")
                    null
                } else IncidentEffectDescriptor.Modifier(parts[1], amount, duration)
            } else {
                errors += ContentValidationError(file, id, field, "Expected modifier:modifierId:amount:durationTicks.")
                null
            }
        }
    }

    private fun parseDifficulty(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): DifficultyContent? =
        DifficultyContent(
            id = id,
            healthMult = fields.requiredPositiveDecimal(file, id, "healthMult", errors) ?: return null,
            countMult = fields.requiredPositiveDecimal(file, id, "countMult", errors) ?: return null,
            rewardMult = fields.requiredPositiveDecimal(file, id, "rewardMult", errors) ?: return null,
            goldRateMult = fields.requiredPositiveDecimal(file, id, "goldRateMult", errors) ?: return null,
        )

    private fun parseMaps(
        root: Path,
        tiles: Map<String, TileContent>,
        resources: Map<String, ResourceContent>,
        errors: MutableList<ContentValidationError>,
    ): Map<String, MapContent> {
        val file = "maps.json"
        val path = root.resolve(file)
        if (!Files.exists(path)) return emptyMap()

        val document = try {
            Files.newBufferedReader(path).use { reader -> Json.parseToJsonElement(reader.readText()) as? JsonObject }
        } catch (error: Exception) {
            errors += ContentValidationError(file, "file", "json", "Invalid JSON: ${error.message ?: error::class.simpleName}.")
            return emptyMap()
        }
        if (document == null) {
            errors += ContentValidationError(file, "file", "json", "Expected a top-level JSON object.")
            return emptyMap()
        }
        val definitions = document["maps"] as? JsonArray
        if (definitions == null) {
            errors += ContentValidationError(file, "file", "maps", "Expected a 'maps' array.")
            return emptyMap()
        }

        val parsed = linkedMapOf<String, MapContent>()
        definitions.forEachIndexed { index, value ->
            val objectValue = value as? JsonObject
            if (objectValue == null) {
                errors += ContentValidationError(file, "maps[$index]", "entry", "Expected a map object.")
                return@forEachIndexed
            }
            val map = parseMap(objectValue, file, "maps[$index]", errors) ?: return@forEachIndexed
            if (parsed.put(map.id, map) != null) {
                errors += ContentValidationError(file, map.id, "id", "Duplicate map id.")
            }
        }
        parsed.values.forEach { validateMap(it, tiles, resources, errors) }
        return parsed
    }

    private fun parseMap(
        value: JsonObject,
        file: String,
        fallbackId: String,
        errors: MutableList<ContentValidationError>,
    ): MapContent? {
        val id = value.requiredString("id", file, fallbackId, "id", errors) ?: return null
        val width = value.requiredInt("width", file, id, "width", errors) ?: 0
        val height = value.requiredInt("height", file, id, "height", errors) ?: 0
        val terrainRows = value.requiredStringArray("terrainRows", file, id, errors)
        val terrainMapping = parseTerrainMapping(value["terrainMapping"], file, id, errors)
        val spawns = parseSpawns(value["spawns"], file, id, errors)
        val core = parseCoordinate(value["core"], file, id, "core", errors) ?: MapCoordinate(-1, -1)
        val terminalRules = parseTerminalRules(value["terminalRules"], file, id, errors)
        return MapContent(id, width, height, terrainRows, terrainMapping, spawns, core, terminalRules)
    }

    private fun parseTerminalRules(
        value: kotlinx.serialization.json.JsonElement?,
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): MapTerminalRules {
        if (value == null) return MapTerminalRules()
        val objectValue = value as? JsonObject
        if (objectValue == null) {
            errors += ContentValidationError(file, id, "terminalRules", "Expected an object with optional winCondition and leakBudget.")
            return MapTerminalRules()
        }

        val winCondition = when (val raw = objectValue["winCondition"]) {
            null -> MapWinCondition.FINITE_WAVES
            is JsonPrimitive -> when (raw.contentOrNull) {
                "finite_waves" -> MapWinCondition.FINITE_WAVES
                "no_win", "endless" -> MapWinCondition.NO_WIN
                else -> {
                    errors += ContentValidationError(file, id, "terminalRules.winCondition", "Expected 'finite_waves', 'no_win', or 'endless'.")
                    MapWinCondition.FINITE_WAVES
                }
            }
            else -> {
                errors += ContentValidationError(file, id, "terminalRules.winCondition", "Expected a string.")
                MapWinCondition.FINITE_WAVES
            }
        }

        val leakBudget = when (val raw = objectValue["leakBudget"]) {
            null -> null
            is JsonPrimitive -> raw.intOrNull?.also { budget ->
                if (budget <= 0) {
                    errors += ContentValidationError(file, id, "terminalRules.leakBudget", "Leak budget must be positive when declared.")
                }
            }?.takeIf { it > 0 } ?: run {
                if (raw.intOrNull == null) {
                    errors += ContentValidationError(file, id, "terminalRules.leakBudget", "Expected a positive integer when declared.")
                }
                null
            }
            else -> {
                errors += ContentValidationError(file, id, "terminalRules.leakBudget", "Expected a positive integer when declared.")
                null
            }
        }
        return MapTerminalRules(winCondition, leakBudget)
    }

    private fun parseTerrainMapping(
        value: kotlinx.serialization.json.JsonElement?,
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): Map<Char, MapTerrainSymbol> {
        val mapping = value as? JsonObject
        if (mapping == null) {
            errors += ContentValidationError(file, id, "terrainMapping", "Expected an object that maps one-character symbols to terrain.")
            return emptyMap()
        }
        return buildMap {
            mapping.forEach { (symbol, definition) ->
                if (symbol.length != 1) {
                    errors += ContentValidationError(file, id, "terrainMapping.$symbol", "Terrain symbols must be exactly one character.")
                    return@forEach
                }
                val objectValue = definition as? JsonObject
                if (objectValue == null) {
                    errors += ContentValidationError(file, id, "terrainMapping.$symbol", "Expected a terrain mapping object.")
                    return@forEach
                }
                val terrainId = objectValue.requiredString("tile", file, id, "terrainMapping.$symbol.tile", errors) ?: return@forEach
                val resourceNode = parseResourceNode(objectValue["resource"], file, id, "terrainMapping.$symbol.resource", errors)
                put(symbol.single(), MapTerrainSymbol(terrainId, resourceNode))
            }
        }
    }

    private fun parseResourceNode(
        value: kotlinx.serialization.json.JsonElement?,
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): MapResourceNode? {
        if (value == null) return null
        val objectValue = value as? JsonObject
        if (objectValue == null) {
            errors += ContentValidationError(file, id, field, "Expected a resource object with id and amount.")
            return null
        }
        val resourceId = objectValue.requiredString("id", file, id, "$field.id", errors) ?: return null
        val amount = objectValue.requiredInt("amount", file, id, "$field.amount", errors) ?: return null
        val infinite = objectValue["infinite"]?.let { raw ->
            (raw as? JsonPrimitive)?.booleanOrNull ?: errors.addAndNull(
                file,
                id,
                "$field.infinite",
                "Expected boolean.",
            )
        } ?: false
        return MapResourceNode(resourceId, amount, infinite)
    }

    private fun parseSpawns(
        value: kotlinx.serialization.json.JsonElement?,
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): Map<String, MapSpawn> {
        val array = value as? JsonArray
        if (array == null) {
            errors += ContentValidationError(file, id, "spawns", "Expected an array of named spawn objects.")
            return emptyMap()
        }
        val spawns = linkedMapOf<String, MapSpawn>()
        array.forEachIndexed { index, item ->
            val objectValue = item as? JsonObject
            if (objectValue == null) {
                errors += ContentValidationError(file, id, "spawns[$index]", "Expected a spawn object.")
                return@forEachIndexed
            }
            val spawnId = objectValue.requiredString("id", file, id, "spawns[$index].id", errors) ?: return@forEachIndexed
            if (spawnId == "all") {
                errors += ContentValidationError(
                    file,
                    id,
                    "spawns[$index].id",
                    "Spawn id 'all' is reserved for wave spawnSelection.",
                )
                return@forEachIndexed
            }
            if ('|' in spawnId) {
                errors += ContentValidationError(
                    file,
                    id,
                    "spawns[$index].id",
                    "Spawn ids cannot contain the '|' delimiter used by wave spawnSelection.",
                )
                return@forEachIndexed
            }
            val coordinate = parseCoordinate(objectValue, file, id, "spawns[$index]", errors) ?: return@forEachIndexed
            if (spawns.put(spawnId, MapSpawn(spawnId, coordinate)) != null) {
                errors += ContentValidationError(file, id, "spawns[$index].id", "Duplicate named spawn '$spawnId'.")
            }
        }
        return spawns
    }

    private fun parseCoordinate(
        value: kotlinx.serialization.json.JsonElement?,
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): MapCoordinate? {
        val objectValue = value as? JsonObject
        if (objectValue == null) {
            errors += ContentValidationError(file, id, field, "Expected an object with integer x and y coordinates.")
            return null
        }
        val x = objectValue.requiredInt("x", file, id, "$field.x", errors) ?: return null
        val y = objectValue.requiredInt("y", file, id, "$field.y", errors) ?: return null
        return MapCoordinate(x, y)
    }

    private fun validateMap(
        map: MapContent,
        tiles: Map<String, TileContent>,
        resources: Map<String, ResourceContent>,
        errors: MutableList<ContentValidationError>,
    ) {
        val file = "maps.json"
        if (map.width <= 0) errors += ContentValidationError(file, map.id, "width", "Map width must be positive.")
        if (map.height <= 0) errors += ContentValidationError(file, map.id, "height", "Map height must be positive.")
        if (map.terrainRows.size != map.height) {
            errors += ContentValidationError(file, map.id, "terrainRows", "Expected ${map.height} rows, found ${map.terrainRows.size}.")
        }
        map.terrainMapping.toSortedMap().forEach { (symbol, mapping) ->
            if (!tiles.containsKey(mapping.terrainId)) {
                errors += ContentValidationError(file, map.id, "terrainMapping.$symbol.tile", "Unknown tile '${mapping.terrainId}'.")
            }
            mapping.resourceNode?.let { resource ->
                if (!resources.containsKey(resource.resourceId)) {
                    errors += ContentValidationError(file, map.id, "terrainMapping.$symbol.resource.id", "Unknown resource '${resource.resourceId}'.")
                }
                if (resource.amount < 0) {
                    errors += ContentValidationError(file, map.id, "terrainMapping.$symbol.resource.amount", "Resource amount must be non-negative.")
                }
            }
        }

        val coreTiles = mutableListOf<MapCoordinate>()
        map.terrainRows.forEachIndexed { y, row ->
            if (row.length != map.width) {
                errors += ContentValidationError(file, map.id, "terrainRows[$y]", "Expected row width ${map.width}, found ${row.length}.")
            }
            row.forEachIndexed { x, symbol ->
                val mapping = map.terrainMapping[symbol]
                if (mapping == null) {
                    errors += ContentValidationError(file, map.id, "terrainRows[$y][$x]", "Unknown terrain symbol '$symbol'; add it to terrainMapping.")
                } else if (tiles[mapping.terrainId]?.isCore == true) {
                    coreTiles += MapCoordinate(x, y)
                }
            }
        }
        if (coreTiles.size != 1) {
            errors += ContentValidationError(file, map.id, "terrainRows", "Expected exactly one core tile, found ${coreTiles.size}.")
        }
        if (!map.inBounds(map.core)) {
            errors += ContentValidationError(file, map.id, "core", "Core coordinate (${map.core.x},${map.core.y}) is outside ${map.width}x${map.height}.")
        } else if (coreTiles.singleOrNull() != map.core) {
            errors += ContentValidationError(file, map.id, "core", "Core coordinate must point at the single core terrain tile.")
        }
        if (map.spawns.isEmpty()) {
            errors += ContentValidationError(file, map.id, "spawns", "At least one named spawn is required.")
        }

        val gridIsUsable = map.width > 0 && map.height > 0 &&
            map.terrainRows.size == map.height &&
            map.terrainRows.all { it.length == map.width } &&
            map.terrainRows.all { row -> row.all(map.terrainMapping::containsKey) } &&
            map.terrainMapping.values.all { tiles.containsKey(it.terrainId) }
        map.spawns.values.sortedBy { it.id }.forEach { spawn ->
            if (!map.inBounds(spawn.position)) {
                errors += ContentValidationError(file, map.id, "spawns.${spawn.id}", "Spawn coordinate (${spawn.position.x},${spawn.position.y}) is outside ${map.width}x${map.height}.")
            } else if (gridIsUsable && !isWalkable(map, spawn.position, tiles)) {
                errors += ContentValidationError(file, map.id, "spawns.${spawn.id}", "Spawn must be on walkable terrain.")
            } else if (gridIsUsable && coreTiles.singleOrNull() == map.core && !hasWalkablePath(map, spawn.position, map.core, tiles)) {
                errors += ContentValidationError(file, map.id, "spawns.${spawn.id}", "No walkable path from spawn '${spawn.id}' to core (${map.core.x},${map.core.y}).")
            }
        }
    }

    private fun hasWalkablePath(
        map: MapContent,
        start: MapCoordinate,
        target: MapCoordinate,
        tiles: Map<String, TileContent>,
    ): Boolean {
        if (!isWalkable(map, start, tiles) || !isWalkable(map, target, tiles)) return false
        val pending = ArrayDeque<MapCoordinate>()
        val visited = hashSetOf(start)
        pending += start
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (current == target) return true
            listOf(
                MapCoordinate(current.x + 1, current.y),
                MapCoordinate(current.x, current.y + 1),
                MapCoordinate(current.x - 1, current.y),
                MapCoordinate(current.x, current.y - 1),
            ).filter { next -> map.inBounds(next) }.forEach { next ->
                if (visited.add(next) && isWalkable(map, next, tiles)) pending += next
            }
        }
        return false
    }

    private fun isWalkable(map: MapContent, position: MapCoordinate, tiles: Map<String, TileContent>): Boolean {
        val mapping = map.terrainMapping[map.symbolAt(position)] ?: return false
        return tiles[mapping.terrainId]?.blocksMovement == false
    }

    private fun MapContent.inBounds(position: MapCoordinate): Boolean =
        position.x in 0 until width && position.y in 0 until height

    private fun JsonObject.requiredString(
        key: String,
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): String? {
        val value = this[key] as? JsonPrimitive
        val text = value?.contentOrNull?.takeIf { it.isNotBlank() }
        return text ?: errors.addAndNull(file, id, field, "Expected a non-blank string.")
    }

    private fun JsonObject.requiredInt(
        key: String,
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): Int? {
        val value = this[key] as? JsonPrimitive
        val number = value?.intOrNull
        return number ?: errors.addAndNull(file, id, field, "Expected an integer.")
    }

    private fun JsonObject.requiredStringArray(
        key: String,
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): List<String> {
        val array = this[key] as? JsonArray
        if (array == null) {
            errors += ContentValidationError(file, id, key, "Expected an array of row strings.")
            return emptyList()
        }
        return array.mapIndexedNotNull { index, value ->
            val text = (value as? JsonPrimitive)?.contentOrNull
            if (text == null) {
                errors += ContentValidationError(file, id, "$key[$index]", "Expected a row string.")
            }
            text
        }
    }

    private fun JsonObject.optionalStringArray(
        key: String,
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): List<String> {
        val value = this[key] ?: return emptyList()
        val array = value as? JsonArray
        if (array == null) {
            errors += ContentValidationError(file, id, key, "Expected an array of strings.")
            return emptyList()
        }
        return array.mapIndexedNotNull { index, item ->
            val text = (item as? JsonPrimitive)?.contentOrNull
            if (text == null || text.isBlank()) {
                errors += ContentValidationError(file, id, "$key[$index]", "Expected a non-blank string.")
                null
            } else text
        }
    }

    private fun validateTechTree(
        techNodes: Map<String, TechNodeContent>,
        resources: Map<String, ResourceContent>,
        towers: Map<String, TowerContent>,
        buildings: Map<String, BuildingContent>,
        recipes: Map<String, RecipeContent>,
        errors: MutableList<ContentValidationError>,
    ) {
        if (techNodes.isEmpty()) return
        val file = "tech-tree.json"
        val unlockOwners = mutableMapOf<String, String>()
        techNodes.toSortedMap().forEach { (id, node) ->
            if (node.costResource !in resources) {
                errors += ContentValidationError(file, id, "cost.resource", "Unknown resource '${node.costResource}'.")
            }
            val duplicatePrerequisites = node.prerequisites.groupingBy { it }.eachCount()
                .filterValues { it > 1 }.keys.sorted()
            duplicatePrerequisites.forEach { prerequisite ->
                errors += ContentValidationError(file, id, "prerequisites", "Duplicate prerequisite '$prerequisite'.")
            }
            node.prerequisites.sorted().forEach { prerequisite ->
                if (prerequisite !in techNodes) {
                    errors += ContentValidationError(file, id, "prerequisites", "Unknown prerequisite '$prerequisite'.")
                }
            }
            val duplicateUnlocks = node.unlocks.groupingBy { it.stableKey }.eachCount()
                .filterValues { it > 1 }.keys.sorted()
            duplicateUnlocks.forEach { unlock ->
                errors += ContentValidationError(file, id, "unlocks", "Duplicate unlock reference '$unlock'.")
            }
            node.unlocks.sortedBy { it.stableKey }.forEach { unlock ->
                unlockOwners.putIfAbsent(unlock.stableKey, id)?.let { owner ->
                    errors += ContentValidationError(
                        file,
                        id,
                        "unlocks",
                        "Duplicate unlock reference '${unlock.stableKey}' already declared by '$owner'.",
                    )
                }
                val known = when (unlock.type) {
                    TechUnlockType.TOWER -> unlock.id in towers
                    TechUnlockType.BUILDING -> unlock.id in buildings
                    TechUnlockType.RECIPE -> unlock.id in recipes
                }
                if (!known) {
                    errors += ContentValidationError(file, id, "unlocks", "Unknown ${unlock.type.id} '${unlock.id}'.")
                }
            }
        }

        val states = mutableMapOf<String, Int>()
        fun visit(id: String, path: List<String>) {
            when (states[id]) {
                1 -> {
                    val cycleStart = path.indexOf(id).coerceAtLeast(0)
                    errors += ContentValidationError(
                        file,
                        id,
                        "prerequisites",
                        "Prerequisite cycle: ${(path.drop(cycleStart) + id).joinToString(" -> ") }.",
                    )
                    return
                }
                2 -> return
            }
            states[id] = 1
            val node = techNodes[id] ?: return
            node.prerequisites.sorted().filter { it in techNodes }.forEach { visit(it, path + id) }
            states[id] = 2
        }
        techNodes.keys.sorted().forEach { visit(it, emptyList()) }
    }

    private fun validateReferences(
        root: Path,
        packId: String,
        tiles: Map<String, TileContent>,
        damageTypes: Map<String, DamageTypeContent>,
        damageTypesDeclared: Boolean,
        towers: Map<String, TowerContent>,
        enemies: Map<String, EnemyContent>,
        buildings: Map<String, BuildingContent>,
        recipes: Map<String, RecipeContent>,
        waves: Map<String, WaveContent>,
        incidents: Map<String, IncidentContent>,
        resources: Map<String, ResourceContent>,
        effects: Map<String, StatusEffectContent>,
        sounds: Map<GameplayEventType, SoundRef>,
        maps: Map<String, MapContent>,
        endlessWave: EndlessWaveContent?,
        errors: MutableList<ContentValidationError>,
    ) {
        tiles.values.forEach { tile ->
            validateVisualAsset(root, packId, "tiles.properties", tile.id, "", tile.assetRef, errors)
        }
        towers.values.forEach { tower ->
            validateVisualAsset(root, packId, "towers.properties", tower.id, "", tower.assetRef, errors)
            if (damageTypesDeclared && tower.damageTypeId == null) {
                errors += ContentValidationError(
                    "towers.properties",
                    tower.id,
                    "damageTypeId",
                    "Typed content requires every tower to declare a non-blank damage type via damageTypeId.",
                )
            } else {
                tower.damageTypeId?.let { damageTypeId ->
                    if (!damageTypes.containsKey(damageTypeId)) {
                        errors += ContentValidationError("towers.properties", tower.id, "damageTypeId", "Unknown damage type '$damageTypeId'.")
                    }
                }
            }
            tower.effectId?.let { effectId ->
                if (!effects.containsKey(effectId)) {
                    errors += ContentValidationError("towers.properties", tower.id, "effectId", "Unknown status effect '$effectId'.")
                }
            }
            if (!resources.containsKey(tower.costResource)) errors += ContentValidationError("towers.properties", tower.id, "costResource", "Unknown resource '${tower.costResource}'.")
            tower.upgradeTiers.values.forEach { tier ->
                val prefix = "upgrade.${tier.branch}.${tier.tier}."
                validateVisualAsset(root, packId, "towers.properties", tower.id, prefix, tier.assetRef, errors)
                if (!resources.containsKey(tier.costResource)) errors += ContentValidationError("towers.properties", tower.id, "upgrade.${tier.branch}.${tier.tier}.costResource", "Unknown resource '${tier.costResource}'.")
            }
        }
        enemies.values.forEach {
            validateVisualAsset(root, packId, "enemies.properties", it.id, "", it.assetRef, errors)
            if (!resources.containsKey(it.rewardResource)) errors += ContentValidationError("enemies.properties", it.id, "rewardResource", "Unknown resource '${it.rewardResource}'.")
            it.resists.toSortedMap().forEach { (damageTypeId, _) ->
                if (!damageTypes.containsKey(damageTypeId)) {
                    errors += ContentValidationError("enemies.properties", it.id, "resist.$damageTypeId", "Unknown damage type '$damageTypeId'.")
                }
            }
        }
        if (damageTypesDeclared) {
            val towerDamageTypes = towers.values.mapNotNull { it.damageTypeId }.toSet()
            val enemyResistanceTypes = enemies.values.flatMap { it.resists.keys }.toSet()
            damageTypes.keys.sorted().forEach { damageTypeId ->
                if (damageTypeId !in towerDamageTypes) {
                    errors += ContentValidationError(
                        "damage-types.properties",
                        damageTypeId,
                        "usage.towers",
                        "Damage type must be used by at least one tower via damageTypeId.",
                    )
                }
                if (damageTypeId !in enemyResistanceTypes) {
                    errors += ContentValidationError(
                        "damage-types.properties",
                        damageTypeId,
                        "usage.enemyResists",
                        "Damage type must be used by at least one enemy resist.<damageTypeId> entry.",
                    )
                }
            }
        }
        buildings.values.forEach { building ->
            validateVisualAsset(root, packId, "buildings.properties", building.id, "", building.assetRef, errors)
            if (!resources.containsKey(building.costResource)) {
                errors += ContentValidationError(
                    "buildings.properties",
                    building.id,
                    "costResource",
                    "Unknown resource '${building.costResource}'.",
                )
            }
            building.producerRecipeId?.let { recipeId ->
                val recipe = recipes[recipeId]
                if (recipe == null) {
                    errors += ContentValidationError(
                        "buildings.properties",
                        building.id,
                        "producerRecipeId",
                        "Unknown recipe '$recipeId'.",
                    )
                } else if (recipe.inputResource != null || recipe.inputAmount != 0) {
                    errors += ContentValidationError(
                        "buildings.properties",
                        building.id,
                        "producerRecipeId",
                        "Extractor recipes must not require input resources.",
                    )
                }
            }
        }
        recipes.values.forEach {
            if (it.inputResource != null && !resources.containsKey(it.inputResource)) errors += ContentValidationError("recipes.properties", it.id, "inputResource", "Unknown resource '${it.inputResource}'.")
            if (!resources.containsKey(it.outputResource)) errors += ContentValidationError("recipes.properties", it.id, "outputResource", "Unknown resource '${it.outputResource}'.")
        }
        waves.values.forEach { wave ->
            wave.spawnSelection?.let { selectedSpawnIds ->
                if (maps.isEmpty()) {
                    errors += ContentValidationError(
                        "waves.properties",
                        wave.id,
                        "spawnSelection",
                        "Named spawn selection requires at least one map definition.",
                    )
                } else {
                    maps.toSortedMap().forEach { (mapId, map) ->
                        selectedSpawnIds.forEach { spawnId ->
                            if (spawnId !in map.spawns) {
                                errors += ContentValidationError(
                                    "waves.properties",
                                    wave.id,
                                    "spawnSelection",
                                    "Unknown spawn '$spawnId' in map '$mapId'.",
                                )
                            }
                        }
                    }
                }
            }
            wave.earlyCallBonus?.let { bonus ->
                if (!resources.containsKey(bonus.resourceId)) {
                    errors += ContentValidationError(
                        "waves.properties",
                        wave.id,
                        "earlyCallBonusResourceId",
                        "Unknown resource '${bonus.resourceId}'.",
                    )
                }
            }
            wave.spawns.forEach { spawn ->
                if (!enemies.containsKey(spawn.enemyId)) errors += ContentValidationError("waves.properties", wave.id, "spawns", "Unknown enemy '${spawn.enemyId}'.")
                if (spawn.count <= 0) errors += ContentValidationError("waves.properties", wave.id, "spawns", "Spawn count must be positive.")
            }
        }
        endlessWave?.let { endless ->
            endless.spawnSelection?.let { selectedSpawnIds ->
                if (maps.isEmpty()) {
                    errors += ContentValidationError(
                        "endless.properties",
                        "endless",
                        "spawnSelection",
                        "Named spawn selection requires at least one map definition.",
                    )
                } else {
                    maps.toSortedMap().forEach { (mapId, map) ->
                        selectedSpawnIds.forEach { spawnId ->
                            if (spawnId !in map.spawns) {
                                errors += ContentValidationError(
                                    "endless.properties",
                                    "endless",
                                    "spawnSelection",
                                    "Unknown spawn '$spawnId' in map '$mapId'.",
                                )
                            }
                        }
                    }
                }
            }
            endless.compositionCycle.forEachIndexed { cycleIndex, composition ->
                composition.spawns.forEach { spawn ->
                    if (!enemies.containsKey(spawn.enemyId)) {
                        errors += ContentValidationError(
                            "endless.properties",
                            "endless",
                            "compositionCycle[$cycleIndex]",
                            "Unknown enemy '${spawn.enemyId}'.",
                        )
                    }
                }
            }
        }
        incidents.values.forEach { incident ->
            incident.effects.forEachIndexed { index, effect ->
                when (effect) {
                    is IncidentEffectDescriptor.SpawnWave -> if (!waves.containsKey(effect.waveId)) {
                        errors += ContentValidationError("incidents.properties", incident.id, "effects[$index]", "Unknown wave '${effect.waveId}'.")
                    }
                    is IncidentEffectDescriptor.ResourceEvent -> if (!resources.containsKey(effect.resourceId)) {
                        errors += ContentValidationError("incidents.properties", incident.id, "effects[$index]", "Unknown resource '${effect.resourceId}'.")
                    }
                    is IncidentEffectDescriptor.Modifier -> Unit
                }
            }
        }
        sounds.toSortedMap(compareBy { it.id }).forEach { (eventType, reference) ->
            validateSoundRef(root, packId, eventType, reference, errors)
        }
    }

    private fun parseSounds(
        root: Path,
        packId: String,
        errors: MutableList<ContentValidationError>,
    ): Map<GameplayEventType, SoundRef> {
        val path = root.resolve("sounds.properties")
        if (!Files.exists(path)) return emptyMap()
        val props = readProperties(path, errors) ?: return emptyMap()
        val sounds = linkedMapOf<GameplayEventType, SoundRef>()
        props.entries
            .map { it.key.toString() to it.value.toString() }
            .sortedBy { it.first }
            .forEach { (rawId, rawPath) ->
                val eventType = GameplayEventType.fromId(rawId)
                if (eventType == null) {
                    errors += ContentValidationError(
                        "sounds.properties",
                        rawId,
                        "eventId",
                        "Unknown gameplay event id '$rawId'.",
                    )
                    return@forEach
                }
                if (eventType in sounds) {
                    errors += ContentValidationError(
                        "sounds.properties",
                        rawId,
                        "eventId",
                        "Duplicate gameplay event id '$rawId' after hyphen/underscore normalization.",
                    )
                    return@forEach
                }
                val trimmedPath = rawPath.trim()
                if (trimmedPath.isBlank()) {
                    errors += ContentValidationError(
                        "sounds.properties",
                        eventType.id,
                        "path",
                        "Sound path is missing.",
                    )
                    return@forEach
                }
                sounds[eventType] = SoundRef(trimmedPath)
            }
        return sounds.toSortedMap(compareBy { it.id })
    }

    private fun validateSoundRef(
        root: Path,
        packId: String,
        eventType: GameplayEventType,
        reference: SoundRef,
        errors: MutableList<ContentValidationError>,
    ) {
        val packRoot = root.toAbsolutePath().normalize()
        val resolved = packRoot.resolve(reference.path).normalize()
        if (!resolved.startsWith(packRoot)) {
            errors += ContentValidationError(
                "sounds.properties",
                eventType.id,
                "path",
                "Pack '$packId' sound path '${reference.path}' escapes the content-pack root.",
            )
            return
        }
        if (!Files.isRegularFile(resolved)) {
            errors += ContentValidationError(
                "sounds.properties",
                eventType.id,
                "path",
                "Pack '$packId' sound file '${reference.path}' does not exist.",
            )
        }
    }

    /**
     * Validates only the opaque reference metadata. The content module never decodes a sprite; a
     * minimal atlas is an original text index with one region key per non-comment line.
     */
    private fun validateVisualAsset(
        root: Path,
        packId: String,
        file: String,
        id: String,
        fieldPrefix: String,
        reference: VisualAssetRef?,
        errors: MutableList<ContentValidationError>,
    ) {
        if (reference == null) return
        val pathField = if (reference.atlasKey == null) "${fieldPrefix}spritePath" else "${fieldPrefix}atlasPath"
        val keyField = "${fieldPrefix}atlasKey"
        val packRoot = root.toAbsolutePath().normalize()
        val resolved = packRoot.resolve(reference.path).normalize()
        if (!resolved.startsWith(packRoot)) {
            errors += ContentValidationError(
                file,
                id,
                pathField,
                "Pack '$packId' asset path '${reference.path}' escapes the content-pack root.",
            )
            return
        }
        if (!Files.isRegularFile(resolved)) {
            errors += ContentValidationError(
                file,
                id,
                pathField,
                "Pack '$packId' asset file '${reference.path}' does not exist.",
            )
            return
        }
        val atlasKey = reference.atlasKey ?: return
        val keys = try {
            Files.readAllLines(resolved)
                .asSequence()
                .map { it.substringBefore('#').trim() }
                .filter { it.isNotEmpty() }
                .map { it.substringBefore('=').trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        } catch (error: Exception) {
            errors += ContentValidationError(
                file,
                id,
                pathField,
                "Pack '$packId' atlas path '${reference.path}' could not be read: ${error.message ?: "unknown error"}.",
            )
            return
        }
        if (atlasKey !in keys) {
            errors += ContentValidationError(
                file,
                id,
                keyField,
                "Pack '$packId' atlas path '${reference.path}' does not define key '$atlasKey'.",
            )
        }
    }

    private fun validateTerminalRules(
        maps: Map<String, MapContent>,
        waves: Map<String, WaveContent>,
        endlessWave: EndlessWaveContent?,
        errors: MutableList<ContentValidationError>,
    ) {
        if (endlessWave != null) {
            if (maps.isEmpty()) {
                errors += ContentValidationError(
                    file = "maps.json",
                    id = "pack",
                    field = "terminalRules.winCondition",
                    message = "Endless wave content requires at least one map with no_win terminal rules.",
                )
            }
            maps.values
                .filter { it.terminalRules.winCondition != MapWinCondition.NO_WIN }
                .sortedBy { it.id }
                .forEach { map ->
                    errors += ContentValidationError(
                        file = "maps.json",
                        id = map.id,
                        field = "terminalRules.winCondition",
                        message = "Endless wave content requires no_win terminal rules.",
                    )
                }
            return
        }
        if (waves.isNotEmpty()) return
        maps.values
            .filter { it.terminalRules.winCondition == MapWinCondition.FINITE_WAVES }
            .sortedBy { it.id }
            .forEach { map ->
                errors += ContentValidationError(
                    file = "maps.json",
                    id = map.id,
                    field = "terminalRules.winCondition",
                    message = "finite_waves requires at least one declared wave; use no_win or endless for an endless map.",
                )
            }
    }

    private fun validateLocalization(
        damageTypes: Map<String, DamageTypeContent>,
        resources: Map<String, ResourceContent>,
        towers: Map<String, TowerContent>,
        buildings: Map<String, BuildingContent>,
        strings: Map<String, String>,
        errors: MutableList<ContentValidationError>,
    ) {
        damageTypes.toSortedMap().values.forEach {
            if (!strings.containsKey(it.displayKey)) {
                errors += ContentValidationError("strings.properties", it.id, "displayKey", "Missing localization key '${it.displayKey}'.")
            }
        }
        resources.values.forEach {
            if (!strings.containsKey(it.displayKey)) {
                errors += ContentValidationError("strings.properties", it.id, "displayKey", "Missing localization key '${it.displayKey}'.")
            }
        }
        towers.values.forEach { tower ->
            if (!strings.containsKey(tower.displayKey)) {
                errors += ContentValidationError("strings.properties", tower.id, "displayKey", "Missing localization key '${tower.displayKey}'.")
            }
            tower.upgradeTiers.values.forEach { tier ->
                if (!strings.containsKey(tier.displayKey)) {
                    errors += ContentValidationError(
                        "strings.properties",
                        tower.id,
                        "upgrade.${tier.branch}.${tier.tier}.displayKey",
                        "Missing localization key '${tier.displayKey}'.",
                    )
                }
            }
        }
        buildings.values.forEach { building ->
            if (!strings.containsKey(building.displayKey)) {
                errors += ContentValidationError("strings.properties", building.id, "displayKey", "Missing localization key '${building.displayKey}'.")
            }
        }
        HudStringKeys.required.forEach { key ->
            if (!strings.containsKey(key)) {
                errors += ContentValidationError("strings.properties", "hud", key, "Missing required HUD localization key '$key'.")
            }
        }
    }

    private fun readProperties(path: Path, errors: MutableList<ContentValidationError>): Properties? {
        if (!Files.exists(path)) {
            errors += ContentValidationError(path.fileName.toString(), "file", "path", "File is missing.")
            return null
        }
        return Properties().also { props ->
            Files.newInputStream(path).use(props::load)
        }
    }

    private fun MutableList<ContentValidationError>.addAndNull(file: String, id: String, field: String, message: String): Nothing? {
        add(ContentValidationError(file, id, field, message))
        return null
    }

    private fun String.splitCsv(): List<String> =
        split(',').map { it.trim() }.filter { it.isNotEmpty() }

    private fun Map<String, String>.required(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): String? =
        this[field]?.takeIf { it.isNotBlank() } ?: errors.addAndNull(file, id, field, "Required field is missing.")

    private fun Map<String, String>.optionalNonBlank(
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): String? {
        val raw = this[field] ?: return null
        return raw.trim().takeIf { it.isNotEmpty() }
            ?: errors.addAndNull(file, id, field, "Expected a non-blank id.")
    }

    private fun Map<String, String>.requiredBool(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): Boolean? =
        required(file, id, field, errors)?.toBooleanStrictOrNull() ?: errors.addAndNull(file, id, field, "Expected boolean.")

    private fun Map<String, String>.optionalBool(
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): Boolean? {
        val raw = this[field] ?: return null
        return raw.toBooleanStrictOrNull() ?: errors.addAndNull(file, id, field, "Expected boolean.")
    }

    private fun Map<String, String>.requiredPositiveInt(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): Int? {
        val value = required(file, id, field, errors)?.toIntOrNull() ?: return errors.addAndNull(file, id, field, "Expected integer.")
        return if (value > 0) value else errors.addAndNull(file, id, field, "Expected positive integer.")
    }

    private fun Map<String, String>.requiredNonNegativeInt(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): Int? {
        val value = required(file, id, field, errors)?.toIntOrNull() ?: return errors.addAndNull(file, id, field, "Expected integer.")
        return if (value >= 0) value else errors.addAndNull(file, id, field, "Expected non-negative integer.")
    }

    private fun Map<String, String>.requiredNonNegativeLong(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): Long? {
        val value = required(file, id, field, errors)?.toLongOrNull() ?: return errors.addAndNull(file, id, field, "Expected integer.")
        return if (value >= 0) value else errors.addAndNull(file, id, field, "Expected non-negative integer.")
    }

    private fun Map<String, String>.requiredPositiveLong(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): Long? {
        val value = required(file, id, field, errors)?.toLongOrNull() ?: return errors.addAndNull(file, id, field, "Expected integer.")
        return if (value > 0) value else errors.addAndNull(file, id, field, "Expected positive integer.")
    }

    private fun Map<String, String>.requiredPositivePercent(
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): Int? {
        val value = requiredPositiveInt(file, id, field, errors) ?: return null
        return if (value <= 10_000) value else errors.addAndNull(
            file,
            id,
            field,
            "Expected a positive percentage no greater than 10000.",
        )
    }

    private fun Map<String, String>.optionalPositivePercent(
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): Int? {
        if (!containsKey(field)) return null
        val value = requiredPositivePercent(file, id, field, errors) ?: return null
        return value
    }

    private fun Map<String, String>.optionalNonNegativeInt(
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): Int? {
        val raw = this[field] ?: return null
        val value = raw.toIntOrNull()
        return if (value != null && value >= 0) value else errors.addAndNull(
            file,
            id,
            field,
            "Expected non-negative integer.",
        )
    }

    private fun Map<String, String>.optionalNonNegativeLong(
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): Long? {
        val raw = this[field] ?: return null
        val value = raw.toLongOrNull()
        return if (value != null && value >= 0) value else errors.addAndNull(
            file,
            id,
            field,
            "Expected non-negative integer.",
        )
    }

    /**
     * Optional splash fields preserve existing content packs. A declared radius must be positive;
     * `falloff` is a percentage of base damage removed for every Manhattan-distance ring and is
     * meaningful only when splash is enabled.
     */
    private fun Map<String, String>.optionalPositiveInt(
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): Int? {
        val raw = this[field] ?: return null
        val value = raw.toIntOrNull()
        if (value == null || value <= 0) {
            errors += ContentValidationError(file, id, field, "Expected positive integer.")
            return null
        }
        return value
    }

    private fun Map<String, String>.optionalFalloffPercent(
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): Int? {
        val raw = this["falloff"]
        if (raw == null) return 0
        if (!containsKey("splashRadius")) {
            errors += ContentValidationError(file, id, "falloff", "falloff requires splashRadius.")
            return null
        }
        val value = raw.toIntOrNull()
        if (value == null || value !in 0..100) {
            errors += ContentValidationError(file, id, "falloff", "Expected integer percentage from 0 to 100.")
            return null
        }
        return value
    }

    /** Optional in schema v1 packs; absence preserves legacy deterministic nearest targeting. */
    private fun Map<String, String>.targetingModeOrDefault(
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): TargetingMode? {
        val value = this["targetingMode"] ?: return TargetingMode.NEAREST
        return TargetingMode.fromId(value) ?: errors.addAndNull(
            file,
            id,
            "targetingMode",
            "Expected one of ${TargetingMode.entries.joinToString { it.id }}.",
        )
    }

    private fun Map<String, String>.requiredPositiveDecimal(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): BigDecimal? {
        val raw = required(file, id, field, errors) ?: return null
        val value = try {
            BigDecimal(raw.trim())
        } catch (_: NumberFormatException) {
            return errors.addAndNull(file, id, field, "Expected decimal multiplier.")
        }
        return if (value.signum() > 0) value else errors.addAndNull(file, id, field, "Expected positive decimal multiplier.")
    }

    private fun Map<String, String>.requiredDecimalInRange(
        file: String,
        id: String,
        field: String,
        minimum: BigDecimal,
        maximum: BigDecimal,
        errors: MutableList<ContentValidationError>,
    ): BigDecimal? {
        val raw = required(file, id, field, errors) ?: return null
        val value = try {
            BigDecimal(raw.trim())
        } catch (_: NumberFormatException) {
            return errors.addAndNull(file, id, field, "Expected decimal between $minimum and $maximum.")
        }
        return if (value >= minimum && value <= maximum) value else {
            errors.addAndNull(file, id, field, "Expected decimal between $minimum and $maximum inclusive.")
        }
    }
}
