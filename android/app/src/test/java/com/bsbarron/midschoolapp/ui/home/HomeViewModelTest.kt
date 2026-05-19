package com.bsbarron.midschoolapp.ui.home

import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import com.bsbarron.midschoolapp.test.FakeSchoolRepository
import com.bsbarron.midschoolapp.MainActivity
import com.bsbarron.midschoolapp.ui.settings.SettingsViewModel
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.annotation.Config
import com.bsbarron.midschoolapp.MisSchoolApplication

@RunWith(RobolectricTestRunner::class)
@Config(application = MisSchoolApplication::class)
class HomeViewModelTest {

    @Test
    fun home_refreshesHeaderAfterSettingsSave() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val initialSchool = SchoolInfo(
            officeCode = "J10",
            schoolCode = "1111111",
            schoolName = "구미중학교",
            schoolKind = "중학교"
        )
        val updatedSchool = SchoolInfo(
            officeCode = "J10",
            schoolCode = "2222222",
            schoolName = "구미고등학교",
            schoolKind = "고등학교"
        )

        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = initialSchool.schoolName,
                officeCode = initialSchool.officeCode,
                schoolCode = initialSchool.schoolCode,
                schoolKind = initialSchool.schoolKind
            )
        )
        val schoolRepository = FakeSchoolRepository()
        val homeViewModel = HomeViewModel(application, schoolRepository, repository)

        homeViewModel.refreshHeader()
        assertEquals("1", repository.currentStudentInfo.grade)
        assertEquals("구미중학교", homeViewModel.uiState.value.schoolName)
        assertEquals(
            application.getString(R.string.home_student_info_format, "1", "2"),
            homeViewModel.uiState.value.classSummary
        )

        val settingsViewModel = SettingsViewModel(application, repository, schoolRepository)
        settingsViewModel.selectSchool(updatedSchool)
        settingsViewModel.updateGrade("2")
        settingsViewModel.updateClassroom("5")

        val messageDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { settingsViewModel.messageEvent.first() }
        }
        val closeDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { settingsViewModel.closeEvent.first() }
        }

        settingsViewModel.saveSettings()
        messageDeferred.await()
        closeDeferred.await()

        homeViewModel.refreshHeader()
        assertEquals("2", repository.currentStudentInfo.grade)
        assertEquals("구미고등학교", homeViewModel.uiState.value.schoolName)
        assertEquals(
            application.getString(R.string.home_student_info_format, "2", "5"),
            homeViewModel.uiState.value.classSummary
        )
    }
}
