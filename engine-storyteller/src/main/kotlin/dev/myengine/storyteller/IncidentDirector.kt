package dev.myengine.storyteller

import dev.myengine.content.IncidentContent
import dev.myengine.core.SeededRandom

data class IncidentSelection(
    val incidentId: String,
    val threat: Int,
)

class IncidentDirector(private val incidents: Collection<IncidentContent>) {
    fun select(threatBudget: Int, random: SeededRandom): IncidentSelection? {
        val eligible = incidents
            .filter { threatBudget in it.minThreat..it.maxThreat }
            .sortedBy { it.id }
        if (eligible.isEmpty()) return null

        val totalWeight = eligible.sumOf { it.weight }
        var cursor = random.nextInt(totalWeight)
        val selected = eligible.first { incident ->
            cursor -= incident.weight
            cursor < 0
        }
        return IncidentSelection(selected.id, threatBudget)
    }
}
