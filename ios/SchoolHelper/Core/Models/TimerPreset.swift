import Foundation

enum TimerPreset: String, Codable, CaseIterable {
    case focus
    case shortBreak
    case deepFocus

    var durationSeconds: Int {
        switch self {
        case .focus: return 40 * 60
        case .shortBreak: return 10 * 60
        case .deepFocus: return 25 * 60
        }
    }

    var title: String {
        switch self {
        case .focus: return "집중"
        case .shortBreak: return "휴식"
        case .deepFocus: return "딥포커스"
        }
    }
}
