package com.lbs.schoolhelper

import android.content.ComponentName
import com.lbs.schoolhelper.widget.WidgetConfigActivity
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = SchoolHelperApplication::class, sdk = [34])
class ComponentExposureTest {

    @Test
    fun widgetConfigActivity_isNotExported() {
        val context = RuntimeEnvironment.getApplication()
        val activityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, WidgetConfigActivity::class.java),
            0
        )

        assertFalse(activityInfo.exported)
    }
}
