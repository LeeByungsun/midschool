package com.lbs.schoolhelper.ui.timer

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lbs.schoolhelper.BuildConfig
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.data.repository.TimerDisplayMode
import com.lbs.schoolhelper.telemetry.AppTelemetry
import com.lbs.schoolhelper.telemetry.NoOpTelemetry
import com.lbs.schoolhelper.telemetry.TimerAction
import com.lbs.schoolhelper.timer.TimerAlarmScheduler
import com.lbs.schoolhelper.timer.TimerCompletionAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    application: Application,
    private val preferencesRepository: PreferencesRepository,
    private val telemetry: AppTelemetry = NoOpTelemetry
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val instanceId = UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
    private var countDownTimer: CountDownTimer? = null
    private var settings = effectiveSettings(preferencesRepository.getPomodoroSettings())
    private var phase = PomodoroPhase.FOCUS
    private var completedRounds = 0
    private var awaitingNextPhase = false
    private var sessionCompleted = false

    init {
        restoreTimerState()
        viewModelScope.launch {
            TimerSyncBus.events.collect { sourceId ->
                if (sourceId != instanceId) refreshFromPersistence()
            }
        }
    }

    fun updatePomodoroSettings(newSettings: PomodoroSettings) {
        if (_uiState.value.isRunning) return
        settings = effectiveSettings(newSettings.normalized())
        preferencesRepository.savePomodoroSettings(newSettings.normalized())
        resetTimer()
    }

    fun selectPreset(preset: TimerPreset) {
        // Kept for compatibility with the existing preset cards; all starts now use Pomodoro settings.
        if (_uiState.value.isRunning) return
        resetTimer()
    }

    fun selectFocusMinutes(minutes: Int) {
        if (_uiState.value.isRunning) return
        val normalized = if (minutes >= 40) 40 else 25
        val persistedSettings = preferencesRepository.getPomodoroSettings().copy(
            focusMinutes = normalized,
            shortBreakMinutes = if (normalized == 40) 10 else 5
        )
        settings = effectiveSettings(persistedSettings)
        preferencesRepository.savePomodoroSettings(persistedSettings)
        resetTimer()
    }

    fun selectRounds(rounds: Int) {
        if (_uiState.value.isRunning) return
        val persistedSettings = preferencesRepository.getPomodoroSettings()
            .copy(rounds = if (rounds >= 4) 4 else 2)
        settings = effectiveSettings(persistedSettings)
        preferencesRepository.savePomodoroSettings(persistedSettings)
        resetTimer()
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) pauseTimer()
        else if (awaitingNextPhase || sessionCompleted) startNextPhase()
        else startCurrentPhase()
        signalSync()
    }

    fun resetTimer() {
        TimerSyncBus.withSessionLock {
            TimerSyncBus.setRunning(false)
            countDownTimer?.cancel()
            TimerAlarmScheduler.cancel(appContext)
            phase = PomodoroPhase.FOCUS
            completedRounds = 0
            awaitingNextPhase = false
            sessionCompleted = false
            render(settings.focusMinutes * MINUTE_MILLIS, running = false)
            saveState(running = false, targetAtMillis = 0L)
            telemetry.timerAction(TimerAction.RESET, settings.focusMinutes * MINUTE_MILLIS)
        }
        signalSync()
    }

    fun refreshDisplayMode() { _uiState.update { it.copy(isCountMode = isCountMode()) } }

    /** Re-reads the shared timer target so home and detail screens stay aligned. */
    fun refreshFromPersistence() {
        countDownTimer?.cancel()
        restoreTimerState()
    }

    override fun onCleared() { countDownTimer?.cancel(); super.onCleared() }

    private fun startNextPhase() {
        if (sessionCompleted) {
            phase = PomodoroPhase.FOCUS
            completedRounds = 0
            sessionCompleted = false
        } else if (awaitingNextPhase) {
            awaitingNextPhase = false
        }
        startCurrentPhase()
    }

    private fun startCurrentPhase(
        recordAction: Boolean = true,
        resumeRemaining: Boolean = true,
        persistedTargetAtMillis: Long? = null
    ) {
        val fullDurationMillis = phaseDurationMillis()
        val savedRemaining = _uiState.value.remainingMillis
        val durationMillis = if (resumeRemaining && savedRemaining in 1 until fullDurationMillis && !awaitingNextPhase) {
            savedRemaining
        } else {
            fullDurationMillis
        }
        val targetAtMillis = persistedTargetAtMillis ?: (System.currentTimeMillis() + durationMillis)
        awaitingNextPhase = false
        sessionCompleted = false
        TimerSyncBus.setRunning(true)
        render(durationMillis, running = true)
        saveState(running = true, targetAtMillis = targetAtMillis)
        TimerAlarmScheduler.schedule(appContext, targetAtMillis)
        if (recordAction) telemetry.timerAction(TimerAction.START, durationMillis)
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                TimerSyncBus.withSessionLock {
                    if (!TimerSyncBus.isRunning()) {
                        countDownTimer?.cancel()
                        return@withSessionLock
                    }
                    render(millisUntilFinished, running = true)
                    saveState(running = true, targetAtMillis = targetAtMillis)
                }
            }
            override fun onFinish() { finishPhase(playAlert = true) }
        }.start()
    }

    private fun pauseTimer() {
        TimerSyncBus.withSessionLock {
            TimerSyncBus.setRunning(false)
            countDownTimer?.cancel()
            TimerAlarmScheduler.cancel(appContext)
            val remaining = _uiState.value.remainingMillis
            render(remaining, running = false)
            saveState(running = false, targetAtMillis = 0L)
            telemetry.timerAction(TimerAction.PAUSE, remaining)
        }
    }

    private fun finishPhase(playAlert: Boolean) {
        TimerSyncBus.setRunning(false)
        TimerAlarmScheduler.cancel(appContext)
        countDownTimer?.cancel()
        if (playAlert) TimerCompletionAlert.play(appContext)
        when (phase) {
            PomodoroPhase.FOCUS -> {
                completedRounds = (completedRounds + 1).coerceAtMost(settings.rounds)
                phase = if (completedRounds >= settings.rounds) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
                awaitingNextPhase = false
                startCurrentPhase(recordAction = false, resumeRemaining = false)
                signalSync()
                return
            }
            PomodoroPhase.SHORT_BREAK -> {
                phase = PomodoroPhase.FOCUS
                awaitingNextPhase = false
                startCurrentPhase(recordAction = false, resumeRemaining = false)
                signalSync()
                return
            }
            PomodoroPhase.LONG_BREAK -> {
                phase = PomodoroPhase.FOCUS
                completedRounds = 0
                awaitingNextPhase = false
                sessionCompleted = true
                render(settings.focusMinutes * MINUTE_MILLIS, running = false)
            }
        }
        saveState(running = false, targetAtMillis = 0L)
        signalSync()
    }

    private fun restoreTimerState() {
        settings = effectiveSettings(preferencesRepository.getPomodoroSettings())
        val saved = preferencesRepository.getTimerState()
        val decoded = decodeState(saved.presetName)
        if (decoded != null) {
            phase = decoded.first
            completedRounds = decoded.second
        }
        val remaining = if (saved.isRunning && saved.targetAtMillis > 0L) {
            (saved.targetAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        } else saved.remainingMillis.coerceAtLeast(0L)
        render(remaining.coerceAtMost(phaseDurationMillis()), running = false)
        if (saved.isRunning && remaining > 0L) {
            startCurrentPhase(recordAction = false, persistedTargetAtMillis = saved.targetAtMillis)
        }
        else if (saved.isRunning && remaining == 0L) finishPhase(playAlert = false)
        else {
            awaitingNextPhase = saved.remainingMillis == 0L && decoded != null
            render(if (awaitingNextPhase) phaseDurationMillis() else remaining, running = false)
        }
    }

    private fun render(remainingMillis: Long, running: Boolean) {
        val total = phaseDurationMillis()
        _uiState.value = TimerUiState(
            selectedPreset = TimerPreset.FOCUS,
            totalMillis = total,
            remainingMillis = remainingMillis.coerceIn(0L, total),
            displayTimeText = formatTimerText(remainingMillis),
            subtitle = phaseLabel(),
            buttonTextRes = when {
                running -> R.string.home_timer_pause
                sessionCompleted -> R.string.home_timer_new_session
                awaitingNextPhase -> nextButtonRes()
                remainingMillis < total -> R.string.home_timer_resume
                else -> R.string.home_timer_start
            },
            isRunning = running,
            isCompleted = sessionCompleted,
            isCountMode = isCountMode(),
            progressFraction = calculateProgress(remainingMillis, total),
            phase = phase,
            completedRounds = completedRounds,
            totalRounds = settings.rounds,
            awaitingNextPhase = awaitingNextPhase,
            sessionCompleted = sessionCompleted,
            focusMinutes = settings.focusMinutes
        )
    }

    private fun nextButtonRes() = when (phase) {
        PomodoroPhase.SHORT_BREAK -> R.string.home_timer_start_short_break
        PomodoroPhase.FOCUS -> R.string.home_timer_start_focus
        PomodoroPhase.LONG_BREAK -> R.string.home_timer_start_long_break
    }

    private fun phaseLabel() = when (phase) {
        PomodoroPhase.FOCUS -> appContext.getString(R.string.home_timer_focus_round, (completedRounds + 1).coerceAtMost(settings.rounds), settings.rounds)
        PomodoroPhase.SHORT_BREAK -> appContext.getString(R.string.home_timer_short_break_round, completedRounds, settings.rounds)
        PomodoroPhase.LONG_BREAK -> appContext.getString(R.string.home_timer_long_break)
    }

    private fun phaseDurationMillis() = when (phase) {
        PomodoroPhase.FOCUS -> settings.focusMinutes
        PomodoroPhase.SHORT_BREAK -> settings.shortBreakMinutes
        PomodoroPhase.LONG_BREAK -> settings.longBreakMinutes
    } * MINUTE_MILLIS

    private fun saveState(running: Boolean, targetAtMillis: Long) {
        preferencesRepository.saveTimerState(
            presetName = "POMODORO_${phase.name}_$completedRounds",
            totalMillis = phaseDurationMillis(),
            remainingMillis = _uiState.value.remainingMillis,
            targetAtMillis = targetAtMillis,
            isRunning = running
        )
    }

    private fun signalSync() = TimerSyncBus.signal(instanceId)

    private fun decodeState(value: String): Pair<PomodoroPhase, Int>? {
        if (!value.startsWith("POMODORO_")) return null
        val encoded = value.removePrefix("POMODORO_")
        val separator = encoded.lastIndexOf('_')
        if (separator <= 0) return null
        val savedPhase = runCatching { PomodoroPhase.valueOf(encoded.substring(0, separator)) }.getOrNull()
            ?: return null
        return savedPhase to encoded.substring(separator + 1).toIntOrNull().orZero()
    }

    private fun Int?.orZero(): Int = this?.coerceAtLeast(0) ?: 0

    private fun effectiveSettings(value: PomodoroSettings) = if (BuildConfig.BUILD_TYPE == "qa") {
        value.copy(focusMinutes = 1, shortBreakMinutes = 1, longBreakMinutes = 1)
    } else value

    private fun isCountMode() = preferencesRepository.getTimerDisplayMode() != TimerDisplayMode.RING
    private fun calculateProgress(remaining: Long, total: Long) = if (total <= 0L) 0f else remaining.toFloat() / total.toFloat()
    private fun formatTimerText(millis: Long): String {
        val seconds = (millis / 1000L).coerceAtLeast(0L)
        return String.format(Locale.KOREAN, "%02d:%02d", seconds / 60L, seconds % 60L)
    }
    private fun createInitialState() = TimerUiState(subtitle = appContext.getString(R.string.home_timer_focus_round, 1, 4), isCountMode = isCountMode())

    companion object { private const val MINUTE_MILLIS = 60_000L }
}
