import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class FeatureViewModelTests: XCTestCase {
    func testHomeViewModelShowsSetupMessagesWhenProfileIsIncomplete() async {
        let viewModel = HomeViewModel(repository: StubSchoolRepository())

        await viewModel.load(profile: StudentProfile())

        XCTAssertEqual(viewModel.todaySummary, "학교와 학년/반을 먼저 설정해 주세요.")
        XCTAssertEqual(viewModel.mealSummary, "학교 설정이 필요해요.")
        XCTAssertEqual(viewModel.eventSummary, "학교 설정이 필요해요.")
        XCTAssertEqual(viewModel.noticeSummary, "학교 설정이 필요해요.")
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
            now: { fixtureDate(year: 2026, month: 5, day: 26) }
        )

        await viewModel.load(profile: .fixture())

        XCTAssertEqual(viewModel.todaySummary, "1교시 국어\n2교시 수학")
        XCTAssertEqual(viewModel.mealSummary, "비빔밥")
        XCTAssertEqual(viewModel.eventSummary, "체육대회")
        XCTAssertEqual(viewModel.noticeSummary, "현장학습 안내")
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
}
