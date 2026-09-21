package com.lbs.schoolhelper.ui.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroModelTest {
    @Test
    fun `normalizes minutes and rounds to supported ranges`() {
        assertEquals(
            PomodoroSettings(focusMinutes = 1, shortBreakMinutes = 120, longBreakMinutes = 1, rounds = 4),
            PomodoroSettings(focusMinutes = 0, shortBreakMinutes = 999, longBreakMinutes = -5, rounds = 9).normalized()
        )
    }

    @Test
    fun `uses standard pomodoro defaults`() {
        assertEquals(PomodoroSettings(25, 5, 10, 4), PomodoroSettings())
    }
}
