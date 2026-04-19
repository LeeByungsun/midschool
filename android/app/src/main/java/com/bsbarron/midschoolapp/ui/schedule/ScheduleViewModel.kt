package com.bsbarron.midschoolapp.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import com.bsbarron.midschoolapp.util.isVisibleSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    application: Application,
    private val schoolRepository: SchoolRepository
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private var currentMonth: YearMonth = YearMonth.now()

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSchedule()
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
        _uiState.update {
            it.copy(
                monthTitle = currentMonth.format(
                    DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)
                ),
                scheduleText = appContext.getString(R.string.schedule_loading)
            )
        }

        viewModelScope.launch {
            val monthKey = currentMonth.format(DateTimeFormatter.ofPattern("yyyyMM"))
            val result = schoolRepository.getSchedules(monthKey)
            val schedules = result.getOrDefault(emptyList())
                .filter { it.isVisibleSchedule() }
                .sortedBy { it.date }

            _uiState.update {
                it.copy(
                    scheduleText = if (schedules.isEmpty()) {
                        appContext.getString(R.string.schedule_empty_month)
                    } else {
                        schedules.joinToString("\n\n") { event ->
                            val dateLabel = runCatching {
                                LocalDate.parse(event.date, DateTimeFormatter.BASIC_ISO_DATE).format(
                                    DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
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
                )
            }
        }
    }
}
