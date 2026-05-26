import Foundation

@MainActor
final class NotificationPermissionCoordinator: ObservableObject {
    private let timerSettingsStore: TimerSettingsStore
    private let authorizationProvider: NotificationAuthorizationProviding

    init(
        timerSettingsStore: TimerSettingsStore = TimerSettingsStore(),
        authorizationProvider: NotificationAuthorizationProviding = NotificationAuthorizationProvider()
    ) {
        self.timerSettingsStore = timerSettingsStore
        self.authorizationProvider = authorizationProvider
    }

    func refreshIfNeeded() async {
        let settings = timerSettingsStore.load()
        guard settings.notificationEnabled else { return }

        let status = await authorizationProvider.authorizationStatus()
        if status == .notDetermined {
            _ = await authorizationProvider.requestAuthorization()
        }
    }
}
