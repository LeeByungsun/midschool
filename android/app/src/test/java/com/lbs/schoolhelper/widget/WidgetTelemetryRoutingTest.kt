package com.lbs.schoolhelper.widget

import android.content.Intent
import com.lbs.schoolhelper.telemetry.TelemetryLifecycleCallbacks
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class WidgetTelemetryRoutingTest {
    @Test
    fun `widget open intent carries widget entry point`() {
        val intent = Intent("com.lbs.schoolhelper.OPEN").withWidgetEntryPoint()

        assertEquals(
            "widget",
            intent.getStringExtra(TelemetryLifecycleCallbacks.ENTRY_POINT_EXTRA)
        )
    }
}
