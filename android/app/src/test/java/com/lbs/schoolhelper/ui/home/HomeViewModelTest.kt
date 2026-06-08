package com.lbs.schoolhelper.ui.home

import com.lbs.schoolhelper.MainActivity
import com.lbs.schoolhelper.MisSchoolApplication
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.model.HomeContentStatus
import com.lbs.schoolhelper.data.model.MealInfo
import com.lbs.schoolhelper.data.model.NoticeFeed
import com.lbs.schoolhelper.data.model.NoticePreview
import com.lbs.schoolhelper.data.model.SchoolEvent
import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.data.repository.SchoolRepository
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.test.FakePreferencesRepository
import com.lbs.schoolhelper.test.FakeSchoolRepository
import com.lbs.schoolhelper.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = MisSchoolApplication::class, sdk = [34])
class HomeViewModelTest {

    @Test
    fun configuredHeader_usesNoticePlaceholderInsteadOfBlankState() {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = "구미중학교",
                officeCode = "J10",
                schoolCode = "1111111",
                schoolKind = "중학교"
            )
        )
        val viewModel = HomeViewModel(application, FakeSchoolRepository(), repository)

        assertEquals(application.getString(R.string.home_notice_empty), viewModel.uiState.value.notices.summary)
        assertEquals(
            application.getString(R.string.home_notice_unavailable_button),
            viewModel.uiState.value.notices.actionText
        )
        assertFalse(viewModel.uiState.value.notices.actionEnabled)
        assertEquals(null, viewModel.uiState.value.notices.latestNoticeUrl)
        assertFalse(viewModel.uiState.value.notices.requiresSetup)
    }

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

    @Test
    fun loadHomeData_marksNotConfiguredStateWhenSchoolSelectionIsMissing() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val repository = FakePreferencesRepository(studentInfo = StudentInfo(grade = "1", classroom = "2"))
        val schoolRepository = FakeSchoolRepository()
        val viewModel = HomeViewModel(application, schoolRepository, repository)

        viewModel.loadHomeData()
        val state = withTimeout(1_000L) {
            viewModel.uiState.first { it.todayStatus == HomeContentStatus.NOT_CONFIGURED }
        }

        assertEquals(HomeContentStatus.NOT_CONFIGURED, state.todayStatus)
        assertEquals(HomeContentStatus.NOT_CONFIGURED, state.mealStatus)
        assertEquals(HomeContentStatus.NOT_CONFIGURED, state.scheduleStatus)
        assertEquals(application.getString(R.string.home_school_not_set_summary), state.todaySummaryText)
        assertEquals(application.getString(R.string.home_notice_setup_required), state.notices.summary)
        assertEquals(application.getString(R.string.home_setup_button), state.notices.actionText)
        assertTrue(state.notices.actionEnabled)
        assertTrue(state.notices.requiresSetup)

        val noticeActionDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.noticeActionEvent.first() }
        }

        viewModel.onNoticeActionClicked()
        assertEquals(HomeNoticeAction.OpenSetup, noticeActionDeferred.await())
    }

    @Test
    fun refreshHeader_prioritizesSchoolSetupHintWhenSchoolSelectionIsMissing() {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = "구미중학교"
            )
        )
        val viewModel = HomeViewModel(application, FakeSchoolRepository(), repository)

        viewModel.refreshHeader()

        assertEquals(
            application.getString(R.string.home_school_not_set_hint),
            viewModel.uiState.value.classSummary
        )
        assertEquals(
            application.getString(R.string.home_notice_setup_required),
            viewModel.uiState.value.notices.summary
        )
    }

    @Test
    fun loadHomeData_exposesLoadingStateBeforeRepositoryResponsesReturn() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = "구미중학교",
                officeCode = "J10",
                schoolCode = "1111111",
                schoolKind = "중학교"
            )
        )
        val schoolRepository = object : SchoolRepository {
            override suspend fun searchSchools(query: String) = Result.success(emptyList<SchoolInfo>())

            override suspend fun getMeals(date: String?): Result<List<MealInfo>> {
                delay(200)
                return Result.success(emptyList())
            }

            override suspend fun getSchedules(date: String?): Result<List<SchoolEvent>> {
                delay(200)
                return Result.success(emptyList())
            }

            override suspend fun getNotices(limit: Int): Result<NoticeFeed> {
                delay(200)
                return Result.success(NoticeFeed())
            }

            override suspend fun getTimetable(
                grade: String,
                classroom: String,
                date: String?
            ) = Result.success(emptyList<com.lbs.schoolhelper.data.model.TimetableItem>())
        }
        val viewModel = HomeViewModel(application, schoolRepository, repository)

        viewModel.loadHomeData()
        val loadingState = withTimeout(1_000L) {
            viewModel.uiState.first {
                it.todayStatus == HomeContentStatus.LOADING &&
                    it.mealStatus == HomeContentStatus.LOADING &&
                    it.scheduleStatus == HomeContentStatus.LOADING
            }
        }

        assertEquals(HomeContentStatus.LOADING, loadingState.todayStatus)
        assertEquals(application.getString(R.string.home_today_summary_loading), loadingState.todaySummaryText)
    }

    @Test
    fun loadHomeData_populatesNoticePreviewAndEmitsOpenUrlAction() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = "구미중학교",
                officeCode = "J10",
                schoolCode = "1111111",
                schoolKind = "중학교"
            )
        )
        val schoolRepository = FakeSchoolRepository(
            noticesResult = Result.success(
                NoticeFeed(
                    items = listOf(
                        NoticePreview(
                            id = "1",
                            title = "수련회 안내",
                            date = "2026-05-22",
                            author = "학교",
                            url = "https://example.com/notices/1",
                            sourceUrl = "https://example.com/notices"
                        ),
                        NoticePreview(
                            id = "2",
                            title = "급식 변경",
                            date = "2026-05-21",
                            author = "학교",
                            url = "https://example.com/notices/2",
                            sourceUrl = "https://example.com/notices"
                        )
                    )
                )
            )
        )
        val viewModel = HomeViewModel(application, schoolRepository, repository)

        viewModel.loadHomeData()
        val state = withTimeout(1_000L) {
            viewModel.uiState.first { it.notices.latestNoticeUrl == "https://example.com/notices/1" }
        }

        assertEquals(
            listOf(
                application.getString(R.string.home_notice_preview_format, "2026-05-22", "수련회 안내"),
                application.getString(R.string.home_notice_preview_format, "2026-05-21", "급식 변경")
            ).joinToString("\n"),
            state.notices.summary
        )
        assertEquals(application.getString(R.string.home_notice_open_button), state.notices.actionText)
        assertTrue(state.notices.actionEnabled)
        assertFalse(state.notices.requiresSetup)
        assertEquals("https://example.com/notices/1", state.notices.latestNoticeUrl)
        assertEquals(3, schoolRepository.lastNoticeLimit)

        val noticeActionDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { viewModel.noticeActionEvent.first() }
        }

        viewModel.onNoticeActionClicked()
        assertEquals(
            HomeNoticeAction.OpenUrl("https://example.com/notices/1"),
            noticeActionDeferred.await()
        )
    }

    @Test
    fun loadHomeData_distinguishesEmptyErrorAndSuccessStates() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = "구미중학교",
                officeCode = "J10",
                schoolCode = "1111111",
                schoolKind = "중학교"
            )
        )
        val schoolRepository = FakeSchoolRepository(
            mealsResult = Result.failure(IllegalStateException("meal fail")),
            schedulesResult = Result.success(
                listOf(
                    SchoolEvent(
                        date = java.time.LocalDate.now().plusDays(1)
                            .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
                        title = "체험학습",
                        description = "1학년"
                    )
                )
            )
        )
        val viewModel = HomeViewModel(application, schoolRepository, repository)

        viewModel.loadHomeData()

        val errorState = withTimeout(1_000L) {
            viewModel.uiState.first { it.mealStatus == HomeContentStatus.ERROR }
        }
        assertEquals(HomeContentStatus.ERROR, errorState.todayStatus)
        assertEquals(HomeContentStatus.ERROR, errorState.mealStatus)
        assertEquals(HomeContentStatus.SUCCESS, errorState.scheduleStatus)
        assertEquals(application.getString(R.string.meal_error_day), errorState.mealSummary)

        schoolRepository.mealsResult = Result.success(emptyList())
        schoolRepository.schedulesResult = Result.success(emptyList())

        viewModel.loadHomeData()
        val emptyState = withTimeout(1_000L) {
            viewModel.uiState.first {
                it.mealStatus == HomeContentStatus.EMPTY &&
                    it.scheduleStatus == HomeContentStatus.EMPTY
            }
        }
        assertEquals(HomeContentStatus.EMPTY, emptyState.todayStatus)
        assertEquals(application.getString(R.string.home_meal_empty), emptyState.mealSummary)

        schoolRepository.mealsResult = Result.success(
            listOf(
                MealInfo(
                    date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
                    mealType = "점심",
                    menu = "잡곡밥<br/>된장국",
                    calorieInfo = "842 kcal"
                )
            )
        )
        schoolRepository.schedulesResult = Result.success(
            listOf(
                SchoolEvent(
                    date = java.time.LocalDate.now().plusDays(1)
                        .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
                    title = "현장체험학습",
                    description = "도서관"
                )
            )
        )

        viewModel.loadHomeData()
        val successState = withTimeout(1_000L) {
            viewModel.uiState.first {
                it.todayStatus == HomeContentStatus.SUCCESS &&
                    it.mealStatus == HomeContentStatus.SUCCESS &&
                    it.scheduleStatus == HomeContentStatus.SUCCESS
            }
        }

        assertEquals("점심 • 842 kcal", successState.mealMeta)
        assertEquals(application.getString(R.string.home_today_summary_body), successState.todaySummaryText)
    }

    @Test
    fun loadHomeData_mapsNoticeStatesForSuccessFallbackAndFailure() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application
        val repository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = "구미중학교",
                officeCode = "J10",
                schoolCode = "1111111",
                schoolKind = "중학교"
            )
        )
        val schoolRepository = FakeSchoolRepository(
            noticesResult = Result.success(
                NoticeFeed(
                    items = listOf(
                        NoticePreview(
                            id = "1",
                            title = "가정통신문 1",
                            date = "2026-05-22",
                            author = "학교",
                            url = "https://example.com/notices/1",
                            sourceUrl = "https://example.com/notices"
                        )
                    )
                )
            )
        )
        val viewModel = HomeViewModel(application, schoolRepository, repository)

        viewModel.loadHomeData()
        val successState = withTimeout(1_000L) {
            viewModel.uiState.first { it.notices.latestNoticeUrl == "https://example.com/notices/1" }
        }
        assertEquals(
            application.getString(R.string.home_notice_preview_format, "2026-05-22", "가정통신문 1"),
            successState.notices.summary
        )
        assertEquals(application.getString(R.string.home_notice_open_button), successState.notices.actionText)
        assertTrue(successState.notices.actionEnabled)
        assertEquals(3, schoolRepository.lastNoticeLimit)

        schoolRepository.noticesResult = Result.success(
            NoticeFeed(message = "대구교육청 학교 홈페이지는 가정통신문 조회를 아직 지원하지 않습니다.")
        )
        viewModel.loadHomeData()
        val messageState = withTimeout(1_000L) {
            viewModel.uiState.first {
                it.notices.summary == "대구교육청 학교 홈페이지는 가정통신문 조회를 아직 지원하지 않습니다."
            }
        }
        assertEquals(
            application.getString(R.string.home_notice_unavailable_button),
            messageState.notices.actionText
        )
        assertFalse(messageState.notices.actionEnabled)

        schoolRepository.noticesResult = Result.success(NoticeFeed())
        viewModel.loadHomeData()
        val emptyState = withTimeout(1_000L) {
            viewModel.uiState.first {
                it.notices.summary == application.getString(R.string.home_notice_empty) &&
                    !it.notices.actionEnabled
            }
        }
        assertEquals(
            application.getString(R.string.home_notice_unavailable_button),
            emptyState.notices.actionText
        )
        assertEquals(null, emptyState.notices.latestNoticeUrl)

        schoolRepository.noticesResult = Result.failure(IllegalStateException("notice fail"))
        viewModel.loadHomeData()
        val failureState = withTimeout(1_000L) {
            viewModel.uiState.first { it.notices.summary == "notice fail" }
        }
        assertEquals(
            application.getString(R.string.home_notice_unavailable_button),
            failureState.notices.actionText
        )
        assertFalse(failureState.notices.actionEnabled)
    }

    @Test
    fun onNoticeActionClicked_emitsSetupAndUrlActions() = runBlocking {
        val application = Robolectric.setupActivity(MainActivity::class.java).application

        val missingSchoolViewModel = HomeViewModel(
            application,
            FakeSchoolRepository(),
            FakePreferencesRepository(studentInfo = StudentInfo(grade = "1", classroom = "2"))
        )
        missingSchoolViewModel.loadHomeData()
        withTimeout(1_000L) {
            missingSchoolViewModel.uiState.first { it.notices.requiresSetup }
        }
        val setupEventDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { missingSchoolViewModel.noticeActionEvent.first() }
        }
        missingSchoolViewModel.onNoticeActionClicked()
        assertEquals(HomeNoticeAction.OpenSetup, setupEventDeferred.await())

        val configuredRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "2",
                schoolName = "구미중학교",
                officeCode = "J10",
                schoolCode = "1111111",
                schoolKind = "중학교"
            )
        )
        val noticeUrl = "https://example.com/notices/2"
        val configuredViewModel = HomeViewModel(
            application,
            FakeSchoolRepository(
                noticesResult = Result.success(
                    NoticeFeed(
                        items = listOf(
                            NoticePreview(
                                id = "2",
                                title = "가정통신문 2",
                                date = "2026-05-23",
                                author = "학교",
                                url = noticeUrl,
                                sourceUrl = "https://example.com/notices"
                            )
                        )
                    )
                )
            ),
            configuredRepository
        )
        configuredViewModel.loadHomeData()
        withTimeout(1_000L) {
            configuredViewModel.uiState.first { it.notices.latestNoticeUrl == noticeUrl }
        }
        val urlEventDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000L) { configuredViewModel.noticeActionEvent.first() }
        }
        configuredViewModel.onNoticeActionClicked()
        assertEquals(HomeNoticeAction.OpenUrl(noticeUrl), urlEventDeferred.await())
    }
}
