package com.lbs.schoolhelper.ui.schedule

import com.lbs.schoolhelper.MainActivity
import com.lbs.schoolhelper.SchoolHelperApplication
import com.lbs.schoolhelper.data.model.SchoolEvent
import com.lbs.schoolhelper.test.FakeSchoolRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
@Config(application = SchoolHelperApplication::class, sdk = [34])
class ScheduleViewModelTest {

    @Test
    fun loadScheduleShowsCachedScheduleBeforeChangedNetworkSchedule() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val monthKey = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
        val schoolRepository = FakeSchoolRepository().apply {
            scheduleFlowResultsByDate[monthKey] = listOf(
                Result.success(
                    listOf(
                        SchoolEvent(
                            date = "${monthKey}10",
                            title = "캐시 행사",
                            description = "캐시 일정"
                        )
                    )
                ),
                Result.success(
                    listOf(
                        SchoolEvent(
                            date = "${monthKey}10",
                            title = "최신 행사",
                            description = "최신 일정"
                        )
                    )
                )
            )
            scheduleFlowEmissionDelayMillisByDate[monthKey] = 200L
        }

        val viewModel = ScheduleViewModel(application, schoolRepository)
        val cachedState = withTimeout(1_000L) {
            viewModel.uiState.first { it.scheduleText.contains("캐시 행사") }
        }

        assertTrue(cachedState.scheduleText.contains("캐시 행사"))
        assertFalse(cachedState.scheduleText.contains("최신 행사"))

        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(250))

        val networkState = withTimeout(1_000L) {
            viewModel.uiState.first { it.scheduleText.contains("최신 행사") }
        }

        assertTrue(networkState.scheduleText.contains("최신 행사"))
    }
}
