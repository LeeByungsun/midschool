import Foundation

enum TimerDisplayMode: String, Codable, CaseIterable {
    case count
    case ring

    var title: String {
        switch self {
        case .count: return "숫자"
        case .ring: return "링"
        }
    }
}

struct TimerSettings: Codable, Equatable {
    var displayMode: TimerDisplayMode = .count
    var notificationEnabled: Bool = true
    var vibrationEnabled: Bool = true
}

final class TimerSettingsStore {
    private let defaults: UserDefaults
    private let key = "timer_settings"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load() -> TimerSettings {
        guard
            let data = defaults.data(forKey: key),
            let settings = try? JSONDecoder().decode(TimerSettings.self, from: data)
        else {
            return TimerSettings()
        }
        return settings
    }

    func save(_ settings: TimerSettings) {
        guard let data = try? JSONEncoder().encode(settings) else { return }
        defaults.set(data, forKey: key)
    }
}
