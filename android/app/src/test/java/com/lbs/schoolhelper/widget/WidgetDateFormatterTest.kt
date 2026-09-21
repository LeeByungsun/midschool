package com.lbs.schoolhelper.widget

import com.lbs.schoolhelper.SchoolHelperApplication
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = SchoolHelperApplication::class, sdk = [34])
class WidgetDateFormatterTest {

    @Test
    fun `formatHeaderDate adds localized weekday to header`() {
        val formatted = WidgetDateFormatter.formatHeaderDate(
            context = RuntimeEnvironment.getApplication(),
            date = LocalDate.of(2026, 4, 19),
            locale = Locale.KOREAN
        )

        assertEquals("📅 4월 19일 (일)", formatted)
    }
}
