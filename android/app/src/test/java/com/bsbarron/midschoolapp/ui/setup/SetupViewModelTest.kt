package com.bsbarron.midschoolapp.ui.setup

import android.app.Application
import android.content.Context
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import com.bsbarron.midschoolapp.test.FakeSchoolRepository
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

    private val application = TestApplication()

    private val selectedSchool = SchoolInfo(
        officeCode = "J10",
        schoolCode = "1234567",
        schoolName = "미사중학교",
        schoolKind = "중학교"
    )

    @Test
    fun saveStudentInfo_whenSchoolIsMissing_emitsSchoolRequiredMessage() = runBlocking {
        val repository = FakePreferencesRepository()
        val viewModel = SetupViewModel(application, repository, FakeSchoolRepository())
        val messageDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.messageEvent.first() }
        }

        viewModel.saveStudentInfo()

        assertEquals(R.string.setup_error_school_required, messageDeferred.await())
        assertTrue(repository.savedStudentInfoCalls.isEmpty())
    }

    @Test
    fun saveStudentInfo_whenClassroomInfoIsMissing_emitsValidationMessage() = runBlocking {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                schoolName = selectedSchool.schoolName,
                officeCode = selectedSchool.officeCode,
                schoolCode = selectedSchool.schoolCode,
                schoolKind = selectedSchool.schoolKind
            )
        )
        val viewModel = SetupViewModel(application, repository, FakeSchoolRepository())
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
    fun saveStudentInfo_whenInputsAreValid_savesStudentInfoAndNavigates() = runBlocking {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                schoolName = selectedSchool.schoolName,
                officeCode = selectedSchool.officeCode,
                schoolCode = selectedSchool.schoolCode,
                schoolKind = selectedSchool.schoolKind
            )
        )
        val viewModel = SetupViewModel(application, repository, FakeSchoolRepository())
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
                    schoolName = selectedSchool.schoolName,
                    officeCode = selectedSchool.officeCode,
                    schoolCode = selectedSchool.schoolCode,
                    schoolKind = selectedSchool.schoolKind
                )
            ),
            repository.savedStudentInfoCalls
        )
    }

    private class TestApplication : Application() {
        override fun getApplicationContext(): Context = this
    }
}
