package dev.myengine.games.sandbox

import dev.myengine.content.ContentRegistry
import dev.myengine.content.TechNodeContent
import dev.myengine.content.TechUnlockRef
import dev.myengine.content.TechUnlockType
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.PlaceBlueprintCommand
import dev.myengine.core.command.PlaceBuildingCommand
import dev.myengine.core.command.ResearchCommand
import dev.myengine.core.command.TileCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SandboxResearchTest {
    @Test
    fun researchSpendsAtomicallyAndGatesTowerAndBlueprint() {
        val registry = registryWithTech(
            TechNodeContent(
                id = "engineering",
                costResource = "bolt",
                costAmount = 2,
                unlocks = listOf(
                    TechUnlockRef(TechUnlockType.TOWER, "pulse"),
                    TechUnlockRef(TechUnlockType.BUILDING, "wall"),
                ),
            ),
        )
        val runtime = SandboxGame.createRuntime(registry)

        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(30, 32)))
        runtime.submit(PlaceBlueprintCommand(CommandId(2), Tick(1), "wall", TileCoordinate(4, 1)))
        runtime.step()

        assertTrue(runtime.state.lastCommandOrError in setOf("locked_tower:pulse", "locked_building:wall"))
        assertTrue(runtime.state.entities.byTag("tower").isEmpty())
        assertTrue(runtime.state.constructionSites.all().isEmpty())
        assertEquals(6, runtime.state.inventory.amount("bolt"))

        runtime.submit(ResearchCommand(CommandId(3), Tick(2), "engineering"))
        runtime.submit(BuildTowerCommand(CommandId(4), Tick(2), "pulse", TileCoordinate(30, 32)))
        runtime.submit(PlaceBlueprintCommand(CommandId(5), Tick(2), "wall", TileCoordinate(4, 1)))
        runtime.step()

        assertEquals(setOf("engineering"), runtime.state.researchedTechIds)
        assertEquals(1, runtime.state.entities.byTag("tower").size)
        assertEquals(1, runtime.state.constructionSites.all().size)
        assertEquals(1, runtime.state.inventory.amount("bolt"))
    }

    @Test
    fun unavailableRecipeDoesNotAdvanceUntilResearchIsApplied() {
        val registry = registryWithTech(
            TechNodeContent(
                id = "automation",
                costResource = "bolt",
                costAmount = 1,
                unlocks = listOf(TechUnlockRef(TechUnlockType.RECIPE, "bolt-generator")),
            ),
        )
        val runtime = SandboxGame.createRuntime(registry)

        runtime.step()
        assertEquals(0, runtime.state.producers.single().progressTicks)
        assertEquals(6, runtime.state.inventory.amount("bolt"))

        runtime.submit(ResearchCommand(CommandId(10), Tick(2), "automation"))
        runtime.step()
        assertEquals(1, runtime.state.producers.single().progressTicks)
        assertEquals(5, runtime.state.inventory.amount("bolt"))
    }

    @Test
    fun immediateBuildingPlacementAndSnapshotReflectResearchState() {
        val registry = registryWithTech(
            TechNodeContent(
                id = "masonry",
                costResource = "bolt",
                costAmount = 1,
                unlocks = listOf(TechUnlockRef(TechUnlockType.BUILDING, "wall")),
            ),
        )
        val runtime = SandboxGame.createRuntime(registry)

        val before = runtime.snapshot().techTree.nodes.single()
        assertEquals(false, before.researched)
        assertEquals(true, before.available)

        runtime.submit(ResearchCommand(CommandId(20), Tick(1), "masonry"))
        runtime.submit(PlaceBuildingCommand(CommandId(21), Tick(2), "wall", TileCoordinate(4, 1)))
        runtime.step(2)

        val after = runtime.snapshot().techTree.nodes.single()
        assertEquals(true, after.researched)
        assertEquals(false, after.available)
        assertEquals(1, runtime.state.entities.byTag("building").size)
    }

    @Test
    fun researchSaveMigrationPendingCommandAndSplitReplayAreStable() {
        val registry = registryWithTech(
            TechNodeContent("engineering", "bolt", 2, unlocks = listOf(TechUnlockRef(TechUnlockType.TOWER, "pulse"))),
        )
        val command = ResearchCommand(CommandId(7), Tick(6), "engineering", actorId = 9L)

        val uninterrupted = SandboxGame.createRuntime(registry)
        uninterrupted.submit(command)
        uninterrupted.step(12)

        val paused = SandboxGame.createRuntime(registry)
        paused.submit(command)
        paused.step(4)
        val save = SandboxSaveCodec.encode(paused.state, seed = 7, pendingCommands = paused.pendingCommands())
        val restored = SandboxSaveCodec.decode(save, registry)
        val resumed = SandboxRuntime(restored, seed = 7)
        assertEquals(listOf(command), SandboxSaveCodec.decodePendingCommands(save))
        resumed.restorePendingCommands(SandboxSaveCodec.decodePendingCommands(save))
        resumed.step(8)

        assertEquals(uninterrupted.state.stableHash(), resumed.state.stableHash())
        assertEquals(setOf("engineering"), resumed.state.researchedTechIds)

        val v19ResearchSave = SandboxSaveCodec.encode(uninterrupted.state, seed = 7)
        assertTrue(v19ResearchSave.lines().any { it.startsWith("researchedTechIds=") && it != "researchedTechIds=" })
        val legacy = v19ResearchSave.replace("saveVersion=19", "saveVersion=17")
        assertTrue(SandboxSaveCodec.decode(legacy, registry).researchedTechIds.isEmpty())

        val invalid = v19ResearchSave.replace(
            Regex("researchedTechIds=.*"),
            "researchedTechIds=${java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("unknown".toByteArray())}",
        )
        assertFailsWith<IllegalArgumentException> { SandboxSaveCodec.decode(invalid, registry) }

        val delimiterNode = TechNodeContent("node|;unsafe", "bolt", 1)
        val delimiterRegistry = registry.copy(techNodes = mapOf(delimiterNode.id to delimiterNode))
        val delimiterSave = SandboxSaveCodec.encode(
            SandboxGame.createInitialState(delimiterRegistry),
            seed = 7,
            pendingCommands = listOf(ResearchCommand(CommandId(8), Tick(4), delimiterNode.id)),
        )
        assertEquals(
            ResearchCommand(CommandId(8), Tick(4), delimiterNode.id),
            SandboxSaveCodec.decodePendingCommands(delimiterSave).single(),
        )
    }

    private fun registryWithTech(node: TechNodeContent): ContentRegistry =
        SandboxGame.loadRegistry().copy(techNodes = mapOf(node.id to node))
}
