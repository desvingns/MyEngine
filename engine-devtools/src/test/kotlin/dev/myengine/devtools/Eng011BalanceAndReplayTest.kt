package dev.myengine.devtools

import dev.myengine.games.sandbox.SandboxGame
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Eng011BalanceAndReplayTest {
    @Test
    fun effectiveDpsMatrixIsStableOrderedAndIncludesBaseAndUpgradeProfiles() {
        val root = typedPack()
        val first = DevtoolReports.balanceDeltaReport(root, root)
        val second = DevtoolReports.balanceDeltaReport(root, root)
        val summary = requireNotNull(first.baseline)
        val repeated = requireNotNull(second.baseline)

        assertTrue(first.valid, first.errors.joinToString("\n"))
        assertEquals(summary, repeated)
        assertEquals(listOf("pulse", "pulse.upgrade.main.1"), summary.effectiveDpsRows.map { it.profileId })
        assertEquals(listOf("drift", "drift"), summary.effectiveDpsRows.map { it.enemyId })
        assertEquals(listOf(null, "main"), summary.effectiveDpsRows.map { it.upgradeBranch })
        assertEquals(listOf(0, 1), summary.effectiveDpsRows.map { it.upgradeTier })
        assertEquals(listOf(50, 50), summary.effectiveDpsRows.map { it.resistPercent })
        assertEquals(listOf(1, 2), summary.effectiveDpsRows.map { it.effectiveDamagePerShot })
        assertTrue(
            summary.effectiveDpsRows.map { it.effectiveDps }
                .zip(listOf(BigDecimal("10"), BigDecimal("40")))
                .all { (actual, expected) -> actual.compareTo(expected) == 0 },
        )
        assertTrue(summary.effectiveDpsRows.all { it.ticksPerSecond == 20 })

        val json = Json.parseToJsonElement(first.toJson()).jsonObject
        val baselineJson = json.getValue("baseline").jsonObject
        assertEquals("20", baselineJson.getValue("ticks_per_second").jsonPrimitive.content)
        assertEquals(
            setOf("targeting", "range", "splash", "ticks_per_second"),
            baselineJson.getValue("assumptions").jsonObject.keys,
        )
        assertEquals(2, baselineJson.getValue("effective_dps_rows").jsonArray.size)
    }

    @Test
    fun replayInspectPinsResistGoldenRepeatAndLegacyHashes() {
        val scenarios = Json.parseToJsonElement(DevtoolReports.replayInspect())
            .jsonObject
            .getValue("scenarios")
            .jsonArray
            .associateBy { it.jsonObject.getValue("scenario").jsonPrimitive.content }

        val canonical = scenarios.getValue("canonical").jsonObject
        val kill = scenarios.getValue("kill").jsonObject
        val resist = scenarios.getValue("resist").jsonObject

        assertEquals("e4892bcc18f9d8dc", canonical.getValue("final_hash").jsonPrimitive.content)
        assertEquals("a763da4ac32b15b4", kill.getValue("final_hash").jsonPrimitive.content)
        assertEquals("3f02607020d48668", resist.getValue("final_hash").jsonPrimitive.content)
        assertEquals("3f02607020d48668", resist.getValue("golden_hash").jsonPrimitive.content)
        assertEquals("true", resist.getValue("golden_match").jsonPrimitive.content)
        assertEquals("true", resist.getValue("repeat_stable").jsonPrimitive.content)
        assertEquals("true", resist.getValue("differs_from_zero_resist").jsonPrimitive.content)
    }

    private fun typedPack(): Path = Files.createTempDirectory("myengine-eng011-balance").also { root ->
        copyFlatPack(SandboxGame.contentRoot(), root)
        root.resolve("damage-types.properties").writeText("arcane.displayKey=damage.arcane\n")
        root.resolve("towers.properties").writeText(
            Files.readString(root.resolve("towers.properties")) + "\npulse.damageTypeId=arcane\n",
        )
        root.resolve("enemies.properties").writeText(
            Files.readString(root.resolve("enemies.properties")) + "\ndrift.resist.arcane=50\n",
        )
        root.resolve("strings.properties").writeText(
            Files.readString(root.resolve("strings.properties")) + "\ndamage.arcane=Arcane damage\n",
        )
    }

    private fun copyFlatPack(source: Path, target: Path) {
        Files.createDirectories(target)
        listOf(
            "manifest.properties",
            "tiles.properties",
            "resources.properties",
            "recipes.properties",
            "towers.properties",
            "enemies.properties",
            "waves.properties",
            "incidents.properties",
            "strings.properties",
        ).forEach { file ->
            Files.copy(source.resolve(file), target.resolve(file), REPLACE_EXISTING)
        }
        source.resolve("visuals").takeIf(Files::exists)?.let { visuals ->
            Files.walk(visuals).use { files ->
                files.filter(Files::isRegularFile).forEach { file ->
                    val destination = target.resolve(source.relativize(file).toString())
                    destination.parent?.let(Files::createDirectories)
                    Files.copy(file, destination, REPLACE_EXISTING)
                }
            }
        }
    }
}
