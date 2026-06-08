import Foundation

enum AppStorageConfig {
    static let appGroupSuiteName = "group.com.lbs.shcoolhelper"

    static func userDefaults() -> UserDefaults {
        UserDefaults(suiteName: appGroupSuiteName) ?? .standard
    }
}
