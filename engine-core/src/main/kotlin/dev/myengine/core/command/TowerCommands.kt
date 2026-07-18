package dev.myengine.core.command

import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.core.Tick

/** Render-free tile coordinate used by commands crossing the simulation boundary. */
data class TileCoordinate(
    val x: Int,
    val y: Int,
)

data class BuildTowerCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    val towerId: String,
    val position: TileCoordinate,
    override val actorId: Long? = null,
) : EngineCommand {
    override val type: String = "build_tower"
    override fun stablePayload(): String = "$towerId:${position.x}:${position.y}"
}

data class UpgradeTowerCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    val towerEntityId: Long,
    val branch: String,
    val tier: Int,
    override val actorId: Long? = null,
) : EngineCommand {
    init {
        require(towerEntityId > 0) { "Tower entity id must be positive." }
        require(branch.matches(BRANCH_ID_REGEX)) { "Upgrade branch must match ${BRANCH_ID_REGEX.pattern}." }
        require(tier > 0) { "Upgrade tier must be positive." }
    }

    override val type: String = "upgrade_tower"
    override fun stablePayload(): String = "$towerEntityId:$branch:$tier"

    companion object {
        val BRANCH_ID_REGEX: Regex = Regex("[A-Za-z0-9_-]+")
    }
}
