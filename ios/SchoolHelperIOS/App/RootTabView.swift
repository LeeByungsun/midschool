import SwiftUI

struct RootTabView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        Group {
            if appState.isSetupComplete {
                TabView(selection: $appState.selectedRoute) {
                    HomeView()
                        .tag(AppRoute.home)
                        .tabItem { Label("홈", systemImage: "house") }
                    TimetableView()
                        .tag(AppRoute.timetable)
                        .tabItem { Label("시간표", systemImage: "calendar") }
                    MealsView()
                        .tag(AppRoute.meals)
                        .tabItem { Label("급식", systemImage: "fork.knife") }
                    ScheduleView()
                        .tag(AppRoute.schedule)
                        .tabItem { Label("일정", systemImage: "list.bullet.rectangle") }
                    TimerView()
                        .tag(AppRoute.timer)
                        .tabItem { Label("타이머", systemImage: "timer") }
                    SettingsView()
                        .tag(AppRoute.settings)
                        .tabItem { Label("설정", systemImage: "gearshape") }
                }
            } else {
                SetupView()
                    .environmentObject(appState)
            }
        }
    }
}
