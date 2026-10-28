import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class NotificationPermissionCoordinatorTests: XCTestCase {
    func testForegroundTimerNotificationPresentationPolicy() {
        let identifier = TimerNotificationPresentationPolicy.timerCompletionIdentifier

        XCTAssertTrue(TimerNotificationPresentationPolicy.shouldPlaySound(identifier: identifier))
        XCTAssertTrue(
            TimerNotificationPresentationPolicy.shouldPlayHaptic(
                identifier: identifier,
                userInfo: [TimerNotificationPresentationPolicy.vibrationEnabledKey: true]
            )
        )
        XCTAssertFalse(
            TimerNotificationPresentationPolicy.shouldPlayHaptic(
                identifier: identifier,
                userInfo: [TimerNotificationPresentationPolicy.vibrationEnabledKey: false]
            )
        )
        XCTAssertEqual(TimerNotificationPresentationPolicy.timerIdentifiers.count, 7)
        for item in TimerNotificationPresentationPolicy.timerIdentifiers {
            XCTAssertTrue(TimerNotificationPresentationPolicy.shouldPlaySound(identifier: item))
            XCTAssertTrue(TimerNotificationPresentationPolicy.shouldPlayHaptic(identifier: item, userInfo: [TimerNotificationPresentationPolicy.vibrationEnabledKey: true]))
        }
        XCTAssertFalse(TimerNotificationPresentationPolicy.shouldPlaySound(identifier: identifier + ".7"))
        XCTAssertFalse(TimerNotificationPresentationPolicy.shouldPlaySound(identifier: "other"))
    }

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
