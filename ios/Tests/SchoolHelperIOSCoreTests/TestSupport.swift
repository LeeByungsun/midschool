import Foundation
@testable import SchoolHelperIOSCore

func fixtureDate(year: Int, month: Int, day: Int) -> Date {
    var components = DateComponents()
    components.calendar = Calendar(identifier: .gregorian)
    components.timeZone = TimeZone(secondsFromGMT: 0)
    components.year = year
    components.month = month
    components.day = day
    return components.date!
}

extension StudentProfile {
    static func fixture() -> StudentProfile {
        StudentProfile(
            grade: "1",
            classroom: "2",
            schoolName: "미사중학교",
            officeCode: "J10",
            schoolCode: "7531093",
            schoolKind: "중학교"
        )
    }
}

struct StubSchoolRepository: SchoolRepository {
    var searchResults: [SchoolInfo] = []
    var todayMeals: [MealInfo] = []
    var weekMeals: [MealInfo] = []
    var timetable: [TimetableItem] = []
    var schedule: [SchoolEvent] = []
    var notices: [NoticePreview] = []

    func searchSchools(query: String) async throws -> [SchoolInfo] {
        searchResults
    }

    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] {
        todayMeals
    }

    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] {
        weekMeals
    }

    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] {
        timetable
    }

    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] {
        schedule
    }

    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] {
        notices
    }
}

actor StubNotificationAuthorizationProvider: NotificationAuthorizationProviding {
    private var statuses: [NotificationAuthorizationStatus]
    private var requests = 0

    init(statusSequence: [NotificationAuthorizationStatus] = [.authorized]) {
        self.statuses = statusSequence
    }

    func authorizationStatus() async -> NotificationAuthorizationStatus {
        if statuses.count > 1 {
            return statuses.removeFirst()
        }
        return statuses.first ?? .authorized
    }

    func requestAuthorization() async -> Bool {
        requests += 1
        return true
    }

    func requestCount() -> Int {
        requests
    }
}

final class SpyWidgetTimelineReloader: WidgetTimelineReloading {
    private(set) var reloadCount = 0

    func reloadAllTimelines() {
        reloadCount += 1
    }
}
