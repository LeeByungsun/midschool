package com.bsbarron.midschoolapp.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class WidgetDateFormatterTest {

    @Test
    fun `formatHeaderDate adds localized weekday to header`() {
        val formatted = WidgetDateFormatter.formatHeaderDate(
            date = LocalDate.of(2026, 4, 19),
            locale = Locale.KOREAN
        )

        assertEquals("📅 4월 19일 (일)", formatted)
    }
}
