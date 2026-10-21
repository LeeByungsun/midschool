import Foundation

final class StudentPreferencesStore {
    private let defaults: UserDefaults
    private let legacyKey = "student_profile"
    private let profilesKey = "student_profiles_data"

    init(defaults: UserDefaults = AppStorageConfig.userDefaults()) {
        self.defaults = defaults
    }

    func load() -> StudentProfile {
        let data = loadData()
        return data.profiles.first(where: { $0.id == data.activeProfileID }) ?? StudentProfile()
    }

    func save(_ profile: StudentProfile) {
        var data = loadData()
        if let index = data.profiles.firstIndex(where: { $0.id == profile.id }) {
            data.profiles[index] = profile
        } else {
            data.profiles.append(profile)
        }
        data.activeProfileID = profile.id
        persist(data)
    }

    func loadProfiles() -> [StudentProfile] {
        loadData().profiles
    }

    func activeProfileID() -> String {
        loadData().activeProfileID
    }

    @discardableResult
    func add(_ profile: StudentProfile) -> Bool {
        var data = loadData()
        let normalizedName = profile.displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard profile.isComplete,
              !data.profiles.contains(where: {
                  $0.id == profile.id || $0.displayName.trimmingCharacters(in: .whitespacesAndNewlines) == normalizedName
              })
        else {
            return false
        }
        var normalizedProfile = profile
        normalizedProfile.displayName = normalizedName
        data.profiles.append(normalizedProfile)
        data.activeProfileID = normalizedProfile.id
        return persist(data)
    }

    @discardableResult
    func selectProfile(id: String) -> Bool {
        var data = loadData()
        guard data.profiles.contains(where: { $0.id == id }) else { return false }
        data.activeProfileID = id
        return persist(data)
    }

    @discardableResult
    func deleteProfile(id: String) -> Bool {
        var data = loadData()
        guard data.profiles.count > 1,
              data.profiles.contains(where: { $0.id == id })
        else {
            return false
        }
        data.profiles.removeAll(where: { $0.id == id })
        if data.activeProfileID == id {
            data.activeProfileID = data.profiles[0].id
        }
        return persist(data)
    }

    func removeProfile() {
        defaults.removeObject(forKey: profilesKey)
        defaults.removeObject(forKey: legacyKey)
        defaults.synchronize()
    }

    private func loadData() -> StudentProfilesData {
        if let encoded = defaults.data(forKey: profilesKey),
           let stored = try? JSONDecoder().decode(StudentProfilesData.self, from: encoded),
           stored.schemaVersion == StudentProfilesData.currentSchemaVersion {
            let normalized = normalize(stored)
            if normalized != stored {
                persist(normalized)
            }
            return normalized
        }

        guard let legacyData = defaults.data(forKey: legacyKey),
              let legacyProfile = try? JSONDecoder().decode(StudentProfile.self, from: legacyData)
        else {
            return StudentProfilesData()
        }

        let migrated = StudentProfilesData(
            activeProfileID: legacyProfile.id,
            profiles: [legacyProfile]
        )
        if persist(migrated) {
            defaults.removeObject(forKey: legacyKey)
            defaults.synchronize()
        }
        return migrated
    }

    private func normalize(_ data: StudentProfilesData) -> StudentProfilesData {
        var seenIDs = Set<String>()
        var seenNames = Set<String>()
        let profiles = data.profiles.filter { profile in
            let name = profile.displayName.trimmingCharacters(in: .whitespacesAndNewlines)
            return !profile.id.isEmpty && !name.isEmpty && seenIDs.insert(profile.id).inserted && seenNames.insert(name).inserted
        }
        let activeProfileID = profiles.contains(where: { $0.id == data.activeProfileID })
            ? data.activeProfileID
            : profiles.first?.id ?? ""
        return StudentProfilesData(activeProfileID: activeProfileID, profiles: profiles)
    }

    @discardableResult
    private func persist(_ profiles: StudentProfilesData) -> Bool {
        guard let encoded = try? JSONEncoder().encode(profiles) else { return false }
        defaults.set(encoded, forKey: profilesKey)
        defaults.synchronize()
        return true
    }
}
