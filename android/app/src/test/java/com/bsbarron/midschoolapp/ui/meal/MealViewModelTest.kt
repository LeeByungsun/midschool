package com.bsbarron.midschoolapp.ui.meal

import com.bsbarron.midschoolapp.MainActivity
import com.bsbarron.midschoolapp.MisSchoolApplication
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import com.bsbarron.midschoolapp.test.FakeSchoolRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = MisSchoolApplication::class, sdk = [34])
class MealViewModelTest {

    @Test
    fun loadWeekMealsBuildsSevenDayState() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDates = (0L..6L).map { offset ->
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

        val viewModel = MealViewModel(application, schoolRepository, preferencesRepository)
        val state = withTimeout(1_000L) {
            viewModel.uiState.first { !it.isLoading && it.items.size == 7 }
        }

        assertEquals(
            "${weekStart.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))} - " +
                "${weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))}",
            state.weekTitle
        )
        assertEquals(7, state.items.size)
        assertTrue(state.statusText.isBlank())
        assertEquals(weekDates, schoolRepository.requestedMealDates)
        assertTrue(state.items.first().detailText.contains("점심 • 842 kcal"))
        assertTrue(state.items.first().detailText.contains("잡곡밥"))
        assertTrue(state.items[1].detailText.contains(application.getString(R.string.meal_empty_day)))
    }

    @Test
    fun loadWeekMealsShowsMissingSchoolMessageWhenStudentInfoIsIncomplete() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val schoolRepository = FakeSchoolRepository()
        val preferencesRepository = FakePreferencesRepository()

        val viewModel = MealViewModel(application, schoolRepository, preferencesRepository)
        val state = withTimeout(1_000L) {
            viewModel.uiState.first { !it.isLoading }
        }

        assertEquals(application.getString(R.string.meal_missing_student_info), state.statusText)
        assertTrue(state.items.isEmpty())
        assertTrue(schoolRepository.requestedMealDates.isEmpty())
    }
}
