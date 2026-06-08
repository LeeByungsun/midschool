import Foundation

final class StudentPreferencesStore {
    private let defaults: UserDefaults
    private let key = "student_profile"

    init(defaults: UserDefaults = AppStorageConfig.userDefaults()) {
        self.defaults = defaults
    }

    func load() -> StudentProfile {
        guard
            let data = defaults.data(forKey: key),
            let profile = try? JSONDecoder().decode(StudentProfile.self, from: data)
        else {
            return StudentProfile()
        }
        return profile
    }

    func save(_ profile: StudentProfile) {
        guard let data = try? JSONEncoder().encode(profile) else { return }
        defaults.set(data, forKey: key)
        defaults.synchronize()
    }

    func removeProfile() {
        defaults.removeObject(forKey: key)
        defaults.synchronize()
    }
}
