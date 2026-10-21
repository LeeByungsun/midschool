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

struct OpenSourceLicense: Codable, Identifiable, Equatable {
    let name: String
    let version: String
    let licenseName: String
    let sourceURL: URL
    let licenseText: String

    var id: String { "\(name)-\(version)" }
}

enum OpenSourceLicenseCatalog {
    static func load(bundle: Bundle = .main) -> [OpenSourceLicense] {
        guard let url = bundle.url(forResource: "OpenSourceLicenses", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let licenses = try? decode(data: data) else {
            return []
        }
        return licenses
    }

    static func decode(data: Data) throws -> [OpenSourceLicense] {
        try JSONDecoder().decode([OpenSourceLicense].self, from: data)
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
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
