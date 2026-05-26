import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class NotificationPermissionCoordinatorTests: XCTestCase {
    func testCoordinatorRequestsPermissionWhenNotificationsEnabledAndUndetermined() async {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let settingsStore = TimerSettingsStore(defaults: defaults)
        settingsStore.save(
            TimerSettings(
                displayMode: .count,
                notificationEnabled: true,
                vibrationEnabled: true
            )
        )
        let provider = StubNotificationAuthorizationProvider(statusSequence: [.notDetermined, .authorized])
        let coordinator = NotificationPermissionCoordinator(
            timerSettingsStore: settingsStore,
            authorizationProvider: provider
        )

        await coordinator.refreshIfNeeded()

        let requestCount = await provider.requestCount()
        XCTAssertEqual(requestCount, 1)
    }

    func testCoordinatorSkipsPermissionRequestWhenNotificationsDisabled() async {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let settingsStore = TimerSettingsStore(defaults: defaults)
        settingsStore.save(
            TimerSettings(
                displayMode: .count,
                notificationEnabled: false,
                vibrationEnabled: true
            )
        )
        let provider = StubNotificationAuthorizationProvider(statusSequence: [.notDetermined])
        let coordinator = NotificationPermissionCoordinator(
            timerSettingsStore: settingsStore,
            authorizationProvider: provider
        )

        await coordinator.refreshIfNeeded()

        let requestCount = await provider.requestCount()
        XCTAssertEqual(requestCount, 0)
    }
}
