import SwiftUI

@main
struct SchoolHelperIOSApp: App {
    @StateObject private var appState = AppState()
    @StateObject private var notificationPermissionCoordinator = NotificationPermissionCoordinator()

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environmentObject(appState)
                .task {
                    appState.refresh()
                    await notificationPermissionCoordinator.refreshIfNeeded()
                }
        }
    }
}
