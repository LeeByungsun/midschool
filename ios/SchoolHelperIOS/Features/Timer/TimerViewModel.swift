import Foundation

@MainActor
final class TimerViewModel: ObservableObject {
    @Published var state: TimerSessionState

    private let store: TimerPreferencesStore

    init(store: TimerPreferencesStore = TimerPreferencesStore()) {
        self.store = store
        self.state = store.load()
    }

    func selectPreset(_ preset: TimerPreset) {
        state = TimerSessionState(
            preset: preset,
            totalSeconds: preset.durationSeconds,
            remainingSeconds: preset.durationSeconds,
            targetDate: nil,
            isRunning: false
        )
        store.save(state)
    }

    func toggle() {
        if state.isRunning {
            pause()
        } else {
            start()
        }
    }

    func reset() {
        state.remainingSeconds = state.totalSeconds
        state.targetDate = nil
        state.isRunning = false
        store.save(state)
    }

    private func start() {
        state.isRunning = true
        state.targetDate = Date().addingTimeInterval(TimeInterval(state.remainingSeconds))
        store.save(state)
    }

    private func pause() {
        if let targetDate = state.targetDate {
            state.remainingSeconds = max(0, Int(targetDate.timeIntervalSinceNow))
        }
        state.targetDate = nil
        state.isRunning = false
        store.save(state)
    }
}
