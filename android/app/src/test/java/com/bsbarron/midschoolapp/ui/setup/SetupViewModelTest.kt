package com.bsbarron.midschoolapp.ui.setup

import android.app.Application
import android.content.Context
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupViewModelTest {

    @Test
    fun saveStudentInfo_whenSchoolNotSelected_emitsSchoolRequiredMessage() = runBlocking {
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
    fun saveStudentInfo_whenInputsAreValid_savesStudentInfoAndNavigates() = runBlocking {
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "2",
                classroom = "3",
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
