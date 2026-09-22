package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.adherence.AdherenceRules
import org.junit.Assert.assertEquals
import org.junit.Test

class AdherenceRulesTest {

    @Test
    fun `training is measured against the sessions the current gap allows`() {
        val adherence = AdherenceRules.training(completedSessions = 5, expectedSessions = 7)
        assertEquals(71, adherence.percent)
    }

    @Test
    fun `percentages are capped at a hundred`() {
        val adherence = AdherenceRules.training(completedSessions = 9, expectedSessions = 4)
        assertEquals(100, adherence.percent)
    }

    @Test
    fun `an empty denominator reads as zero rather than dividing`() {
        val adherence = AdherenceRules.training(completedSessions = 0, expectedSessions = 0)
        assertEquals(0, adherence.percent)
    }
}
