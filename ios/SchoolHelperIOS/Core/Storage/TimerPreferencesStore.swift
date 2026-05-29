import Foundation

final class TimerPreferencesStore {
    private let defaults: UserDefaults
    private let key = "timer_session_state"

    init(defaults: UserDefaults = AppStorageConfig.userDefaults()) {
        self.defaults = defaults
    }

    func load() -> TimerSessionState {
        guard
            let data = defaults.data(forKey: key),
            let state = try? JSONDecoder().decode(TimerSessionState.self, from: data)
        else {
            return TimerSessionState()
        }
        return state
    }

    func save(_ state: TimerSessionState) {
        guard let data = try? JSONEncoder().encode(state) else { return }
        defaults.set(data, forKey: key)
        defaults.synchronize()
    }

    func clear() {
        defaults.removeObject(forKey: key)
        defaults.synchronize()
    }
}
