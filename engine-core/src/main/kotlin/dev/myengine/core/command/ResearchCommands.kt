package dev.myengine.core.command

import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.core.Tick

/** Render-free request to atomically research one content-defined technology node. */
data class ResearchCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    val nodeId: String,
    override val actorId: Long? = null,
) : EngineCommand {
    init {
        require(nodeId.isNotBlank()) { "Research node id cannot be blank." }
    }

    override val type: String = "research"
    override fun stablePayload(): String = nodeId
}
