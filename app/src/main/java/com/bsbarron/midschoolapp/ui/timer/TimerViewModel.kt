package com.bsbarron.midschoolapp.ui.timer

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.TimerDisplayMode
import com.bsbarron.midschoolapp.timer.TimerAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    application: Application,
    private val preferencesRepository: PreferencesRepository
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var countDownTimer: CountDownTimer? = null

    init {
        restoreTimerState()
    }

    fun selectPreset(preset: TimerPreset) {
        countDownTimer?.cancel()
        TimerAlarmScheduler.cancel(appContext)

        _uiState.value = TimerUiState(
            selectedPreset = preset,
            totalMillis = preset.durationMillis,
            remainingMillis = preset.durationMillis,
            displayTimeText = formatTimerText(preset.durationMillis),
            subtitle = appContext.getString(preset.subtitleRes),
            buttonTextRes = R.string.home_timer_start,
            isRunning = false,
            isCountMode = isCountMode(),
            progressFraction = 1f
        )
        saveTimerState(isRunning = false, targetAtMillis = 0L)
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) pauseTimer() else startTimer()
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        TimerAlarmScheduler.cancel(appContext)
        _uiState.update {
            it.copy(
                remainingMillis = it.totalMillis,
                displayTimeText = formatTimerText(it.totalMillis),
                buttonTextRes = R.string.home_timer_start,
                isRunning = false,
                progressFraction = 1f,
                isCountMode = isCountMode()
            )
        }
        saveTimerState(isRunning = false, targetAtMillis = 0L)
    }

    fun refreshDisplayMode() {
        _uiState.update { it.copy(isCountMode = isCountMode()) }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }

    private fun startTimer() {
        val currentState = _uiState.value
        val targetAtMillis = System.currentTimeMillis() + currentState.remainingMillis
        saveTimerState(isRunning = true, targetAtMillis = targetAtMillis)
        TimerAlarmScheduler.schedule(appContext, targetAtMillis)

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(currentState.remainingMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _uiState.update {
                    it.copy(
                        remainingMillis = millisUntilFinished,
                        displayTimeText = formatTimerText(millisUntilFinished),
                        buttonTextRes = R.string.home_timer_pause,
                        isRunning = true,
                        progressFraction = calculateProgress(millisUntilFinished, it.totalMillis)
                    )
                }
                saveTimerState(
                    isRunning = true,
                    targetAtMillis = System.currentTimeMillis() + millisUntilFinished
                )
            }

            override fun onFinish() {
                TimerAlarmScheduler.cancel(appContext)
                preferencesRepository.clearTimerState()
                _uiState.update {
                    it.copy(
                        remainingMillis = 0L,
                        displayTimeText = formatTimerText(0L),
                        buttonTextRes = R.string.home_timer_restart,
                        isRunning = false,
                        progressFraction = 0f
                    )
                }
            }
        }.start()

        _uiState.update { it.copy(isRunning = true, buttonTextRes = R.string.home_timer_pause) }
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        TimerAlarmScheduler.cancel(appContext)
        _uiState.update { it.copy(isRunning = false, buttonTextRes = R.string.home_timer_resume) }
        saveTimerState(isRunning = false, targetAtMillis = 0L)
    }

    private fun restoreTimerState() {
        val savedState = preferencesRepository.getTimerState()
        val preset = runCatching { TimerPreset.valueOf(savedState.presetName) }.getOrDefault(TimerPreset.FOCUS)
        val remainingMillis = if (savedState.isRunning && savedState.targetAtMillis > 0L) {
            (savedState.targetAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        } else {
            savedState.remainingMillis
        }

        _uiState.value = TimerUiState(
            selectedPreset = preset,
            totalMillis = savedState.totalMillis,
            remainingMillis = remainingMillis,
            displayTimeText = formatTimerText(remainingMillis),
            subtitle = appContext.getString(preset.subtitleRes),
            buttonTextRes = when {
                remainingMillis == 0L -> R.string.home_timer_restart
                savedState.isRunning -> R.string.home_timer_pause
                remainingMillis < savedState.totalMillis -> R.string.home_timer_resume
                else -> R.string.home_timer_start
            },
            isRunning = false,
            isCountMode = isCountMode(),
            progressFraction = calculateProgress(remainingMillis, savedState.totalMillis)
        )

        if (savedState.isRunning && remainingMillis > 0L) {
            startTimer()
        } else if (savedState.isRunning && remainingMillis == 0L) {
            preferencesRepository.clearTimerState()
            TimerAlarmScheduler.cancel(appContext)
        }
    }

    private fun saveTimerState(isRunning: Boolean, targetAtMillis: Long) {
        val state = _uiState.value
        preferencesRepository.saveTimerState(
            presetName = state.selectedPreset.name,
            totalMillis = state.totalMillis,
            remainingMillis = state.remainingMillis,
            targetAtMillis = targetAtMillis,
            isRunning = isRunning
        )
    }

    private fun isCountMode(): Boolean {
        return preferencesRepository.getTimerDisplayMode() != TimerDisplayMode.RING
    }

    private fun calculateProgress(remainingMillis: Long, totalMillis: Long): Float {
        if (totalMillis <= 0L) return 0f
        return remainingMillis.toFloat() / totalMillis.toFloat()
    }

    private fun formatTimerText(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.KOREAN, "%02d:%02d", minutes, seconds)
    }

    private fun createInitialState(): TimerUiState {
        return TimerUiState(
            subtitle = appContext.getString(TimerPreset.FOCUS.subtitleRes),
            isCountMode = isCountMode()
        )
    }
}
