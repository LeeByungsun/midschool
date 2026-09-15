package com.lbs.schoolhelper.timer

import android.os.Build
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerNotificationSettingsTest {
    @Test
    fun `opens app notification settings when notifications are blocked`() {
        assertEquals(
            Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            TimerNotificationSettings.actionFor(notificationsEnabled = false, sdkInt = Build.VERSION_CODES.TIRAMISU)
        )
    }

    @Test
    fun `opens timer channel settings when notifications are enabled`() {
        assertEquals(
            Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS,
            TimerNotificationSettings.actionFor(notificationsEnabled = true, sdkInt = Build.VERSION_CODES.TIRAMISU)
        )
    }
}
