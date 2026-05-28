import Foundation

enum AppRoute: String, CaseIterable {
    case home
    case timetable
    case meals
    case schedule
    case timer
    case settings
}

final class AppState: ObservableObject {
    @Published var profile: StudentProfile
    @Published var selectedRoute: AppRoute = .home

    private let store: StudentPreferencesStore
    private let widgetTimelineReloader: WidgetTimelineReloading

    init(
        store: StudentPreferencesStore = StudentPreferencesStore(),
        widgetTimelineReloader: WidgetTimelineReloading = WidgetTimelineReloader()
    ) {
        self.store = store
        self.widgetTimelineReloader = widgetTimelineReloader
        if AppLaunchOverrides.shouldResetProfile() {
            store.removeProfile()
        }
        self.profile = store.load()
    }

    var isSetupComplete: Bool {
        profile.isComplete
    }

    func refresh() {
        if AppLaunchOverrides.shouldResetProfile() {
            store.removeProfile()
        }
        if let seededProfile = AppLaunchOverrides.seededProfile() {
            saveProfile(seededProfile)
            applyInitialRouteOverride()
            return
        }
        profile = store.load()
        applyInitialRouteOverride()
    }

    func saveProfile(_ profile: StudentProfile) {
        store.save(profile)
        self.profile = profile
        selectedRoute = .home
        widgetTimelineReloader.reloadAllTimelines()
    }

    func handleDeepLink(_ url: URL) {
        guard url.scheme == "schoolhelper" else { return }
        if !isSetupComplete {
            selectedRoute = .settings
            return
        }

        let routeString = if !url.host.orEmpty.isEmpty {
            url.host.orEmpty
        } else {
            url.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        }

        guard let route = AppRoute(rawValue: routeString) else { return }
        selectedRoute = route
    }

    private func applyInitialRouteOverride() {
        guard let route = AppLaunchOverrides.initialRoute() else { return }
        if !isSetupComplete && route != .settings {
            return
        }
        selectedRoute = route
    }
}

private extension Optional where Wrapped == String {
    var orEmpty: String { self ?? "" }
}
