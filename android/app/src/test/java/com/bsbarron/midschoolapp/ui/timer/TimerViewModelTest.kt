package com.bsbarron.midschoolapp.ui.timer

import android.app.Application
import com.bsbarron.midschoolapp.data.repository.TimerPreferenceState
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import org.junit.Assert.assertEquals
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
    fun toggleTimer_runningTickDoesNotPersistEverySecond() {
        val repository = FakePreferencesRepository(
            timerState = TimerPreferenceState(
                presetName = TimerPreset.FOCUS.name,
                totalMillis = 2_000L,
                remainingMillis = 2_000L,
                targetAtMillis = 0L,
                isRunning = false
            )
        )
        val viewModel = TimerViewModel(application, repository)

        viewModel.toggleTimer()

        assertEquals(1, repository.savedTimerStates.size)
        assertTrue(repository.savedTimerStates.last().isRunning)

        invokeOnTick(viewModel, 1_000L)

        assertEquals(1, repository.savedTimerStates.size)
        assertTrue(viewModel.uiState.value.remainingMillis < 2_000L)
    }

    @Test
    fun pauseTimerPersistsLatestRemainingTimeOnce() {
        val repository = FakePreferencesRepository(
            timerState = TimerPreferenceState(
                presetName = TimerPreset.FOCUS.name,
                totalMillis = 2_000L,
                remainingMillis = 2_000L,
                targetAtMillis = 0L,
                isRunning = false
            )
        )
        val viewModel = TimerViewModel(application, repository)

        viewModel.toggleTimer()
        invokeOnTick(viewModel, 1_000L)
        val remainingBeforePause = viewModel.uiState.value.remainingMillis

        viewModel.toggleTimer()

        assertEquals(2, repository.savedTimerStates.size)
        val pausedState = repository.savedTimerStates.last()
        assertEquals(false, pausedState.isRunning)
        assertEquals(0L, pausedState.targetAtMillis)
        assertEquals(remainingBeforePause, pausedState.remainingMillis)
    }

    private fun invokeOnTick(viewModel: TimerViewModel, millisUntilFinished: Long) {
        val timerField = TimerViewModel::class.java.getDeclaredField("countDownTimer")
        timerField.isAccessible = true
        val timer = timerField.get(viewModel) ?: error("countDownTimer missing")
        val onTick = timer.javaClass.getDeclaredMethod("onTick", Long::class.javaPrimitiveType)
        onTick.isAccessible = true
        onTick.invoke(timer, millisUntilFinished)
    }
}
