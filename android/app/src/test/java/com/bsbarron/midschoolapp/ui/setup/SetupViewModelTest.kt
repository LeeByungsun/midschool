package com.bsbarron.midschoolapp.ui.setup

import android.app.Application
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupViewModelTest {

    @Test
    fun saveStudentInfo_whenInputsAreBlank_emitsValidationMessage() = runBlocking {
        val repository = FakePreferencesRepository()
        val viewModel = SetupViewModel(Application(), repository)
        val messageDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.messageEvent.first() }
        }

        viewModel.saveStudentInfo()

        assertEquals(R.string.setup_error_empty, messageDeferred.await())
        assertTrue(repository.savedStudentInfoCalls.isEmpty())
    }

    @Test
    fun saveStudentInfo_whenInputsAreValid_savesStudentInfoAndNavigates() = runBlocking {
        val repository = FakePreferencesRepository()
        val viewModel = SetupViewModel(Application(), repository)
        val navigationDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.navigationEvent.first() }
        }

        viewModel.updateGrade("2")
        viewModel.updateClassroom("3")
        viewModel.saveStudentInfo()

        navigationDeferred.await()
        assertEquals(listOf(StudentInfo("2", "3")), repository.savedStudentInfoCalls)
    }
}
