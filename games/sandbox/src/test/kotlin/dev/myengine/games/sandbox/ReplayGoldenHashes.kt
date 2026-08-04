package dev.myengine.games.sandbox

internal object ReplayGoldenHashes {
    private val HASH_PATTERN = Regex("[0-9a-f]{16}")

    val canonical: String = read("canonical")
    val kill: String = read("kill")
    val resist: String = read("resist")

    private fun read(scenario: String): String {
        val resource = "/golden/$scenario.hash"
        val value = requireNotNull(ReplayGoldenHashes::class.java.getResourceAsStream(resource)) {
            "Missing replay golden resource $resource"
        }.bufferedReader().use { it.readText().trim() }
        require(value.matches(HASH_PATTERN)) {
            "Invalid replay golden hash in $resource: '$value'"
        }
        return value
    }

}
