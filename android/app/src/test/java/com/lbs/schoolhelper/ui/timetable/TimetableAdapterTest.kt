package com.lbs.schoolhelper.ui.timetable

import com.lbs.schoolhelper.data.model.TimetableItem
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimetableAdapterTest {

    @Test
    fun submitListKeepsTheSeventhLesson() {
        val adapter = TimetableAdapter()
        val submittedItems = (1..7).map { period ->
            TimetableItem(
                date = "20260915",
                period = period.toString(),
                subject = "과목$period",
                grade = "1",
                classroom = "4"
            )
        }
        val committed = CountDownLatch(1)

        adapter.submitList(submittedItems) { committed.countDown() }

        assertTrue(committed.await(1, TimeUnit.SECONDS))
        assertEquals(7, adapter.itemCount)
        assertEquals("7", adapter.currentList.last().period)
    }
}
