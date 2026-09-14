package com.lbs.schoolhelper.timer

import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.telemetry.AppTelemetry
import com.lbs.schoolhelper.telemetry.TimerAction

/** Both alarm and UI callbacks execute on the main thread. Claim before reporting. */
object TimerCompletion {
    fun complete(preferences: PreferencesRepository, telemetry: AppTelemetry, nowMillis: Long): Boolean {
        val state = preferences.getTimerState()
        if (!state.isRunning || state.targetAtMillis <= 0 || state.targetAtMillis > nowMillis) return false
        preferences.clearTimerState()
        telemetry.timerAction(TimerAction.COMPLETE, state.totalMillis)
        return true
    }
}
