package com.lbs.schoolhelper.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.telemetry.AppTelemetry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var telemetry: AppTelemetry

    override fun onReceive(context: Context, intent: Intent?) {
        if (!TimerCompletion.complete(preferencesRepository, telemetry, System.currentTimeMillis())) {
            return
        }
        TimerCompletionAlert.play(context)
    }

    companion object {
        const val ACTION_TIMER_FINISHED = "com.lbs.schoolhelper.ACTION_TIMER_FINISHED"
    }
}
