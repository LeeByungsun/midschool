import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class FeatureViewModelTests: XCTestCase {
    func testTimetableViewModelShowsCacheBeforeRemoteRefreshCompletes() async {
        let date = fixtureDate(year: 2026, month: 5, day: 26)
        let viewModel = TimetableViewModel(
            repository: DelayedCacheFirstTimetableRepository(),
            todayProvider: { date }
        )
        viewModel.date = date

        let loading = Task { await viewModel.load(profile: .fixture()) }
        try? await Task.sleep(nanoseconds: 20_000_000)

        XCTAssertEqual(viewModel.items.map(\.subject), ["캐시 국어"])
        await loading.value
        XCTAssertEqual(viewModel.items.map(\.subject), ["최신 수학"])
    }

    func testMealsViewModelShowsCacheBeforeRemoteRefreshCompletes() async {
        let date = fixtureDate(year: 2026, month: 5, day: 26)
        let viewModel = MealsViewModel(
            repository: DelayedCacheFirstMealsRepository(),
            todayProvider: { date }
        )

        let loading = Task { await viewModel.load(profile: .fixture(), referenceDate: date) }
        try? await Task.sleep(nanoseconds: 20_000_000)

        XCTAssertEqual(viewModel.items.map(\.menu), ["캐시 급식"])
        await loading.value
        XCTAssertEqual(viewModel.items.map(\.menu), ["최신 급식"])
    }

    func testScheduleViewModelShowsCacheBeforeRemoteRefreshCompletes() async {
        let date = fixtureDate(year: 2026, month: 5, day: 1)
        let viewModel = ScheduleViewModel(repository: DelayedCacheFirstScheduleRepository())
        let loading = Task { await viewModel.load(profile: .fixture(), month: date) }
        try? await Task.sleep(nanoseconds: 20_000_000)
        XCTAssertEqual(viewModel.items.map(\.title), ["캐시 일정"])
        await loading.value
        XCTAssertEqual(viewModel.items.map(\.title), ["최신 일정"])
    }

    func testHomeViewModelShowsSetupMessagesWhenProfileIsIncomplete() async {
        let viewModel = HomeViewModel(
            repository: StubSchoolRepository(),
            timerStateProvider: {
                TimerSessionState(
                    preset: .focus,
                    totalSeconds: 2400,
                    remainingSeconds: 2400,
                    targetDate: nil,
                    isRunning: false
                )
            },
            now: { fixtureDate(year: 2026, month: 5, day: 26) }
        )

        await viewModel.load(profile: StudentProfile())

        XCTAssertEqual(viewModel.dateLabel, "5월 26일 화요일")
        XCTAssertEqual(viewModel.timerSummary, "집중 • 40:00")
        XCTAssertEqual(viewModel.todaySummary, "학교와 학년/반을 먼저 설정해 주세요.")
        XCTAssertEqual(viewModel.mealSummary, "학교 설정이 필요해요.")
        XCTAssertEqual(viewModel.mealMeta, "")
        XCTAssertEqual(viewModel.eventSummary, "학교 설정이 필요해요.")
        XCTAssertEqual(viewModel.noticeSummary, "학교 설정 후 최근 가정통신문을 확인할 수 있어요.")
        XCTAssertEqual(viewModel.noticeActionText, "학교 설정하러 가기")
        XCTAssertTrue(viewModel.noticeActionEnabled)
        XCTAssertTrue(viewModel.noticeRequiresSetup)
        XCTAssertNil(viewModel.latestNoticeDestination())
    }

    func testHomeViewModelBuildsSummariesFromRepositoryData() async {
        let repository = StubSchoolRepository(
            todayMeals: [MealInfo(date: "20260526", mealType: "점심", menu: "비빔밥", calorieInfo: "700 kcal")],
            timetable: [
                TimetableItem(date: "20260526", period: "1", subject: "국어", grade: "1", classroom: "2"),
                TimetableItem(date: "20260526", period: "2", subject: "수학", grade: "1", classroom: "2")
            ],
            schedule: [SchoolEvent(date: "20260526", title: "체육대회", description: "운동장")],
            notices: [NoticePreview(id: "1", title: "현장학습 안내", date: "2026-05-26", author: "교무실", url: "https://example.com/notices/1", sourceUrl: "https://example.com/notices")]
        )
        let viewModel = HomeViewModel(
            repository: repository,
            timerStateProvider: {
                TimerSessionState(
                    preset: .shortBreak,
                    totalSeconds: 600,
                    remainingSeconds: 420,
                    targetDate: fixtureDate(year: 2026, month: 5, day: 26).addingTimeInterval(420),
                    isRunning: true
                )
            },
            now: { fixtureDate(year: 2026, month: 5, day: 26) }
        )

        await viewModel.load(profile: .fixture())

        XCTAssertEqual(viewModel.dateLabel, "5월 26일 화요일")
        XCTAssertEqual(viewModel.timerSummary, "휴식 • 07:00 남음")
        XCTAssertEqual(viewModel.todaySummary, "1교시 국어\n2교시 수학")
        XCTAssertEqual(viewModel.mealSummary, "비빔밥")
        XCTAssertEqual(viewModel.mealMeta, "점심 • 700 kcal")
        XCTAssertEqual(viewModel.eventSummary, "5월 26일  체육대회\n운동장")
        XCTAssertEqual(viewModel.noticeSummary, "2026-05-26  현장학습 안내")
        XCTAssertEqual(viewModel.noticeActionText, "가정통신문 목록 열기")
        XCTAssertTrue(viewModel.noticeActionEnabled)
        XCTAssertFalse(viewModel.noticeRequiresSetup)
        XCTAssertEqual(viewModel.latestNoticeDestination()?.absoluteString, "https://example.com/notices")
    }

    func testHomeViewModelOmitsBlankNoticeDateLikeAndroid() async {
        let repository = StubSchoolRepository(
            notices: [
                NoticePreview(
                    id: "1",
                    title: "학부모 공지",
                    date: "",
                    author: "행정실",
                    url: "https://example.com/notices/1",
                    sourceUrl: "https://example.com/notices"
                )
            ]
        )
        let viewModel = HomeViewModel(
            repository: repository,
            now: { fixtureDate(year: 2026, month: 5, day: 26) }
        )

        await viewModel.load(profile: .fixture())

        XCTAssertEqual(viewModel.noticeSummary, "학부모 공지")
        XCTAssertEqual(viewModel.noticeActionText, "가정통신문 목록 열기")
        XCTAssertTrue(viewModel.noticeActionEnabled)
        XCTAssertEqual(viewModel.latestNoticeDestination()?.absoluteString, "https://example.com/notices")
    }

    func testHomeViewModelFiltersPastAndBlockedSchedulesAndFormatsMealMenu() async {
        let repository = StubSchoolRepository(
            todayMeals: [
                MealInfo(
                    date: "20260526",
                    mealType: "점심",
                    menu: "비빔밥(1.5)<br/>미역국",
                    calorieInfo: "700 kcal"
                )
            ],
            schedule: [
                SchoolEvent(date: "20260520", title: "지난 일정", description: "무시"),
                SchoolEvent(date: "20260527", title: "토요휴업일", description: ""),
                SchoolEvent(date: "20260528", title: "과학 행사", description: "강당"),
                SchoolEvent(date: "20260529", title: "체육대회", description: "운동장")
            ]
        )
        let viewModel = HomeViewModel(
            repository: repository,
            timerStateProvider: {
                TimerSessionState(
                    preset: .focus,
                    totalSeconds: 2400,
                    remainingSeconds: 2400,
                    targetDate: nil,
                    isRunning: false
                )
            },
            now: { fixtureDate(year: 2026, month: 5, day: 26) }
        )

        await viewModel.load(profile: .fixture())

        XCTAssertEqual(viewModel.mealSummary, "비빔밥 (1.5)\n미역국")
        XCTAssertEqual(viewModel.mealMeta, "점심 • 700 kcal")
        XCTAssertEqual(
            viewModel.eventSummary,
            "5월 28일  과학 행사\n강당\n\n5월 29일  체육대회\n운동장"
        )
    }

    func testMealsScheduleAndTimetableViewModelsRespectProfileCompletion() async {
        let repository = StubSchoolRepository(
            weekMeals: [MealInfo(date: "20260526", mealType: "점심", menu: "급식", calorieInfo: "650 kcal")],
            timetable: [TimetableItem(date: "20260526", period: "1", subject: "영어", grade: "1", classroom: "2")],
            schedule: [SchoolEvent(date: "20260526", title: "시험", description: "교실")]
        )
        let mealsViewModel = MealsViewModel(repository: repository)
        let timetableViewModel = TimetableViewModel(repository: repository)
        let scheduleViewModel = ScheduleViewModel(repository: repository)

        await mealsViewModel.load(profile: StudentProfile(), referenceDate: fixtureDate(year: 2026, month: 5, day: 26))
        await timetableViewModel.load(profile: StudentProfile())
        await scheduleViewModel.load(profile: StudentProfile(), month: fixtureDate(year: 2026, month: 5, day: 1))

        XCTAssertTrue(mealsViewModel.items.isEmpty)
        XCTAssertTrue(timetableViewModel.items.isEmpty)
        XCTAssertTrue(scheduleViewModel.items.isEmpty)
        XCTAssertFalse(mealsViewModel.hasLoadError)
        XCTAssertFalse(timetableViewModel.hasLoadError)
        XCTAssertFalse(scheduleViewModel.hasLoadError)

        await mealsViewModel.load(profile: StudentProfile.fixture(), referenceDate: fixtureDate(year: 2026, month: 5, day: 26))
        timetableViewModel.date = fixtureDate(year: 2026, month: 5, day: 26)
        await timetableViewModel.load(profile: StudentProfile.fixture())
        await scheduleViewModel.load(profile: StudentProfile.fixture(), month: fixtureDate(year: 2026, month: 5, day: 1))

        XCTAssertEqual(mealsViewModel.items.count, 1)
        XCTAssertEqual(timetableViewModel.items.count, 1)
        XCTAssertEqual(scheduleViewModel.items.count, 1)
    }

    func testDateNavigationViewModelsUpdateTitlesAndReload() async {
        let repository = StubSchoolRepository(
            weekMeals: [MealInfo(date: "20260526", mealType: "점심", menu: "급식", calorieInfo: "650 kcal")],
            timetable: [TimetableItem(date: "20260526", period: "1", subject: "영어", grade: "1", classroom: "2")],
            schedule: [SchoolEvent(date: "20260526", title: "시험", description: "교실")]
        )
        let profile = StudentProfile.fixture()
        let referenceDate = fixtureDate(year: 2026, month: 5, day: 26)

        let mealsViewModel = MealsViewModel(repository: repository, todayProvider: { referenceDate })
        await mealsViewModel.load(profile: profile, referenceDate: referenceDate)
        XCTAssertEqual(mealsViewModel.weekTitle, "5월 25일 - 5월 29일")

        let scheduleViewModel = ScheduleViewModel(repository: repository)
        await scheduleViewModel.load(profile: profile, month: referenceDate)
        XCTAssertEqual(scheduleViewModel.monthTitle, "2026년 5월")
        await scheduleViewModel.showNextMonth()
        XCTAssertEqual(scheduleViewModel.monthTitle, "2026년 6월")

        let timetableViewModel = TimetableViewModel(repository: repository, todayProvider: { referenceDate })
        timetableViewModel.date = referenceDate
        await timetableViewModel.load(profile: profile)
        XCTAssertEqual(timetableViewModel.dateTitle, "5월 26일 화요일")
        await timetableViewModel.showPreviousDay()
        XCTAssertEqual(timetableViewModel.dateTitle, "5월 25일 월요일")
        await timetableViewModel.showNextDay()
        XCTAssertEqual(timetableViewModel.dateTitle, "5월 26일 화요일")
        await timetableViewModel.showNextDay()
        XCTAssertEqual(timetableViewModel.dateTitle, "5월 27일 수요일")
    }

    func testMealsViewModelFormatsHtmlMenuForMealScreen() async {
        let repository = StubSchoolRepository(
            weekMeals: [
                MealInfo(
                    date: "20260526",
                    mealType: "점심",
                    menu: "비빔밥(1.5)<br />미역국<br>김치&nbsp;&amp;&nbsp;요거트",
                    calorieInfo: "650 kcal"
                )
            ]
        )
        let viewModel = MealsViewModel(
            repository: repository,
            todayProvider: { fixtureDate(year: 2026, month: 5, day: 26) }
        )

        await viewModel.load(profile: .fixture())

        XCTAssertEqual(viewModel.items.first?.menu, "비빔밥 (1.5)\n미역국\n김치 & 요거트")
    }

    func testScheduleViewModelFiltersBlockedEventsAndSortsByDate() async {
        let repository = StubSchoolRepository(
            schedule: [
                SchoolEvent(date: "20260529", title: "행사 B", description: "강당"),
                SchoolEvent(date: "20260527", title: "토요휴업일", description: ""),
                SchoolEvent(date: "20260528", title: "행사 A", description: "교실")
            ]
        )
        let viewModel = ScheduleViewModel(repository: repository)

        await viewModel.load(profile: .fixture(), month: fixtureDate(year: 2026, month: 5, day: 1))

        XCTAssertEqual(
            viewModel.items,
            [
                SchoolEvent(date: "20260528", title: "행사 A", description: "교실"),
                SchoolEvent(date: "20260529", title: "행사 B", description: "강당")
            ]
        )
    }

    func testFeatureViewModelsDistinguishLoadFailuresFromEmptyData() async {
        let repository = FailingSchoolRepository()
        let profile = StudentProfile.fixture()
        let referenceDate = fixtureDate(year: 2026, month: 5, day: 26)
        let mealsViewModel = MealsViewModel(repository: repository, todayProvider: { referenceDate })
        let timetableViewModel = TimetableViewModel(repository: repository, todayProvider: { referenceDate })
        let scheduleViewModel = ScheduleViewModel(repository: repository, todayProvider: { referenceDate })

        await mealsViewModel.load(profile: profile, referenceDate: referenceDate)
        await timetableViewModel.load(profile: profile)
        await scheduleViewModel.load(profile: profile, month: referenceDate)

        XCTAssertEqual(mealsViewModel.statusText, "급식을 불러오지 못했어요. 다시 시도해 주세요.")
        XCTAssertEqual(timetableViewModel.statusText, "시간표를 불러오지 못했어요. 다시 시도해 주세요.")
        XCTAssertEqual(scheduleViewModel.statusText, "일정을 불러오지 못했어요. 다시 시도해 주세요.")
        XCTAssertTrue(mealsViewModel.hasLoadError)
        XCTAssertTrue(timetableViewModel.hasLoadError)
        XCTAssertTrue(scheduleViewModel.hasLoadError)
    }

    func testHomeViewModelShowsPerSectionErrorsWhenLoadingFails() async {
        let viewModel = HomeViewModel(
            repository: FailingSchoolRepository(),
            now: { fixtureDate(year: 2026, month: 5, day: 26) }
        )

        await viewModel.load(profile: .fixture())

        XCTAssertEqual(viewModel.todaySummary, "시간표를 불러오지 못했어요. 새로고침해 주세요.")
        XCTAssertEqual(viewModel.mealSummary, "급식을 불러오지 못했어요. 새로고침해 주세요.")
        XCTAssertEqual(viewModel.mealMeta, "")
        XCTAssertEqual(viewModel.eventSummary, "일정을 불러오지 못했어요. 새로고침해 주세요.")
        XCTAssertEqual(viewModel.noticeSummary, "가정통신문을 불러오지 못했어요. 새로고침해 주세요.")
        XCTAssertEqual(viewModel.noticeActionText, "새로고침 필요")
        XCTAssertFalse(viewModel.noticeActionEnabled)
        XCTAssertNil(viewModel.latestNoticeDestination())
    }

    func testRetryReloadsTheCurrentlySelectedPeriod() async {
        let referenceDate = fixtureDate(year: 2026, month: 5, day: 26)
        let repository = FailOnceSchoolRepository()
        let mealsViewModel = MealsViewModel(repository: repository, todayProvider: { referenceDate })
        let timetableViewModel = TimetableViewModel(repository: repository, todayProvider: { referenceDate })
        let scheduleViewModel = ScheduleViewModel(repository: repository, todayProvider: { referenceDate })

        await mealsViewModel.load(profile: .fixture(), referenceDate: referenceDate)
        timetableViewModel.date = referenceDate
        await timetableViewModel.load(profile: .fixture())
        await scheduleViewModel.load(profile: .fixture(), month: referenceDate)

        await mealsViewModel.retry()
        await timetableViewModel.retry()
        await scheduleViewModel.retry()

        XCTAssertEqual(mealsViewModel.weekTitle, "5월 25일 - 5월 29일")
        XCTAssertEqual(mealsViewModel.items.first?.menu, "재시도 급식")
        XCTAssertEqual(timetableViewModel.dateTitle, "5월 26일 화요일")
        XCTAssertEqual(timetableViewModel.items.first?.subject, "재시도 시간표")
        XCTAssertEqual(scheduleViewModel.monthTitle, "2026년 5월")
        XCTAssertEqual(scheduleViewModel.items.first?.title, "재시도 일정")
        XCTAssertFalse(mealsViewModel.hasLoadError)
        XCTAssertFalse(timetableViewModel.hasLoadError)
        XCTAssertFalse(scheduleViewModel.hasLoadError)
    }
}

private struct DelayedCacheFirstTimetableRepository: SchoolRepository {
    func searchSchools(query: String) async throws -> [SchoolInfo] { [] }
    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] { [] }
    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] { [] }
    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] {
        try await Task.sleep(nanoseconds: 80_000_000)
        return [TimetableItem(date: "20260526", period: "1", subject: "최신 수학", grade: "1", classroom: "2")]
    }
    func cachedTimetable(for profile: StudentProfile, date: Date) -> [TimetableItem]? {
        [TimetableItem(date: "20260526", period: "1", subject: "캐시 국어", grade: "1", classroom: "2")]
    }
    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] { [] }
    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] { [] }
}

private struct DelayedCacheFirstMealsRepository: SchoolRepository {
    func searchSchools(query: String) async throws -> [SchoolInfo] { [] }
    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] { [] }
    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] {
        try await Task.sleep(nanoseconds: 80_000_000)
        return [MealInfo(date: "20260526", mealType: "점심", menu: "최신 급식", calorieInfo: "700 kcal")]
    }
    func cachedTodayMeals(for profile: StudentProfile, date: Date) -> [MealInfo]? {
        [MealInfo(date: "20260526", mealType: "점심", menu: "캐시 급식", calorieInfo: "690 kcal")]
    }
    func cachedWeekMeals(for profile: StudentProfile, weekStart: Date) -> [MealInfo]? {
        cachedTodayMeals(for: profile, date: weekStart)
    }
    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] { [] }
    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] { [] }
    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] { [] }
}

private struct DelayedCacheFirstScheduleRepository: SchoolRepository {
    func searchSchools(query: String) async throws -> [SchoolInfo] { [] }
    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] { [] }
    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] { [] }
    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] { [] }
    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] {
        try await Task.sleep(nanoseconds: 80_000_000)
        return [SchoolEvent(date: "20260520", title: "최신 일정", description: "")]
    }
    func cachedSchedule(for profile: StudentProfile, month: Date) -> [SchoolEvent]? {
        [SchoolEvent(date: "20260510", title: "캐시 일정", description: "")]
    }
    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] { [] }
}

private struct FailingSchoolRepository: SchoolRepository {
    func searchSchools(query: String) async throws -> [SchoolInfo] { throw Failure.expected }
    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] { throw Failure.expected }
    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] { throw Failure.expected }
    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] { throw Failure.expected }
    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] { throw Failure.expected }
    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] { throw Failure.expected }

    private enum Failure: Error {
        case expected
    }
}

private final class FailOnceSchoolRepository: SchoolRepository {
    private var mealCalls = 0
    private var timetableCalls = 0
    private var scheduleCalls = 0

    func searchSchools(query: String) async throws -> [SchoolInfo] { [] }
    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] { [] }

    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] {
        mealCalls += 1
        guard mealCalls > 1 else { throw Failure.expected }
        return [MealInfo(date: "20260526", mealType: "점심", menu: "재시도 급식", calorieInfo: "700 kcal")]
    }

    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] {
        timetableCalls += 1
        guard timetableCalls > 1 else { throw Failure.expected }
        return [TimetableItem(date: "20260526", period: "1", subject: "재시도 시간표", grade: profile.grade, classroom: profile.classroom)]
    }

    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] {
        scheduleCalls += 1
        guard scheduleCalls > 1 else { throw Failure.expected }
        return [SchoolEvent(date: "20260526", title: "재시도 일정", description: "")]
    }

    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] { [] }

    private enum Failure: Error {
        case expected
    }
}
