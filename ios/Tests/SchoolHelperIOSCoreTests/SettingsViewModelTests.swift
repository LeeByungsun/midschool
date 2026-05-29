import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class SettingsViewModelTests: XCTestCase {
    func testSettingsViewModelLoadsSavedTimerSettings() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let timerSettingsStore = TimerSettingsStore(defaults: defaults)
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
        timerSettingsStore.save(
            TimerSettings(
                displayMode: .ring,
                notificationEnabled: false,
                vibrationEnabled: false
            )
        )

        let viewModel = SettingsViewModel(
            initialProfile: StudentProfile.fixture(),
            repository: MockSchoolRepository(),
            timerSettingsStore: timerSettingsStore,
            widgetSettingsStore: widgetSettingsStore,
            notificationAuthorizationProvider: StubNotificationAuthorizationProvider()
        )

        XCTAssertEqual(viewModel.timerDisplayMode, .ring)
        XCTAssertFalse(viewModel.notificationEnabled)
        XCTAssertFalse(viewModel.vibrationEnabled)
        XCTAssertTrue(viewModel.showTomorrowTimetable)
    }

    func testSettingsViewModelRefreshesAndRequestsNotificationPermission() async {
        let provider = StubNotificationAuthorizationProvider(
            statusSequence: [.notDetermined, .authorized]
        )
        let viewModel = SettingsViewModel(
            initialProfile: StudentProfile.fixture(),
            repository: MockSchoolRepository(),
            notificationAuthorizationProvider: provider
        )

        await viewModel.refreshNotificationPermission()
        XCTAssertEqual(viewModel.notificationPermissionSummary, "타이머 완료 알림을 받으려면 권한이 필요해요.")
        XCTAssertTrue(viewModel.canRequestNotificationPermission)

        await viewModel.requestNotificationPermission()
        XCTAssertEqual(viewModel.notificationPermissionSummary, "알림 권한이 허용되어 있어요.")
        XCTAssertFalse(viewModel.canRequestNotificationPermission)
        let requestCount = await provider.requestCount()
        XCTAssertEqual(requestCount, 1)
    }

    func testSettingsViewModelSavesTimerAndWidgetSettingsThenReloadsWidget() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let timerSettingsStore = TimerSettingsStore(defaults: defaults)
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
        let reloader = SpyWidgetTimelineReloader()
        let scheduler = SpyTimerNotificationScheduler()
        let viewModel = SettingsViewModel(
            initialProfile: StudentProfile.fixture(),
            repository: MockSchoolRepository(),
            timerSettingsStore: timerSettingsStore,
            widgetSettingsStore: widgetSettingsStore,
            notificationAuthorizationProvider: StubNotificationAuthorizationProvider(),
            notificationScheduler: scheduler,
            widgetTimelineReloader: reloader
        )

        viewModel.showTomorrowTimetable = false
        viewModel.timerDisplayMode = .ring
        viewModel.notificationEnabled = false
        viewModel.vibrationEnabled = false
        viewModel.saveTimerSettings()

        XCTAssertEqual(timerSettingsStore.load().displayMode, .ring)
        XCTAssertFalse(timerSettingsStore.load().notificationEnabled)
        XCTAssertFalse(timerSettingsStore.load().vibrationEnabled)
        XCTAssertFalse(widgetSettingsStore.load().showTomorrowTimetable)
        XCTAssertEqual(scheduler.cancelCalls, 1)
        XCTAssertEqual(reloader.reloadCount, 1)
    }

    func testSettingsViewModelCancelsPendingTimerNotificationWhenNotificationsAreDisabled() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let timerSettingsStore = TimerSettingsStore(defaults: defaults)
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
        let scheduler = SpyTimerNotificationScheduler()
        let viewModel = SettingsViewModel(
            initialProfile: StudentProfile.fixture(),
            repository: MockSchoolRepository(),
            timerSettingsStore: timerSettingsStore,
            widgetSettingsStore: widgetSettingsStore,
            notificationAuthorizationProvider: StubNotificationAuthorizationProvider(),
            notificationScheduler: scheduler
        )

        viewModel.notificationEnabled = false
        viewModel.saveTimerSettings()

        XCTAssertFalse(timerSettingsStore.load().notificationEnabled)
        XCTAssertEqual(scheduler.cancelCalls, 1)
    }

    func testSettingsViewModelKeepsPendingTimerNotificationWhenNotificationsRemainEnabled() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let timerSettingsStore = TimerSettingsStore(defaults: defaults)
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
        let scheduler = SpyTimerNotificationScheduler()
        let viewModel = SettingsViewModel(
            initialProfile: StudentProfile.fixture(),
            repository: MockSchoolRepository(),
            timerSettingsStore: timerSettingsStore,
            widgetSettingsStore: widgetSettingsStore,
            notificationAuthorizationProvider: StubNotificationAuthorizationProvider(),
            notificationScheduler: scheduler
        )

        viewModel.notificationEnabled = true
        viewModel.saveTimerSettings()

        XCTAssertTrue(timerSettingsStore.load().notificationEnabled)
        XCTAssertEqual(scheduler.cancelCalls, 0)
    }
}
