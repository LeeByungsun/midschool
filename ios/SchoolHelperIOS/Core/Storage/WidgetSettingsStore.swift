import Foundation

struct WidgetSettings: Codable, Equatable {
    var showTomorrowTimetable: Bool = true
}

final class WidgetSettingsStore {
    private let defaults: UserDefaults
    private let key = "widget_settings"

    init(defaults: UserDefaults = AppStorageConfig.userDefaults()) {
        self.defaults = defaults
    }

    func load() -> WidgetSettings {
        guard
            let data = defaults.data(forKey: key),
            let settings = try? JSONDecoder().decode(WidgetSettings.self, from: data)
        else {
            return WidgetSettings()
        }
        return settings
    }

    func save(_ settings: WidgetSettings) {
        guard let data = try? JSONEncoder().encode(settings) else { return }
        defaults.set(data, forKey: key)
        defaults.synchronize()
    }
}
