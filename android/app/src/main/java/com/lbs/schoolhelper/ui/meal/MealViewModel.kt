package com.lbs.schoolhelper.ui.meal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.model.MealInfo
import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.profile.StudentProfileRepository
import com.lbs.schoolhelper.data.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MealViewModel @Inject constructor(
    application: Application,
    private val schoolRepository: SchoolRepository,
    private val studentProfileRepository: StudentProfileRepository
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private var loadJob: Job? = null
    private var observedProfileId: String? = null
    private var profileObservationStarted = false
    private var loadGeneration = 0L
    private val _uiState = MutableStateFlow(MealUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            studentProfileRepository.activeProfile.collect { profile ->
                val changed = !profileObservationStarted || observedProfileId != profile?.id
                profileObservationStarted = true
                observedProfileId = profile?.id
                if (changed) loadWeekMeals()
            }
        }
    }

    fun loadWeekMeals(referenceDate: LocalDate = LocalDate.now()) {
        loadJob?.cancel()
        val profile = studentProfileRepository.activeProfile.value
        val profileId = profile?.id
        val generation = ++loadGeneration
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(4)
        val weekdays = (0L..4L).map { offset -> weekStart.plusDays(offset) }
        val dayStates = weekdays.map(::buildLoadingDayUiModel).toMutableList()

        _uiState.update {
            it.copy(
                profileContextText = profileContext(profile),
                weekTitle = formatWeekTitle(weekStart, weekEnd),
                statusText = appContext.getString(R.string.meal_loading),
                isLoading = true,
                items = dayStates.map { dayState -> dayState.item }
            )
        }

        val studentInfo = profile?.studentInfo
        if (studentInfo == null || !studentInfo.hasSchoolSelection()) {
            if (isCurrent(generation, profileId)) {
                _uiState.update {
                    it.copy(
                        statusText = appContext.getString(R.string.meal_missing_student_info),
                        isLoading = false,
                        items = emptyList()
                    )
                }
            }
            return
        }

        loadJob = viewModelScope.launch {
            weekdays.forEachIndexed { index, day ->
                launch {
                    val date = day.format(DateTimeFormatter.BASIC_ISO_DATE)
                    schoolRepository.observeMeals(studentInfo, date).collect { result ->
                        if (!isCurrent(generation, profileId)) return@collect
                        dayStates[index] = buildDayUiModel(day = day, result = result)
                        publishWeekMealState(dayStates, generation, profileId)
                    }
                }
            }
        }
    }

    private fun isCurrent(generation: Long, profileId: String?): Boolean {
        return generation == loadGeneration && studentProfileRepository.activeProfile.value?.id == profileId
    }

    private fun profileContext(profile: StudentProfile?): String {
        val info = profile?.studentInfo ?: return ""
        return listOf(profile.displayName, info.schoolName).filter { it.isNotBlank() }.joinToString(" · ")
    }

    private fun buildLoadingDayUiModel(day: LocalDate): DayMealState {
        return DayMealState(
            item = MealDayUiModel(
                dateLabel = day.format(
                    DateTimeFormatter.ofPattern(appContext.getString(R.string.date_format_day_with_weekday), Locale.KOREAN)
                ),
                detailText = appContext.getString(R.string.meal_loading),
                isToday = day == LocalDate.now()
            ),
            hasMealData = false,
            hasError = false,
            isLoaded = false
        )
    }

    private fun publishWeekMealState(dayStates: List<DayMealState>, generation: Long, profileId: String?) {
        if (!isCurrent(generation, profileId)) return
        val hasAnyMeals = dayStates.any { it.hasMealData }
        val hasErrors = dayStates.any { it.hasError }
        val isLoading = dayStates.any { !it.isLoaded }

        _uiState.update {
            it.copy(
                statusText = when {
                    isLoading -> appContext.getString(R.string.meal_loading)
                    hasAnyMeals && hasErrors -> appContext.getString(R.string.meal_partial_error)
                    hasAnyMeals -> ""
                    hasErrors -> appContext.getString(R.string.meal_error_day)
                    else -> appContext.getString(R.string.meal_empty_week)
                },
                isLoading = isLoading,
                items = dayStates.map { dayState -> dayState.item }
            )
        }
    }

    private fun buildDayUiModel(day: LocalDate, result: Result<List<MealInfo>>): DayMealState {
        val meals = result.getOrDefault(emptyList())
        val detailText = when {
            result.isFailure -> appContext.getString(R.string.meal_error_day)
            meals.isEmpty() -> appContext.getString(R.string.meal_empty_day)
            else -> formatMeals(meals)
        }
        return DayMealState(
            item = MealDayUiModel(
                dateLabel = day.format(
                    DateTimeFormatter.ofPattern(appContext.getString(R.string.date_format_day_with_weekday), Locale.KOREAN)
                ),
                detailText = detailText,
                isToday = day == LocalDate.now()
            ),
            hasMealData = meals.isNotEmpty(),
            hasError = result.isFailure,
            isLoaded = true
        )
    }

    private fun formatWeekTitle(weekStart: LocalDate, weekEnd: LocalDate): String {
        return "${formatShortDate(weekStart)} - ${formatShortDate(weekEnd)}"
    }

    private fun formatShortDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern(appContext.getString(R.string.date_format_short_day), Locale.KOREAN))
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
        return rawMenu.replace(Regex("<br\\s*/?>"), "\n")
            .replace(Regex("[ \\t]+"), " ")
            .lines().map { it.trim() }.filter { it.isNotEmpty() }.map(::formatMealLine).joinToString("\n")
    }

    private fun formatMealLine(line: String): String {
        val match = Regex("^(.*?)(\\(([^)]*)\\))?$").matchEntire(line.trim()) ?: return line.trim()
        val name = match.groupValues[1].trim()
        val allergy = match.groupValues.getOrNull(3)?.trim().orEmpty()
        return if (allergy.isNotBlank()) "$name ($allergy)" else name
    }

    private data class DayMealState(
        val item: MealDayUiModel,
        val hasMealData: Boolean,
        val hasError: Boolean,
        val isLoaded: Boolean
    )
}
