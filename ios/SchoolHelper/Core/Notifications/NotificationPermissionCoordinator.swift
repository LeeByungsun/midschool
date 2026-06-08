import Foundation

@MainActor
final class NotificationPermissionCoordinator: ObservableObject {
    private let timerSettingsStore: TimerSettingsStore
    private let authorizationProvider: NotificationAuthorizationProviding

    init(
        timerSettingsStore: TimerSettingsStore = TimerSettingsStore(),
        authorizationProvider: NotificationAuthorizationProviding = NotificationAuthorizationProvider.fromEnvironment()
    ) {
        self.timerSettingsStore = timerSettingsStore
        self.authorizationProvider = authorizationProvider
    }

    func refreshIfNeeded(isSetupComplete: Bool) async {
        if shouldSkipNotificationRequest() { return }
        guard isSetupComplete else { return }
        let settings = timerSettingsStore.load()
        guard settings.notificationEnabled else { return }

        let status = await authorizationProvider.authorizationStatus()
        if status == .notDetermined {
            _ = await authorizationProvider.requestAuthorization()
        }
    }

    private func shouldSkipNotificationRequest(
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> Bool {
        let value = environment["SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST"]?.lowercased()
        return value == "1" || value == "true" || value == "yes"
    }

}
