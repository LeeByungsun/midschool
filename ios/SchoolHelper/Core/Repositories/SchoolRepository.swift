import Foundation

protocol SchoolRepository {
    func searchSchools(query: String) async throws -> [SchoolInfo]
    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo]
    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo]
    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem]
    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent]
    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview]
}

protocol NEISServicing {
    func fetchMeals(
        officeCode: String,
        schoolCode: String,
        date: String
    ) async throws -> [MealInfo]
    func fetchSchedule(
        officeCode: String,
        schoolCode: String,
        month: String
    ) async throws -> [SchoolEvent]
    func fetchTimetable(
        officeCode: String,
        schoolCode: String,
        schoolKind: String,
        grade: String,
        classroom: String,
        date: String
    ) async throws -> [TimetableItem]
}

protocol NoticesServicing {
    func fetchNotices(
        officeCode: String,
        schoolCode: String,
        limit: Int
    ) async throws -> [NoticePreview]
}

extension NEISClient: SchoolSearchService, NEISServicing {}
extension NoticesClient: NoticesServicing {}

struct DefaultSchoolRepository: SchoolRepository {
    private let schoolSearchService: SchoolSearchService
    private let neisService: NEISServicing
    private let noticesService: NoticesServicing
    private let cacheStore: SchoolDataCacheStore
    private let calendar: Calendar

    init(
        schoolSearchService: SchoolSearchService? = nil,
        neisClient: NEISClient = NEISClient(),
        noticesService: NoticesServicing = NoticesClient(),
        cacheStore: SchoolDataCacheStore = SchoolDataCacheStore(),
        calendar: Calendar = .current
    ) {
        self.schoolSearchService = schoolSearchService ?? neisClient
        self.neisService = neisClient
        self.noticesService = noticesService
        self.cacheStore = cacheStore
        self.calendar = calendar
    }

    init(
        schoolSearchService: SchoolSearchService,
        neisService: NEISServicing,
        noticesService: NoticesServicing,
        cacheStore: SchoolDataCacheStore = SchoolDataCacheStore(),
        calendar: Calendar = .current
    ) {
        self.schoolSearchService = schoolSearchService
        self.neisService = neisService
        self.noticesService = noticesService
        self.cacheStore = cacheStore
        self.calendar = calendar
    }

    func searchSchools(query: String) async throws -> [SchoolInfo] {
        try await schoolSearchService.searchSchools(query: query)
    }

    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] {
        let key = formattedString(from: date, format: "yyyyMMdd")
        do {
            let meals = try await neisService.fetchMeals(
                officeCode: profile.officeCode,
                schoolCode: profile.schoolCode,
                date: key
            )
            .filter { $0.date == key }
            .map { $0.withDisplayMenu() }
            cacheStore.saveMeals(meals, officeCode: profile.officeCode, schoolCode: profile.schoolCode, date: key)
            return meals
        } catch {
            if let cached = cacheStore.getMeals(officeCode: profile.officeCode, schoolCode: profile.schoolCode, date: key) {
                return cached.map { $0.withDisplayMenu() }
            }
            throw error
        }
    }

    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] {
        let dates = weekDates(startingAt: weekStart)
        let meals = try await withThrowingTaskGroup(of: [MealInfo].self) { group in
            for date in dates {
                group.addTask {
                    try await self.fetchTodayMeals(for: profile, date: date)
                }
            }

            var collected: [MealInfo] = []
            for try await dayMeals in group {
                collected.append(contentsOf: dayMeals)
            }
            return collected
        }

        return meals.sorted {
            if $0.date == $1.date {
                return $0.mealType < $1.mealType
            }
            return $0.date < $1.date
        }
    }

    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] {
        let key = formattedString(from: date, format: "yyyyMMdd")
        do {
            let items = try await neisService.fetchTimetable(
                officeCode: profile.officeCode,
                schoolCode: profile.schoolCode,
                schoolKind: profile.schoolKind,
                grade: profile.grade,
                classroom: profile.classroom,
                date: key
            )
            cacheStore.saveTimetable(
                items,
                officeCode: profile.officeCode,
                schoolCode: profile.schoolCode,
                grade: profile.grade,
                classroom: profile.classroom,
                date: key
            )
            return items
        } catch {
            if let cached = cacheStore.getTimetable(
                officeCode: profile.officeCode,
                schoolCode: profile.schoolCode,
                grade: profile.grade,
                classroom: profile.classroom,
                date: key
            ) {
                return cached
            }
            throw error
        }
    }

    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] {
        let key = formattedString(from: month, format: "yyyyMM")
        do {
            let events = try await neisService.fetchSchedule(
                officeCode: profile.officeCode,
                schoolCode: profile.schoolCode,
                month: key
            )
            cacheStore.saveSchedule(events, officeCode: profile.officeCode, schoolCode: profile.schoolCode, month: key)
            return events
        } catch {
            if let cached = cacheStore.getSchedule(officeCode: profile.officeCode, schoolCode: profile.schoolCode, month: key) {
                return cached
            }
            throw error
        }
    }

    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] {
        try await noticesService.fetchNotices(
            officeCode: profile.officeCode,
            schoolCode: profile.schoolCode,
            limit: limit
        )
    }

    private func formattedString(from date: Date, format: String) -> String {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.dateFormat = format
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = calendar.timeZone
        return formatter.string(from: date)
    }

    private func weekDates(startingAt weekStart: Date) -> [Date] {
        let startOfDay = calendar.startOfDay(for: weekStart)
        return (0..<5).compactMap { offset in
            calendar.date(byAdding: .day, value: offset, to: startOfDay)
        }
    }
}
