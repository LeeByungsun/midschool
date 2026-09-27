package com.lbs.schoolhelper.ui.schedule

import com.lbs.schoolhelper.MainActivity
import com.lbs.schoolhelper.SchoolHelperApplication
import com.lbs.schoolhelper.data.model.SchoolEvent
import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.test.FakePreferencesRepository
import com.lbs.schoolhelper.test.FakeSchoolRepository
import com.lbs.schoolhelper.test.FakeStudentProfileRepository
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
    fun schedule_activeProfileChangeReloadsCurrentMonth() {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val first = StudentProfile("first", "민준", StudentInfo("1", "2", "첫중학교", "J10", "1111111", "중학교"))
        val second = StudentProfile("second", "서연", StudentInfo("2", "3", "둘중학교", "J10", "2222222", "중학교"))
        val profiles = FakeStudentProfileRepository(listOf(first, second), first.id)
        val schoolRepository = FakeSchoolRepository()
        val viewModel = ScheduleViewModel(application, schoolRepository, profiles)

        profiles.selectProfile(second.id)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertTrue(viewModel.uiState.value.profileContextText.contains("서연"))
        assertTrue(schoolRepository.requestedScheduleStudents.any { it.schoolCode == "2222222" })
    }

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

        val viewModel = createViewModel(application, schoolRepository, FakePreferencesRepository(
            studentInfo = StudentInfo("2", "3", "테스트중학교", "B10", "7010000", "중학교")
        ))
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

    private fun createViewModel(
        application: android.app.Application,
        schoolRepository: com.lbs.schoolhelper.data.repository.SchoolRepository,
        preferences: FakePreferencesRepository
    ): ScheduleViewModel {
        val student = preferences.getStudentInfo()
        val profiles = if (student.schoolName.isNotBlank() || student.grade.isNotBlank() || student.classroom.isNotBlank()) {
            listOf(StudentProfile("profile-legacy", "테스트", student))
        } else {
            emptyList()
        }
        return createViewModel(
            application,
            schoolRepository,
            FakeStudentProfileRepository(profiles, profiles.firstOrNull()?.id)
        )
    }

    private fun createViewModel(
        application: android.app.Application,
        schoolRepository: com.lbs.schoolhelper.data.repository.SchoolRepository,
        profiles: FakeStudentProfileRepository
    ): ScheduleViewModel = ScheduleViewModel(application, schoolRepository, profiles)
}
