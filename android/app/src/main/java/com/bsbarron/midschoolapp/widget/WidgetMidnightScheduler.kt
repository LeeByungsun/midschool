package com.bsbarron.midschoolapp.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

object WidgetMidnightScheduler {
    const val ACTION_MIDNIGHT_REFRESH =
        "com.bsbarron.midschoolapp.widget.ACTION_MIDNIGHT_REFRESH"

    fun scheduleNext(context: Context) {
        scheduleNext(context, ZonedDateTime.now())
    }

    fun scheduleNext(context: Context, now: ZonedDateTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            nextRefreshAtMillis(now),
            REFRESH_WINDOW_MILLIS,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    internal fun nextRefreshAtMillis(now: ZonedDateTime): Long {
        return now.toLocalDate()
            .plusDays(1)
            .atStartOfDay(now.zone)
            .toInstant()
            .toEpochMilli()
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MisSchoolWidgetProvider::class.java).apply {
            action = ACTION_MIDNIGHT_REFRESH
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val REQUEST_CODE = 40_001
    private const val REFRESH_WINDOW_MILLIS = 30L * 60L * 1000L
}
