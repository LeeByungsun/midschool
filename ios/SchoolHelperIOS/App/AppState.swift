import Foundation

final class AppState: ObservableObject {
    @Published var profile: StudentProfile

    private let store: StudentPreferencesStore

    init(store: StudentPreferencesStore = StudentPreferencesStore()) {
        self.store = store
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
    }
}
