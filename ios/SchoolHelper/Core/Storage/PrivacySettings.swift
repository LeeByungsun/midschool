import Foundation

enum AppLegalResources {
    static let privacyPolicyURL = URL(string: "https://leebyungsun.github.io/schoolon-privacy/")!
}

final class DiagnosticsConsentStore {
    private let defaults: UserDefaults
    private let key = "diagnostics_collection_enabled"

    init(defaults: UserDefaults = AppStorageConfig.userDefaults()) {
        self.defaults = defaults
    }

    func isEnabled() -> Bool {
        defaults.bool(forKey: key)
    }

    func save(isEnabled: Bool) {
        defaults.set(isEnabled, forKey: key)
        defaults.synchronize()
    }
}
