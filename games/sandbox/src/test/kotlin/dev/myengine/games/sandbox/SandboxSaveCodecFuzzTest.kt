package dev.myengine.games.sandbox

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.CallWaveEarlyCommand
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SandboxSaveCodecFuzzTest {
    @Test
    fun fixedSeedValidStatesRoundTripWithStableHashAndPendingCommands() {
        val registry = SandboxGame.loadRegistry()
        VALID_STATE_CASES.forEachIndexed { index, (seed, ticks) ->
            val first = makeSession(registry, seed, ticks, index)
            val second = makeSession(registry, seed, ticks, index)
            val save = first.save()
            val restored = SandboxSession.restore(save, registry)

            assertEquals(
                first.stableHash(),
                second.stableHash(),
                "State generation is not deterministic for seed=$seed ticks=$ticks",
            )
            assertEquals(
                first.stableHash(),
                restored.stableHash(),
                "Save round-trip changed stableHash for seed=$seed ticks=$ticks",
            )
            assertEquals(
                first.runtime.pendingCommands(),
                restored.runtime.pendingCommands(),
                "Pending commands changed for seed=$seed ticks=$ticks",
            )
            assertEquals(
                first.runtime.pendingCommands(),
                SandboxSaveCodec.decodePendingCommands(save),
                "decodePendingCommands changed for seed=$seed ticks=$ticks",
            )
        }
    }

    @Test
    fun boundedFixedSeedSaveCorruptionFuzzRejectsWithTypedFailures() {
        val registry = SandboxGame.loadRegistry()
        val base = makeSession(registry, seed = 31L, ticks = 4, commandIndex = 99).save()
        val mutations = saveCorruptionMutations()
        val cases = mutations + List(EXTRA_CORRUPTION_CASES) { index ->
            val random = Random(CORRUPTION_SEED + index)
            mutations[random.nextInt(mutations.size)]
        }

        cases.forEachIndexed { caseIndex, mutation ->
            val seed = CORRUPTION_SEED + caseIndex
            val corrupted = mutation.apply(base, Random(seed))
            assertEquals(
                corrupted,
                mutation.apply(base, Random(seed)),
                "Corruption mutation is not deterministic for seed=$seed mutation=${mutation.id}",
            )
            val decodeFailure = runCatching { SandboxSaveCodec.decode(corrupted, registry) }.exceptionOrNull()
            if (mutation.decodeMustReject) {
                assertTypedFailure(
                    decodeFailure,
                    "decode seed=$seed mutation=${mutation.id}",
                )
            } else {
                decodeFailure?.let {
                    assertTypedFailure(it, "decode seed=$seed mutation=${mutation.id}")
                }
            }

            val restoreFailure = runCatching { SandboxSession.restore(corrupted, registry) }.exceptionOrNull()
            assertTypedFailure(
                restoreFailure,
                "restore seed=$seed mutation=${mutation.id}",
            )

            val pendingFailure = runCatching { SandboxSaveCodec.decodePendingCommands(corrupted) }.exceptionOrNull()
            pendingFailure?.let {
                assertTypedFailure(it, "pending seed=$seed mutation=${mutation.id}")
            }
        }
    }

    private fun makeSession(
        registry: dev.myengine.content.ContentRegistry,
        seed: Long,
        ticks: Int,
        commandIndex: Int,
    ): SandboxSession = SandboxSession.start(registry, seed = seed).also { session ->
        session.submit(
            CallWaveEarlyCommand(
                id = CommandId(commandIndex.toLong() + 1000L),
                scheduledTick = Tick(ticks.toLong() + 5L),
                actorId = commandIndex.toLong(),
            ),
        )
        session.step(ticks)
    }

    private fun assertTypedFailure(failure: Throwable?, context: String) {
        val actual = assertNotNull(failure, "Corruption was accepted: $context")
        assertTrue(
            actual is IllegalArgumentException || actual is IllegalStateException,
            "Unexpected ${actual::class.qualifiedName} for $context: ${actual.message}",
        )
    }

    private data class SaveMutation(
        val id: String,
        val decodeMustReject: Boolean = true,
        val apply: (String, Random) -> String,
    )

    private fun saveCorruptionMutations(): List<SaveMutation> = listOf(
        SaveMutation("future-version") { text, _ ->
            replaceProperty(text, "saveVersion", (SandboxSaveCodec.SAVE_VERSION + 1).toString())
        },
        SaveMutation("pack-identity") { text, _ ->
            replaceProperty(text, "packId", "other-pack")
        },
        SaveMutation("content-identity") { text, _ ->
            replaceProperty(text, "contentVersion", "other-version")
        },
        SaveMutation("map-identity") { text, _ ->
            replaceProperty(text, "mapId", "missing-map")
        },
        SaveMutation("run-status") { text, _ ->
            replaceProperty(text, "runStatus", "UNKNOWN")
        },
        SaveMutation("core-health-number") { text, random ->
            replaceProperty(text, "coreHealth", "broken-${random.nextInt()}")
        },
        SaveMutation("random-cursor-number") { text, random ->
            replaceProperty(text, "randomCursor", "broken-${random.nextInt()}")
        },
        SaveMutation("metrics-delimiter") { text, _ ->
            replaceProperty(text, "metrics", "not-a-number,0,0,0,0")
        },
        SaveMutation("inventory-delimiter") { text, _ ->
            replaceProperty(text, "inventory", "bolt:not-a-number")
        },
        SaveMutation("producer-delimiter") { text, _ ->
            replaceProperty(text, "producers", "producer|recipe|not-a-number")
        },
        SaveMutation("entity-delimiter") { text, _ ->
            replaceProperty(text, "entities", "not-a-number|enemy:spark")
        },
        SaveMutation("incident-delimiter") { text, _ ->
            replaceProperty(text, "incidentLastSelection", "broken")
        },
        SaveMutation("belt-delimiter") { text, _ ->
            replaceProperty(text, "belts", "broken")
        },
        SaveMutation("pending-command-delimiter", decodeMustReject = false) { text, _ ->
            replaceProperty(text, "pendingCommands", "call_wave_early|not-a-number|0||")
        },
        SaveMutation("pending-command-base64", decodeMustReject = false) { text, _ ->
            replaceProperty(text, "pendingCommands", "research|1|5||not-base64!")
        },
        SaveMutation("researched-tech-base64") { text, _ ->
            replaceProperty(text, "researchedTechIds", "not-base64!")
        },
    )

    private fun replaceProperty(text: String, key: String, value: String): String {
        val property = Regex("(?m)^${Regex.escape(key)}=.*$")
        check(property.containsMatchIn(text)) { "Missing save property '$key'." }
        return property.replace(text) { "$key=$value" }
    }

    private companion object {
        const val CORRUPTION_SEED: Int = 0xC0DEC07
        const val EXTRA_CORRUPTION_CASES: Int = 16
        const val VALID_STATE_SEED: Int = 0xD1CE007
        const val EXTRA_VALID_STATE_CASES: Int = 12
        val VALID_STATE_CASES = buildList {
            addAll(
                listOf(
                    7L to 0,
                    7L to 3,
                    19L to 1,
                    19L to 8,
                    31L to 4,
                    47L to 6,
                ),
            )
            val random = Random(VALID_STATE_SEED)
            repeat(EXTRA_VALID_STATE_CASES) {
                add(random.nextInt(1, 10_000).toLong() to random.nextInt(0, 12))
            }
        }
    }
}
