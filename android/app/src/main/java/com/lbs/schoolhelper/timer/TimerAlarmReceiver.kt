package com.lbs.schoolhelper.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        TimerCompletionAlert.play(context)
    }

    companion object {
        const val ACTION_TIMER_FINISHED = "com.lbs.schoolhelper.ACTION_TIMER_FINISHED"
    }
}
