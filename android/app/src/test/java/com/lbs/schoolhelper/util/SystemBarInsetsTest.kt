package com.lbs.schoolhelper.util

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
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
    fun applySystemBarPadding_usesImeBottomWhenKeyboardIsTallerThanNavigationBar() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = View(activity).apply { setPadding(1, 2, 3, 4) }
        activity.setContentView(view)
        view.applySystemBarPadding()

        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(10, 20, 30, 40))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 320))
            .build()
        ViewCompat.dispatchApplyWindowInsets(view, insets)

        assertEquals(11, view.paddingLeft)
        assertEquals(22, view.paddingTop)
        assertEquals(33, view.paddingRight)
        assertEquals(324, view.paddingBottom)
    }

    @Test
    fun applySystemBarPadding_whenImeAppears_requestsFocusedFieldVisibilityAgain() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val scrollView = ScrollView(activity)
        val content = FrameLayout(activity)
        val child = View(activity).apply { isFocusableInTouchMode = true }
        activity.setContentView(scrollView)
        scrollView.addView(content, FrameLayout.LayoutParams(400, 700))
        content.addView(
            child,
            FrameLayout.LayoutParams(200, 80).apply { topMargin = 200 }
        )
        scrollView.measure(exactly(400), exactly(600))
        scrollView.layout(0, 0, 400, 600)
        scrollView.applySystemBarPadding()
        assertTrue(child.requestFocus())

        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 40))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 320))
            .build()
        ViewCompat.dispatchApplyWindowInsets(scrollView, insets)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(scrollView.scrollY > 0)
    }

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

    @Test
    fun requestVisibleAboveKeyboard_withVisibleIme_scrollsAncestorScrollView() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val scrollView = ScrollView(activity)
        val content = FrameLayout(activity)
        val child = View(activity).apply { isFocusableInTouchMode = true }
        activity.setContentView(scrollView)
        scrollView.addView(content, FrameLayout.LayoutParams(400, 700))
        content.addView(
            child,
            FrameLayout.LayoutParams(200, 80).apply { topMargin = 200 }
        )
        scrollView.measure(exactly(400), exactly(600))
        scrollView.layout(0, 0, 400, 600)
        scrollView.applySystemBarPadding()

        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 40))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 320))
            .build()
        ViewCompat.dispatchApplyWindowInsets(scrollView, insets)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(child.requestFocus())

        child.requestVisibleAboveKeyboard()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))

        assertTrue(scrollView.scrollY > 0)
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
