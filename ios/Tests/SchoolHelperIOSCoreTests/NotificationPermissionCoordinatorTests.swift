import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class NotificationPermissionCoordinatorTests: XCTestCase {
    func testLaunchOverrideNotificationProviderUpdatesStatusAfterRequest() async {
        let provider = NotificationAuthorizationProvider.fromEnvironment([
            "SCHOOLHELPER_NOTIFICATION_AUTHORIZATION_STATUS": "not_determined",
            "SCHOOLHELPER_NOTIFICATION_AUTHORIZATION_REQUEST_GRANTED": "true",
        ])

        let initialStatus = await provider.authorizationStatus()
        XCTAssertEqual(initialStatus, .notDetermined)

        let granted = await provider.requestAuthorization()
        let updatedStatus = await provider.authorizationStatus()
        XCTAssertTrue(granted)
        XCTAssertEqual(updatedStatus, .authorized)
    }

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

        await coordinator.refreshIfNeeded(isSetupComplete: true)

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

        await coordinator.refreshIfNeeded(isSetupComplete: true)

        let requestCount = await provider.requestCount()
        XCTAssertEqual(requestCount, 0)
    }

    func testCoordinatorSkipsPermissionRequestWhenSetupIsIncomplete() async {
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
        let provider = StubNotificationAuthorizationProvider(statusSequence: [.notDetermined])
        let coordinator = NotificationPermissionCoordinator(
            timerSettingsStore: settingsStore,
            authorizationProvider: provider
        )

        await coordinator.refreshIfNeeded(isSetupComplete: false)

        let requestCount = await provider.requestCount()
        XCTAssertEqual(requestCount, 0)
    }

    func testCoordinatorSkipsPermissionRequestWhenLaunchOverrideDisablesIt() async {
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
        let provider = StubNotificationAuthorizationProvider(statusSequence: [.notDetermined])
        let coordinator = NotificationPermissionCoordinator(
            timerSettingsStore: settingsStore,
            authorizationProvider: provider
        )

        await withAsyncEnvironment([
            "SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST": "1"
        ]) {
            await coordinator.refreshIfNeeded(isSetupComplete: true)
        }

        let requestCount = await provider.requestCount()
        XCTAssertEqual(requestCount, 0)
    }
}
