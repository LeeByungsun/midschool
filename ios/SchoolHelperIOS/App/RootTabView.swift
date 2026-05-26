import SwiftUI

struct RootTabView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        Group {
            if appState.isSetupComplete {
                TabView {
                    HomeView()
                        .tabItem { Label("홈", systemImage: "house") }
                    TimetableView()
                        .tabItem { Label("시간표", systemImage: "calendar") }
                    MealsView()
                        .tabItem { Label("급식", systemImage: "fork.knife") }
                    ScheduleView()
                        .tabItem { Label("일정", systemImage: "list.bullet.rectangle") }
                    TimerView()
                        .tabItem { Label("타이머", systemImage: "timer") }
                    SettingsView()
                        .tabItem { Label("설정", systemImage: "gearshape") }
                }
            } else {
                SetupView()
                    .environmentObject(appState)
            }
        }
    }
}
