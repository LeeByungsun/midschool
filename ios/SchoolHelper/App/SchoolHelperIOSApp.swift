import SwiftUI

@main
struct SchoolHelperIOSApp: App {
    @StateObject private var appState = AppState()
    @StateObject private var notificationPermissionCoordinator = NotificationPermissionCoordinator()
    private let timerPreferencesStore = TimerPreferencesStore()
    private let timerSettingsStore = TimerSettingsStore()
    private let timerNotificationScheduler = TimerNotificationScheduler()

    init() {
        FirebaseCrashReporting.configureIfAvailable()
    }

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environmentObject(appState)
                .onOpenURL { url in
                    appState.handleDeepLink(url)
                }
                .task {
                    applyTimerLaunchOverrideIfNeeded()
                    appState.refresh()
                    await notificationPermissionCoordinator.refreshIfNeeded(
                        isSetupComplete: appState.isSetupComplete
                    )
                }
        }
    }

    private func applyTimerLaunchOverrideIfNeeded() {
        guard let overrideState = AppLaunchOverrides.timerState() else { return }
        timerPreferencesStore.save(overrideState)
        scheduleTimerNotificationSmokeIfNeeded(for: overrideState)
    }

    private func scheduleTimerNotificationSmokeIfNeeded(for state: TimerSessionState) {
        guard AppLaunchOverrides.shouldScheduleTimerNotification() else { return }
        guard let targetDate = state.targetDate else { return }
        let settings = timerSettingsStore.load()
        guard settings.notificationEnabled else { return }

        timerNotificationScheduler.requestAuthorizationIfNeeded()
        timerNotificationScheduler.scheduleTimerCompletion(
            at: targetDate,
            presetTitle: state.preset.title,
            vibrationEnabled: settings.vibrationEnabled
        )
    }
}
