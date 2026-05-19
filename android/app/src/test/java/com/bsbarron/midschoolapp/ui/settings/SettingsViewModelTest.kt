package com.bsbarron.midschoolapp.ui.settings

import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.data.repository.TimerDisplayMode
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import com.bsbarron.midschoolapp.test.FakeSchoolRepository
import com.bsbarron.midschoolapp.test.TestApplication
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

    @Test
    fun init_readsCurrentStudentAndTimerSettings() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "4",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            ),
            timerDisplayMode = TimerDisplayMode.RING,
            notificationEnabled = false,
            vibrationEnabled = true
        )

        val viewModel = SettingsViewModel(TestApplication(), repository, FakeSchoolRepository())
        val state = viewModel.uiState.value

        assertEquals("미사중학교", state.schoolQuery)
        assertEquals("1", state.grade)
        assertEquals("4", state.classroom)
        assertEquals("1234567", state.selectedSchool?.schoolCode)
        assertTrue(state.isRingMode)
        assertFalse(state.notificationEnabled)
        assertTrue(state.vibrationEnabled)
    }

    @Test
    fun updateSchoolQuery_withDifferentText_clearsSelectedSchool() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "4",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val viewModel = SettingsViewModel(TestApplication(), repository, FakeSchoolRepository())

        viewModel.updateSchoolQuery("다른학교")

        val state = viewModel.uiState.value
        assertEquals("다른학교", state.schoolQuery)
        assertNull(state.selectedSchool)
    }

    @Test
    fun saveSettings_whenSchoolIsMissing_emitsValidationMessage() = runBlocking {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(grade = "1", classroom = "2")
        )
        val viewModel = SettingsViewModel(TestApplication(), repository, FakeSchoolRepository())
        val messageDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.messageEvent.first() }
        }

        viewModel.saveSettings()

        assertEquals(R.string.setup_error_school_required, messageDeferred.await())
        assertTrue(repository.savedStudentInfoCalls.isEmpty())
        assertTrue(repository.savedTimerDisplayModes.isEmpty())
    }

    @Test
    fun saveSettings_whenInputsAreValid_persistsSettingsAndCloses() = runBlocking {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = "미사초등학교",
                officeCode = "B10",
                schoolCode = "7654321",
                schoolKind = "초등학교"
            ),
            timerDisplayMode = TimerDisplayMode.COUNT,
            notificationEnabled = true,
            vibrationEnabled = true
        )
        val viewModel = SettingsViewModel(TestApplication(), repository, FakeSchoolRepository())
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
                    schoolName = "미사초등학교",
                    officeCode = "B10",
                    schoolCode = "7654321",
                    schoolKind = "초등학교"
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

    private class FakeSchoolRepository : SchoolRepository {
        override suspend fun searchSchools(query: String): Result<List<com.bsbarron.midschoolapp.data.model.SchoolInfo>> =
            Result.failure(IllegalStateException("unused"))

        override suspend fun getMeals(date: String?): Result<List<MealInfo>> =
            Result.failure(IllegalStateException("unused"))

        override suspend fun getSchedules(date: String?): Result<List<SchoolEvent>> =
            Result.failure(IllegalStateException("unused"))

        override suspend fun getTimetable(
            grade: String,
            classroom: String,
            date: String?
        ): Result<List<TimetableItem>> =
            Result.failure(IllegalStateException("unused"))
    }
}
