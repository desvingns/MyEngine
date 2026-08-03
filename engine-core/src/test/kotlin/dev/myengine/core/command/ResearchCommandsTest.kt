package dev.myengine.core.command

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResearchCommandsTest {
    @Test
    fun researchCommandHasStableTypeAndPayload() {
        val command = ResearchCommand(CommandId(4), Tick(9), "foundation", actorId = 12L)

        assertEquals("research", command.type)
        assertEquals("foundation", command.stablePayload())
        assertEquals(12L, command.actorId)
    }

    @Test
    fun blankNodeIdIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            ResearchCommand(CommandId(1), Tick(1), " ")
        }
    }
}
