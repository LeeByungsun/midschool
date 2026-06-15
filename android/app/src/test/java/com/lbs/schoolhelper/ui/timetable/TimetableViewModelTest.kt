package com.lbs.schoolhelper.ui.timetable

import com.lbs.schoolhelper.MainActivity
import com.lbs.schoolhelper.SchoolHelperApplication
import com.lbs.schoolhelper.data.model.TimetableItem
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.test.FakePreferencesRepository
import com.lbs.schoolhelper.test.FakeSchoolRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
@Config(application = SchoolHelperApplication::class, sdk = [34])
class TimetableViewModelTest {

    @Test
    fun loadTimetableShowsCachedTimetableBeforeChangedNetworkTimetable() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val request = FakeSchoolRepository.TimetableRequest("1", "2", date)
        val schoolRepository = FakeSchoolRepository().apply {
            timetableFlowResultsByRequest[request] = listOf(
                Result.success(
                    listOf(
                        TimetableItem(
                            date = date,
                            period = "1",
                            subject = "캐시 과목",
                            grade = "1",
                            classroom = "2"
                        )
                    )
                ),
                Result.success(
                    listOf(
                        TimetableItem(
                            date = date,
                            period = "1",
                            subject = "최신 과목",
                            grade = "1",
                            classroom = "2"
                        )
                    )
                )
            )
            timetableFlowEmissionDelayMillisByRequest[request] = 200L
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = "구미중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )

        val viewModel = TimetableViewModel(application, schoolRepository, preferencesRepository)
        val cachedState = withTimeout(1_000L) {
            viewModel.uiState.first { state ->
                state.items.firstOrNull()?.subject == "캐시 과목"
            }
        }

        assertEquals("캐시 과목", cachedState.items.first().subject)

        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(250))

        val networkState = withTimeout(1_000L) {
            viewModel.uiState.first { state ->
                state.items.firstOrNull()?.subject == "최신 과목"
            }
        }

        assertEquals("최신 과목", networkState.items.first().subject)
    }
}
