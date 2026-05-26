import Foundation

enum AppStorageConfig {
    static let appGroupSuiteName = "group.com.leebyungsun.schoolhelperios"

    static func userDefaults() -> UserDefaults {
        UserDefaults(suiteName: appGroupSuiteName) ?? .standard
    }
}
