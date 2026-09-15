package com.lbs.schoolhelper.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.lbs.schoolhelper.R

/** A stable channel leaves sound, vibration, and silent mode under the system's control. */
object TimerNotificationChannel {
    // v2 resets the app-created channel defaults for users who previously had
    // the first channel created with vibration disabled.
    const val ID = "timer_completion_v2"

    fun ensure(context: Context, notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ID,
            context.getString(R.string.timer_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.timer_notification_channel_description)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 250L, 150L, 250L)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
