package dev.myengine.core.command

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TowerCommandsTest {
    @Test
    fun buildTowerCommandExposesStableIdentityAndPayload() {
        val command = BuildTowerCommand(
            id = CommandId(7),
            scheduledTick = Tick(12),
            towerId = "pulse",
            position = TileCoordinate(4, 5),
            actorId = 99L,
        )

        assertEquals(CommandId(7), command.id)
        assertEquals(Tick(12), command.scheduledTick)
        assertEquals(99L, command.actorId)
        assertEquals("build_tower", command.type)
        assertEquals("pulse:4:5", command.stablePayload())
    }

    @Test
    fun upgradeTowerCommandExposesStableIdentityAndPayload() {
        val command = UpgradeTowerCommand(
            id = CommandId(8),
            scheduledTick = Tick(20),
            towerEntityId = 123L,
            branch = "main_branch",
            tier = 2,
            actorId = 77L,
        )

        assertEquals(CommandId(8), command.id)
        assertEquals(Tick(20), command.scheduledTick)
        assertEquals(77L, command.actorId)
        assertEquals("upgrade_tower", command.type)
        assertEquals("123:main_branch:2", command.stablePayload())
    }

    @Test
    fun upgradeTowerCommandRejectsInvalidTargetsAndTier() {
        fun command(
            towerEntityId: Long = 1L,
            branch: String = "main",
            tier: Int = 1,
        ) = UpgradeTowerCommand(CommandId(1), Tick(1), towerEntityId, branch, tier)

        assertFailsWith<IllegalArgumentException> { command(towerEntityId = 0L) }
        assertFailsWith<IllegalArgumentException> { command(branch = "") }
        assertFailsWith<IllegalArgumentException> { command(branch = "main.branch") }
        assertFailsWith<IllegalArgumentException> { command(tier = 0) }
    }

    @Test
    fun towerCommandsRetainDeterministicQueueOrdering() {
        val queue = dev.myengine.core.CommandQueue()
        queue.submit(BuildTowerCommand(CommandId(9), Tick(2), "pulse", TileCoordinate(4, 5)))
        queue.submit(UpgradeTowerCommand(CommandId(3), Tick(2), 123L, "main", 1))
        queue.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(4, 5)))

        assertEquals(
            listOf(CommandId(1), CommandId(3), CommandId(9)),
            queue.drainFor(Tick(2)).map { it.id },
        )
    }

    @Test
    fun commandDtosDoNotImportRenderOrWorld() {
        val source = listOf(
            Paths.get("src/main/kotlin/dev/myengine/core/command/TowerCommands.kt"),
            Paths.get("engine-core/src/main/kotlin/dev/myengine/core/command/TowerCommands.kt"),
        ).firstOrNull { Files.exists(it) }
            ?: error("TowerCommands.kt source was not found from the test working directory")

        val text = Files.readString(source)
        assertTrue(text.contains("package dev.myengine.core.command"))
        assertFalse(Regex("^\\s*import\\s+dev\\.myengine\\.render\\.", RegexOption.MULTILINE).containsMatchIn(text))
        assertFalse(Regex("^\\s*import\\s+dev\\.myengine\\.world\\.", RegexOption.MULTILINE).containsMatchIn(text))
    }
}
