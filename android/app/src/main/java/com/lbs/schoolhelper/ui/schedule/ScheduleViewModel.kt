package com.lbs.schoolhelper.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.model.SchoolEvent
import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.profile.StudentProfileRepository
import com.lbs.schoolhelper.data.repository.SchoolRepository
import com.lbs.schoolhelper.util.isVisibleSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
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
class ScheduleViewModel @Inject constructor(
    application: Application,
    private val schoolRepository: SchoolRepository,
    private val studentProfileRepository: StudentProfileRepository
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private var currentMonth: YearMonth = YearMonth.now()
    private var loadJob: Job? = null
    private var observedProfileId: String? = null
    private var profileObservationStarted = false
    private var loadGeneration = 0L
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            studentProfileRepository.activeProfile.collect { profile ->
                val changed = !profileObservationStarted || observedProfileId != profile?.id
                profileObservationStarted = true
                observedProfileId = profile?.id
                if (changed) loadSchedule()
            }
        }
    }

    fun showPreviousMonth() {
        currentMonth = currentMonth.minusMonths(1)
        loadSchedule()
    }

    fun showNextMonth() {
        currentMonth = currentMonth.plusMonths(1)
        loadSchedule()
    }

    private fun loadSchedule() {
        loadJob?.cancel()
        val profile = studentProfileRepository.activeProfile.value
        val profileId = profile?.id
        val generation = ++loadGeneration
        val monthKey = currentMonth.format(DateTimeFormatter.ofPattern("yyyyMM"))
        _uiState.update {
            it.copy(
                profileContextText = profileContext(profile),
                monthTitle = currentMonth.format(
                    DateTimeFormatter.ofPattern(appContext.getString(R.string.date_format_month), Locale.KOREAN)
                ),
                scheduleText = if (profile?.studentInfo?.hasSchoolSelection() == true) {
                    appContext.getString(R.string.schedule_loading)
                } else {
                    appContext.getString(R.string.home_schedule_missing_school)
                }
            )
        }
        val studentInfo = profile?.studentInfo
        if (studentInfo == null || !studentInfo.hasSchoolSelection()) return

        loadJob = viewModelScope.launch {
            schoolRepository.observeSchedules(studentInfo, monthKey).collect { result ->
                if (generation != loadGeneration || studentProfileRepository.activeProfile.value?.id != profileId) return@collect
                val schedules = result.getOrDefault(emptyList())
                    .filter { it.isVisibleSchedule() }
                    .sortedBy { it.date }
                _uiState.update { it.copy(scheduleText = formatScheduleText(schedules)) }
            }
        }
    }

    private fun profileContext(profile: StudentProfile?): String {
        val info = profile?.studentInfo ?: return ""
        return listOf(profile.displayName, info.schoolName).filter { it.isNotBlank() }.joinToString(" · ")
    }

    private fun formatScheduleText(schedules: List<SchoolEvent>): String {
        return if (schedules.isEmpty()) {
            appContext.getString(R.string.schedule_empty_month)
        } else {
            schedules.joinToString("\n\n") { event ->
                val dateLabel = runCatching {
                    LocalDate.parse(event.date, DateTimeFormatter.BASIC_ISO_DATE).format(
                        DateTimeFormatter.ofPattern(appContext.getString(R.string.date_format_day_with_short_weekday), Locale.KOREAN)
                    )
                }.getOrDefault(event.date)
                buildString {
                    append(dateLabel)
                    append("\n")
                    append(event.title.ifBlank { appContext.getString(R.string.schedule_no_title) })
                    if (event.description.isNotBlank()) {
                        append("\n")
                        append(event.description)
                    }
                }
            }
        }
    }
}
