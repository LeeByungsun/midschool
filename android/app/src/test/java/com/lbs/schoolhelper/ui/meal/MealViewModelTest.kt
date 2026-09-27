package com.lbs.schoolhelper.ui.meal

import com.lbs.schoolhelper.MainActivity
import com.lbs.schoolhelper.SchoolHelperApplication
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.model.MealInfo
import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.test.FakePreferencesRepository
import com.lbs.schoolhelper.test.FakeSchoolRepository
import com.lbs.schoolhelper.test.FakeStudentProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = SchoolHelperApplication::class, sdk = [34])
class MealViewModelTest {

    @Test
    fun meal_activeProfileChangeReloadsSameWeekForNewSchool() {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val first = StudentProfile("first", "민준", StudentInfo("1", "2", "첫중학교", "J10", "1111111", "중학교"))
        val second = StudentProfile("second", "서연", StudentInfo("2", "3", "둘중학교", "J10", "2222222", "중학교"))
        val profiles = FakeStudentProfileRepository(listOf(first, second), first.id)
        val schoolRepository = FakeSchoolRepository()
        val viewModel = MealViewModel(application, schoolRepository, profiles)

        profiles.selectProfile(second.id)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals("서연 · 둘중학교", viewModel.uiState.value.profileContextText)
        assertTrue(schoolRepository.requestedMealStudents.any { it.schoolCode == "2222222" })
    }

    @Test
    fun loadWeekMealsBuildsWeekdayOnlyState() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDates = (0L..4L).map { offset ->
            weekStart.plusDays(offset).format(DateTimeFormatter.BASIC_ISO_DATE)
        }
        val schoolRepository = FakeSchoolRepository().apply {
            mealResultsByDate[weekDates.first()] = Result.success(
                listOf(
                    MealInfo(
                        date = weekDates.first(),
                        mealType = "점심",
                        menu = "잡곡밥<br/>근대된장국",
                        calorieInfo = "842 kcal"
                    )
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "구미중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )

        val viewModel = createViewModel(application, schoolRepository, preferencesRepository)
        val state = withTimeout(1_000L) {
            viewModel.uiState.first { !it.isLoading && it.items.size == 5 }
        }

        assertEquals(
            "${weekStart.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))} - " +
                "${weekStart.plusDays(4).format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))}",
            state.weekTitle
        )
        assertEquals(5, state.items.size)
        assertTrue(state.statusText.isBlank())
        assertEquals(weekDates, schoolRepository.requestedMealDates)
        assertTrue(state.items.first().detailText.contains("점심 • 842 kcal"))
        assertTrue(state.items.first().detailText.contains("잡곡밥"))
        assertTrue(state.items[1].detailText.contains(application.getString(R.string.meal_empty_day)))
    }

    @Test
    fun loadWeekMealsShowsCachedMealBeforeChangedNetworkMeal() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDates = (0L..4L).map { offset ->
            weekStart.plusDays(offset).format(DateTimeFormatter.BASIC_ISO_DATE)
        }
        val schoolRepository = FakeSchoolRepository().apply {
            weekDates.forEach { date ->
                mealFlowResultsByDate[date] = listOf(Result.success(emptyList()))
            }
            mealFlowResultsByDate[weekDates.first()] = listOf(
                Result.success(
                    listOf(
                        MealInfo(
                            date = weekDates.first(),
                            mealType = "점심",
                            menu = "캐시밥",
                            calorieInfo = "600 kcal"
                        )
                    )
                ),
                Result.success(
                    listOf(
                        MealInfo(
                            date = weekDates.first(),
                            mealType = "점심",
                            menu = "최신밥",
                            calorieInfo = "700 kcal"
                        )
                    )
                )
            )
            mealFlowEmissionDelayMillisByDate[weekDates.first()] = 200L
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "구미중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )

        val viewModel = createViewModel(application, schoolRepository, preferencesRepository)
        val cachedState = withTimeout(1_000L) {
            viewModel.uiState.first { state ->
                state.items.firstOrNull()?.detailText?.contains("캐시밥") == true
            }
        }

        assertTrue(cachedState.items.first().detailText.contains("캐시밥"))
        assertTrue(!cachedState.items.first().detailText.contains("최신밥"))

        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(250))

        val networkState = withTimeout(1_000L) {
            viewModel.uiState.first { state ->
                state.items.firstOrNull()?.detailText?.contains("최신밥") == true
            }
        }

        assertTrue(networkState.items.first().detailText.contains("최신밥"))
    }

    @Test
    fun loadWeekMealsShowsMissingSchoolMessageWhenStudentInfoIsIncomplete() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val schoolRepository = FakeSchoolRepository()
        val preferencesRepository = FakePreferencesRepository()

        val viewModel = createViewModel(application, schoolRepository, preferencesRepository)
        val state = withTimeout(1_000L) {
            viewModel.uiState.first { !it.isLoading }
        }

        assertEquals(application.getString(R.string.meal_missing_student_info), state.statusText)
        assertTrue(state.items.isEmpty())
        assertTrue(schoolRepository.requestedMealDates.isEmpty())
    }

    @Test
    fun loadWeekMealsStartsWeekdayRequestsInParallel() {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDates = (0L..4L).map { offset ->
            weekStart.plusDays(offset).format(DateTimeFormatter.BASIC_ISO_DATE)
        }
        val schoolRepository = FakeSchoolRepository().apply {
            weekDates.forEach { date ->
                mealResultsByDate[date] = Result.success(emptyList())
            }
            mealDelayMillisByDate[weekDates.first()] = 200L
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "구미중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )

        createViewModel(application, schoolRepository, preferencesRepository)

        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(50))

        assertEquals(5, schoolRepository.requestedMealDates.size)
        assertEquals(weekDates, schoolRepository.requestedMealDates)
    }

    private fun createViewModel(
        application: android.app.Application,
        schoolRepository: com.lbs.schoolhelper.data.repository.SchoolRepository,
        preferences: FakePreferencesRepository
    ): MealViewModel {
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
    ): MealViewModel = MealViewModel(application, schoolRepository, profiles)
}
