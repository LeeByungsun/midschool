package com.lbs.schoolhelper.ui.settings

import android.app.Application
import android.os.Looper
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.data.repository.TimerDisplayMode
import com.lbs.schoolhelper.test.FakePreferencesRepository
import com.lbs.schoolhelper.test.FakeSchoolRepository
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
    fun init_whenLegacySchoolNameExists_requiresSchoolReselection() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "4",
                schoolName = selectedSchool.schoolName
            )
        )

        val viewModel = SettingsViewModel(application, repository, FakeSchoolRepository())
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

        val viewModel = SettingsViewModel(application, repository, FakeSchoolRepository())
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

        val viewModel = SettingsViewModel(application, repository, FakeSchoolRepository())
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
        val viewModel = SettingsViewModel(application, repository, FakeSchoolRepository())

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
        val viewModel = SettingsViewModel(application, repository, FakeSchoolRepository())

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
        val viewModel = SettingsViewModel(application, FakePreferencesRepository(), schoolRepository)

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
        val viewModel = SettingsViewModel(application, FakePreferencesRepository(), schoolRepository)

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
        val viewModel = SettingsViewModel(application, FakePreferencesRepository(), schoolRepository)

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
        val viewModel = SettingsViewModel(application, FakePreferencesRepository(), schoolRepository)

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
        val viewModel = SettingsViewModel(application, repository, schoolRepository)

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
        val viewModel = SettingsViewModel(application, repository, FakeSchoolRepository())
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
        val viewModel = SettingsViewModel(application, repository, FakeSchoolRepository())
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
        val viewModel = SettingsViewModel(application, repository, FakeSchoolRepository())
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
            listOf(
                StudentInfo(
                    grade = "3",
                    classroom = "5",
                    schoolName = selectedSchool.schoolName,
                    officeCode = selectedSchool.officeCode,
                    schoolCode = selectedSchool.schoolCode,
                    schoolKind = selectedSchool.schoolKind
                )
            ),
            repository.savedStudentInfoCalls
        )
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
        val viewModel = SettingsViewModel(application, repository, FakeSchoolRepository())
        val messageDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.messageEvent.first() }
        }

        viewModel.updateSchoolQuery("다른 학교")
        viewModel.saveSettings()

        assertEquals(R.string.setup_error_school_required, messageDeferred.await())
        assertEquals(selectedSchool, viewModel.uiState.value.selectedSchool)
        assertTrue(repository.savedStudentInfoCalls.isEmpty())
    }
}
