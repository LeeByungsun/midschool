import XCTest
@testable import SchoolHelperIOSCore

final class DefaultSchoolRepositoryTests: XCTestCase {
    func testFetchTodayMealsFormatsDateForRemoteService() async throws {
        let searchService = StubSchoolSearchService(result: .success([]))
        let neisService = SpyNEISService()
        await neisService.setMealsHandler { _, _, date in
            [
                MealInfo(date: date, mealType: "점심", menu: "원격 급식", calorieInfo: "700 kcal")
            ]
        }
        let noticesService = StubNoticesService(result: [])
        let repository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: neisService,
            noticesService: noticesService,
            fallback: StubSchoolRepository()
        )

        let meals = try await repository.fetchTodayMeals(
            for: .fixture(),
            date: fixtureDate(year: 2026, month: 5, day: 26)
        )

        let requestedMealDates = await neisService.requestedMealDates()
        XCTAssertEqual(requestedMealDates, ["20260526"])
        XCTAssertEqual(meals.first?.menu, "원격 급식")
    }

    func testFetchWeekMealsRequestsFiveConsecutiveDates() async throws {
        let searchService = StubSchoolSearchService(result: .success([]))
        let neisService = SpyNEISService()
        await neisService.setMealsHandler { _, _, date in
            [MealInfo(date: date, mealType: "점심", menu: date, calorieInfo: "650 kcal")]
        }
        let noticesService = StubNoticesService(result: [])
        let repository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: neisService,
            noticesService: noticesService,
            fallback: StubSchoolRepository()
        )

        let meals = try await repository.fetchWeekMeals(
            for: .fixture(),
            weekStart: fixtureDate(year: 2026, month: 5, day: 26)
        )

        let requestedMealDates = await neisService.requestedMealDates()
        XCTAssertEqual(
            requestedMealDates.sorted(),
            ["20260526", "20260527", "20260528", "20260529", "20260530"]
        )
        XCTAssertEqual(meals.map(\.date), ["20260526", "20260527", "20260528", "20260529", "20260530"])
    }

    func testFetchTodayMealsFiltersOutRowsFromOtherDates() async throws {
        let searchService = StubSchoolSearchService(result: .success([]))
        let neisService = SpyNEISService()
        await neisService.setMealsHandler { _, _, date in
            [
                MealInfo(date: date, mealType: "점심", menu: "오늘 급식", calorieInfo: "700 kcal"),
                MealInfo(date: "20260530", mealType: "점심", menu: "다른 날짜 급식", calorieInfo: "600 kcal")
            ]
        }
        let noticesService = StubNoticesService(result: [])
        let repository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: neisService,
            noticesService: noticesService,
            fallback: StubSchoolRepository()
        )

        let meals = try await repository.fetchTodayMeals(
            for: .fixture(),
            date: fixtureDate(year: 2026, month: 5, day: 26)
        )

        XCTAssertEqual(meals.map(\.menu), ["오늘 급식"])
        XCTAssertEqual(meals.map(\.date), ["20260526"])
    }

    func testFetchScheduleFallsBackWhenRemoteServiceFails() async throws {
        let fallbackEvents = [
            SchoolEvent(date: "202605", title: "체육대회", description: "운동장")
        ]
        let searchService = StubSchoolSearchService(result: .success([]))
        let neisService = SpyNEISService(scheduleError: TestError.expected)
        let noticesService = StubNoticesService(result: [])
        let repository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: neisService,
            noticesService: noticesService,
            fallback: StubSchoolRepository(schedule: fallbackEvents)
        )

        let events = try await repository.fetchSchedule(
            for: .fixture(),
            month: fixtureDate(year: 2026, month: 5, day: 1)
        )

        let requestedScheduleMonths = await neisService.requestedScheduleMonths()
        XCTAssertEqual(requestedScheduleMonths, ["202605"])
        XCTAssertEqual(events, fallbackEvents)
    }
}

private struct StubSchoolSearchService: SchoolSearchService {
    let result: Result<[SchoolInfo], Error>

    func searchSchools(query: String) async throws -> [SchoolInfo] {
        try result.get()
    }
}

private struct StubNoticesService: NoticesServicing {
    let result: [NoticePreview]

    func fetchNotices(
        officeCode: String,
        schoolCode: String,
        limit: Int
    ) async throws -> [NoticePreview] {
        result
    }
}

private actor SpyNEISService: NEISServicing {
    typealias MealsHandler = (_ officeCode: String, _ schoolCode: String, _ date: String) -> [MealInfo]

    private var mealsHandler: MealsHandler?
    private var mealDates: [String] = []
    private var scheduleMonths: [String] = []
    private let mealsError: Error?
    private let scheduleError: Error?

    init(mealsError: Error? = nil, scheduleError: Error? = nil) {
        self.mealsError = mealsError
        self.scheduleError = scheduleError
    }

    func setMealsHandler(_ handler: @escaping MealsHandler) {
        mealsHandler = handler
    }

    func requestedMealDates() -> [String] {
        mealDates
    }

    func requestedScheduleMonths() -> [String] {
        scheduleMonths
    }

    func fetchMeals(
        officeCode: String,
        schoolCode: String,
        date: String
    ) async throws -> [MealInfo] {
        mealDates.append(date)
        if let mealsError {
            throw mealsError
        }
        return mealsHandler?(officeCode, schoolCode, date) ?? []
    }

    func fetchSchedule(
        officeCode: String,
        schoolCode: String,
        month: String
    ) async throws -> [SchoolEvent] {
        scheduleMonths.append(month)
        if let scheduleError {
            throw scheduleError
        }
        return []
    }

    func fetchTimetable(
        officeCode: String,
        schoolCode: String,
        schoolKind: String,
        grade: String,
        classroom: String,
        date: String
    ) async throws -> [TimetableItem] {
        []
    }
}

extension DefaultSchoolRepositoryTests {
    func testFetchWeekMealsFallbackUsesRequestedDates() async throws {
        let searchService = StubSchoolSearchService(result: .success([]))
        let neisService = SpyNEISService(mealsError: TestError.expected)
        let noticesService = StubNoticesService(result: [])
        let repository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: neisService,
            noticesService: noticesService,
            fallback: MockSchoolRepository()
        )

        let meals = try await repository.fetchWeekMeals(
            for: .fixture(),
            weekStart: fixtureDate(year: 2026, month: 5, day: 26)
        )

        XCTAssertEqual(
            meals.map(\.date),
            ["20260526", "20260527", "20260528", "20260529", "20260530"]
        )
    }
}

private enum TestError: Error {
    case expected
}
