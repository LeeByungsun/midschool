import Foundation

struct TimerSessionState: Codable, Equatable {
    var preset: TimerPreset = .focus
    var totalSeconds: Int = TimerPreset.focus.durationSeconds
    var remainingSeconds: Int = TimerPreset.focus.durationSeconds
    var targetDate: Date?
    var isRunning: Bool = false
    // Nil represents a legacy one-shot timer; keep its countdown until it finishes.
    var pomodoro: PomodoroSession? = nil
}

/// Session rules mirror Android: focus and rest alternate automatically until the final focus round.
struct PomodoroSession: Codable, Equatable {
    enum Phase: String, Codable {
        case focus
        case rest
    }

    let focusMinutes: Int
    let totalRounds: Int
    private(set) var phase: Phase = .focus
    private(set) var completedRounds = 0
    private(set) var isCompleted = false

    init(focusMinutes: Int = 25, totalRounds: Int = 4) {
        self.focusMinutes = focusMinutes >= 40 ? 40 : 25
        self.totalRounds = totalRounds >= 4 ? 4 : 2
    }

    var phaseDurationSeconds: Int {
        (phase == .focus ? focusMinutes : (focusMinutes == 40 ? 10 : 5)) * 60
    }

    mutating func completePhase() {
        guard !isCompleted else { return }
        switch phase {
        case .focus:
            completedRounds += 1
            if completedRounds == totalRounds {
                isCompleted = true
            } else {
                phase = .rest
            }
        case .rest:
            phase = .focus
        }
    }
}

struct PomodoroPhaseCompletion: Equatable {
    let phase: PomodoroSession.Phase
    let deadline: Date
    let completedRound: Int
}

extension PomodoroSession {
    /// Compute remaining phase boundaries so iOS can schedule alerts before suspension.
    func completionSchedule(firstDeadline: Date) -> [PomodoroPhaseCompletion] {
        var session = self
        var deadline = firstDeadline
        var result: [PomodoroPhaseCompletion] = []
        // Four focus rounds plus three rests is the maximum valid session.
        for _ in 0..<7 {
            guard !session.isCompleted else { break }
            let round = session.phase == .focus ? session.completedRounds + 1 : session.completedRounds
            result.append(PomodoroPhaseCompletion(phase: session.phase, deadline: deadline, completedRound: round))
            session.completePhase()
            if !session.isCompleted {
                deadline = deadline.addingTimeInterval(TimeInterval(session.phaseDurationSeconds))
            }
        }
        return result
    }
}
