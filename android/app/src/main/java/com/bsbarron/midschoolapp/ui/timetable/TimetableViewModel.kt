package com.bsbarron.midschoolapp.ui.timetable

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TimetableViewModel @Inject constructor(
    application: Application,
    private val schoolRepository: SchoolRepository,
    private val preferencesRepository: PreferencesRepository
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private var currentDate: LocalDate = LocalDate.now()

    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTimetable()
    }

    fun showPreviousDay() {
        currentDate = currentDate.minusDays(1)
        loadTimetable()
    }

    fun showToday() {
        currentDate = LocalDate.now()
        loadTimetable()
    }

    fun showNextDay() {
        currentDate = currentDate.plusDays(1)
        loadTimetable()
    }

    private fun loadTimetable() {
        val studentInfo = preferencesRepository.getStudentInfo()
        val grade = studentInfo.grade
        val classroom = studentInfo.classroom
        val hasRequiredInfo = studentInfo.isComplete()

        _uiState.update {
            it.copy(
                dateTitle = currentDate.format(
                    DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
                ),
                classInfoText = if (studentInfo.hasClassroomInfo()) {
                    appContext.getString(R.string.home_student_info_format, grade, classroom)
                } else {
                    appContext.getString(R.string.timetable_missing_student_info)
                },
                lessonCountText = appContext.getString(R.string.timetable_lesson_count_empty),
                statusText = if (!hasRequiredInfo) {
                    appContext.getString(R.string.timetable_missing_student_info)
                } else {
                    appContext.getString(R.string.timetable_loading)
                },
                showTodayButton = currentDate != LocalDate.now(),
                items = emptyList()
            )
        }

        if (!hasRequiredInfo) return

        viewModelScope.launch {
            val result = schoolRepository.getTimetable(
                grade = grade,
                classroom = classroom,
                date = currentDate.format(DateTimeFormatter.BASIC_ISO_DATE)
            )

            val items = result.getOrDefault(emptyList())
                .sortedWith(
                    compareBy(
                        { item -> item.period.toIntOrNull() ?: Int.MAX_VALUE },
                        { item -> item.period }
                    )
                )

            _uiState.update {
                it.copy(
                    lessonCountText = if (items.isEmpty()) {
                        appContext.getString(R.string.timetable_lesson_count_empty)
                    } else {
                        appContext.resources.getQuantityString(
                            R.plurals.timetable_lesson_count,
                            items.size,
                            items.size
                        )
                    },
                    statusText = when {
                        result.isFailure -> appContext.getString(R.string.timetable_error)
                        items.isEmpty() -> appContext.getString(R.string.timetable_empty)
                        else -> ""
                    },
                    items = items
                )
            }
        }
    }
}
