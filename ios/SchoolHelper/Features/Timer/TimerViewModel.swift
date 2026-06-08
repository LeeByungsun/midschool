import Foundation
import Combine

@MainActor
final class TimerViewModel: ObservableObject {
    @Published var state: TimerSessionState
    @Published var displayMode: TimerDisplayMode

    private let store: TimerPreferencesStore
    private let settingsStore: TimerSettingsStore
    private let notificationScheduler: TimerNotificationScheduling
    private let widgetTimelineReloader: WidgetTimelineReloading
    private let now: () -> Date
    private let sleep: @Sendable (UInt64) async -> Void
    private var countdownTask: Task<Void, Never>?

    init(
        store: TimerPreferencesStore = TimerPreferencesStore(),
        settingsStore: TimerSettingsStore = TimerSettingsStore(),
        notificationScheduler: TimerNotificationScheduling = TimerNotificationScheduler(),
        widgetTimelineReloader: WidgetTimelineReloading = WidgetTimelineReloader(),
        now: @escaping () -> Date = Date.init,
        sleep: @escaping @Sendable (UInt64) async -> Void = { try? await Task.sleep(nanoseconds: $0) }
    ) {
        self.store = store
        self.settingsStore = settingsStore
        self.notificationScheduler = notificationScheduler
        self.widgetTimelineReloader = widgetTimelineReloader
        self.now = now
        self.sleep = sleep
        self.state = store.load()
        self.displayMode = settingsStore.load().displayMode
        refreshRunningState()
    }

    deinit {
        countdownTask?.cancel()
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
        widgetTimelineReloader.reloadAllTimelines()
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
        updateState {
            $0.remainingSeconds = $0.totalSeconds
            $0.targetDate = nil
            $0.isRunning = false
        }
        store.save(state)
        widgetTimelineReloader.reloadAllTimelines()
    }

    func refreshRunningState() {
        refreshSettings()
        guard state.isRunning else { return }
        syncWithCurrentTime()
        if state.isRunning {
            startCountdownLoop()
        }
    }

    func refreshSettings() {
        displayMode = settingsStore.load().displayMode
    }

    var progressFraction: Double {
        guard state.totalSeconds > 0 else { return 0 }
        return Double(state.remainingSeconds) / Double(state.totalSeconds)
    }

    func syncWithCurrentTime() {
        guard state.isRunning, let targetDate = state.targetDate else { return }
        let remaining = max(0, Int(targetDate.timeIntervalSince(now())))
        updateState {
            $0.remainingSeconds = remaining
            if remaining == 0 {
                $0.targetDate = nil
                $0.isRunning = false
            }
        }

        if remaining == 0 {
            countdownTask?.cancel()
            notificationScheduler.cancelPendingTimerCompletion()
            store.save(state)
            widgetTimelineReloader.reloadAllTimelines()
        }
    }

    private func start() {
        countdownTask?.cancel()
        updateState {
            $0.isRunning = true
            $0.targetDate = now().addingTimeInterval(TimeInterval($0.remainingSeconds))
        }
        let timerSettings = settingsStore.load()
        if timerSettings.notificationEnabled {
            notificationScheduler.requestAuthorizationIfNeeded()
        }
        if let targetDate = state.targetDate, timerSettings.notificationEnabled {
            notificationScheduler.scheduleTimerCompletion(
                at: targetDate,
                presetTitle: state.preset.title,
                vibrationEnabled: timerSettings.vibrationEnabled
            )
        }
        store.save(state)
        widgetTimelineReloader.reloadAllTimelines()
        startCountdownLoop()
    }

    private func pause() {
        countdownTask?.cancel()
        notificationScheduler.cancelPendingTimerCompletion()
        updateState {
            if let targetDate = $0.targetDate {
                $0.remainingSeconds = max(0, Int(targetDate.timeIntervalSince(now())))
            }
            $0.targetDate = nil
            $0.isRunning = false
        }
        store.save(state)
        widgetTimelineReloader.reloadAllTimelines()
    }

    private func startCountdownLoop() {
        countdownTask?.cancel()
        countdownTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { break }
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

    private func updateState(_ mutate: (inout TimerSessionState) -> Void) {
        var next = state
        mutate(&next)
        state = next
    }
}
