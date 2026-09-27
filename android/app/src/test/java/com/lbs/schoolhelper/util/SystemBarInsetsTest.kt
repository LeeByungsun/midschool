package com.lbs.schoolhelper.util

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class SystemBarInsetsTest {
    @Test
    fun requestVisibleAboveKeyboard_requestsExtraSpaceBelowFocusedView() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = RecordingFrameLayout(activity)
        val child = View(activity)
        activity.setContentView(parent)
        parent.addView(child, FrameLayout.LayoutParams(200, 80))
        parent.measure(exactly(400), exactly(600))
        parent.layout(0, 0, 400, 600)

        child.requestVisibleAboveKeyboard()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))

        assertNotNull(parent.requestedRectangle)
        assertTrue(parent.requestedRectangle!!.bottom > child.height)
        assertTrue(parent.requestedImmediately)
    }

    private fun exactly(size: Int): Int =
        View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private class RecordingFrameLayout(context: Context) : FrameLayout(context) {
        var requestedRectangle: Rect? = null
        var requestedImmediately: Boolean = false

        override fun requestChildRectangleOnScreen(
            child: View,
            rectangle: Rect,
            immediate: Boolean
        ): Boolean {
            requestedRectangle = Rect(rectangle)
            requestedImmediately = immediate
            return true
        }
    }
}
