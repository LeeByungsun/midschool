package com.lbs.schoolhelper.ui.timer

import android.app.Application
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.repository.TimerPreferenceState
import com.lbs.schoolhelper.telemetry.AppTelemetry
import com.lbs.schoolhelper.telemetry.TimerAction
import com.lbs.schoolhelper.test.FakePreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class TimerViewModelTest {
    private val application: Application = RuntimeEnvironment.getApplication()

    @Test
    fun `timer controls expose expected labels`() {
        assertEquals("시작", application.getString(R.string.home_timer_start))
        assertEquals("일시정지", application.getString(R.string.home_timer_pause))
        assertEquals("이어하기", application.getString(R.string.home_timer_resume))
    }

    @Test
    fun `screen view models share one session controller state`() {
        val controller = TimerSessionController(application, FakePreferencesRepository())
        val homeViewModel = TimerViewModel(controller)
        val detailViewModel = TimerViewModel(controller)

        homeViewModel.toggleTimer()

        assertTrue(homeViewModel.uiState.value.isRunning)
        assertTrue(detailViewModel.uiState.value.isRunning)
        assertEquals(homeViewModel.uiState.value.remainingMillis, detailViewModel.uiState.value.remainingMillis)

        detailViewModel.toggleTimer()

        assertFalse(homeViewModel.uiState.value.isRunning)
        assertFalse(detailViewModel.uiState.value.isRunning)
    }

    @Test
    fun `user actions are recorded but restored session is not a new start`() {
        val actions = mutableListOf<TimerAction>()
        val telemetry = object : AppTelemetry {
            override fun timerAction(action: TimerAction, durationMillis: Long) {
                assertTrue(durationMillis > 0)
                actions += action
            }
        }
        val prefs = FakePreferencesRepository(timerState = TimerPreferenceState("FOCUS", 10_000, 10_000, 0, false))
        val controller = TimerSessionController(application, prefs, telemetry)

        controller.toggleTimer()
        controller.toggleTimer()
        controller.resetTimer()
        assertEquals(listOf(TimerAction.START, TimerAction.PAUSE, TimerAction.RESET), actions)

        prefs.saveTimerState("POMODORO_FOCUS_0", 10_000, 9_000, System.currentTimeMillis() + 9_000, true)
        TimerSessionController(application, prefs, telemetry)
        assertEquals(3, actions.size)
    }

    @Test
    fun `alarm completion advances one persisted phase only once`() {
        val nowMillis = 10_000L
        val repository = FakePreferencesRepository(
            timerState = TimerPreferenceState(
                presetName = "POMODORO_FOCUS_0",
                totalMillis = 1_500_000L,
                remainingMillis = 0L,
                targetAtMillis = nowMillis,
                isRunning = true
            )
        )
        val controller = TimerSessionController(application, repository)

        assertTrue(controller.completeFromAlarm(nowMillis))
        assertEquals(PomodoroPhase.SHORT_BREAK, controller.uiState.value.phase)
        assertTrue(controller.uiState.value.isRunning)
        assertFalse(controller.completeFromAlarm(nowMillis))
    }
}
