package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.today.Focus
import com.tom.fourhourbody.domain.today.FocusInputs
import com.tom.fourhourbody.domain.today.FocusRules
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusRulesTest {

    @Test
    fun `a due session is the focus`() {
        assertEquals(Focus.TRAIN, FocusRules.pick(FocusInputs(sessionDueToday = true)))
    }

    @Test
    fun `a session already done today stops being the focus`() {
        assertEquals(
            Focus.CLEAR,
            FocusRules.pick(FocusInputs(sessionDueToday = true, sessionCompletedToday = true))
        )
    }

    @Test
    fun `nothing due reads as clear`() {
        assertEquals(Focus.CLEAR, FocusRules.pick(FocusInputs()))
    }
}
