package com.lbs.schoolhelper.timer

import android.app.Application
import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class TimerNotificationChannelTest {
    private val application: Application = RuntimeEnvironment.getApplication()

    @Test
    fun `creates one system managed timer completion channel`() {
        val manager = application.getSystemService(NotificationManager::class.java)

        TimerNotificationChannel.ensure(application, manager)

        val channel = manager.getNotificationChannel(TimerNotificationChannel.ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }
}
