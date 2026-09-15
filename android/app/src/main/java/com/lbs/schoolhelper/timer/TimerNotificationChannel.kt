package com.lbs.schoolhelper.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.lbs.schoolhelper.R

/** A stable channel leaves sound, vibration, and silent mode under the system's control. */
object TimerNotificationChannel {
    const val ID = "timer_completion"

    fun ensure(context: Context, notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ID,
            context.getString(R.string.timer_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.timer_notification_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
