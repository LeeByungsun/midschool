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

    func testSettingsViewModelSavesWidgetSettings() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let timerSettingsStore = TimerSettingsStore(defaults: defaults)
        let widgetSettingsStore = WidgetSettingsStore(defaults: defaults)
        let viewModel = SettingsViewModel(
            initialProfile: StudentProfile.fixture(),
            repository: MockSchoolRepository(),
            timerSettingsStore: timerSettingsStore,
            widgetSettingsStore: widgetSettingsStore,
            notificationAuthorizationProvider: StubNotificationAuthorizationProvider()
        )

        viewModel.showTomorrowTimetable = false
        viewModel.saveTimerSettings()

        XCTAssertFalse(widgetSettingsStore.load().showTomorrowTimetable)
    }
}
