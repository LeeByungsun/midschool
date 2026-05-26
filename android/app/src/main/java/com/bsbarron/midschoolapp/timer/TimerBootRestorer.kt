package com.bsbarron.midschoolapp.timer

import android.content.Context
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository

object TimerBootRestorer {
    fun restore(
        context: Context,
        preferencesRepository: PreferencesRepository,
        nowMillis: Long = System.currentTimeMillis(),
        scheduleAlarm: (Long) -> Unit = { triggerAtMillis ->
            TimerAlarmScheduler.schedule(context, triggerAtMillis)
        },
        cancelAlarm: () -> Unit = {
            TimerAlarmScheduler.cancel(context)
        }
    ) {
        val timerState = preferencesRepository.getTimerState()
        if (!timerState.isRunning) return

        val targetAtMillis = timerState.targetAtMillis
        if (targetAtMillis <= nowMillis) {
            preferencesRepository.clearTimerState()
            cancelAlarm()
            return
        }

        scheduleAlarm(targetAtMillis)
    }
}
