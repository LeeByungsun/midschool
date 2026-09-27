package com.lbs.schoolhelper.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.data.profile.MAX_PROFILE_NAME_LENGTH
import com.lbs.schoolhelper.data.profile.ProfileMutationFailure
import com.lbs.schoolhelper.data.profile.ProfileMutationResult
import com.lbs.schoolhelper.data.profile.StudentProfileRepository
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.data.repository.SchoolRepository
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.telemetry.AppTelemetry
import com.lbs.schoolhelper.telemetry.NoOpTelemetry
import com.lbs.schoolhelper.widget.MisSchoolWidgetProvider
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
    private val schoolRepository: SchoolRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val telemetry: AppTelemetry = NoOpTelemetry
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val initialProfile = studentProfileRepository.activeProfile.value
    private val initialStudentInfo = initialProfile?.studentInfo ?: preferencesRepository.getStudentInfo()
    private var schoolSearchJob: Job? = null
    private var latestSearchRequestId: Long = 0L

    private val _uiState = MutableStateFlow(
        SetupUiState(
            displayName = initialProfile?.displayName.orEmpty(),
            schoolQuery = initialStudentInfo.schoolName,
            selectedSchool = initialStudentInfo.takeIf { it.hasSchoolSelection() }?.toSchoolInfo(),
            searchMessage = if (initialStudentInfo.schoolName.isNotBlank() && !initialStudentInfo.hasSchoolSelection()) {
                appContext.getString(R.string.school_search_reselect_required)
            } else {
                ""
            },
            grade = initialStudentInfo.grade,
            classroom = initialStudentInfo.classroom,
            isTelemetryConsentStepVisible = initialProfile?.let {
                it.displayName.isNotBlank() && it.studentInfo.isComplete()
            } == true &&
                !preferencesRepository.hasCompletedTelemetryConsentPrompt(),
            analyticsEnabled = preferencesRepository.isAnalyticsEnabled(),
            diagnosticsEnabled = preferencesRepository.isDiagnosticsEnabled()
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _messageEvent = MutableSharedFlow<Int>()
    val messageEvent = _messageEvent.asSharedFlow()

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun updateDisplayName(displayName: String) {
        _uiState.update { it.copy(displayName = displayName) }
    }

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

    fun updateAnalyticsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(analyticsEnabled = enabled) }
    }

    fun updateDiagnosticsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(diagnosticsEnabled = enabled) }
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
        val displayName = state.displayName.trim()
        if (displayName.isBlank()) {
            _messageEvent.emit(R.string.setup_error_profile_name_required)
            return
        }
        if (displayName.length > MAX_PROFILE_NAME_LENGTH) {
            _messageEvent.emit(R.string.setup_error_profile_name_too_long)
            return
        }

        val previousStudentInfo = studentProfileRepository.activeProfile.value?.studentInfo ?: StudentInfo()
        val studentInfo = StudentInfo(
            grade = state.grade,
            classroom = state.classroom,
            schoolName = state.selectedSchool.schoolName,
            officeCode = state.selectedSchool.officeCode,
            schoolCode = state.selectedSchool.schoolCode,
            schoolKind = state.selectedSchool.schoolKind
        )
        val activeProfile = studentProfileRepository.activeProfile.value
        val result = if (activeProfile == null) {
            studentProfileRepository.addProfile(displayName, studentInfo)
        } else {
            studentProfileRepository.updateProfile(
                activeProfile.copy(displayName = displayName, studentInfo = studentInfo)
            )
        }
        if (result is ProfileMutationResult.Failure) {
            _messageEvent.emit(result.reason.messageResource())
            return
        }

        telemetry.schoolSaved(previousStudentInfo, studentInfo)
        MisSchoolWidgetProvider.requestAllWidgetUpdates(appContext)
        if (preferencesRepository.hasCompletedTelemetryConsentPrompt()) {
            _navigationEvent.emit(Unit)
        } else {
            _uiState.update {
                it.copy(
                    isTelemetryConsentStepVisible = true,
                    analyticsEnabled = preferencesRepository.isAnalyticsEnabled(),
                    diagnosticsEnabled = preferencesRepository.isDiagnosticsEnabled()
                )
            }
        }
    }

    suspend fun completeTelemetryConsentStep() {
        val state = _uiState.value
        if (!state.isTelemetryConsentStepVisible) return

        preferencesRepository.saveAnalyticsEnabled(state.analyticsEnabled)
        preferencesRepository.saveDiagnosticsEnabled(state.diagnosticsEnabled)
        preferencesRepository.saveTelemetryConsentPromptCompleted()
        telemetry.setCollection(state.analyticsEnabled, state.diagnosticsEnabled)
        _uiState.update { it.copy(isTelemetryConsentStepVisible = false) }
        _navigationEvent.emit(Unit)
    }

    companion object {
        private const val MIN_SCHOOL_QUERY_LENGTH = 2
    }

    private fun ProfileMutationFailure.messageResource(): Int = when (this) {
        ProfileMutationFailure.BLANK_NAME -> R.string.setup_error_profile_name_required
        ProfileMutationFailure.NAME_TOO_LONG -> R.string.setup_error_profile_name_too_long
        ProfileMutationFailure.DUPLICATE_NAME -> R.string.setup_error_profile_name_duplicate
        else -> R.string.setup_error_profile_save_failed
    }
}
