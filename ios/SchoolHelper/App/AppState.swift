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
    @Published private(set) var profiles: [StudentProfile]
    @Published var selectedRoute: AppRoute = .home
    @Published private(set) var settingsRevision = 0

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
        self.profiles = store.loadProfiles()
    }

    var isSetupComplete: Bool {
        profile.isComplete
    }

    func refresh() {
        if AppLaunchOverrides.shouldResetProfile() {
            store.removeProfile()
        }
        if let seededProfiles = AppLaunchOverrides.seededProfiles(),
           let firstProfile = seededProfiles.first {
            store.removeProfile()
            if store.save(firstProfile) {
                for profile in seededProfiles.dropFirst() {
                    _ = store.add(profile)
                }
            }
            profile = store.load()
            profiles = store.loadProfiles()
            applyInitialRouteOverride()
            return
        }
        if let seededProfile = AppLaunchOverrides.seededProfile() {
            saveProfile(seededProfile)
            applyInitialRouteOverride()
            return
        }
        profile = store.load()
        profiles = store.loadProfiles()
        applyInitialRouteOverride()
    }

    @discardableResult
    func saveProfile(_ profile: StudentProfile) -> Bool {
        guard store.save(profile) else { return false }
        self.profile = store.load()
        profiles = store.loadProfiles()
        selectedRoute = .home
        widgetTimelineReloader.reloadAllTimelines()
        return true
    }

    @discardableResult
    func selectProfile(id: String) -> Bool {
        guard store.selectProfile(id: id) else { return false }
        profile = store.load()
        profiles = store.loadProfiles()
        widgetTimelineReloader.reloadAllTimelines()
        return true
    }

    @discardableResult
    func addProfile(_ profile: StudentProfile) -> Bool {
        guard store.add(profile) else { return false }
        self.profile = store.load()
        profiles = store.loadProfiles()
        widgetTimelineReloader.reloadAllTimelines()
        return true
    }

    @discardableResult
    func deleteProfile(id: String) -> Bool {
        guard store.deleteProfile(id: id) else { return false }
        profile = store.load()
        profiles = store.loadProfiles()
        widgetTimelineReloader.reloadAllTimelines()
        return true
    }

    func notifySettingsChanged() {
        settingsRevision += 1
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
