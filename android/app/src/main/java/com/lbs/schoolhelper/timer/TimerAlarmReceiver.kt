package com.lbs.schoolhelper.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lbs.schoolhelper.ui.timer.TimerSessionController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Advances the persisted Pomodoro phase before playing its completion feedback. */
@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var timerSession: TimerSessionController

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_TIMER_FINISHED) {
            timerSession.completeFromAlarm()
        }
    }

    companion object {
        const val ACTION_TIMER_FINISHED = "com.lbs.schoolhelper.ACTION_TIMER_FINISHED"
    }
}
