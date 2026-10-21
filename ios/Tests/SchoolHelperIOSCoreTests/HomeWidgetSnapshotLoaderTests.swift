import XCTest
@testable import SchoolHelperIOSCore

final class HomeWidgetSnapshotLoaderTests: XCTestCase {
    func testSnapshotUsesRequestedProfileAndFallsBackToActiveProfile() async {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let profileStore = StudentPreferencesStore(defaults: defaults)
        let first = StudentProfile.fixture()
        profileStore.save(first)
        var second = StudentProfile.fixture()
        second.id = "second-profile"
        second.displayName = "민준"
        second.schoolName = "민준중학교"
        XCTAssertTrue(profileStore.add(second))
        let loader = HomeWidgetSnapshotLoader(
            profileStore: profileStore,
            timerStore: TimerPreferencesStore(defaults: defaults),
            widgetSettingsStore: WidgetSettingsStore(defaults: defaults),
            repository: ProfileAwareWidgetRepository()
        )
        let date = fixtureDate(year: 2026, month: 5, day: 26)

        let selected = await loader.load(now: date, profileID: first.id)
        let fallback = await loader.load(now: date, profileID: "deleted-profile")

        XCTAssertEqual(selected.schoolLabel, "미사중학교 1학년 2반")
        XCTAssertEqual(selected.todayTimetable, "1교시 지율")
        XCTAssertEqual(fallback.schoolLabel, "민준중학교 1학년 2반")
        XCTAssertEqual(fallback.todayTimetable, "1교시 민준")
    }

    func testSmallWidgetSnapshotAlwaysHidesTomorrowTimetable() {
        let snapshot = HomeWidgetSnapshot(
            headerDate: "📅 5월 26일 (화)",
            schoolLabel: "미사중학교 1학년 2반",
            timerSummary: "집중 • 25:00",
            todayTimetable: "1교시 국어",
            tomorrowTimetable: "1교시 영어",
            requiresSetup: false
        )

        let smallSnapshot = snapshot.forSmallWidget()

        XCTAssertEqual(smallSnapshot.todayTimetable, "1교시 국어")
        XCTAssertNil(smallSnapshot.tomorrowTimetable)
    }

    func testSnapshotRequiresSetupWhenProfileIsIncomplete() async {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let profileStore = StudentPreferencesStore(defaults: defaults)
        let timerStore = TimerPreferencesStore(defaults: defaults)
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
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
            widgetSettingsStore: widgetSettingsStore,
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
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
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
            widgetSettingsStore: widgetSettingsStore,
            repository: repository
        )

        let snapshot = await loader.load(now: fixtureDate(year: 2026, month: 5, day: 26))

        XCTAssertFalse(snapshot.requiresSetup)
        XCTAssertEqual(snapshot.schoolLabel, "미사중학교 1학년 2반")
        XCTAssertEqual(snapshot.timerSummary, "휴식 • 07:00 남음")
        XCTAssertEqual(snapshot.todayTimetable, "1교시 국어\n2교시 수학")
        XCTAssertEqual(snapshot.tomorrowTimetable, "1교시 영어")
    }

    func testSnapshotTruncatesLongSubjectsLikeAndroidWidget() async {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let profileStore = StudentPreferencesStore(defaults: defaults)
        profileStore.save(.fixture())
        let timerStore = TimerPreferencesStore(defaults: defaults)
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
        widgetSettingsStore.save(WidgetSettings(showTomorrowTimetable: false))

        let loader = HomeWidgetSnapshotLoader(
            profileStore: profileStore,
            timerStore: timerStore,
            widgetSettingsStore: widgetSettingsStore,
            repository: WidgetTimetableRepository(
                todayItems: [
                    TimetableItem(date: "20260526", period: "1", subject: "창의적체험활동", grade: "1", classroom: "2"),
                    TimetableItem(date: "20260526", period: "2", subject: "수학", grade: "1", classroom: "2"),
                ],
                tomorrowItems: []
            )
        )

        let snapshot = await loader.load(now: fixtureDate(year: 2026, month: 5, day: 26))

        XCTAssertEqual(snapshot.todayTimetable, "1교시 창의적체험\n2교시 수학")
    }

    func testSnapshotKeepsPeriodsAfterFifthPeriod() async {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let profileStore = StudentPreferencesStore(defaults: defaults)
        profileStore.save(.fixture())
        let timerStore = TimerPreferencesStore(defaults: defaults)
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
        widgetSettingsStore.save(WidgetSettings(showTomorrowTimetable: false))

        let loader = HomeWidgetSnapshotLoader(
            profileStore: profileStore,
            timerStore: timerStore,
            widgetSettingsStore: widgetSettingsStore,
            repository: WidgetTimetableRepository(
                todayItems: (1...7).map { period in
                    TimetableItem(
                        date: "20260526",
                        period: "\(period)",
                        subject: "수업\(period)",
                        grade: "1",
                        classroom: "2"
                    )
                },
                tomorrowItems: []
            )
        )

        let snapshot = await loader.load(now: fixtureDate(year: 2026, month: 5, day: 26))

        XCTAssertEqual(
            snapshot.todayTimetable,
            """
            1교시 수업1
            2교시 수업2
            3교시 수업3
            4교시 수업4
            5교시 수업5
            6교시 수업6
            7교시 수업7
            """
        )
    }


    func testSnapshotKeepsTomorrowPeriodsAfterFifthPeriod() async {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let profileStore = StudentPreferencesStore(defaults: defaults)
        profileStore.save(.fixture())
        let timerStore = TimerPreferencesStore(defaults: defaults)
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
        widgetSettingsStore.save(WidgetSettings(showTomorrowTimetable: true))

        let loader = HomeWidgetSnapshotLoader(
            profileStore: profileStore,
            timerStore: timerStore,
            widgetSettingsStore: widgetSettingsStore,
            repository: WidgetTimetableRepository(
                todayItems: [],
                tomorrowItems: (1...7).map { period in
                    TimetableItem(
                        date: "20260527",
                        period: "\(period)",
                        subject: "내일수업\(period)",
                        grade: "1",
                        classroom: "2"
                    )
                }
            )
        )

        let snapshot = await loader.load(now: fixtureDate(year: 2026, month: 5, day: 26))

        XCTAssertEqual(
            snapshot.tomorrowTimetable,
            """
            1교시 내일수업1
            2교시 내일수업2
            3교시 내일수업3
            4교시 내일수업4
            5교시 내일수업5
            6교시 내일수업6
            7교시 내일수업7
            """
        )
    }

    func testSnapshotCanHideTomorrowTimetableFromSharedSettings() async {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let profileStore = StudentPreferencesStore(defaults: defaults)
        profileStore.save(.fixture())
        let timerStore = TimerPreferencesStore(defaults: defaults)
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
        widgetSettingsStore.save(WidgetSettings(showTomorrowTimetable: false))

        let loader = HomeWidgetSnapshotLoader(
            profileStore: profileStore,
            timerStore: timerStore,
            widgetSettingsStore: widgetSettingsStore,
            repository: WidgetTimetableRepository(todayItems: [], tomorrowItems: [])
        )

        let snapshot = await loader.load(now: fixtureDate(year: 2026, month: 5, day: 26))

        XCTAssertNil(snapshot.tomorrowTimetable)
    }

    func testSharedWidgetSettingOverridesPerWidgetTomorrowRequest() async {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let profileStore = StudentPreferencesStore(defaults: defaults)
        profileStore.save(.fixture())
        let timerStore = TimerPreferencesStore(defaults: defaults)
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
        widgetSettingsStore.save(WidgetSettings(showTomorrowTimetable: false))

        let loader = HomeWidgetSnapshotLoader(
            profileStore: profileStore,
            timerStore: timerStore,
            widgetSettingsStore: widgetSettingsStore,
            repository: WidgetTimetableRepository(
                todayItems: [],
                tomorrowItems: [
                    TimetableItem(
                        date: "20260527",
                        period: "1",
                        subject: "영어",
                        grade: "1",
                        classroom: "2"
                    ),
                ]
            )
        )

        let snapshot = await loader.load(
            now: fixtureDate(year: 2026, month: 5, day: 26),
            showTomorrow: true
        )

        XCTAssertNil(snapshot.tomorrowTimetable)
    }
}

private struct ProfileAwareWidgetRepository: SchoolRepository {
    func searchSchools(query: String) async throws -> [SchoolInfo] { [] }
    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] { [] }
    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] { [] }
    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] {
        [TimetableItem(date: "20260526", period: "1", subject: profile.displayName, grade: profile.grade, classroom: profile.classroom)]
    }
    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] { [] }
    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] { [] }
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
