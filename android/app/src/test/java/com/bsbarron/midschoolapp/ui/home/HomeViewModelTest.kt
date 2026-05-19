package com.bsbarron.midschoolapp.ui.home

import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import com.bsbarron.midschoolapp.test.FakeSchoolRepository
import com.bsbarron.midschoolapp.test.TestApplication
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {

    private val application = TestApplication()

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
