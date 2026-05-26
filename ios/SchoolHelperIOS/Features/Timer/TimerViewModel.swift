import Foundation
import Combine

@MainActor
final class TimerViewModel: ObservableObject {
    @Published var state: TimerSessionState

    private let store: TimerPreferencesStore
    private let notificationScheduler: TimerNotificationScheduling
    private let now: () -> Date
    private let sleep: @Sendable (UInt64) async -> Void
    private var countdownTask: Task<Void, Never>?

    init(
        store: TimerPreferencesStore = TimerPreferencesStore(),
        notificationScheduler: TimerNotificationScheduling = TimerNotificationScheduler(),
        now: @escaping () -> Date = Date.init,
        sleep: @escaping @Sendable (UInt64) async -> Void = { try? await Task.sleep(nanoseconds: $0) }
    ) {
        self.store = store
        self.notificationScheduler = notificationScheduler
        self.now = now
        self.sleep = sleep
        self.state = store.load()
        refreshRunningState()
    }

    func selectPreset(_ preset: TimerPreset) {
        countdownTask?.cancel()
        notificationScheduler.cancelPendingTimerCompletion()
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
        countdownTask?.cancel()
        notificationScheduler.cancelPendingTimerCompletion()
        state.remainingSeconds = state.totalSeconds
        state.targetDate = nil
        state.isRunning = false
        store.save(state)
    }

    func refreshRunningState() {
        guard state.isRunning else { return }
        syncWithCurrentTime()
        if state.isRunning {
            startCountdownLoop()
        }
    }

    func syncWithCurrentTime() {
        guard state.isRunning, let targetDate = state.targetDate else { return }
        let remaining = max(0, Int(targetDate.timeIntervalSince(now())))
        state.remainingSeconds = remaining

        if remaining == 0 {
            countdownTask?.cancel()
            notificationScheduler.cancelPendingTimerCompletion()
            state.targetDate = nil
            state.isRunning = false
            store.save(state)
        }
    }

    private func start() {
        countdownTask?.cancel()
        state.isRunning = true
        state.targetDate = now().addingTimeInterval(TimeInterval(state.remainingSeconds))
        notificationScheduler.requestAuthorizationIfNeeded()
        if let targetDate = state.targetDate {
            notificationScheduler.scheduleTimerCompletion(
                at: targetDate,
                presetTitle: state.preset.title
            )
        }
        store.save(state)
        startCountdownLoop()
    }

    private func pause() {
        countdownTask?.cancel()
        notificationScheduler.cancelPendingTimerCompletion()
        if let targetDate = state.targetDate {
            state.remainingSeconds = max(0, Int(targetDate.timeIntervalSince(now())))
        }
        state.targetDate = nil
        state.isRunning = false
        store.save(state)
    }

    private func startCountdownLoop() {
        countdownTask?.cancel()
        countdownTask = Task { [weak self] in
            guard let self else { return }
            while !Task.isCancelled {
                await self.sleep(1_000_000_000)
                if Task.isCancelled { break }
                await MainActor.run {
                    self.syncWithCurrentTime()
                }
                if await MainActor.run(body: { !self.state.isRunning }) {
                    break
                }
            }
        }
    }
}
