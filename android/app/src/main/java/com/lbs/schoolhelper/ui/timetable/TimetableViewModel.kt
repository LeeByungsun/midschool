package com.lbs.schoolhelper.ui.timetable

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.profile.StudentProfileRepository
import com.lbs.schoolhelper.data.remote.NeisApiKeyMissingException
import com.lbs.schoolhelper.data.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TimetableViewModel @Inject constructor(
    application: Application,
    private val schoolRepository: SchoolRepository,
    private val studentProfileRepository: StudentProfileRepository
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private var currentDate: LocalDate = LocalDate.now()
    private var loadJob: Job? = null
    private var observedProfileId: String? = null
    private var profileObservationStarted = false
    private var loadGeneration = 0L
    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            studentProfileRepository.activeProfile.collect { profile ->
                val changed = !profileObservationStarted || observedProfileId != profile?.id
                profileObservationStarted = true
                observedProfileId = profile?.id
                if (changed) loadTimetable()
            }
        }
    }

    fun showPreviousDay() {
        currentDate = currentDate.minusDays(1)
        loadTimetable()
    }

    fun showNextDay() {
        currentDate = currentDate.plusDays(1)
        loadTimetable()
    }

    private fun loadTimetable() {
        loadJob?.cancel()
        val profile = studentProfileRepository.activeProfile.value
        val profileId = profile?.id
        val generation = ++loadGeneration
        val studentInfo = profile?.studentInfo
        val hasRequiredInfo = studentInfo?.isComplete() == true
        _uiState.update {
            it.copy(
                profileContextText = profileContext(profile),
                dateTitle = currentDate.format(
                    DateTimeFormatter.ofPattern(appContext.getString(R.string.date_format_day_with_weekday), Locale.KOREAN)
                ),
                classInfoText = if (studentInfo?.hasClassroomInfo() == true) {
                    appContext.getString(R.string.home_student_info_format, studentInfo.grade, studentInfo.classroom)
                } else {
                    appContext.getString(R.string.timetable_missing_student_info)
                },
                lessonCountText = appContext.getString(R.string.timetable_lesson_count_empty),
                statusText = if (!hasRequiredInfo) {
                    appContext.getString(R.string.timetable_missing_student_info)
                } else {
                    appContext.getString(R.string.timetable_loading)
                },
                items = emptyList()
            )
        }
        if (!hasRequiredInfo || studentInfo == null) return

        loadJob = viewModelScope.launch {
            schoolRepository.observeTimetable(
                student = studentInfo,
                date = currentDate.format(DateTimeFormatter.BASIC_ISO_DATE)
            ).collect { result ->
                if (generation != loadGeneration || studentProfileRepository.activeProfile.value?.id != profileId) return@collect
                val items = result.getOrDefault(emptyList())
                    .sortedWith(compareBy({ item -> item.period.toIntOrNull() ?: Int.MAX_VALUE }, { item -> item.period }))
                _uiState.update {
                    it.copy(
                        lessonCountText = if (items.isEmpty()) {
                            appContext.getString(R.string.timetable_lesson_count_empty)
                        } else {
                            appContext.resources.getQuantityString(R.plurals.timetable_lesson_count, items.size, items.size)
                        },
                        statusText = when {
                            result.exceptionOrNull() is NeisApiKeyMissingException -> appContext.getString(R.string.timetable_neis_api_key_missing)
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

    private fun profileContext(profile: StudentProfile?): String {
        val info = profile?.studentInfo ?: return ""
        return listOf(profile.displayName, info.schoolName).filter { it.isNotBlank() }.joinToString(" · ")
    }
}
