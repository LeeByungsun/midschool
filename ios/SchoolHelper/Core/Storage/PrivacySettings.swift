import Foundation

enum AppLegalResources {
    static let privacyPolicyURL = URL(string: "https://leebyungsun.github.io/schoolon-privacy/")!
}

enum AppVersionLabel {
    static func current(bundle: Bundle = .main) -> String {
        let versionName = bundle.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "-"
        let buildNumber = bundle.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "-"
        return format(versionName: versionName, buildNumber: buildNumber)
    }

    static func format(versionName: String, buildNumber: String) -> String {
        "v\(versionName)(\(buildNumber))"
    }
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
