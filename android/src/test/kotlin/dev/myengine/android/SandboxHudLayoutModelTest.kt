package dev.myengine.android

import dev.myengine.render.ScreenPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SandboxHudLayoutModelTest {
    private data class Case(
        val density: Float,
        val widthDp: Float,
        val heightDp: Float,
        val sideBySide: Boolean,
    )

    private val cases = listOf(
        Case(density = 1f, widthDp = 360f, heightDp = 900f, sideBySide = false),
        Case(density = 1f, widthDp = 800f, heightDp = 900f, sideBySide = true),
        Case(density = 2f, widthDp = 360f, heightDp = 900f, sideBySide = false),
        Case(density = 2f, widthDp = 800f, heightDp = 900f, sideBySide = true),
    )

    private val compactCases = listOf(
        Case(density = 1f, widthDp = 200f, heightDp = 600f, sideBySide = false),
        Case(density = 1f, widthDp = 220f, heightDp = 640f, sideBySide = false),
        Case(density = 1f, widthDp = 320f, heightDp = 720f, sideBySide = false),
    )

    @Test
    fun `mdpi and xhdpi layouts keep touch rows and panel chrome valid`() {
        cases.forEach { case ->
            val layout = calculate(case)
            val selectedPanel = assertNotNull(layout.selectedPanel)
            val selectedHeader = assertNotNull(layout.selectedHeader)
            val minimumTouchHeight = 48f * case.density

            assertEquals(PresentationSpeed.entries.size, layout.speedControls.size)
            layout.speedControls.zipWithNext().forEach { (left, right) ->
                assertFalse(left.overlaps(right))
            }
            layout.speedControls.forEach { speedBounds ->
                assertFalse(speedBounds.overlaps(layout.buildPanel))
                assertFalse(selectedPanel.overlaps(speedBounds))
            }

            (layout.buildRows + layout.upgradeRows).forEach { row ->
                assertTrue(row.bottom - row.top >= minimumTouchHeight)
            }
            assertFalse(layout.buildPanel.overlaps(selectedPanel))
            assertTrue(layout.buildPanel.contains(layout.buildHeader))
            layout.buildRows.forEach { assertTrue(layout.buildPanel.contains(it)) }
            assertTrue(selectedPanel.contains(selectedHeader))
            layout.selectedInfoRows.forEach { assertTrue(selectedPanel.contains(it)) }
            layout.upgradeRows.forEach { assertTrue(selectedPanel.contains(it)) }

            if (case.sideBySide) {
                assertTrue(selectedPanel.left > layout.buildPanel.right)
            } else {
                assertTrue(selectedPanel.top > layout.buildPanel.bottom)
            }
        }
    }

    @Test
    fun `representative upgrade point never hits a build row`() {
        cases.forEach { case ->
            val layout = calculate(case)
            val upgrade = layout.upgradeRows.first()
            val point = ScreenPoint(
                x = (upgrade.left + upgrade.right) / 2f,
                y = (upgrade.top + upgrade.bottom) / 2f,
            )

            assertTrue(upgrade.contains(point))
            assertTrue(layout.buildRows.none { it.contains(point) })
        }
    }

    @Test
    fun `compact widths keep speed grid positive inside viewport and clear of panels`() {
        compactCases.forEach { case ->
            val width = case.widthDp * case.density
            val height = case.heightDp * case.density
            val layout = calculate(case)
            val viewport = HudBounds(0f, 0f, width, height)
            val contentWidth = width - layout.padding * 2f
            val minimumSpeedWidth = 48f * case.density
            val speedGap = 4f * case.density
            val gridCanFitMinimumWidth =
                contentWidth >= minimumSpeedWidth * PresentationSpeed.entries.size + speedGap * 3f

            assertEquals(PresentationSpeed.entries.size, layout.speedControls.size)
            layout.speedControls.forEach { speedBounds ->
                assertTrue(speedBounds.right > speedBounds.left)
                assertTrue(speedBounds.bottom > speedBounds.top)
                assertTrue(viewport.contains(speedBounds))
                assertFalse(speedBounds.overlaps(layout.buildPanel))
                assertFalse(layout.selectedPanel?.overlaps(speedBounds) == true)
                if (gridCanFitMinimumWidth) {
                    assertTrue(speedBounds.right - speedBounds.left >= minimumSpeedWidth)
                }
            }
            layout.speedControls.zipWithNext().forEach { (left, right) ->
                assertFalse(left.overlaps(right))
            }
            assertFalse(layout.buildPanel.overlaps(layout.selectedPanel!!))
        }
    }

    private fun calculate(case: Case): SandboxHudLayout = SandboxHudLayoutModel.calculate(
        viewWidth = case.widthDp * case.density,
        viewHeight = case.heightDp * case.density,
        density = case.density,
        buildRowCount = 3,
        upgradeRowCount = 2,
        hasSelection = true,
    )

    private fun HudBounds.contains(other: HudBounds): Boolean =
        other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom

    private fun HudBounds.overlaps(other: HudBounds): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top
}
