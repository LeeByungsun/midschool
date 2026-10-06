package com.lbs.schoolhelper.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.data.profile.MAX_PROFILE_NAME_LENGTH
import com.lbs.schoolhelper.data.profile.ProfileMutationFailure
import com.lbs.schoolhelper.data.profile.ProfileMutationResult
import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.profile.StudentProfileRepository
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.data.repository.SchoolRepository
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.data.repository.TimerDisplayMode
import com.lbs.schoolhelper.telemetry.AppTelemetry
import com.lbs.schoolhelper.telemetry.NoOpTelemetry
import com.lbs.schoolhelper.widget.MisSchoolWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UnsavedProfileDecision { SAVE, DISCARD, CANCEL }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val preferencesRepository: PreferencesRepository,
    private val schoolRepository: SchoolRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val telemetry: AppTelemetry = NoOpTelemetry
) : AndroidViewModel(application) {

    private sealed interface PendingDestination {
        data class Profile(val id: String) : PendingDestination
        data object Add : PendingDestination
        data object Close : PendingDestination
    }

    private val appContext = application.applicationContext
    private var editingOriginalProfile: StudentProfile? = studentProfileRepository.activeProfile.value
    private var pendingDestination: PendingDestination? = null
    private var schoolSearchJob: Job? = null
    private var latestSearchRequestId: Long = 0L

    private val _uiState = MutableStateFlow(
        profileState(
            base = SettingsUiState(
                profiles = studentProfileRepository.profiles.value,
                isRingMode = preferencesRepository.getTimerDisplayMode() == TimerDisplayMode.RING,
                notificationEnabled = preferencesRepository.isTimerNotificationEnabled(),
                vibrationEnabled = preferencesRepository.isTimerVibrationEnabled(),
                analyticsEnabled = preferencesRepository.isAnalyticsEnabled(),
                diagnosticsEnabled = preferencesRepository.isDiagnosticsEnabled()
            ),
            profile = editingOriginalProfile
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _messageEvent = MutableSharedFlow<Int>()
    val messageEvent = _messageEvent.asSharedFlow()

    private val _closeEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closeEvent = _closeEvent.asSharedFlow()

    private val _unsavedProfileChangesEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unsavedProfileChangesEvent = _unsavedProfileChangesEvent.asSharedFlow()

    fun updateAnalyticsEnabled(enabled: Boolean) {
        preferencesRepository.saveAnalyticsEnabled(enabled)
        _uiState.update { it.copy(analyticsEnabled = enabled) }
        telemetry.setCollection(enabled, preferencesRepository.isDiagnosticsEnabled())
    }

    fun updateDiagnosticsEnabled(enabled: Boolean) {
        preferencesRepository.saveDiagnosticsEnabled(enabled)
        _uiState.update { it.copy(diagnosticsEnabled = enabled) }
        telemetry.setCollection(preferencesRepository.isAnalyticsEnabled(), enabled)
    }

    fun updateDisplayName(displayName: String) = updateProfileState {
        it.copy(displayName = displayName)
    }

    fun updateSchoolQuery(query: String) {
        val trimmedQuery = query.trim()
        updateProfileState { state ->
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
                    schools.isEmpty() -> updateProfileState {
                        it.copy(
                            isSearching = false,
                            schoolResults = emptyList(),
                            selectedSchool = it.selectedSchool,
                            searchMessage = appContext.getString(R.string.school_search_empty)
                        )
                    }
                    schools.size == 1 -> {
                        val selectedSchool = schools.first()
                        updateProfileState {
                            it.copy(
                                schoolQuery = selectedSchool.schoolName,
                                selectedSchool = selectedSchool,
                                schoolResults = schools,
                                searchMessage = appContext.getString(R.string.school_search_single_result),
                                isSearching = false
                            )
                        }
                    }
                    else -> updateProfileState {
                        it.copy(
                            schoolResults = schools,
                            selectedSchool = null,
                            searchMessage = appContext.getString(R.string.school_search_select_result),
                            isSearching = false
                        )
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

    fun selectSchool(school: SchoolInfo) = updateProfileState {
        it.copy(
            schoolQuery = school.schoolName,
            selectedSchool = school,
            searchMessage = appContext.getString(R.string.school_search_selected, school.schoolName)
        )
    }

    fun updateGrade(grade: String) = updateProfileState { it.copy(grade = grade) }

    fun updateClassroom(classroom: String) = updateProfileState { it.copy(classroom = classroom) }

    fun updateDisplayMode(isRingMode: Boolean) {
        _uiState.update { it.copy(isRingMode = isRingMode) }
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationEnabled = enabled) }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
    }

    fun selectEditingProfile(id: String) {
        if (id == _uiState.value.editingProfileId || studentProfileRepository.getProfile(id) == null) return
        requestDestination(PendingDestination.Profile(id))
    }

    fun startAddingProfile() {
        if (_uiState.value.editingProfileId == null && !hasProfileDraft(_uiState.value)) return
        requestDestination(PendingDestination.Add)
    }

    fun requestClose() {
        requestDestination(PendingDestination.Close)
    }

    suspend fun resolveUnsavedProfileChanges(decision: UnsavedProfileDecision) {
        val destination = pendingDestination ?: return
        pendingDestination = null
        when (decision) {
            UnsavedProfileDecision.CANCEL -> Unit
            UnsavedProfileDecision.DISCARD -> applyDestination(destination)
            UnsavedProfileDecision.SAVE -> if (persistEditingProfile(showSavedMessage = false)) {
                applyDestination(destination)
            }
        }
    }

    suspend fun saveEditingProfile() {
        persistEditingProfile(showSavedMessage = true)
    }

    suspend fun deleteEditingProfile() {
        val id = _uiState.value.editingProfileId ?: return
        when (val result = studentProfileRepository.deleteProfile(id)) {
            is ProfileMutationResult.Success -> {
                editingOriginalProfile = studentProfileRepository.activeProfile.value
                _uiState.value = profileState(
                    _uiState.value.copy(profiles = studentProfileRepository.profiles.value),
                    editingOriginalProfile
                )
                MisSchoolWidgetProvider.requestAllWidgetUpdates(appContext)
                _messageEvent.emit(R.string.settings_profile_deleted)
            }
            is ProfileMutationResult.Failure -> _messageEvent.emit(result.reason.messageResource())
        }
    }

    suspend fun saveSettings() {
        val profileSaved = if (_uiState.value.hasUnsavedProfileChanges) {
            persistEditingProfile(showSavedMessage = false)
        } else {
            true
        }

        val state = _uiState.value
        preferencesRepository.saveTimerDisplayMode(
            if (state.isRingMode) TimerDisplayMode.RING else TimerDisplayMode.COUNT
        )
        preferencesRepository.saveTimerNotificationEnabled(state.notificationEnabled)
        preferencesRepository.saveTimerVibrationEnabled(state.vibrationEnabled)

        if (!profileSaved) return

        _messageEvent.emit(R.string.settings_saved)
        _closeEvent.emit(Unit)
    }

    private fun requestDestination(destination: PendingDestination) {
        if (_uiState.value.hasUnsavedProfileChanges) {
            pendingDestination = destination
            _unsavedProfileChangesEvent.tryEmit(Unit)
        } else {
            applyDestination(destination)
        }
    }

    private fun applyDestination(destination: PendingDestination) {
        when (destination) {
            is PendingDestination.Profile -> {
                val profile = studentProfileRepository.getProfile(destination.id) ?: return
                editingOriginalProfile = profile
                _uiState.value = profileState(
                    _uiState.value.copy(profiles = studentProfileRepository.profiles.value),
                    profile
                )
            }
            PendingDestination.Add -> {
                editingOriginalProfile = null
                _uiState.value = profileState(
                    _uiState.value.copy(profiles = studentProfileRepository.profiles.value),
                    null
                )
            }
            PendingDestination.Close -> _closeEvent.tryEmit(Unit)
        }
    }

    private suspend fun persistEditingProfile(showSavedMessage: Boolean): Boolean {
        val state = _uiState.value
        val selectedSchool = state.selectedSchool
        if (selectedSchool == null || selectedSchool.schoolName != state.schoolQuery.trim()) {
            _messageEvent.emit(R.string.setup_error_school_required)
            return false
        }
        if (state.grade.isBlank() || state.classroom.isBlank()) {
            _messageEvent.emit(R.string.setup_error_empty)
            return false
        }
        val displayName = state.displayName.trim()
        if (displayName.isBlank()) {
            _messageEvent.emit(R.string.setup_error_profile_name_required)
            return false
        }
        if (displayName.length > MAX_PROFILE_NAME_LENGTH) {
            _messageEvent.emit(R.string.setup_error_profile_name_too_long)
            return false
        }

        val currentStudentInfo = StudentInfo(
            grade = state.grade,
            classroom = state.classroom,
            schoolName = selectedSchool.schoolName,
            officeCode = selectedSchool.officeCode,
            schoolCode = selectedSchool.schoolCode,
            schoolKind = selectedSchool.schoolKind
        )
        val original = editingOriginalProfile
        val previousStudentInfo = original?.studentInfo ?: StudentInfo()
        val result = if (original == null) {
            studentProfileRepository.addProfile(displayName, currentStudentInfo)
        } else {
            studentProfileRepository.updateProfile(
                original.copy(displayName = displayName, studentInfo = currentStudentInfo)
            )
        }

        val savedProfile = when (result) {
            is ProfileMutationResult.Success<*> -> if (original == null) {
                studentProfileRepository.activeProfile.value
            } else {
                studentProfileRepository.getProfile(original.id)
            }
            is ProfileMutationResult.Failure -> {
                _messageEvent.emit(result.reason.messageResource())
                return false
            }
        } ?: run {
            _messageEvent.emit(R.string.setup_error_profile_save_failed)
            return false
        }

        editingOriginalProfile = savedProfile
        _uiState.value = profileState(
            _uiState.value.copy(profiles = studentProfileRepository.profiles.value),
            savedProfile
        )
        telemetry.schoolSaved(previousStudentInfo, currentStudentInfo)
        MisSchoolWidgetProvider.requestAllWidgetUpdates(appContext)
        if (showSavedMessage) _messageEvent.emit(R.string.settings_profile_saved)
        return true
    }

    private fun updateProfileState(transform: (SettingsUiState) -> SettingsUiState) {
        _uiState.update { state ->
            val updated = transform(state)
            updated.copy(hasUnsavedProfileChanges = isProfileDirty(updated))
        }
    }

    private fun profileState(base: SettingsUiState, profile: StudentProfile?): SettingsUiState {
        val student = profile?.studentInfo ?: StudentInfo()
        return base.copy(
            profiles = studentProfileRepository.profiles.value,
            editingProfileId = profile?.id,
            displayName = profile?.displayName.orEmpty(),
            schoolQuery = student.schoolName,
            selectedSchool = student.takeIf { it.hasSchoolSelection() }?.toSchoolInfo(),
            schoolResults = emptyList(),
            searchMessage = if (student.schoolName.isNotBlank() && !student.hasSchoolSelection()) {
                appContext.getString(R.string.school_search_reselect_required)
            } else {
                ""
            },
            isSearching = false,
            grade = student.grade,
            classroom = student.classroom,
            canDeleteProfile = profile != null && studentProfileRepository.profiles.value.size > 1,
            hasUnsavedProfileChanges = false
        )
    }

    private fun isProfileDirty(state: SettingsUiState): Boolean {
        val original = editingOriginalProfile
        if (original == null) return hasProfileDraft(state)
        val student = original.studentInfo
        return state.displayName != original.displayName ||
            state.schoolQuery != student.schoolName ||
            state.selectedSchool?.officeCode != student.officeCode ||
            state.selectedSchool?.schoolCode != student.schoolCode ||
            state.grade != student.grade ||
            state.classroom != student.classroom
    }

    private fun hasProfileDraft(state: SettingsUiState): Boolean {
        return state.displayName.isNotBlank() || state.schoolQuery.isNotBlank() ||
            state.selectedSchool != null || state.grade.isNotBlank() || state.classroom.isNotBlank()
    }

    private fun ProfileMutationFailure.messageResource(): Int = when (this) {
        ProfileMutationFailure.BLANK_NAME -> R.string.setup_error_profile_name_required
        ProfileMutationFailure.NAME_TOO_LONG -> R.string.setup_error_profile_name_too_long
        ProfileMutationFailure.DUPLICATE_NAME -> R.string.setup_error_profile_name_duplicate
        ProfileMutationFailure.LAST_PROFILE -> R.string.settings_profile_last_delete_error
        else -> R.string.setup_error_profile_save_failed
    }

    companion object {
        private const val MIN_SCHOOL_QUERY_LENGTH = 2
    }
}
