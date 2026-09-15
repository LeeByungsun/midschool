package com.lbs.schoolhelper.timer

import android.os.Build
import android.provider.Settings

object TimerNotificationSettings {
    fun actionFor(notificationsEnabled: Boolean, sdkInt: Int): String {
        return if (!notificationsEnabled || sdkInt < Build.VERSION_CODES.O) {
            Settings.ACTION_APP_NOTIFICATION_SETTINGS
        } else {
            Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
        }
    }
}
