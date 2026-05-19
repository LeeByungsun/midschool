package com.bsbarron.midschoolapp.ui.settings

import android.app.Application
import android.content.Context
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.data.repository.TimerDisplayMode
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import com.bsbarron.midschoolapp.test.FakeSchoolRepository
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

class SettingsViewModelTest {

    private val application = TestApplication()

    private val selectedSchool = SchoolInfo(
        officeCode = "J10",
        schoolCode = "1234567",
        schoolName = "미사중학교",
        schoolKind = "중학교"
    )

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

    private class TestApplication : Application() {
        override fun getApplicationContext(): Context = this
    }
}
