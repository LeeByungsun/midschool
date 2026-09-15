package com.lbs.schoolhelper.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lbs.schoolhelper.MainActivity
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.telemetry.AppTelemetry
import com.lbs.schoolhelper.telemetry.TelemetryLifecycleCallbacks
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
        postCompletionNotification(context)
    }

    companion object {
        const val ACTION_TIMER_FINISHED = "com.lbs.schoolhelper.ACTION_TIMER_FINISHED"
        private const val NOTIFICATION_ID = 3010
        private const val REQUEST_CODE_OPEN = 3011

        fun postCompletionNotification(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            TimerNotificationChannel.ensure(context, notificationManager)

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(TelemetryLifecycleCallbacks.ENTRY_POINT_EXTRA, "notification")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, TimerNotificationChannel.ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.timer_notification_title))
                .setContentText(context.getString(R.string.timer_notification_body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
