package com.lbs.schoolhelper.ui.timer

import android.content.Context
import android.os.CountDownTimer
import com.lbs.schoolhelper.BuildConfig
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.data.repository.TimerDisplayMode
import com.lbs.schoolhelper.telemetry.AppTelemetry
import com.lbs.schoolhelper.telemetry.NoOpTelemetry
import com.lbs.schoolhelper.telemetry.TimerAction
import com.lbs.schoolhelper.timer.TimerAlarmScheduler
import com.lbs.schoolhelper.timer.TimerCompletionAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The application-scoped source of truth for the active Pomodoro session.
 *
 * Home and detail activities each have their own ViewModel, but both consume this same state flow
 * and delegate controls here. This prevents one screen from overwriting another screen's timer.
 */
@Singleton
class TimerSessionController @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val preferencesRepository: PreferencesRepository,
    private val telemetry: AppTelemetry = NoOpTelemetry
) {
    private val lock = Any()
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
    }

    fun updatePomodoroSettings(newSettings: PomodoroSettings) = synchronized(lock) {
        if (_uiState.value.isRunning) return
        settings = effectiveSettings(newSettings.normalized())
        preferencesRepository.savePomodoroSettings(newSettings.normalized())
        resetTimerLocked(recordTelemetry = false)
    }

    fun selectPreset(preset: TimerPreset) = synchronized(lock) {
        if (_uiState.value.isRunning) return
        resetTimerLocked(recordTelemetry = false)
    }

    fun selectFocusMinutes(minutes: Int) = synchronized(lock) {
        if (_uiState.value.isRunning) return
        val normalized = if (minutes >= 40) 40 else 25
        val persistedSettings = preferencesRepository.getPomodoroSettings().copy(
            focusMinutes = normalized,
            shortBreakMinutes = if (normalized == 40) 10 else 5
        )
        settings = effectiveSettings(persistedSettings)
        preferencesRepository.savePomodoroSettings(persistedSettings)
        resetTimerLocked(recordTelemetry = false)
    }

    fun selectRounds(rounds: Int) = synchronized(lock) {
        if (_uiState.value.isRunning) return
        val persistedSettings = preferencesRepository.getPomodoroSettings()
            .copy(rounds = if (rounds >= 4) 4 else 2)
        settings = effectiveSettings(persistedSettings)
        preferencesRepository.savePomodoroSettings(persistedSettings)
        resetTimerLocked(recordTelemetry = false)
    }

    fun toggleTimer() = synchronized(lock) {
        if (_uiState.value.isRunning) pauseTimerLocked()
        else if (awaitingNextPhase || sessionCompleted) startNextPhaseLocked()
        else startCurrentPhaseLocked()
    }

    fun resetTimer() = synchronized(lock) {
        resetTimerLocked(recordTelemetry = true)
    }

    fun refreshDisplayMode() {
        _uiState.update { it.copy(isCountMode = isCountMode()) }
    }

    /** Re-read persisted state after process recreation; normal screens already share [uiState]. */
    fun refreshFromPersistence() = synchronized(lock) {
        countDownTimer?.cancel()
        restoreTimerState()
    }

    /**
     * Advances the exact persisted phase after the scheduled alarm fires.
     * Returns false for an old/cancelled alarm so no duplicate completion alert is emitted.
     */
    fun completeFromAlarm(nowMillis: Long = System.currentTimeMillis()): Boolean = synchronized(lock) {
        completePersistedPhase(
            expectedTargetAtMillis = null,
            nowMillis = nowMillis,
            requireExpiredTarget = true,
            playAlert = true
        )
    }

    private fun resetTimerLocked(recordTelemetry: Boolean) {
        countDownTimer?.cancel()
        TimerAlarmScheduler.cancel(appContext)
        phase = PomodoroPhase.FOCUS
        completedRounds = 0
        awaitingNextPhase = false
        sessionCompleted = false
        val duration = settings.focusMinutes * MINUTE_MILLIS
        render(duration, running = false)
        saveState(running = false, targetAtMillis = 0L)
        if (recordTelemetry) telemetry.timerAction(TimerAction.RESET, duration)
    }

    private fun startNextPhaseLocked() {
        if (sessionCompleted) {
            phase = PomodoroPhase.FOCUS
            completedRounds = 0
            sessionCompleted = false
        } else if (awaitingNextPhase) {
            awaitingNextPhase = false
        }
        startCurrentPhaseLocked()
    }

    private fun startCurrentPhaseLocked(
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
        render(durationMillis, running = true)
        saveState(running = true, targetAtMillis = targetAtMillis)
        TimerAlarmScheduler.schedule(appContext, targetAtMillis)
        if (recordAction) telemetry.timerAction(TimerAction.START, durationMillis)

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationMillis, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                synchronized(lock) {
                    val state = preferencesRepository.getTimerState()
                    if (!state.isRunning || state.targetAtMillis != targetAtMillis) {
                        cancel()
                        return
                    }
                    // Target time is already persisted; writing every tick only adds I/O and races.
                    render(millisUntilFinished, running = true)
                }
            }

            override fun onFinish() {
                synchronized(lock) {
                    completePersistedPhase(
                        expectedTargetAtMillis = targetAtMillis,
                        nowMillis = System.currentTimeMillis(),
                        requireExpiredTarget = false,
                        playAlert = true
                    )
                }
            }
        }.start()
    }

    private fun pauseTimerLocked() {
        countDownTimer?.cancel()
        TimerAlarmScheduler.cancel(appContext)
        val remaining = _uiState.value.remainingMillis
        render(remaining, running = false)
        saveState(running = false, targetAtMillis = 0L)
        telemetry.timerAction(TimerAction.PAUSE, remaining)
    }

    private fun completePersistedPhase(
        expectedTargetAtMillis: Long?,
        nowMillis: Long,
        requireExpiredTarget: Boolean,
        playAlert: Boolean
    ): Boolean {
        val persisted = preferencesRepository.getTimerState()
        if (!persisted.isRunning || persisted.targetAtMillis <= 0L) return false
        if (expectedTargetAtMillis != null && persisted.targetAtMillis != expectedTargetAtMillis) return false
        if (requireExpiredTarget && persisted.targetAtMillis > nowMillis) return false

        restoreSessionMetadata(persisted.presetName)
        countDownTimer?.cancel()
        TimerAlarmScheduler.cancel(appContext)
        telemetry.timerAction(TimerAction.COMPLETE, persisted.totalMillis)
        if (playAlert) TimerCompletionAlert.play(appContext)

        when (phase) {
            PomodoroPhase.FOCUS -> {
                completedRounds = (completedRounds + 1).coerceAtMost(settings.rounds)
                phase = if (completedRounds >= settings.rounds) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
                awaitingNextPhase = false
                startCurrentPhaseLocked(recordAction = false, resumeRemaining = false)
            }
            PomodoroPhase.SHORT_BREAK -> {
                phase = PomodoroPhase.FOCUS
                awaitingNextPhase = false
                startCurrentPhaseLocked(recordAction = false, resumeRemaining = false)
            }
            PomodoroPhase.LONG_BREAK -> {
                phase = PomodoroPhase.FOCUS
                completedRounds = 0
                awaitingNextPhase = false
                sessionCompleted = true
                render(settings.focusMinutes * MINUTE_MILLIS, running = false)
                saveState(running = false, targetAtMillis = 0L)
            }
        }
        return true
    }

    private fun restoreTimerState() {
        settings = effectiveSettings(preferencesRepository.getPomodoroSettings())
        val saved = preferencesRepository.getTimerState()
        restoreSessionMetadata(saved.presetName)
        val remaining = if (saved.isRunning && saved.targetAtMillis > 0L) {
            (saved.targetAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        } else {
            saved.remainingMillis.coerceAtLeast(0L)
        }
        render(remaining.coerceAtMost(phaseDurationMillis()), running = false)
        if (saved.isRunning && remaining > 0L) {
            startCurrentPhaseLocked(recordAction = false, persistedTargetAtMillis = saved.targetAtMillis)
        } else if (!saved.isRunning) {
            awaitingNextPhase = saved.remainingMillis == 0L && decodeState(saved.presetName) != null
            render(if (awaitingNextPhase) phaseDurationMillis() else remaining, running = false)
        }
    }

    private fun restoreSessionMetadata(value: String) {
        val decoded = decodeState(value) ?: return
        phase = decoded.first
        completedRounds = decoded.second
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

    private fun decodeState(value: String): Pair<PomodoroPhase, Int>? {
        if (!value.startsWith("POMODORO_")) return null
        val encoded = value.removePrefix("POMODORO_")
        val separator = encoded.lastIndexOf('_')
        if (separator <= 0) return null
        val savedPhase = runCatching { PomodoroPhase.valueOf(encoded.substring(0, separator)) }.getOrNull() ?: return null
        return savedPhase to encoded.substring(separator + 1).toIntOrNull().orZero()
    }

    private fun Int?.orZero(): Int = this?.coerceAtLeast(0) ?: 0
    private fun effectiveSettings(value: PomodoroSettings) = if (BuildConfig.BUILD_TYPE == "qa") value.copy(focusMinutes = 1, shortBreakMinutes = 1, longBreakMinutes = 1) else value
    private fun isCountMode() = preferencesRepository.getTimerDisplayMode() != TimerDisplayMode.RING
    private fun calculateProgress(remaining: Long, total: Long) = if (total <= 0L) 0f else remaining.toFloat() / total.toFloat()
    private fun formatTimerText(millis: Long): String {
        val seconds = (millis / 1_000L).coerceAtLeast(0L)
        return String.format(Locale.KOREAN, "%02d:%02d", seconds / 60L, seconds % 60L)
    }

    private fun createInitialState() = TimerUiState(
        subtitle = appContext.getString(R.string.home_timer_focus_round, 1, 4),
        isCountMode = isCountMode()
    )

    companion object {
        private const val MINUTE_MILLIS = 60_000L
    }
}
