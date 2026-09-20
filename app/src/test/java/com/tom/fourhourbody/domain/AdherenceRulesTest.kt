package com.tom.fourhourbody.domain

import com.tom.fourhourbody.data.entity.CreatineLogEntity
import com.tom.fourhourbody.data.entity.DietDayLogEntity
import com.tom.fourhourbody.data.entity.DietMode
import com.tom.fourhourbody.data.entity.SleepLogEntity
import com.tom.fourhourbody.domain.adherence.AdherenceRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AdherenceRulesTest {

    private val date = LocalDate.of(2026, 5, 4)

    private fun dietDay(
        cheat: Boolean = false,
        white: Boolean = true,
        liquid: Boolean = true,
        fruit: Boolean = true
    ) = DietDayLogEntity(
        date = date,
        mode = DietMode.SLOW_CARB,
        avoidedWhiteCarbs = white,
        noLiquidCalories = liquid,
        noFruit = fruit,
        isCheatDay = cheat
    )

    @Test
    fun `a logged cheat day counts as on plan`() {
        assertTrue(AdherenceRules.isDietDayCompliant(dietDay(cheat = true, white = false)))
    }

    @Test
    fun `a normal day needs all three rules`() {
        assertTrue(AdherenceRules.isDietDayCompliant(dietDay()))
        assertFalse(AdherenceRules.isDietDayCompliant(dietDay(fruit = false)))
    }

    @Test
    fun `sleep counts socks in place of the temperature check`() {
        val log = SleepLogEntity(
            date = date,
            roomTempOk = false,
            socksUsed = true,
            darkness = true,
            noScreensBeforeBed = true,
            wineWithinLimit = true,
            consistentWakeTime = false
        )
        assertEquals(4, AdherenceRules.sleepChecksPassed(log))
        assertTrue(AdherenceRules.isSleepNightCompliant(log))
    }

    @Test
    fun `three of five checks is not an adherent night`() {
        val log = SleepLogEntity(
            date = date,
            roomTempOk = true,
            darkness = true,
            noScreensBeforeBed = true
        )
        assertEquals(3, AdherenceRules.sleepChecksPassed(log))
        assertFalse(AdherenceRules.isSleepNightCompliant(log))
    }

    @Test
    fun `percentages are capped at a hundred`() {
        val cold = AdherenceRules.cold(sessions = 20, targetPerWeek = 2, windowDays = 7)
        assertEquals(100, cold.percent)
    }

    @Test
    fun `an empty denominator reads as zero rather than dividing`() {
        val creatine = AdherenceRules.creatine(emptyList(), cycleDaysInWindow = 0)
        assertEquals(0, creatine.percent)
        assertEquals("No cycle running", creatine.detail)
    }

    @Test
    fun `creatine needs both doses to count the day`() {
        val logs = listOf(
            CreatineLogEntity(date = date, morningTaken = true, eveningTaken = true, cycleDay = 1),
            CreatineLogEntity(date = date.plusDays(1), morningTaken = true, eveningTaken = false, cycleDay = 2)
        )
        assertEquals(1, AdherenceRules.creatine(logs, cycleDaysInWindow = 2).completed)
        assertEquals(50, AdherenceRules.creatine(logs, cycleDaysInWindow = 2).percent)
    }

    @Test
    fun `training is measured against the sessions the current gap allows`() {
        val adherence = AdherenceRules.training(completedSessions = 5, expectedSessions = 7)
        assertEquals(71, adherence.percent)
    }
}
