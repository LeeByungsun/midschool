import SwiftUI

struct RootTabView: View {
    @EnvironmentObject private var appState: AppState
    @State private var selectedTab: PrimaryTab = .home

    var body: some View {
        Group {
            if appState.isSetupComplete {
                TabView(selection: tabSelection) {
                    HomeView()
                        .tag(PrimaryTab.home)
                        .tabItem { Label("홈", systemImage: "house") }
                    TimetableView()
                        .tag(PrimaryTab.timetable)
                        .tabItem { Label("시간표", systemImage: "calendar") }
                    MealsView()
                        .tag(PrimaryTab.meals)
                        .tabItem { Label("급식", systemImage: "fork.knife") }
                    ScheduleView()
                        .tag(PrimaryTab.schedule)
                        .tabItem { Label("일정", systemImage: "list.bullet.rectangle") }
                }
                .sheet(isPresented: timerPresented) {
                    TimerView()
                }
                .sheet(isPresented: settingsPresented) {
                    SettingsView()
                        .environmentObject(appState)
                }
                .onAppear {
                    if !isPresentedRoute(appState.selectedRoute) {
                        selectedTab = PrimaryTab(route: appState.selectedRoute)
                    }
                }
                .onChange(of: appState.selectedRoute) { route in
                    guard !isPresentedRoute(route) else { return }
                    let nextTab = PrimaryTab(route: route)
                    if selectedTab != nextTab {
                        selectedTab = nextTab
                    }
                }
            } else {
                SetupView()
                    .environmentObject(appState)
            }
        }
    }

    private func isPresentedRoute(_ route: AppRoute) -> Bool {
        route == .settings || route == .timer
    }

    private var tabSelection: Binding<PrimaryTab> {
        Binding(
            get: { selectedTab },
            set: { newTab in
                selectedTab = newTab
                if appState.selectedRoute != newTab.route {
                    appState.selectedRoute = newTab.route
                }
            }
        )
    }

    private var timerPresented: Binding<Bool> {
        Binding(
            get: { appState.selectedRoute == .timer },
            set: { isPresented in
                if !isPresented {
                    appState.selectedRoute = selectedTab.route
                }
            }
        )
    }

    private var settingsPresented: Binding<Bool> {
        Binding(
            get: { appState.selectedRoute == .settings },
            set: { isPresented in
                if !isPresented {
                    appState.selectedRoute = selectedTab.route
                }
            }
        )
    }
}

private enum PrimaryTab: Hashable {
    case home
    case timetable
    case meals
    case schedule

    init(route: AppRoute) {
        switch route {
        case .home:
            self = .home
        case .timetable:
            self = .timetable
        case .meals:
            self = .meals
        case .schedule:
            self = .schedule
        case .settings:
            self = .home
        case .timer:
            self = .home
        }
    }

    var route: AppRoute {
        switch self {
        case .home:
            return .home
        case .timetable:
            return .timetable
        case .meals:
            return .meals
        case .schedule:
            return .schedule
        }
    }
}
