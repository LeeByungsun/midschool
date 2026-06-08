package com.lbs.schoolhelper.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.data.repository.SchoolRepository
import com.lbs.schoolhelper.data.repository.StudentInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    application: Application,
    private val preferencesRepository: PreferencesRepository,
    private val schoolRepository: SchoolRepository
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val initialStudentInfo = preferencesRepository.getStudentInfo()
    private var schoolSearchJob: Job? = null
    private var latestSearchRequestId: Long = 0L

    private val _uiState = MutableStateFlow(
        SetupUiState(
            schoolQuery = initialStudentInfo.schoolName,
            selectedSchool = initialStudentInfo.takeIf { it.hasSchoolSelection() }?.toSchoolInfo(),
            searchMessage = if (initialStudentInfo.schoolName.isNotBlank() && !initialStudentInfo.hasSchoolSelection()) {
                appContext.getString(R.string.school_search_reselect_required)
            } else {
                ""
            },
            grade = initialStudentInfo.grade,
            classroom = initialStudentInfo.classroom
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _messageEvent = MutableSharedFlow<Int>()
    val messageEvent = _messageEvent.asSharedFlow()

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun updateSchoolQuery(query: String) {
        val trimmedQuery = query.trim()
        _uiState.update { state ->
            state.copy(
                schoolQuery = query,
                selectedSchool = state.selectedSchool,
                schoolResults = emptyList(),
                searchMessage = when {
                    state.selectedSchool != null && state.selectedSchool.schoolName != trimmedQuery ->
                        appContext.getString(R.string.school_search_reselect_current_selection)
                    else -> state.searchMessage
                }
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
                    selectedSchool = it.selectedSchool
                )
            }
            return
        }

        val requestId = ++latestSearchRequestId
        schoolSearchJob?.cancel()
        schoolSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchMessage = "", schoolResults = emptyList()) }
            val result = schoolRepository.searchSchools(query)
            if (requestId != latestSearchRequestId) return@launch

            result.onSuccess { schools ->
                when {
                    schools.isEmpty() -> {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                schoolResults = emptyList(),
                                selectedSchool = it.selectedSchool,
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
                        selectedSchool = it.selectedSchool,
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

    suspend fun saveStudentInfo() {
        val state = _uiState.value
        if (state.selectedSchool == null || state.selectedSchool.schoolName != state.schoolQuery.trim()) {
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
        _navigationEvent.emit(Unit)
    }

    companion object {
        private const val MIN_SCHOOL_QUERY_LENGTH = 2
    }
}
