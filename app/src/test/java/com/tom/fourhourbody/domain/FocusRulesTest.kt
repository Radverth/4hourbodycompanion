package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.today.Focus
import com.tom.fourhourbody.domain.today.FocusInputs
import com.tom.fourhourbody.domain.today.FocusRules
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusRulesTest {

    @Test
    fun `a due session outranks everything else`() {
        assertEquals(
            Focus.TRAIN,
            FocusRules.pick(
                FocusInputs(
                    trainingEnabled = true,
                    sessionDueToday = true,
                    comboHalfOpen = true,
                    nutritionEnabled = true
                )
            )
        )
    }

    @Test
    fun `a session already done today stops being the focus`() {
        assertEquals(
            Focus.LOG_DAY,
            FocusRules.pick(
                FocusInputs(
                    trainingEnabled = true,
                    sessionDueToday = true,
                    sessionCompletedToday = true,
                    nutritionEnabled = true
                )
            )
        )
    }

    @Test
    fun `training switched off never takes the focus`() {
        assertEquals(
            Focus.CLEAR,
            FocusRules.pick(FocusInputs(trainingEnabled = false, sessionDueToday = true))
        )
    }

    @Test
    fun `a half-open combo beats an unlogged day, because half of it is already spent`() {
        assertEquals(
            Focus.COMBO,
            FocusRules.pick(FocusInputs(comboHalfOpen = true, nutritionEnabled = true))
        )
    }

    @Test
    fun `an unlogged day is the focus when nothing else is asking`() {
        assertEquals(Focus.LOG_DAY, FocusRules.pick(FocusInputs(nutritionEnabled = true)))
    }

    @Test
    fun `everything done reads as clear rather than inventing something to ask for`() {
        assertEquals(
            Focus.CLEAR,
            FocusRules.pick(
                FocusInputs(
                    trainingEnabled = true,
                    sessionDueToday = true,
                    sessionCompletedToday = true,
                    nutritionEnabled = true,
                    dayLogged = true
                )
            )
        )
        assertEquals(Focus.CLEAR, FocusRules.pick(FocusInputs()))
    }
}
