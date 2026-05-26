import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class FeatureViewModelTests: XCTestCase {
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
        XCTAssertEqual(viewModel.eventSummary, "학교 설정이 필요해요.")
        XCTAssertEqual(viewModel.noticeSummary, "학교 설정이 필요해요.")
        XCTAssertEqual(viewModel.noticeActionText, "확인 불가")
        XCTAssertFalse(viewModel.noticeActionEnabled)
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
            notices: [NoticePreview(id: "1", title: "현장학습 안내", date: "2026-05-26", author: "교무실", url: "https://example.com")]
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
        XCTAssertEqual(viewModel.eventSummary, "5월 26일  체육대회\n운동장")
        XCTAssertEqual(viewModel.noticeSummary, "2026-05-26  현장학습 안내")
        XCTAssertEqual(viewModel.noticeActionText, "가정통신문 열기")
        XCTAssertTrue(viewModel.noticeActionEnabled)
        XCTAssertEqual(viewModel.latestNoticeDestination()?.absoluteString, "https://example.com")
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

        await mealsViewModel.load(profile: .fixture(), referenceDate: fixtureDate(year: 2026, month: 5, day: 26))
        timetableViewModel.date = fixtureDate(year: 2026, month: 5, day: 26)
        await timetableViewModel.load(profile: .fixture())
        await scheduleViewModel.load(profile: .fixture(), month: fixtureDate(year: 2026, month: 5, day: 1))

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

        let mealsViewModel = MealsViewModel(repository: repository)
        await mealsViewModel.load(profile: profile, referenceDate: referenceDate)
        XCTAssertEqual(mealsViewModel.weekTitle, "5월 25일 - 5월 29일")
        await mealsViewModel.showNextWeek()
        XCTAssertEqual(mealsViewModel.weekTitle, "6월 1일 - 6월 5일")

        let scheduleViewModel = ScheduleViewModel(repository: repository)
        await scheduleViewModel.load(profile: profile, month: referenceDate)
        XCTAssertEqual(scheduleViewModel.monthTitle, "2026년 5월")
        await scheduleViewModel.showNextMonth()
        XCTAssertEqual(scheduleViewModel.monthTitle, "2026년 6월")

        let timetableViewModel = TimetableViewModel(repository: repository)
        timetableViewModel.date = referenceDate
        await timetableViewModel.load(profile: profile)
        XCTAssertEqual(timetableViewModel.dateTitle, "5월 26일 화요일")
        await timetableViewModel.showNextDay()
        XCTAssertEqual(timetableViewModel.dateTitle, "5월 27일 수요일")
    }
}
