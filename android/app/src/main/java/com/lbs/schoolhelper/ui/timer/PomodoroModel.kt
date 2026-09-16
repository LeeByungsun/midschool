package com.lbs.schoolhelper.ui.timer

data class PomodoroSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val rounds: Int = 4
) {
    fun normalized(): PomodoroSettings = copy(
        focusMinutes = focusMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES),
        shortBreakMinutes = shortBreakMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES),
        longBreakMinutes = longBreakMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES),
        rounds = rounds.coerceIn(MIN_ROUNDS, MAX_ROUNDS)
    )

    companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 120
        const val MIN_ROUNDS = 1
        const val MAX_ROUNDS = 4
    }
}

enum class PomodoroPhase { FOCUS, SHORT_BREAK, LONG_BREAK }
