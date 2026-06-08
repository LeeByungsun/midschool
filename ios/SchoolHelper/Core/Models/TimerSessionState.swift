import Foundation

struct TimerSessionState: Codable, Equatable {
    var preset: TimerPreset = .focus
    var totalSeconds: Int = TimerPreset.focus.durationSeconds
    var remainingSeconds: Int = TimerPreset.focus.durationSeconds
    var targetDate: Date?
    var isRunning: Bool = false
}
