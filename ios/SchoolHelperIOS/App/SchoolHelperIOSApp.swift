import SwiftUI

@main
struct SchoolHelperIOSApp: App {
    @StateObject private var appState = AppState()
    @StateObject private var notificationPermissionCoordinator = NotificationPermissionCoordinator()
    private let timerPreferencesStore = TimerPreferencesStore()

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
    }
}
