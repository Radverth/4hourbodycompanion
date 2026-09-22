package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.progress.Attributes
import com.tom.fourhourbody.domain.progress.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttributesTest {

    private val stats = Stats(
        sessionsCompleted = 14,
        runsCompleted = 2,
        totalBankedKg = 46.0,
        bestGainOnOneLiftKg = 34.0
    )

    @Test
    fun `every attribute is a count already in the log`() {
        val attrs = Attributes.of(stats, adherencePercent = 71)
        assertEquals(listOf(46, 14, 71, 34), attrs.map { it.value })
    }

    @Test
    fun `every attribute names where its number came from`() {
        Attributes.of(stats, adherencePercent = 71).forEach { attribute ->
            assertTrue(attribute.name, attribute.source.isNotBlank())
            assertTrue(attribute.name, attribute.name.isNotBlank())
        }
    }

    @Test
    fun `an empty log reads as zeroes rather than as missing`() {
        val attrs = Attributes.of(Stats(), adherencePercent = 0)
        assertEquals(4, attrs.size)
        assertTrue(attrs.all { it.value == 0 })
        assertTrue(attrs.all { it.source.isNotBlank() })
    }

    @Test
    fun `banked weight rounds down rather than flattering the number`() {
        val attrs = Attributes.of(stats.copy(totalBankedKg = 46.9), adherencePercent = 0)
        assertEquals(46, attrs.first().value)
    }
}
