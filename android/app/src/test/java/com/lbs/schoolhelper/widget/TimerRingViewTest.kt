package com.lbs.schoolhelper.widget

import android.content.Context
import android.graphics.Paint
import com.lbs.schoolhelper.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimerRingViewTest {

    @Test
    fun setTimerState_exposesRemainingTimeToAccessibilityServices() {
        val context = RuntimeEnvironment.getApplication() as Context
        val view = TimerRingView(context)

        view.setTimerState(0.5f, "24:59", "남은 시간")

        assertEquals("남은 시간 24:59", view.contentDescription)
    }

    @Test
    fun timerText_usesScalableTextDimensions() {
        val context = RuntimeEnvironment.getApplication() as Context
        val view = TimerRingView(context)

        assertEquals(context.resources.getDimension(R.dimen.text_timer_value), timePaint(view).textSize)
        assertEquals(context.resources.getDimension(R.dimen.text_body), labelPaint(view).textSize)
    }

    private fun timePaint(view: TimerRingView): Paint = paint(view, "timePaint")

    private fun labelPaint(view: TimerRingView): Paint = paint(view, "labelPaint")

    private fun paint(view: TimerRingView, fieldName: String): Paint {
        return TimerRingView::class.java.getDeclaredField(fieldName)
            .apply { isAccessible = true }
            .get(view) as Paint
    }
}
