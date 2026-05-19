package com.bsbarron.midschoolapp.ui.setup

import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import com.bsbarron.midschoolapp.test.FakeSchoolRepository
import com.bsbarron.midschoolapp.test.TestApplication
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupViewModelTest {

    @Test
    fun init_withSavedStudentInfo_restoresSchoolAndClassroomState() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "2",
                classroom = "5",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val viewModel = SetupViewModel(TestApplication(), repository, FakeSchoolRepository())

        val state = viewModel.uiState.value

        assertEquals("미사중학교", state.schoolQuery)
        assertEquals("2", state.grade)
        assertEquals("5", state.classroom)
        assertEquals("1234567", state.selectedSchool?.schoolCode)
    }

    @Test
    fun updateSchoolQuery_withDifferentText_clearsSelectedSchool() {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "2",
                classroom = "5",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val viewModel = SetupViewModel(TestApplication(), repository, FakeSchoolRepository())

        viewModel.updateSchoolQuery("다른학교")

        val state = viewModel.uiState.value
        assertEquals("다른학교", state.schoolQuery)
        assertNull(state.selectedSchool)
    }

    @Test
    fun saveStudentInfo_whenSchoolIsMissing_emitsValidationMessage() = runBlocking {
        val repository = FakePreferencesRepository()
        val viewModel = SetupViewModel(TestApplication(), repository, FakeSchoolRepository())
        val messageDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.messageEvent.first() }
        }

        viewModel.updateGrade("2")
        viewModel.updateClassroom("3")
        viewModel.saveStudentInfo()

        assertEquals(R.string.setup_error_school_required, messageDeferred.await())
        assertTrue(repository.savedStudentInfoCalls.isEmpty())
    }

    @Test
    fun saveStudentInfo_whenStateIsComplete_savesStudentInfoAndNavigates() = runBlocking {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "1",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val viewModel = SetupViewModel(TestApplication(), repository, FakeSchoolRepository())
        val navigationDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.navigationEvent.first() }
        }

        viewModel.updateGrade("2")
        viewModel.updateClassroom("3")
        viewModel.saveStudentInfo()

        navigationDeferred.await()
        assertEquals(
            listOf(
                StudentInfo(
                    grade = "2",
                    classroom = "3",
                    schoolName = "미사중학교",
                    officeCode = "J10",
                    schoolCode = "1234567",
                    schoolKind = "중학교"
                )
            ),
            repository.savedStudentInfoCalls
        )
    }
}
