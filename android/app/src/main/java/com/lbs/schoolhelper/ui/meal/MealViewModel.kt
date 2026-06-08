package com.lbs.schoolhelper.ui.meal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.model.MealInfo
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.data.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MealViewModel @Inject constructor(
    application: Application,
    private val schoolRepository: SchoolRepository,
    private val preferencesRepository: PreferencesRepository
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val _uiState = MutableStateFlow(MealUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadWeekMeals()
    }

    fun loadWeekMeals(referenceDate: LocalDate = LocalDate.now()) {
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(4)
        _uiState.update {
            it.copy(
                weekTitle = formatWeekTitle(weekStart, weekEnd),
                statusText = appContext.getString(R.string.meal_loading),
                isLoading = true,
                items = emptyList()
            )
        }

        if (!preferencesRepository.getStudentInfo().hasSchoolSelection()) {
            _uiState.update {
                it.copy(
                    statusText = appContext.getString(R.string.meal_missing_student_info),
                    isLoading = false,
                    items = emptyList()
                )
            }
            return
        }

        viewModelScope.launch {
            val dayStates = (0L..4L)
                .map { offset ->
                    async {
                        val day = weekStart.plusDays(offset)
                        val result = schoolRepository.getMeals(day.format(DateTimeFormatter.BASIC_ISO_DATE))
                        buildDayUiModel(day = day, result = result)
                    }
                }
                .awaitAll()

            val hasAnyMeals = dayStates.any { it.hasMealData }
            val hasErrors = dayStates.any { it.hasError }

            _uiState.update {
                it.copy(
                    statusText = when {
                        hasAnyMeals && hasErrors -> appContext.getString(R.string.meal_partial_error)
                        hasAnyMeals -> ""
                        hasErrors -> appContext.getString(R.string.meal_error_day)
                        else -> appContext.getString(R.string.meal_empty_week)
                    },
                    isLoading = false,
                    items = dayStates.map { dayState -> dayState.item }
                )
            }
        }
    }

    private fun buildDayUiModel(
        day: LocalDate,
        result: Result<List<MealInfo>>
    ): DayMealState {
        val meals = result.getOrDefault(emptyList())
        val detailText = when {
            result.isFailure -> appContext.getString(R.string.meal_error_day)
            meals.isEmpty() -> appContext.getString(R.string.meal_empty_day)
            else -> formatMeals(meals)
        }

        return DayMealState(
            item = MealDayUiModel(
                dateLabel = day.format(
                    DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
                ),
                detailText = detailText,
                isToday = day == LocalDate.now()
            ),
            hasMealData = meals.isNotEmpty(),
            hasError = result.isFailure
        )
    }

    private fun formatWeekTitle(weekStart: LocalDate, weekEnd: LocalDate): String {
        return "${formatShortDate(weekStart)} - ${formatShortDate(weekEnd)}"
    }

    private fun formatShortDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))
    }

    private fun formatMeals(meals: List<MealInfo>): String {
        return meals.joinToString("\n\n") { meal ->
            val meta = listOfNotNull(
                meal.mealType.takeIf { it.isNotBlank() },
                meal.calorieInfo.takeIf { it.isNotBlank() }
            ).joinToString(" • ")

            buildString {
                if (meta.isNotBlank()) {
                    append(meta)
                    append("\n")
                }
                append(formatMealMenu(meal.menu))
            }.trim()
        }
    }

    private fun formatMealMenu(rawMenu: String): String {
        return rawMenu
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace(Regex("[ \\t]+"), " ")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map(::formatMealLine)
            .joinToString("\n")
    }

    private fun formatMealLine(line: String): String {
        val match = Regex("^(.*?)(\\(([^)]*)\\))?$").matchEntire(line.trim()) ?: return line.trim()
        val name = match.groupValues[1].trim()
        val allergy = match.groupValues.getOrNull(3)?.trim().orEmpty()

        return if (allergy.isNotBlank()) {
            "$name ($allergy)"
        } else {
            name
        }
    }

    private data class DayMealState(
        val item: MealDayUiModel,
        val hasMealData: Boolean,
        val hasError: Boolean
    )
}
