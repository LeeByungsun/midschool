package com.bsbarron.midschoolapp.ui.timer

import com.bsbarron.midschoolapp.R

data class TimerUiState(
    val selectedPreset: TimerPreset = TimerPreset.FOCUS,
    val totalMillis: Long = TimerPreset.FOCUS.durationMillis,
    val remainingMillis: Long = TimerPreset.FOCUS.durationMillis,
    val displayTimeText: String = "40:00",
    val subtitle: String = "",
    val buttonTextRes: Int = R.string.home_timer_start,
    val isRunning: Boolean = false,
    val isCountMode: Boolean = true,
    val progressFraction: Float = 1f
)

enum class TimerPreset(val durationMillis: Long, val subtitleRes: Int) {
    FOCUS(40L * 60L * 1000L, R.string.home_timer_focus_label),
    BREAK(10L * 60L * 1000L, R.string.home_timer_break_label),
    DEEP_FOCUS(25L * 60L * 1000L, R.string.home_timer_deep_label)
}
