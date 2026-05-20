package com.bsbarron.midschoolapp

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = MisSchoolApplication::class)
class MainActivityNavigationTest {

    @Test
    fun mealCardClickStartsMealActivity() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        activity.findViewById<View>(R.id.mealCard).performClick()

        val nextIntent = shadowOf(activity).nextStartedActivity
        assertEquals(MealActivity::class.java.name, nextIntent.component?.className)
    }
}
