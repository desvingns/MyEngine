package dev.myengine.devtools

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.firstOrNull() == "replay-bisect" ||
        (args.firstOrNull() == "replay-inspect" && args.getOrNull(1) == "--trajectory")
    ) {
        val result = DevtoolReports.runReplayCommand(args)
        println(result.output)
        if (result.exitCode != 0) exitProcess(result.exitCode)
        return
    }
    val command = args.firstOrNull() ?: "scenario"
    val output = when (command) {
        "scenario", "balance", "benchmark" -> DevtoolReports.runScenarioSuite()
        "goal-field-benchmark" -> DevtoolReports.goalFieldRebuildBenchmark().toJson()
        "spatial-index-benchmark" -> DevtoolReports.spatialIndexBenchmark().toJson()
        "procedural-map", "map-generate" -> {
            val seed = args.getOrNull(1)?.toLongOrNull() ?: 7L
            DevtoolReports.proceduralMapReport(seed = seed).toJson()
        }
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
        "inspect", "state-inspect", "headless-inspect" -> {
            val shortForm = args.size == 1 || args.getOrNull(1)?.toIntOrNull() != null
            val factoryId = if (shortForm) "sandbox" else args[1]
            val scenarioId = if (shortForm) "default" else args[2]
            val packArg = if (shortForm) args.getOrNull(2) else args.getOrNull(3)
            val packRoot = packArg?.let { DevtoolReports.repoRoot().resolve(it) }
                ?: dev.myengine.games.sandbox.SandboxGame.contentRoot()
            val ticks = if (shortForm) {
                args.getOrNull(1)?.toIntOrNull() ?: 35
            } else {
                args.getOrNull(4)?.let { value ->
                    value.toIntOrNull() ?: error("Invalid inspection tick count '$value'.")
                } ?: 35
            }
            val scriptArg = if (shortForm) args.getOrNull(3) else args.getOrNull(5)
            val scriptPath = scriptArg?.let { DevtoolReports.repoRoot().resolve(it) }
            val seedArg = if (shortForm) args.getOrNull(4) else args.getOrNull(6)
            val seed = seedArg?.toLongOrNull() ?: 7L
            DevtoolReports.headlessStateInspect(factoryId, scenarioId, packRoot, ticks, scriptPath, seed).toJson()
        }
        "replay-inspect" -> DevtoolReports.replayInspect()
        else -> buildJson("error" to "unknown_command", "command" to command)
    }
    println(output)
}
