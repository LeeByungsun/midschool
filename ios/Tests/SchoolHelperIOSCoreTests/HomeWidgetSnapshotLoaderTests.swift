import XCTest
@testable import SchoolHelperIOSCore

final class HomeWidgetSnapshotLoaderTests: XCTestCase {
    func testSnapshotRequiresSetupWhenProfileIsIncomplete() async {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let profileStore = StudentPreferencesStore(defaults: defaults)
        let timerStore = TimerPreferencesStore(defaults: defaults)
        timerStore.save(
            TimerSessionState(
                preset: .focus,
                totalSeconds: 2400,
                remainingSeconds: 1800,
                targetDate: nil,
                isRunning: false
            )
        )
        let loader = HomeWidgetSnapshotLoader(
            profileStore: profileStore,
            timerStore: timerStore,
            repository: StubSchoolRepository()
        )

        let snapshot = await loader.load(now: fixtureDate(year: 2026, month: 5, day: 26))

        XCTAssertTrue(snapshot.requiresSetup)
        XCTAssertEqual(snapshot.headerDate, "📅 5월 26일 (화)")
        XCTAssertEqual(snapshot.schoolLabel, "학교와 학년/반 설정 필요")
        XCTAssertEqual(snapshot.timerSummary, "집중 • 30:00")
        XCTAssertEqual(snapshot.todayTimetable, "시간표를 보려면 설정을 완료해 주세요.")
    }

    func testSnapshotBuildsTodayAndTomorrowTimetable() async {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let profileStore = StudentPreferencesStore(defaults: defaults)
        profileStore.save(.fixture())
        let timerStore = TimerPreferencesStore(defaults: defaults)
        timerStore.save(
            TimerSessionState(
                preset: .shortBreak,
                totalSeconds: 600,
                remainingSeconds: 420,
                targetDate: fixtureDate(year: 2026, month: 5, day: 26).addingTimeInterval(420),
                isRunning: true
            )
        )

        let repository = WidgetTimetableRepository(
            todayItems: [
                TimetableItem(date: "20260526", period: "1", subject: "국어", grade: "1", classroom: "2"),
                TimetableItem(date: "20260526", period: "2", subject: "수학", grade: "1", classroom: "2"),
            ],
            tomorrowItems: [
                TimetableItem(date: "20260527", period: "1", subject: "영어", grade: "1", classroom: "2"),
            ]
        )

        let loader = HomeWidgetSnapshotLoader(
            profileStore: profileStore,
            timerStore: timerStore,
            repository: repository
        )

        let snapshot = await loader.load(now: fixtureDate(year: 2026, month: 5, day: 26))

        XCTAssertFalse(snapshot.requiresSetup)
        XCTAssertEqual(snapshot.schoolLabel, "미사중학교 1학년 2반")
        XCTAssertEqual(snapshot.timerSummary, "휴식 • 07:00 남음")
        XCTAssertEqual(snapshot.todayTimetable, "1교시 국어\n2교시 수학")
        XCTAssertEqual(snapshot.tomorrowTimetable, "1교시 영어")
    }
}

private struct WidgetTimetableRepository: SchoolRepository {
    let todayItems: [TimetableItem]
    let tomorrowItems: [TimetableItem]

    func searchSchools(query: String) async throws -> [SchoolInfo] { [] }
    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] { [] }
    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] { [] }

    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "yyyyMMdd"
        let key = formatter.string(from: date)
        return key == "20260526" ? todayItems : tomorrowItems
    }

    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] { [] }
    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] { [] }
}
