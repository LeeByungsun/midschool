package com.bsbarron.midschoolapp.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class WidgetMidnightSchedulerTest {

    @Test
    fun `next refresh is scheduled for the next local midnight`() {
        val zone = ZoneId.of("Asia/Seoul")
        val now = ZonedDateTime.of(2026, 6, 8, 23, 59, 0, 0, zone)
        val expected = ZonedDateTime.of(2026, 6, 9, 0, 0, 0, 0, zone)
            .toInstant()
            .toEpochMilli()

        assertEquals(expected, WidgetMidnightScheduler.nextRefreshAtMillis(now))
    }

    @Test
    fun `next refresh remains next day when current time is exactly midnight`() {
        val zone = ZoneId.of("Asia/Seoul")
        val now = ZonedDateTime.of(2026, 6, 9, 0, 0, 0, 0, zone)
        val expected = ZonedDateTime.of(2026, 6, 10, 0, 0, 0, 0, zone)
            .toInstant()
            .toEpochMilli()

        assertEquals(expected, WidgetMidnightScheduler.nextRefreshAtMillis(now))
    }
}
