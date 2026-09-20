package com.lbs.schoolhelper.ui.timer

import androidx.lifecycle.ViewModel
import com.lbs.schoolhelper.ui.timer.TimerSessionController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Screen adapter for the singleton [TimerSessionController]. */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerSession: TimerSessionController
) : ViewModel() {
    val uiState: StateFlow<TimerUiState> = timerSession.uiState

    fun updatePomodoroSettings(newSettings: PomodoroSettings) = timerSession.updatePomodoroSettings(newSettings)
    fun selectPreset(preset: TimerPreset) = timerSession.selectPreset(preset)
    fun selectFocusMinutes(minutes: Int) = timerSession.selectFocusMinutes(minutes)
    fun selectRounds(rounds: Int) = timerSession.selectRounds(rounds)
    fun toggleTimer() = timerSession.toggleTimer()
    fun resetTimer() = timerSession.resetTimer()
    fun refreshDisplayMode() = timerSession.refreshDisplayMode()
    fun refreshFromPersistence() = timerSession.refreshFromPersistence()
}
