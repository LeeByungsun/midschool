package com.lbs.schoolhelper.widget

import com.lbs.schoolhelper.data.model.TimetableItem
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetTimetableContentResolverTest {

    @Test
    fun `returns failure content instead of an exception message`() {
        val content = WidgetTimetableContentResolver.resolve(
            Result.failure(IllegalStateException("Unable to resolve timetable response"))
        )

        assertEquals(WidgetTimetableContent.LoadError, content)
    }

    @Test
    fun `returns no classes when the successful response has no visible subjects`() {
        val content = WidgetTimetableContentResolver.resolve(
            Result.success(
                listOf(
                    TimetableItem(
                        date = "20260920",
                        period = "1",
                        subject = " ",
                        grade = "1",
                        classroom = "4"
                    )
                )
            )
        )

        assertEquals(WidgetTimetableContent.NoClasses, content)
    }

    @Test
    fun `formats sorted and truncated visible subjects`() {
        val content = WidgetTimetableContentResolver.resolve(
            Result.success(
                listOf(
                    TimetableItem("20260920", "2", "사회문화와법경제", "1", "4"),
                    TimetableItem("20260920", "1", "국어", "1", "4")
                )
            )
        )

        assertEquals(
            WidgetTimetableContent.Lessons("1교시 국어\n2교시 사회문화와법"),
            content
        )
    }
}
