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
            schoolCode: "7692129",
            schoolKind: "중학교"
        )
    }
}

func withEnvironment<T>(_ overrides: [String: String], run: () -> T) -> T {
    var previous: [String: String?] = [:]
    for (key, value) in overrides {
        previous[key] = ProcessInfo.processInfo.environment[key]
        setenv(key, value, 1)
    }
    defer {
        for (key, value) in previous {
            if let value {
                setenv(key, value, 1)
            } else {
                unsetenv(key)
            }
        }
    }
    return run()
}

func withAsyncEnvironment<T>(_ overrides: [String: String], run: () async -> T) async -> T {
    var previous: [String: String?] = [:]
    for (key, value) in overrides {
        previous[key] = ProcessInfo.processInfo.environment[key]
        setenv(key, value, 1)
    }
    defer {
        for (key, value) in previous {
            if let value {
                setenv(key, value, 1)
            } else {
                unsetenv(key)
            }
        }
    }
    return await run()
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

final class SpyTimerNotificationScheduler: TimerNotificationScheduling {
    private(set) var requestAuthorizationCalls = 0
    private(set) var cancelCalls = 0
    private(set) var scheduleCalls: [(date: Date, presetTitle: String, vibrationEnabled: Bool)] = []

    func requestAuthorizationIfNeeded() {
        requestAuthorizationCalls += 1
    }

    func scheduleTimerCompletion(at date: Date, presetTitle: String, vibrationEnabled: Bool) {
        scheduleCalls.append((date, presetTitle, vibrationEnabled))
    }

    func cancelPendingTimerCompletion() {
        cancelCalls += 1
    }
}
