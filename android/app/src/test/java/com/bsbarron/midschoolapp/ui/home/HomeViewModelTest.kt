package com.bsbarron.midschoolapp.ui.home

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.MisSchoolApplication
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import com.bsbarron.midschoolapp.test.FakeSchoolRepository
import com.bsbarron.midschoolapp.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.annotation.Config
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(application = MisSchoolApplication::class, sdk = [34])
class HomeViewModelTest {

    @Test
    fun home_refreshesHeaderAfterSettingsSave() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<Application>()
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

    @Test
    fun `refreshHeader_updatesStudentInfoAfterSettingsChangedWithoutRecreatingViewModel`() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val viewModel = HomeViewModel(application, FakeSchoolRepository(), repository)

        assertEquals("미사중학교", viewModel.uiState.value.schoolName)
        assertEquals("1학년 2반", viewModel.uiState.value.classSummary)

        repository.saveStudentInfo(
            StudentInfo(
                grade = "2",
                classroom = "4",
                schoolName = "미사고등학교",
                officeCode = "J10",
                schoolCode = "7654321",
                schoolKind = "고등학교"
            )
        )

        viewModel.refreshHeader()
        val updated = viewModel.uiState.value

        assertEquals("미사고등학교", updated.schoolName)
        assertEquals("2학년 4반", updated.classSummary)
        assertEquals(true, updated.isSchoolConfigured)
    }

    @Test
    fun `refreshHeader_showsPlaceholderWhenSchoolSelectionIsMissing`() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val viewModel = HomeViewModel(application, FakeSchoolRepository(), repository)

        repository.saveStudentInfo(StudentInfo())
        viewModel.refreshHeader()
        val updated = viewModel.uiState.value

        assertEquals(false, updated.isSchoolConfigured)
        assertEquals("학교를 설정해 주세요", updated.schoolName)
        assertEquals("학교 설정이 필요해요", updated.classSummary)
    }
}
