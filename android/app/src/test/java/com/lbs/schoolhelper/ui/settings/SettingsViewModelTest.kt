package com.lbs.schoolhelper.ui.settings

import android.app.Application
import android.os.Looper
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.data.repository.TimerDisplayMode
import com.lbs.schoolhelper.test.FakePreferencesRepository
import com.lbs.schoolhelper.test.FakeSchoolRepository
import com.lbs.schoolhelper.test.FakeStudentProfileRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class SettingsViewModelTest {

    private val application: Application = RuntimeEnvironment.getApplication()

    private val selectedSchool = SchoolInfo(
        officeCode = "J10",
        schoolCode = "1234567",
        schoolName = "미사중학교",
        schoolKind = "중학교"
    )

    @Test
    fun loadSettings_selectsActiveProfileForEditing() {
        val first = profile("first", "민준", "1")
        val second = profile("second", "서연", "2")

        val viewModel = createViewModel(
            profiles = FakeStudentProfileRepository(listOf(first, second), second.id)
        )

        assertEquals(second.id, viewModel.uiState.value.editingProfileId)
        assertEquals("서연", viewModel.uiState.value.displayName)
        assertEquals("2", viewModel.uiState.value.grade)
        assertEquals(listOf(first, second), viewModel.uiState.value.profiles)
    }

    @Test
    fun startAddingProfile_clearsOnlyProfileFormAndPreservesGlobalSettings() {
        val preferences = FakePreferencesRepository(
            timerDisplayMode = TimerDisplayMode.RING,
            notificationEnabled = false,
            vibrationEnabled = true
        )
        val viewModel = createViewModel(
            preferences = preferences,
            profiles = FakeStudentProfileRepository(listOf(profile("first", "민준", "1")), "first")
        )

        viewModel.startAddingProfile()

        val state = viewModel.uiState.value
        assertNull(state.editingProfileId)
        assertEquals("", state.displayName)
        assertEquals("", state.schoolQuery)
        assertTrue(state.isRingMode)
        assertFalse(state.notificationEnabled)
        assertTrue(state.vibrationEnabled)
    }

    @Test
    fun saveNewProfile_makesNewProfileActive() = runBlocking {
        val first = profile("first", "민준", "1")
        val profiles = FakeStudentProfileRepository(listOf(first), first.id)
        val viewModel = createViewModel(profiles = profiles)

        viewModel.startAddingProfile()
        fillValidProfile(viewModel, "서연", "2")
        viewModel.saveEditingProfile()

        assertEquals("서연", profiles.activeProfile.value?.displayName)
        assertEquals(profiles.activeProfile.value?.id, viewModel.uiState.value.editingProfileId)
    }

    @Test
    fun saveExistingInactiveProfile_doesNotChangeActiveProfile() = runBlocking {
        val first = profile("first", "민준", "1")
        val second = profile("second", "서연", "2")
        val profiles = FakeStudentProfileRepository(listOf(first, second), first.id)
        val viewModel = createViewModel(profiles = profiles)

        viewModel.selectEditingProfile(second.id)
        viewModel.updateDisplayName("서연이")
        viewModel.saveEditingProfile()

        assertEquals(first.id, profiles.activeProfile.value?.id)
        assertEquals("서연이", profiles.getProfile(second.id)?.displayName)
    }

    @Test
    fun saveProfile_duplicateTrimmedNameShowsDuplicateError() = runBlocking {
        val first = profile("first", "민준", "1")
        val second = profile("second", "서연", "2")
        val profiles = FakeStudentProfileRepository(listOf(first, second), first.id)
        val viewModel = createViewModel(profiles = profiles)
        val message = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.messageEvent.first() }
        }

        viewModel.selectEditingProfile(second.id)
        viewModel.updateDisplayName("  민준  ")
        viewModel.saveEditingProfile()

        assertEquals(R.string.setup_error_profile_name_duplicate, message.await())
        assertEquals("서연", profiles.getProfile(second.id)?.displayName)
    }

    @Test
    fun switchEditingProfile_withDirtyFormRequestsSaveDiscardOrCancel() = runBlocking {
        val first = profile("first", "민준", "1")
        val second = profile("second", "서연", "2")
        val viewModel = createViewModel(
            profiles = FakeStudentProfileRepository(listOf(first, second), first.id)
        )
        val prompt = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.unsavedProfileChangesEvent.first() }
        }

        viewModel.updateDisplayName("수정 중")
        viewModel.selectEditingProfile(second.id)

        prompt.await()
        assertEquals(first.id, viewModel.uiState.value.editingProfileId)
        assertTrue(viewModel.uiState.value.hasUnsavedProfileChanges)

        viewModel.resolveUnsavedProfileChanges(UnsavedProfileDecision.DISCARD)
        assertEquals(second.id, viewModel.uiState.value.editingProfileId)
        assertFalse(viewModel.uiState.value.hasUnsavedProfileChanges)
    }

    @Test
    fun deleteProfile_whenOnlyOneDisablesDeletion() {
        val only = profile("only", "민준", "1")
        val viewModel = createViewModel(
            profiles = FakeStudentProfileRepository(listOf(only), only.id)
        )

        assertFalse(viewModel.uiState.value.canDeleteProfile)
    }

    @Test
    fun deleteActiveProfile_selectsRepositoryFallbackAndRefreshesForm() = runBlocking {
        val first = profile("first", "민준", "1")
        val second = profile("second", "서연", "2")
        val profiles = FakeStudentProfileRepository(listOf(first, second), first.id)
        val viewModel = createViewModel(profiles = profiles)

        viewModel.deleteEditingProfile()

        assertEquals(second.id, profiles.activeProfile.value?.id)
        assertEquals(second.id, viewModel.uiState.value.editingProfileId)
        assertEquals("서연", viewModel.uiState.value.displayName)
    }

    @Test
    fun `collection choice applies without saving invalid school form`() {
        val repository = FakePreferencesRepository()
        val changes = mutableListOf<Pair<Boolean, Boolean>>()
        val telemetry = object : com.lbs.schoolhelper.telemetry.AppTelemetry {
            override fun setCollection(analytics: Boolean, diagnostics: Boolean) { changes += analytics to diagnostics }
        }
        val vm = createViewModel(repository, FakeSchoolRepository(), telemetry = telemetry)
        vm.updateAnalyticsEnabled(true)
        vm.updateDiagnosticsEnabled(true)
        vm.updateAnalyticsEnabled(false)
        assertEquals(listOf(true to false, true to true, false to true), changes)
        assertFalse(repository.isAnalyticsEnabled())
        assertTrue(repository.isDiagnosticsEnabled())
        val restored = createViewModel(repository, FakeSchoolRepository(), telemetry = telemetry)
        assertFalse(restored.uiState.value.analyticsEnabled)
        assertTrue(restored.uiState.value.diagnosticsEnabled)
    }

    @Test
    fun `school change reports old identity after new identity is saved`() = runBlocking {
        val old = StudentInfo("2", "5", "이전학교", "B10", "7654321", "중학교")
        val prefs = FakePreferencesRepository(studentInfo = old)
        val changes = mutableListOf<StudentInfo>()
        val telemetry = object : com.lbs.schoolhelper.telemetry.AppTelemetry {
            override fun schoolSaved(previous: StudentInfo, current: StudentInfo) {
                assertEquals("7654321", prefs.getStudentInfo().schoolCode)
                assertEquals("1234567", current.schoolCode)
                changes += previous
            }
        }
        val vm = createViewModel(prefs, FakeSchoolRepository(), telemetry = telemetry)
        vm.selectSchool(selectedSchool)
        vm.saveSettings()
        assertEquals(listOf(old), changes)
    }

    @Test
    fun init_whenLegacySchoolNameExists_requiresSchoolReselection() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "4",
                schoolName = selectedSchool.schoolName
            )
        )

        val viewModel = createViewModel(repository, FakeSchoolRepository())
        val state = viewModel.uiState.value

        assertEquals(selectedSchool.schoolName, state.schoolQuery)
        assertEquals(application.getString(R.string.school_search_reselect_required), state.searchMessage)
        assertEquals(null, state.selectedSchool)
    }

    @Test
    fun init_readsCurrentStudentAndTimerSettings() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "4",
                schoolName = selectedSchool.schoolName,
                officeCode = selectedSchool.officeCode,
                schoolCode = selectedSchool.schoolCode,
                schoolKind = selectedSchool.schoolKind
            ),
            timerDisplayMode = TimerDisplayMode.RING,
            notificationEnabled = false,
            vibrationEnabled = true
        )

        val viewModel = createViewModel(repository, FakeSchoolRepository())
        val state = viewModel.uiState.value

        assertEquals(selectedSchool.schoolName, state.schoolQuery)
        assertEquals(selectedSchool, state.selectedSchool)
        assertEquals("1", state.grade)
        assertEquals("4", state.classroom)
        assertEquals("1234567", state.selectedSchool?.schoolCode)
        assertTrue(state.isRingMode)
        assertFalse(state.notificationEnabled)
        assertTrue(state.vibrationEnabled)
    }

    @Test
    fun init_whenLegacySchoolInfoIsIncomplete_prefillsQueryWithoutSelectingSchool() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "4",
                schoolName = selectedSchool.schoolName
            )
        )

        val viewModel = createViewModel(repository, FakeSchoolRepository())
        val state = viewModel.uiState.value

        assertEquals(selectedSchool.schoolName, state.schoolQuery)
        assertNull(state.selectedSchool)
        assertEquals("1", state.grade)
        assertEquals("4", state.classroom)
    }

    @Test
    fun updateSchoolQuery_whenTrimmedTextMatchesSelectedSchool_keepsSelection() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "4",
                schoolName = selectedSchool.schoolName,
                officeCode = selectedSchool.officeCode,
                schoolCode = selectedSchool.schoolCode,
                schoolKind = selectedSchool.schoolKind
            )
        )
        val viewModel = createViewModel(repository, FakeSchoolRepository())

        viewModel.updateSchoolQuery("  ${selectedSchool.schoolName}  ")

        assertEquals("  ${selectedSchool.schoolName}  ", viewModel.uiState.value.schoolQuery)
        assertEquals(selectedSchool, viewModel.uiState.value.selectedSchool)
    }

    @Test
    fun updateSchoolQuery_whenQueryChanges_preservesCurrentSelectionUntilReselect() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "4",
                schoolName = selectedSchool.schoolName,
                officeCode = selectedSchool.officeCode,
                schoolCode = selectedSchool.schoolCode,
                schoolKind = selectedSchool.schoolKind
            )
        )
        val viewModel = createViewModel(repository, FakeSchoolRepository())

        viewModel.updateSchoolQuery("다른 학교")

        assertEquals(selectedSchool, viewModel.uiState.value.selectedSchool)
        assertEquals(
            application.getString(R.string.school_search_reselect_current_selection),
            viewModel.uiState.value.searchMessage
        )
    }

    @Test
    fun searchSchools_whenQueryTooShort_showsValidationAndSkipsRepository() {
        val schoolRepository = FakeSchoolRepository()
        val viewModel = createViewModel(FakePreferencesRepository(), schoolRepository)

        viewModel.updateSchoolQuery("미")
        viewModel.searchSchools()
        shadowOf(Looper.getMainLooper()).idle()

        val state = viewModel.uiState.value
        assertEquals(application.getString(R.string.school_search_min_query), state.searchMessage)
        assertTrue(state.schoolResults.isEmpty())
        assertNull(state.selectedSchool)
        assertNull(schoolRepository.lastSearchQuery)
    }

    @Test
    fun searchSchools_whenSingleResult_selectsSchoolAndShowsMessage() {
        val schoolRepository = FakeSchoolRepository(
            schoolSearchResult = Result.success(listOf(selectedSchool))
        )
        val viewModel = createViewModel(FakePreferencesRepository(), schoolRepository)

        viewModel.updateSchoolQuery("미사중")
        viewModel.searchSchools()
        shadowOf(Looper.getMainLooper()).idle()

        val state = viewModel.uiState.value
        assertEquals("미사중", schoolRepository.lastSearchQuery)
        assertEquals(selectedSchool.schoolName, state.schoolQuery)
        assertEquals(selectedSchool, state.selectedSchool)
        assertEquals(listOf(selectedSchool), state.schoolResults)
        assertEquals(application.getString(R.string.school_search_single_result), state.searchMessage)
        assertFalse(state.isSearching)
    }

    @Test
    fun searchSchools_whenPreviousRequestFinishesLate_ignoresStaleResult() = runBlocking {
        val firstSchool = selectedSchool.copy(schoolName = "구미중학교", schoolCode = "1111111")
        val latestSchool = selectedSchool.copy(schoolName = "미사중학교", schoolCode = "2222222")
        val schoolRepository = FakeSchoolRepository().apply {
            searchResultsByQuery["구미"] = Result.success(listOf(firstSchool))
            searchResultsByQuery["미사"] = Result.success(listOf(latestSchool))
            searchDelayMillisByQuery["구미"] = 200L
            searchDelayMillisByQuery["미사"] = 10L
        }
        val viewModel = createViewModel(FakePreferencesRepository(), schoolRepository)

        viewModel.updateSchoolQuery("구미")
        viewModel.searchSchools()
        viewModel.updateSchoolQuery("미사")
        viewModel.searchSchools()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250))
        val state = viewModel.uiState.value

        assertEquals(listOf("구미", "미사"), schoolRepository.requestedSearchQueries)
        assertEquals(latestSchool.schoolName, state.schoolQuery)
        assertEquals(latestSchool, state.selectedSchool)
        assertEquals(listOf(latestSchool), state.schoolResults)
    }

    @Test
    fun searchSchools_whenMultipleResults_requiresExplicitSelection() {
        val alternativeSchool = selectedSchool.copy(
            schoolCode = "7654321",
            schoolName = "미사여자중학교"
        )
        val schoolRepository = FakeSchoolRepository(
            schoolSearchResult = Result.success(listOf(selectedSchool, alternativeSchool))
        )
        val viewModel = createViewModel(FakePreferencesRepository(), schoolRepository)

        viewModel.updateSchoolQuery("미사")
        viewModel.searchSchools()
        shadowOf(Looper.getMainLooper()).idle()

        val searchState = viewModel.uiState.value
        assertEquals(listOf(selectedSchool, alternativeSchool), searchState.schoolResults)
        assertNull(searchState.selectedSchool)
        assertEquals(application.getString(R.string.school_search_select_result), searchState.searchMessage)

        viewModel.selectSchool(alternativeSchool)

        val selectedState = viewModel.uiState.value
        assertEquals(alternativeSchool.schoolName, selectedState.schoolQuery)
        assertEquals(alternativeSchool, selectedState.selectedSchool)
        assertEquals(
            application.getString(R.string.school_search_selected, alternativeSchool.schoolName),
            selectedState.searchMessage
        )
    }

    @Test
    fun searchSchools_whenQueryReturnsEmpty_preservesCurrentSelection() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "4",
                schoolName = selectedSchool.schoolName,
                officeCode = selectedSchool.officeCode,
                schoolCode = selectedSchool.schoolCode,
                schoolKind = selectedSchool.schoolKind
            )
        )
        val schoolRepository = FakeSchoolRepository(
            schoolSearchResult = Result.success(emptyList())
        )
        val viewModel = createViewModel(repository, schoolRepository)

        viewModel.updateSchoolQuery("없는 학교")
        viewModel.searchSchools()
        shadowOf(Looper.getMainLooper()).idle()

        val state = viewModel.uiState.value
        assertEquals(selectedSchool, state.selectedSchool)
        assertEquals(application.getString(R.string.school_search_empty), state.searchMessage)
    }

    @Test
    fun saveSettings_whenSchoolIsMissing_emitsSchoolRequiredMessage() = runBlocking {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(grade = "1", classroom = "2")
        )
        val viewModel = createViewModel(repository, FakeSchoolRepository())
        val messageDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.messageEvent.first() }
        }

        viewModel.saveSettings()

        assertEquals(R.string.setup_error_school_required, messageDeferred.await())
        assertTrue(repository.savedStudentInfoCalls.isEmpty())
        assertTrue(repository.savedTimerDisplayModes.isEmpty())
    }

    @Test
    fun saveSettings_whenClassroomInfoIsMissing_emitsValidationMessage() = runBlocking {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                schoolName = selectedSchool.schoolName,
                officeCode = selectedSchool.officeCode,
                schoolCode = selectedSchool.schoolCode,
                schoolKind = selectedSchool.schoolKind
            ),
            timerDisplayMode = TimerDisplayMode.COUNT,
            notificationEnabled = true,
            vibrationEnabled = true
        )
        val viewModel = createViewModel(repository, FakeSchoolRepository())
        val messageDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.messageEvent.first() }
        }

        viewModel.saveSettings()

        assertEquals(R.string.setup_error_empty, messageDeferred.await())
        assertTrue(repository.savedStudentInfoCalls.isEmpty())
    }

    @Test
    fun saveSettings_whenInputsAreValid_persistsSettingsAndCloses() = runBlocking {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = selectedSchool.schoolName,
                officeCode = selectedSchool.officeCode,
                schoolCode = selectedSchool.schoolCode,
                schoolKind = selectedSchool.schoolKind
            ),
            timerDisplayMode = TimerDisplayMode.COUNT,
            notificationEnabled = true,
            vibrationEnabled = true
        )
        val original = StudentProfile("profile-1", "민준", repository.getStudentInfo())
        val profiles = FakeStudentProfileRepository(listOf(original), original.id)
        val viewModel = createViewModel(repository, FakeSchoolRepository(), profiles)
        val messageDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.messageEvent.first() }
        }
        val closeDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.closeEvent.first() }
        }

        viewModel.updateGrade("3")
        viewModel.updateClassroom("5")
        viewModel.updateDisplayMode(isRingMode = true)
        viewModel.updateNotificationEnabled(enabled = false)
        viewModel.updateVibrationEnabled(enabled = false)
        viewModel.saveSettings()

        assertEquals(R.string.settings_saved, messageDeferred.await())
        closeDeferred.await()
        assertEquals(
            StudentInfo(
                grade = "3",
                classroom = "5",
                schoolName = selectedSchool.schoolName,
                officeCode = selectedSchool.officeCode,
                schoolCode = selectedSchool.schoolCode,
                schoolKind = selectedSchool.schoolKind
            ),
            profiles.activeProfile.value?.studentInfo
        )
        assertTrue(repository.savedStudentInfoCalls.isEmpty())
        assertEquals(listOf(TimerDisplayMode.RING), repository.savedTimerDisplayModes)
        assertEquals(listOf(false), repository.savedNotificationEnabledValues)
        assertEquals(listOf(false), repository.savedVibrationEnabledValues)
    }

    @Test
    fun saveSettings_afterChangingSchoolQuery_requiresSchoolReselect() = runBlocking {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "4",
                schoolName = selectedSchool.schoolName,
                officeCode = selectedSchool.officeCode,
                schoolCode = selectedSchool.schoolCode,
                schoolKind = selectedSchool.schoolKind
            )
        )
        val viewModel = createViewModel(repository, FakeSchoolRepository())
        val messageDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.messageEvent.first() }
        }

        viewModel.updateSchoolQuery("다른 학교")
        viewModel.saveSettings()

        assertEquals(R.string.setup_error_school_required, messageDeferred.await())
        assertEquals(selectedSchool, viewModel.uiState.value.selectedSchool)
        assertTrue(repository.savedStudentInfoCalls.isEmpty())
    }

    private fun createViewModel(
        preferences: FakePreferencesRepository = FakePreferencesRepository(),
        schoolRepository: FakeSchoolRepository = FakeSchoolRepository(),
        profiles: FakeStudentProfileRepository? = null,
        telemetry: com.lbs.schoolhelper.telemetry.AppTelemetry = com.lbs.schoolhelper.telemetry.NoOpTelemetry
    ): SettingsViewModel {
        val resolvedProfiles = profiles ?: preferences.getStudentInfo().let { student ->
            val initial = if (
                student.schoolName.isNotBlank() || student.grade.isNotBlank() || student.classroom.isNotBlank()
            ) {
                listOf(StudentProfile("profile-1", "민준", student))
            } else {
                emptyList()
            }
            FakeStudentProfileRepository(initial, initial.firstOrNull()?.id)
        }
        return SettingsViewModel(
            application = application,
            preferencesRepository = preferences,
            schoolRepository = schoolRepository,
            studentProfileRepository = resolvedProfiles,
            telemetry = telemetry
        )
    }

    private fun profile(id: String, name: String, grade: String): StudentProfile {
        return StudentProfile(
            id = id,
            displayName = name,
            studentInfo = StudentInfo(
                grade = grade,
                classroom = "3",
                schoolName = selectedSchool.schoolName,
                officeCode = selectedSchool.officeCode,
                schoolCode = selectedSchool.schoolCode,
                schoolKind = selectedSchool.schoolKind
            )
        )
    }

    private fun fillValidProfile(viewModel: SettingsViewModel, name: String, grade: String) {
        viewModel.updateDisplayName(name)
        viewModel.selectSchool(selectedSchool)
        viewModel.updateGrade(grade)
        viewModel.updateClassroom("3")
    }
}
