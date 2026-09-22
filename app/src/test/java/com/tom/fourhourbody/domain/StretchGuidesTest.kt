package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.stretch.StretchGuides
import org.junit.Assert.assertTrue
import org.junit.Test

class StretchGuidesTest {

    /**
     * Mirrors the stretch names in DatabaseSeeder. The seeder itself pulls in Room, which this
     * test source set deliberately does not, so the coupling is checked by listing them here —
     * adding a stretch to the seeder without a guide fails this test.
     */
    private val seededStretches = listOf(
        "Hip flexor stretch",
        "Double-leg glute bridge",
        "Single-leg glute bridge",
        "Super quad (couch) stretch",
        "Pelvic symmetry / glute flexibility",
        "Pelvis repositioning",
        "Static Back",
        "Static Extension on Elbows",
        "Shoulder Bridge with Pillow",
        "Active Bridges with Pillow",
        "Supine Groin Progressive",
        "Air Bench"
    )

    @Test
    fun `every stretch the app seeds has a guide`() {
        val missing = seededStretches.filterNot(StretchGuides::has)
        assertTrue("No guide for: $missing", missing.isEmpty())
    }

    @Test
    fun `no guide exists for a stretch that is not seeded`() {
        val orphans = StretchGuides.documented - seededStretches.toSet()
        assertTrue("Guide with no stretch: $orphans", orphans.isEmpty())
    }

    @Test
    fun `every guide says how to set up, what to do, and what to watch for`() {
        seededStretches.forEach { name ->
            val guide = StretchGuides[name]!!
            assertTrue("$name: no setup steps", guide.setup.isNotEmpty())
            assertTrue("$name: no execution steps", guide.execution.isNotEmpty())
            assertTrue("$name: nothing to watch for", guide.watchFor.isNotBlank())
            (guide.setup + guide.execution).forEach { line ->
                assertTrue("$name: blank step", line.isNotBlank())
            }
        }
    }

    @Test
    fun `an unknown stretch returns nothing rather than throwing`() {
        assertTrue(StretchGuides["Not a stretch"] == null)
        assertTrue(!StretchGuides.has(""))
    }
}
