package dev.myengine.devtools

fun main(args: Array<String>) {
    val command = args.firstOrNull() ?: "scenario"
    val output = when (command) {
        "scenario", "balance", "benchmark" -> DevtoolReports.runScenarioSuite()
        "goal-field-benchmark" -> DevtoolReports.goalFieldRebuildBenchmark().toJson()
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
        "replay-inspect" -> DevtoolReports.replayInspect()
        else -> buildJson("error" to "unknown_command", "command" to command)
    }
    println(output)
}
