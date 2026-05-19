package com.bsbarron.midschoolapp.ui.home

import android.app.Application
import android.content.Context
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import com.bsbarron.midschoolapp.test.FakeSchoolRepository
import com.bsbarron.midschoolapp.ui.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    private val application = TestApplication()

    private val initialSchool = SchoolInfo(
        officeCode = "J10",
        schoolCode = "1111111",
        schoolName = "미사중학교",
        schoolKind = "중학교"
    )

    private val updatedSchool = SchoolInfo(
        officeCode = "J10",
        schoolCode = "2222222",
        schoolName = "미사고등학교",
        schoolKind = "고등학교"
    )

    @Test
    fun refreshHeader_whenSchoolIsMissing_showsSetupFallback() {
        val repository = FakePreferencesRepository()
        val viewModel = HomeViewModel(application, FakeSchoolRepository(), repository)

        val state = viewModel.uiState.value

        assertFalse(state.isSchoolConfigured)
        assertEquals("학교명 없음", state.schoolName)
        assertEquals("학교를 먼저 설정해 주세요.", state.classSummary)
        assertEquals("오늘의 요약", state.todaySummaryText)
    }

    @Test
    fun refreshHeader_afterSettingsSave_readsLatestSharedStudentInfo() = runBlocking {
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
        val homeViewModel = HomeViewModel(application, FakeSchoolRepository(), repository)
        val settingsViewModel = SettingsViewModel(application, repository, FakeSchoolRepository())

        settingsViewModel.selectSchool(updatedSchool)
        settingsViewModel.updateGrade("3")
        settingsViewModel.updateClassroom("4")
        settingsViewModel.saveSettings()

        homeViewModel.refreshHeader()

        val state = homeViewModel.uiState.value
        assertTrue(state.isSchoolConfigured)
        assertEquals(updatedSchool.schoolName, state.schoolName)
        assertEquals("3학년 4반", state.classSummary)
        assertEquals(
            StudentInfo(
                grade = "3",
                classroom = "4",
                schoolName = updatedSchool.schoolName,
                officeCode = updatedSchool.officeCode,
                schoolCode = updatedSchool.schoolCode,
                schoolKind = updatedSchool.schoolKind
            ),
            repository.currentStudentInfo
        )
    }

    @Test
    fun loadHomeData_whenSchoolIsNotConfigured_showsSetupMessagesAndSkipsRemoteFetch() = runBlocking {
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(schoolName = "미사중학교")
        )
        val schoolRepository = FakeSchoolRepository()
        val viewModel = HomeViewModel(application, schoolRepository, preferencesRepository)

        viewModel.loadHomeData()
        awaitLoadComplete(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isSchoolConfigured)
        assertEquals("학교 설정 후 급식·일정 정보를 확인해 주세요.", state.todaySummaryText)
        assertEquals("학교 설정 후 급식 정보를 확인할 수 있어요.", state.mealSummary)
        assertEquals("", state.mealMeta)
        assertEquals("학교 설정 후 이번 달 학사 일정을 확인해 주세요.", state.eventSummary)
        assertEquals(0, schoolRepository.mealsCallCount)
        assertEquals(0, schoolRepository.schedulesCallCount)
    }

    @Test
    fun loadHomeData_afterSettingsChanged_readsLatestStudentInfo_onResyncedLoad() = runBlocking {
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "4",
                schoolName = initialSchool.schoolName,
                officeCode = initialSchool.officeCode,
                schoolCode = initialSchool.schoolCode,
                schoolKind = initialSchool.schoolKind
            )
        )
        val schoolRepository = FakeSchoolRepository(
            mealsResult = Result.failure(RuntimeException("MEAL_FAIL")),
            schedulesResult = Result.success(emptyList())
        )
        val homeViewModel = HomeViewModel(application, schoolRepository, preferencesRepository)
        val settingsViewModel = SettingsViewModel(application, preferencesRepository, schoolRepository)

        settingsViewModel.selectSchool(updatedSchool)
        settingsViewModel.updateGrade("3")
        settingsViewModel.updateClassroom("4")
        settingsViewModel.saveSettings()

        homeViewModel.loadHomeData()
        awaitLoadComplete(homeViewModel)

        val state = homeViewModel.uiState.value
        assertTrue(state.isSchoolConfigured)
        assertEquals(updatedSchool.schoolName, state.schoolName)
        assertEquals("3학년 4반", state.classSummary)
        assertEquals("오늘의 요약을 불러오지 못했어요.", state.todaySummaryText)
        assertEquals("MEAL_FAIL", state.errorMessage)
        assertEquals("오늘은 등록된 급식이 없어요.", state.mealSummary)
        assertEquals("이번 달에 남아 있는 학사 일정이 없어요.", state.eventSummary)
        assertEquals(1, schoolRepository.mealsCallCount)
        assertEquals(1, schoolRepository.schedulesCallCount)
    }

    private suspend fun awaitLoadComplete(homeViewModel: HomeViewModel) {
        var hasStartedLoading = false
        repeat(20) {
            if (homeViewModel.uiState.value.isLoading) {
                hasStartedLoading = true
            } else if (hasStartedLoading) {
                return
            }
            delay(10)
        }
    }

    private class TestApplication : Application() {
        override fun getApplicationContext(): Context = this

        override fun getString(id: Int): String {
            return when (id) {
                R.string.home_school_name_placeholder -> "학교명 없음"
                R.string.home_school_not_set_hint -> "학교를 먼저 설정해 주세요."
                R.string.home_school_not_set_summary -> "학교 설정 후 급식·일정 정보를 확인해 주세요."
                R.string.home_meal_missing_school -> "학교 설정 후 급식 정보를 확인할 수 있어요."
                R.string.home_schedule_missing_school -> "학교 설정 후 이번 달 학사 일정을 확인해 주세요."
                R.string.home_today_summary_body -> "오늘의 요약"
                R.string.home_today_summary_error -> "오늘의 요약을 불러오지 못했어요."
                else -> super.getString(id)
            }
        }

        override fun getString(id: Int, vararg formatArgs: Any?): String {
            return when (id) {
                R.string.home_student_info_format -> String.format("%s학년 %s반", *formatArgs)
                R.string.school_search_selected -> String.format("%s 선택됨", *formatArgs)
                else -> super.getString(id, *formatArgs)
            }
        }
    }
}
