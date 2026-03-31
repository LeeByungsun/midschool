package com.bsbarron.midschoolapp.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bsbarron.midschoolapp.MainActivity
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var preferencesRepository: PreferencesRepository

    override fun onReceive(context: Context, intent: Intent?) {
        preferencesRepository.clearTimerState()

        if (!preferencesRepository.isTimerNotificationEnabled()) {
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val vibrationEnabled = preferencesRepository.isTimerVibrationEnabled()
        val channelId = if (vibrationEnabled) CHANNEL_VIBRATE else CHANNEL_SOUND_ONLY
        createChannel(notificationManager, context, channelId, vibrationEnabled)

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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.timer_notification_title))
            .setContentText(context.getString(R.string.timer_notification_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(
        notificationManager: NotificationManager,
        context: Context,
        channelId: String,
        vibrationEnabled: Boolean
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.timer_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.timer_notification_channel_description)
            setSound(soundUri, audioAttributes)
            enableVibration(vibrationEnabled)
            vibrationPattern = if (vibrationEnabled) longArrayOf(0, 250, 150, 250) else longArrayOf(0)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_TIMER_FINISHED = "com.bsbarron.midschoolapp.ACTION_TIMER_FINISHED"
        private const val CHANNEL_SOUND_ONLY = "timer_sound_only"
        private const val CHANNEL_VIBRATE = "timer_vibrate"
        private const val NOTIFICATION_ID = 3010
        private const val REQUEST_CODE_OPEN = 3011
    }
}
