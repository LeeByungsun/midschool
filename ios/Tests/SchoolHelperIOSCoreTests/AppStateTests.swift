import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class AppStateTests: XCTestCase {
    func testSaveProfileTriggersWidgetReload() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let reloader = SpyWidgetTimelineReloader()
        let appState = AppState(
            store: StudentPreferencesStore(defaults: defaults),
            widgetTimelineReloader: reloader
        )

        appState.saveProfile(.fixture())

        XCTAssertEqual(reloader.reloadCount, 1)
    }

    func testHandleDeepLinkRoutesToTargetTab() {
        let appState = AppState(
            store: StudentPreferencesStore(defaults: UserDefaults(suiteName: #function)!),
            widgetTimelineReloader: SpyWidgetTimelineReloader()
        )
        appState.profile = .fixture()

        appState.handleDeepLink(URL(string: "schoolhelper://timer")!)
        XCTAssertEqual(appState.selectedRoute, .timer)

        appState.handleDeepLink(URL(string: "schoolhelper://settings")!)
        XCTAssertEqual(appState.selectedRoute, .settings)
    }

    func testRefreshUsesSeededProfileWhenRequested() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let appState = AppState(
            store: StudentPreferencesStore(defaults: defaults),
            widgetTimelineReloader: SpyWidgetTimelineReloader()
        )

        withEnvironment([
            "SCHOOLHELPER_SEED_PROFILE": "fixture"
        ]) {
            appState.refresh()
        }

        XCTAssertEqual(appState.profile.schoolName, "미사중학교")
        XCTAssertTrue(appState.isSetupComplete)
    }

    func testRefreshAppliesInitialRouteOverrideWhenSetupIsComplete() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let appState = AppState(
            store: StudentPreferencesStore(defaults: defaults),
            widgetTimelineReloader: SpyWidgetTimelineReloader()
        )

        withEnvironment([
            "SCHOOLHELPER_SEED_PROFILE": "fixture",
            "SCHOOLHELPER_INITIAL_ROUTE": "timetable"
        ]) {
            appState.refresh()
        }

        XCTAssertEqual(appState.selectedRoute, .timetable)
    }

    func testRefreshIgnoresNonSettingsRouteOverrideWhenSetupIsIncomplete() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let appState = AppState(
            store: StudentPreferencesStore(defaults: defaults),
            widgetTimelineReloader: SpyWidgetTimelineReloader()
        )

        withEnvironment([
            "SCHOOLHELPER_INITIAL_ROUTE": "timetable"
        ]) {
            appState.refresh()
        }

        XCTAssertEqual(appState.selectedRoute, .home)
    }

    func testTimerLaunchOverrideBuildsRunningState() {
        let referenceNow = Date(timeIntervalSince1970: 1_700_000_000)

        let state = AppLaunchOverrides.timerState(
            from: [
                "SCHOOLHELPER_TIMER_PRESET": "shortBreak",
                "SCHOOLHELPER_TIMER_REMAINING_SECONDS": "123",
                "SCHOOLHELPER_TIMER_RUNNING": "1"
            ],
            now: referenceNow
        )

        XCTAssertEqual(state?.preset, .shortBreak)
        XCTAssertEqual(state?.totalSeconds, TimerPreset.shortBreak.durationSeconds)
        XCTAssertEqual(state?.remainingSeconds, 123)
        XCTAssertEqual(state?.targetDate, referenceNow.addingTimeInterval(123))
        XCTAssertEqual(state?.isRunning, true)
    }
}
