package com.bsbarron.midschoolapp.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.data.repository.TimerDisplayMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val preferencesRepository: PreferencesRepository,
    private val schoolRepository: SchoolRepository
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val studentInfo = preferencesRepository.getStudentInfo()
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            schoolQuery = studentInfo.schoolName,
            selectedSchool = studentInfo.takeIf { it.hasSchoolSelection() }?.toSchoolInfo(),
            grade = studentInfo.grade,
            classroom = studentInfo.classroom,
            isRingMode = preferencesRepository.getTimerDisplayMode() == TimerDisplayMode.RING,
            notificationEnabled = preferencesRepository.isTimerNotificationEnabled(),
            vibrationEnabled = preferencesRepository.isTimerVibrationEnabled()
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _messageEvent = MutableSharedFlow<Int>()
    val messageEvent = _messageEvent.asSharedFlow()

    private val _closeEvent = MutableSharedFlow<Unit>()
    val closeEvent = _closeEvent.asSharedFlow()

    fun updateSchoolQuery(query: String) {
        val trimmedQuery = query.trim()
        _uiState.update { state ->
            val keepSelection = state.selectedSchool?.schoolName == trimmedQuery
            state.copy(
                schoolQuery = query,
                selectedSchool = if (keepSelection) state.selectedSchool else null
            )
        }
    }

    fun searchSchools() {
        val query = _uiState.value.schoolQuery.trim()
        if (query.length < MIN_SCHOOL_QUERY_LENGTH) {
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchMessage = appContext.getString(R.string.school_search_min_query),
                    schoolResults = emptyList(),
                    selectedSchool = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchMessage = "", schoolResults = emptyList()) }
            val result = schoolRepository.searchSchools(query)

            result.onSuccess { schools ->
                when {
                    schools.isEmpty() -> {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                schoolResults = emptyList(),
                                selectedSchool = null,
                                searchMessage = appContext.getString(R.string.school_search_empty)
                            )
                        }
                    }

                    schools.size == 1 -> {
                        val selectedSchool = schools.first()
                        _uiState.update {
                            it.copy(
                                schoolQuery = selectedSchool.schoolName,
                                selectedSchool = selectedSchool,
                                schoolResults = schools,
                                searchMessage = appContext.getString(R.string.school_search_single_result),
                                isSearching = false
                            )
                        }
                    }

                    else -> {
                        _uiState.update {
                            it.copy(
                                schoolResults = schools,
                                selectedSchool = null,
                                searchMessage = appContext.getString(R.string.school_search_select_result),
                                isSearching = false
                            )
                        }
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        schoolResults = emptyList(),
                        selectedSchool = null,
                        searchMessage = error.message ?: appContext.getString(R.string.school_search_error)
                    )
                }
            }
        }
    }

    fun selectSchool(school: SchoolInfo) {
        _uiState.update {
            it.copy(
                schoolQuery = school.schoolName,
                selectedSchool = school,
                searchMessage = appContext.getString(R.string.school_search_selected, school.schoolName)
            )
        }
    }

    fun updateGrade(grade: String) {
        _uiState.update { it.copy(grade = grade) }
    }

    fun updateClassroom(classroom: String) {
        _uiState.update { it.copy(classroom = classroom) }
    }

    fun updateDisplayMode(isRingMode: Boolean) {
        _uiState.update { it.copy(isRingMode = isRingMode) }
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationEnabled = enabled) }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
    }

    suspend fun saveSettings() {
        val state = _uiState.value
        if (state.selectedSchool == null) {
            _messageEvent.emit(R.string.setup_error_school_required)
            return
        }
        if (state.grade.isBlank() || state.classroom.isBlank()) {
            _messageEvent.emit(R.string.setup_error_empty)
            return
        }

        preferencesRepository.saveStudentInfo(
            StudentInfo(
                grade = state.grade,
                classroom = state.classroom,
                schoolName = state.selectedSchool.schoolName,
                officeCode = state.selectedSchool.officeCode,
                schoolCode = state.selectedSchool.schoolCode,
                schoolKind = state.selectedSchool.schoolKind
            )
        )
        preferencesRepository.saveTimerDisplayMode(
            if (state.isRingMode) TimerDisplayMode.RING else TimerDisplayMode.COUNT
        )
        preferencesRepository.saveTimerNotificationEnabled(state.notificationEnabled)
        preferencesRepository.saveTimerVibrationEnabled(state.vibrationEnabled)
        _messageEvent.emit(R.string.settings_saved)
        _closeEvent.emit(Unit)
    }

    companion object {
        private const val MIN_SCHOOL_QUERY_LENGTH = 2
    }
}
