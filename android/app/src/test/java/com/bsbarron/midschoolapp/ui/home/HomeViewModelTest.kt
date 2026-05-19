package com.bsbarron.midschoolapp.ui.home

import android.app.Application
import android.content.Context
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import com.bsbarron.midschoolapp.test.FakeSchoolRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    private val application = TestApplication()
    private val textResolver = { id: Int, formatArgs: Array<out Any?> ->
        when (id) {
            R.string.home_school_name_placeholder -> "학교를 설정해 주세요"
            R.string.home_student_info_format -> "${formatArgs[0]}학년 ${formatArgs[1]}반"
            R.string.home_school_not_set_hint -> "학교 설정이 필요해요"
            R.string.home_school_not_set_summary ->
                "학교/학년/반이 설정되지 않아 급식·일정·시간표 조회가 잠시 중단돼 있어요. 설정에서 학교를 먼저 등록해 주세요."
            R.string.home_meal_missing_school -> "학교 설정 후 급식 정보를 확인할 수 있어요."
            R.string.home_schedule_missing_school -> "학교 설정 후 이번 달 학사 일정을 확인해 주세요."
            R.string.home_today_summary_error -> "나이스 데이터를 불러오지 못해 일부 정보는 기본 화면으로 표시 중입니다."
            R.string.home_today_summary_body -> "오늘 급식과 학사 일정을 먼저 확인하고, 시간표는 별도 화면에서 자세히 볼 수 있어요."
            R.string.home_semester_label -> "학년과 반을 설정해 주세요"
            else -> error("Unexpected string id: $id")
        }
    }

    @Test
    fun refreshHeader_whenSchoolIsMissing_showsSetupFallback() {
        val viewModel = HomeViewModel.createForTest(
            application = application,
            schoolRepository = FakeSchoolRepository(),
            preferencesRepository = FakePreferencesRepository(),
            textResolver = textResolver
        )

        val state = viewModel.uiState.value

        assertFalse(state.isSchoolConfigured)
        assertEquals("학교를 설정해 주세요", state.schoolName)
        assertEquals("학교 설정이 필요해요", state.classSummary)
        assertEquals(
            "오늘 급식과 학사 일정을 먼저 확인하고, 시간표는 별도 화면에서 자세히 볼 수 있어요.",
            state.todaySummaryText
        )
    }

    @Test
    fun refreshHeader_afterStudentInfoChanges_readsLatestSharedStudentInfo() {
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
        val viewModel = HomeViewModel.createForTest(
            application = application,
            schoolRepository = FakeSchoolRepository(),
            preferencesRepository = repository,
            textResolver = textResolver
        )

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
        val state = viewModel.uiState.value

        assertTrue(state.isSchoolConfigured)
        assertEquals("미사고등학교", state.schoolName)
        assertEquals("2학년 4반", state.classSummary)
    }

    @Test
    fun loadHomeData_whenSchoolIsNotConfigured_showsSetupMessagesAndSkipsRemoteFetch() = runBlocking {
        val schoolRepository = FakeSchoolRepository()
        val viewModel = HomeViewModel.createForTest(
            application = application,
            schoolRepository = schoolRepository,
            preferencesRepository = FakePreferencesRepository(studentInfo = StudentInfo(schoolName = "미사중학교")),
            textResolver = textResolver
        )

        viewModel.loadHomeData()
        awaitLoadComplete(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isSchoolConfigured)
        assertEquals(
            "학교/학년/반이 설정되지 않아 급식·일정·시간표 조회가 잠시 중단돼 있어요. 설정에서 학교를 먼저 등록해 주세요.",
            state.todaySummaryText
        )
        assertEquals("학교 설정 후 급식 정보를 확인할 수 있어요.", state.mealSummary)
        assertEquals("", state.mealMeta)
        assertEquals("학교 설정 후 이번 달 학사 일정을 확인해 주세요.", state.eventSummary)
        assertEquals(0, schoolRepository.mealsCallCount)
        assertEquals(0, schoolRepository.schedulesCallCount)
    }

    @Test
    fun loadHomeData_afterSettingsChanged_readsLatestStudentInfoOnResyncedLoad() = runBlocking {
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
        val schoolRepository = FakeSchoolRepository(
            mealsResult = Result.failure(IllegalStateException("MEAL_FAIL")),
            schedulesResult = Result.success(emptyList())
        )
        val viewModel = HomeViewModel.createForTest(
            application = application,
            schoolRepository = schoolRepository,
            preferencesRepository = repository,
            textResolver = textResolver
        )

        repository.saveStudentInfo(
            StudentInfo(
                grade = "3",
                classroom = "4",
                schoolName = "미사고등학교",
                officeCode = "J10",
                schoolCode = "7654321",
                schoolKind = "고등학교"
            )
        )

        viewModel.loadHomeData()
        awaitLoadComplete(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.isSchoolConfigured)
        assertEquals("미사고등학교", state.schoolName)
        assertEquals("3학년 4반", state.classSummary)
        assertEquals("나이스 데이터를 불러오지 못해 일부 정보는 기본 화면으로 표시 중입니다.", state.todaySummaryText)
        assertEquals("MEAL_FAIL", state.errorMessage)
        assertEquals("오늘은 등록된 급식이 없어요.", state.mealSummary)
        assertEquals("이번 달에 남아 있는 학사 일정이 없어요.", state.eventSummary)
        assertEquals(1, schoolRepository.mealsCallCount)
        assertEquals(1, schoolRepository.schedulesCallCount)
    }

    private suspend fun awaitLoadComplete(homeViewModel: HomeViewModel) {
        repeat(50) {
            if (!homeViewModel.uiState.value.isLoading) {
                delay(10)
                if (!homeViewModel.uiState.value.isLoading) return
            }
            delay(10)
        }
    }

    private class TestApplication : Application() {
        override fun getApplicationContext(): Context = this
    }
}
