import Foundation

final class AppState: ObservableObject {
    @Published var profile: StudentProfile

    private let store: StudentPreferencesStore
    private let widgetTimelineReloader: WidgetTimelineReloading

    init(
        store: StudentPreferencesStore = StudentPreferencesStore(),
        widgetTimelineReloader: WidgetTimelineReloading = WidgetTimelineReloader()
    ) {
        self.store = store
        self.widgetTimelineReloader = widgetTimelineReloader
        self.profile = store.load()
    }

    var isSetupComplete: Bool {
        profile.isComplete
    }

    func refresh() {
        profile = store.load()
    }

    func saveProfile(_ profile: StudentProfile) {
        store.save(profile)
        self.profile = profile
        widgetTimelineReloader.reloadAllTimelines()
    }
}
