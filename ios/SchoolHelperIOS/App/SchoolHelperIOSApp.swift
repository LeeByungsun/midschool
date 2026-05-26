import SwiftUI

@main
struct SchoolHelperIOSApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environmentObject(appState)
                .task {
                    appState.refresh()
                }
        }
    }
}
