package com.lbs.schoolhelper.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/** Plays a short local completion cue without creating a notification. */
object TimerCompletionAlert {
    fun play(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> playSound(context)
            AudioManager.RINGER_MODE_VIBRATE -> playVibration(context)
            AudioManager.RINGER_MODE_SILENT -> Unit
        }
    }

    private fun playSound(context: Context) {
        val ringtone = RingtoneManager.getRingtone(
            context,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
        }
        ringtone.play()
    }

    @Suppress("DEPRECATION")
    private fun playVibration(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, -1))
        } else {
            vibrator.vibrate(VIBRATION_PATTERN, -1)
        }
    }

    private val VIBRATION_PATTERN = longArrayOf(0L, 250L, 150L, 250L)
}
