import XCTest
@testable import SchoolHelperIOSCore

final class DefaultSchoolRepositoryTests: XCTestCase {
    func testSearchSchoolsFallsBackToWhitespaceInsensitiveMockWhenRemoteFails() async throws {
        let repository = DefaultSchoolRepository(
            schoolSearchService: StubSchoolSearchService(result: .failure(TestError.expected)),
            neisService: SpyNEISService(),
            noticesService: StubNoticesService(result: []),
            fallback: MockSchoolRepository()
        )

        let schools = try await repository.searchSchools(query: "미사 중학교")

        XCTAssertEqual(schools.map(\.schoolName), ["미사중학교"])
    }

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

    func testFetchTodayMealsNormalizesNeisHtmlMenuBeforeReturningAndCaching() async throws {
        let defaults = makeIsolatedDefaults()
        let cacheStore = SchoolDataCacheStore(defaults: defaults, now: { fixtureDate(year: 2026, month: 5, day: 26) })
        let searchService = StubSchoolSearchService(result: .success([]))
        let neisService = SpyNEISService()
        await neisService.setMealsHandler { _, _, date in
            [
                MealInfo(
                    date: date,
                    mealType: "점심",
                    menu: "비빔밥(1.5)<br/>미역국",
                    calorieInfo: "700 kcal"
                )
            ]
        }
        let repository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: neisService,
            noticesService: StubNoticesService(result: []),
            fallback: StubSchoolRepository(),
            cacheStore: cacheStore
        )

        let meals = try await repository.fetchTodayMeals(
            for: .fixture(),
            date: fixtureDate(year: 2026, month: 5, day: 26)
        )

        XCTAssertEqual(meals.first?.menu, "비빔밥 (1.5)\n미역국")
        XCTAssertEqual(
            cacheStore.getMeals(
                officeCode: StudentProfile.fixture().officeCode,
                schoolCode: StudentProfile.fixture().schoolCode,
                date: "20260526"
            )?.first?.menu,
            "비빔밥 (1.5)\n미역국"
        )
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

    func testFetchTodayMealsUsesFreshCacheWhenRemoteFails() async throws {
        let defaults = makeIsolatedDefaults()
        let currentDate = fixtureDate(year: 2026, month: 5, day: 26)
        let cacheStore = SchoolDataCacheStore(defaults: defaults, now: { currentDate })
        let searchService = StubSchoolSearchService(result: .success([]))
        let noticesService = StubNoticesService(result: [])
        let successfulNEIS = SpyNEISService()
        await successfulNEIS.setMealsHandler { _, _, date in
            [MealInfo(date: date, mealType: "점심", menu: "캐시될 급식", calorieInfo: "700 kcal")]
        }
        let profile = StudentProfile.fixture()
        let date = fixtureDate(year: 2026, month: 5, day: 26)
        let primingRepository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: successfulNEIS,
            noticesService: noticesService,
            fallback: StubSchoolRepository(),
            cacheStore: cacheStore
        )

        _ = try await primingRepository.fetchTodayMeals(for: profile, date: date)

        let failingRepository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: SpyNEISService(mealsError: TestError.expected),
            noticesService: noticesService,
            fallback: StubSchoolRepository(todayMeals: [MealInfo(date: "20260526", mealType: "점심", menu: "fallback", calorieInfo: "0 kcal")]),
            cacheStore: cacheStore
        )

        let meals = try await failingRepository.fetchTodayMeals(for: profile, date: date)

        XCTAssertEqual(meals.map(\.menu), ["캐시될 급식"])
    }

    func testFetchTodayMealsIgnoresExpiredCache() async throws {
        let defaults = makeIsolatedDefaults()
        var currentDate = fixtureDate(year: 2026, month: 5, day: 26)
        let cacheStore = SchoolDataCacheStore(defaults: defaults, now: { currentDate })
        let searchService = StubSchoolSearchService(result: .success([]))
        let noticesService = StubNoticesService(result: [])
        let successfulNEIS = SpyNEISService()
        await successfulNEIS.setMealsHandler { _, _, date in
            [MealInfo(date: date, mealType: "점심", menu: "만료될 급식", calorieInfo: "700 kcal")]
        }
        let profile = StudentProfile.fixture()
        let date = fixtureDate(year: 2026, month: 5, day: 26)
        let primingRepository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: successfulNEIS,
            noticesService: noticesService,
            fallback: StubSchoolRepository(),
            cacheStore: cacheStore
        )

        _ = try await primingRepository.fetchTodayMeals(for: profile, date: date)
        currentDate = Calendar(identifier: .gregorian).date(byAdding: .hour, value: 13, to: currentDate)!

        let failingRepository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: SpyNEISService(mealsError: TestError.expected),
            noticesService: noticesService,
            fallback: StubSchoolRepository(todayMeals: [MealInfo(date: "20260526", mealType: "점심", menu: "fallback", calorieInfo: "0 kcal")]),
            cacheStore: cacheStore
        )

        let meals = try await failingRepository.fetchTodayMeals(for: profile, date: date)

        XCTAssertEqual(meals.map(\.menu), ["fallback"])
    }

    func testFetchTimetableUsesFreshCacheWhenRemoteFails() async throws {
        let defaults = makeIsolatedDefaults()
        let cacheStore = SchoolDataCacheStore(defaults: defaults, now: { fixtureDate(year: 2026, month: 5, day: 26) })
        let searchService = StubSchoolSearchService(result: .success([]))
        let noticesService = StubNoticesService(result: [])
        let successfulNEIS = SpyNEISService()
        await successfulNEIS.setTimetableHandler { _, _, _, grade, classroom, date in
            [TimetableItem(date: date, period: "1", subject: "국어", grade: grade, classroom: classroom)]
        }
        let profile = StudentProfile.fixture()
        let date = fixtureDate(year: 2026, month: 5, day: 26)
        let primingRepository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: successfulNEIS,
            noticesService: noticesService,
            fallback: StubSchoolRepository(),
            cacheStore: cacheStore
        )

        _ = try await primingRepository.fetchTimetable(for: profile, date: date)

        let failingRepository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: SpyNEISService(timetableError: TestError.expected),
            noticesService: noticesService,
            fallback: StubSchoolRepository(timetable: [TimetableItem(date: "20260526", period: "1", subject: "fallback", grade: "1", classroom: "2")]),
            cacheStore: cacheStore
        )

        let items = try await failingRepository.fetchTimetable(for: profile, date: date)

        XCTAssertEqual(items.map(\.subject), ["국어"])
    }

    func testFetchScheduleUsesFreshCacheWhenRemoteFails() async throws {
        let defaults = makeIsolatedDefaults()
        let cacheStore = SchoolDataCacheStore(defaults: defaults, now: { fixtureDate(year: 2026, month: 5, day: 26) })
        let searchService = StubSchoolSearchService(result: .success([]))
        let noticesService = StubNoticesService(result: [])
        let successfulNEIS = SpyNEISService()
        await successfulNEIS.setScheduleHandler { _, _, month in
            [SchoolEvent(date: "\(month)26", title: "체육대회", description: "운동장")]
        }
        let profile = StudentProfile.fixture()
        let month = fixtureDate(year: 2026, month: 5, day: 1)
        let primingRepository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: successfulNEIS,
            noticesService: noticesService,
            fallback: StubSchoolRepository(),
            cacheStore: cacheStore
        )

        _ = try await primingRepository.fetchSchedule(for: profile, month: month)

        let failingRepository = DefaultSchoolRepository(
            schoolSearchService: searchService,
            neisService: SpyNEISService(scheduleError: TestError.expected),
            noticesService: noticesService,
            fallback: StubSchoolRepository(schedule: [SchoolEvent(date: "202605", title: "fallback", description: "")]),
            cacheStore: cacheStore
        )

        let events = try await failingRepository.fetchSchedule(for: profile, month: month)

        XCTAssertEqual(events.map(\.title), ["체육대회"])
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
    typealias ScheduleHandler = (_ officeCode: String, _ schoolCode: String, _ month: String) -> [SchoolEvent]
    typealias TimetableHandler = (_ officeCode: String, _ schoolCode: String, _ schoolKind: String, _ grade: String, _ classroom: String, _ date: String) -> [TimetableItem]

    private var mealsHandler: MealsHandler?
    private var scheduleHandler: ScheduleHandler?
    private var timetableHandler: TimetableHandler?
    private var mealDates: [String] = []
    private var scheduleMonths: [String] = []
    private let mealsError: Error?
    private let scheduleError: Error?
    private let timetableError: Error?

    init(mealsError: Error? = nil, scheduleError: Error? = nil, timetableError: Error? = nil) {
        self.mealsError = mealsError
        self.scheduleError = scheduleError
        self.timetableError = timetableError
    }

    func setMealsHandler(_ handler: @escaping MealsHandler) {
        mealsHandler = handler
    }

    func setScheduleHandler(_ handler: @escaping ScheduleHandler) {
        scheduleHandler = handler
    }

    func setTimetableHandler(_ handler: @escaping TimetableHandler) {
        timetableHandler = handler
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
        return scheduleHandler?(officeCode, schoolCode, month) ?? []
    }

    func fetchTimetable(
        officeCode: String,
        schoolCode: String,
        schoolKind: String,
        grade: String,
        classroom: String,
        date: String
    ) async throws -> [TimetableItem] {
        if let timetableError {
            throw timetableError
        }
        return timetableHandler?(officeCode, schoolCode, schoolKind, grade, classroom, date) ?? []
    }
}

private func makeIsolatedDefaults() -> UserDefaults {
    let suiteName = "SchoolHelperIOSCoreTests.\(UUID().uuidString)"
    let defaults = UserDefaults(suiteName: suiteName)!
    defaults.removePersistentDomain(forName: suiteName)
    return defaults
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
