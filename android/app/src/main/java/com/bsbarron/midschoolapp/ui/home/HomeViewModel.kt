package com.bsbarron.midschoolapp.ui.home

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.HomeUiState
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import com.bsbarron.midschoolapp.ui.common.UiStringProvider
import com.bsbarron.midschoolapp.util.isVisibleSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel private constructor(
    application: Application,
    private val schoolRepository: SchoolRepository,
    private val preferencesRepository: PreferencesRepository,
    private val textResolver: (Int, Array<out Any?>) -> String
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    @Inject
    constructor(
        application: Application,
        schoolRepository: SchoolRepository,
        preferencesRepository: PreferencesRepository
    ) : this(application, schoolRepository, preferencesRepository, defaultTextResolver(application))

    companion object {
        fun createForTest(
            application: Application,
            schoolRepository: SchoolRepository,
            preferencesRepository: PreferencesRepository,
            textResolver: (Int, Array<out Any?>) -> String
        ): HomeViewModel {
            return HomeViewModel(
                application,
                schoolRepository,
                preferencesRepository,
                textResolver
            )
        }

        private fun defaultTextResolver(application: Application): (Int, Array<out Any?>) -> String {
            val context = application.applicationContext
            return { id, formatArgs ->
                if (formatArgs.isEmpty()) {
                    context.getString(id)
                } else {
                    context.getString(id, *formatArgs)
                }
            }
        }
    }

    init {
        refreshHeader()
    }

    private fun resolveString(@StringRes id: Int): String = textResolver(id, emptyArray<Any?>())

    private fun resolveString(@StringRes id: Int, vararg args: Any?): String = textResolver(id, args)

    fun refreshHeader() {
        val studentInfo = preferencesRepository.getStudentInfo()
        val grade = studentInfo.grade
        val classroom = studentInfo.classroom
        val hasSchoolSelection = studentInfo.hasSchoolSelection()
        _uiState.update {
            it.copy(
                isSchoolConfigured = hasSchoolSelection,
                schoolName = studentInfo.schoolName.ifBlank {
                    strings.getString(R.string.home_school_name_placeholder)
                },
                dateLabel = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
                ),
                classSummary = if (grade.isNotBlank() && classroom.isNotBlank()) {
                    strings.getString(R.string.home_student_info_format, grade, classroom)
                } else if (!hasSchoolSelection) {
                    strings.getString(R.string.home_school_not_set_hint)
                } else {
                    strings.getString(R.string.home_semester_label)
                },
                todaySummaryText = if (it.errorMessage != null) {
                    strings.getString(R.string.home_today_summary_error)
                } else {
                    strings.getString(R.string.home_today_summary_body)
                }
            )
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            refreshHeader()
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val studentInfo = preferencesRepository.getStudentInfo()
            if (!studentInfo.hasSchoolSelection()) {
                _uiState.update {
                    it.copy(
                        isSchoolConfigured = false,
                        todaySummaryText = strings.getString(R.string.home_school_not_set_summary),
                        mealSummary = strings.getString(R.string.home_meal_missing_school),
                        mealMeta = "",
                        eventSummary = strings.getString(R.string.home_schedule_missing_school),
                        isLoading = false,
                        errorMessage = null
                    )
                }
                return@launch
            }

            val mealsResult = schoolRepository.getMeals(today)
            val schedulesResult = schoolRepository.getSchedules(today.take(6))

            val firstMeal = mealsResult.getOrNull()?.firstOrNull()
            val mealSummary = firstMeal?.menu
                ?.let(::formatMealMenu)
                ?.trim()
                .orEmpty()
            val mealMeta = listOfNotNull(
                firstMeal?.mealType?.takeIf { it.isNotBlank() },
                firstMeal?.calorieInfo?.takeIf { it.isNotBlank() }
            ).joinToString(" • ")
            val eventSummary = schedulesResult.getOrNull()
                .orEmpty()
                .filter { it.isVisibleSchedule() }
                .mapNotNull { event ->
                    val eventDate = runCatching {
                        LocalDate.parse(event.date, DateTimeFormatter.BASIC_ISO_DATE)
                    }.getOrNull() ?: return@mapNotNull null

                    if (eventDate.isBefore(LocalDate.now())) {
                        return@mapNotNull null
                    }

                    val title = event.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val formattedDate = eventDate.format(
                        DateTimeFormatter.ofPattern("M/d(E)", Locale.KOREAN)
                    )
                    val detail = event.description.takeIf { it.isNotBlank() }

                    buildString {
                        append(formattedDate)
                        append("  ")
                        append(title)
                        if (detail != null) {
                            append("\n")
                            append(detail)
                        }
                    }
                }
                .take(3)
                .joinToString("\n\n")
            val errorMessage = mealsResult.exceptionOrNull()?.message
                ?: schedulesResult.exceptionOrNull()?.message

            _uiState.update {
                it.copy(
                    isSchoolConfigured = true,
                    todaySummaryText = if (errorMessage != null) {
                        strings.getString(R.string.home_today_summary_error)
                    } else {
                        strings.getString(R.string.home_today_summary_body)
                    },
                    mealSummary = mealSummary.ifBlank { "오늘은 등록된 급식이 없어요." },
                    mealMeta = mealMeta.ifBlank { "급식 없음" },
                    eventSummary = eventSummary.ifBlank { "이번 달에 남아 있는 학사 일정이 없어요." },
                    isLoading = false,
                    errorMessage = errorMessage
                )
            }
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

}
