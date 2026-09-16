package com.lbs.schoolhelper.ui.timer

import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.BuildConfig

data class TimerUiState(
    val selectedPreset: TimerPreset = TimerPreset.FOCUS,
    val totalMillis: Long = TimerPreset.FOCUS.durationMillis,
    val remainingMillis: Long = TimerPreset.FOCUS.durationMillis,
    val displayTimeText: String = "40:00",
    val subtitle: String = "",
    val buttonTextRes: Int = R.string.home_timer_start,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val isCountMode: Boolean = true,
    val progressFraction: Float = 1f,
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val completedRounds: Int = 0,
    val totalRounds: Int = 4,
    val awaitingNextPhase: Boolean = false,
    val sessionCompleted: Boolean = false,
    val focusMinutes: Int = 25
)

enum class TimerPreset(private val productionDurationMillis: Long, val subtitleRes: Int) {
    FOCUS(40L * 60L * 1000L, R.string.home_timer_focus_label),
    BREAK(10L * 60L * 1000L, R.string.home_timer_break_label),
    DEEP_FOCUS(25L * 60L * 1000L, R.string.home_timer_deep_label);

    /** QA uses one-minute presets so completion and notification flows are quick to verify. */
    val durationMillis: Long
        get() = if (BuildConfig.BUILD_TYPE == "qa") 60L * 1000L else productionDurationMillis
}
