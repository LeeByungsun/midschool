package com.lbs.schoolhelper

import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = SchoolHelperApplication::class, sdk = [34])
class TimerCompletionAnimatorLifecycleTest {

    @Test
    fun mainActivity_stopsCompletionAnimatorWhenBackgrounded() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        startCompletionAnimator(activity)

        assertNotNull(completionAnimator(activity))
        controller.pause().stop()

        assertNull(completionAnimator(activity))
    }

    @Test
    fun timerActivity_stopsCompletionAnimatorWhenBackgrounded() {
        val controller = Robolectric.buildActivity(TimerActivity::class.java).setup()
        val activity = controller.get()
        startCompletionAnimator(activity)

        assertNotNull(completionAnimator(activity))
        controller.pause().stop()

        assertNull(completionAnimator(activity))
    }

    private fun startCompletionAnimator(activity: AppCompatActivity) {
        activity.javaClass.getDeclaredMethod("updateTimerCompletionBlink", Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }
            .invoke(activity, true)
    }

    private fun completionAnimator(activity: AppCompatActivity): ValueAnimator? {
        return activity.javaClass.getDeclaredField("timerCompletionAnimator")
            .apply { isAccessible = true }
            .get(activity) as ValueAnimator?
    }
}
