package dev.myengine.devtools

fun main(args: Array<String>) {
    val command = args.firstOrNull() ?: "scenario"
    val output = when (command) {
        "scenario", "balance", "benchmark" -> DevtoolReports.runScenarioSuite()
        "goal-field-benchmark" -> DevtoolReports.goalFieldRebuildBenchmark().toJson()
        "spatial-index-benchmark" -> DevtoolReports.spatialIndexBenchmark().toJson()
        "balance-delta", "balance-report" -> {
            val baselineRoot = args.getOrNull(1)?.let { DevtoolReports.repoRoot().resolve(it) }
                ?: dev.myengine.games.sandbox.SandboxGame.contentRoot()
            val changedRoot = args.getOrNull(2)?.let { DevtoolReports.repoRoot().resolve(it) }
                ?: DevtoolReports.repoRoot().resolve("games/signal-garden/content/signal-garden")
            DevtoolReports.balanceDeltaReport(baselineRoot, changedRoot).toJson()
        }
        "content-report", "content-validate" -> {
            val pathArg = args.getOrNull(1)
            // Relative paths resolve from the repo root (not the module working dir); absolute
            // paths pass through unchanged. With no arg, fall back to the sandbox pack.
            val root = if (pathArg != null) {
                DevtoolReports.repoRoot().resolve(pathArg)
            } else {
                dev.myengine.games.sandbox.SandboxGame.contentRoot()
            }
            DevtoolReports.contentReport(root).toJson()
        }
        "content-report-all", "content-validate-all" -> DevtoolReports.contentReportAll().toJson()
        "endless-scaling", "endless-wave-scaling" -> {
            val pathArg = args.getOrNull(1)
            val root = pathArg?.let { DevtoolReports.repoRoot().resolve(it) }
                ?: dev.myengine.games.sandbox.SandboxGame.contentRoot()
            val waveCount = args.getOrNull(2)?.toIntOrNull() ?: 10
            val seed = args.getOrNull(3)?.toLongOrNull() ?: 7L
            DevtoolReports.endlessWaveScalingReport(root, waveCount, seed).toJson()
        }
        "replay-inspect" -> DevtoolReports.replayInspect()
        else -> buildJson("error" to "unknown_command", "command" to command)
    }
    println(output)
}
