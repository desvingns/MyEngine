package dev.myengine.games.sandbox

import dev.myengine.content.ContentRegistry
import dev.myengine.core.RunSummary
import dev.myengine.core.RunState
import java.io.StringReader
import java.util.Base64
import java.util.Properties

/** Persistent profile data; intentionally separate from a single run's SandboxSaveCodec payload. */
data class MetaProgressionProfile(
    val metaCurrency: Int = 0,
    val unlockedIds: Set<String> = emptySet(),
    val creditedRunIds: Set<String> = emptySet(),
) {
    init {
        require(metaCurrency >= 0) { "Meta currency cannot be negative." }
        require(unlockedIds.all { it.isNotBlank() }) { "Meta unlock ids cannot be blank." }
        require(creditedRunIds.all { it.isNotBlank() }) { "Credited run ids cannot be blank." }
    }

    val normalizedUnlockIds: Set<String> get() = unlockedIds.toSortedSet()
    val normalizedCreditedRunIds: Set<String> get() = creditedRunIds.toSortedSet()
}

/** Independently versioned profile codec. It never changes the run-save version. */
object MetaProgressionCodec {
    const val PROFILE_VERSION: Int = 1

    fun encode(profile: MetaProgressionProfile): String = buildString {
        // Keep profile bytes stable: Properties.store adds a wall-clock timestamp.
        append("profileVersion=").append(PROFILE_VERSION).append('\n')
        append("metaCurrency=").append(profile.metaCurrency).append('\n')
        append("unlockedIds=")
            .append(profile.normalizedUnlockIds.joinToString(",", transform = ::encodeToken))
            .append('\n')
        append("creditedRunIds=")
            .append(profile.normalizedCreditedRunIds.joinToString(",", transform = ::encodeToken))
            .append('\n')
    }

    fun decode(text: String): MetaProgressionProfile {
        val props = Properties().also { it.load(StringReader(text)) }
        val version = props.getProperty("profileVersion")?.toIntOrNull()
            ?: error("Meta profile is missing a numeric profileVersion.")
        require(version in 1..PROFILE_VERSION) { "Unsupported meta profile version '$version'." }
        val currency = props.getProperty("metaCurrency")?.toIntOrNull()
            ?: error("Meta profile is missing a numeric metaCurrency.")
        require(currency >= 0) { "Meta profile metaCurrency cannot be negative." }
        val encodedIds = props.getProperty("unlockedIds", "")
        val ids = encodedIds.split(',')
            .filter { it.isNotBlank() }
            .map(::decodeToken)
        require(ids.size == ids.toSet().size) { "Meta profile contains duplicate unlock ids." }
        val encodedRunIds = props.getProperty("creditedRunIds", "")
        val runIds = encodedRunIds.split(',')
            .filter { it.isNotBlank() }
            .map(::decodeToken)
        require(runIds.size == runIds.toSet().size) { "Meta profile contains duplicate credited run ids." }
        return MetaProgressionProfile(currency, ids.toSortedSet(), runIds.toSortedSet())
    }

    private fun encodeToken(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodeToken(value: String): String = try {
        Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8).also {
            require(it.isNotBlank()) { "Meta profile contains a blank unlock id." }
        }
    } catch (error: Exception) {
        throw IllegalArgumentException("Invalid meta profile unlock id encoding.", error)
    }
}

/** Small Android-free profile service used by a game shell around completed runs. */
class MetaProgressionStore(
    profile: MetaProgressionProfile = MetaProgressionProfile(),
) {
    var profile: MetaProgressionProfile = profile
        private set

    fun recordCompletedRun(runId: String, summary: RunSummary, registry: ContentRegistry): Int {
        require(runId.isNotBlank()) { "Completed run id cannot be blank." }
        if (runId in profile.creditedRunIds) return 0
        val currencyResourceId = registry.metaProgression?.currencyResourceId ?: return 0
        val earned = summary.resources[currencyResourceId]?.takeIf { it > 0 } ?: 0
        profile = profile.copy(
            metaCurrency = if (earned > 0) Math.addExact(profile.metaCurrency, earned) else profile.metaCurrency,
            creditedRunIds = profile.creditedRunIds + runId,
        )
        return earned
    }

    fun recordCompletedRun(runId: String, run: RunState, registry: ContentRegistry): Int {
        require(run.isTerminal) { "Only terminal runs can award meta currency." }
        return recordCompletedRun(runId, requireNotNull(run.summary), registry)
    }

    fun unlock(unlockId: String, registry: ContentRegistry): Boolean {
        require(unlockId.isNotBlank()) { "Meta unlock id cannot be blank." }
        require(registry.metaProgression?.unlockables?.containsKey(unlockId) == true) {
            "Unknown meta unlock '$unlockId'."
        }
        if (unlockId in profile.unlockedIds) return false
        profile = profile.copy(unlockedIds = profile.unlockedIds + unlockId)
        return true
    }

    fun hasUnlock(unlockId: String): Boolean = unlockId in profile.unlockedIds

    fun save(): String = MetaProgressionCodec.encode(profile)

    companion object {
        fun restore(text: String): MetaProgressionStore = MetaProgressionStore(MetaProgressionCodec.decode(text))
    }
}
