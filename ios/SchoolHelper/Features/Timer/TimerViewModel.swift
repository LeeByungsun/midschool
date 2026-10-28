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
    private var appliedTimerSettings: TimerSettings
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
        if store.hasSavedState {
            self.state = store.load()
        } else {
            let session = PomodoroSession()
            let initial = TimerSessionState(preset: .deepFocus, totalSeconds: session.phaseDurationSeconds,
                                            remainingSeconds: session.phaseDurationSeconds, pomodoro: session)
            self.state = initial
            store.save(initial)
        }
        let currentSettings = settingsStore.load()
        self.displayMode = currentSettings.displayMode
        self.appliedTimerSettings = currentSettings
        refreshRunningState()
    }

    deinit {
        countdownTask?.cancel()
    }

    func configurePomodoro(focusMinutes: Int, rounds: Int) {
        guard !state.isRunning else { return }
        setPomodoro(focusMinutes: focusMinutes, rounds: rounds)
    }

    private func setPomodoro(focusMinutes: Int, rounds: Int) {
        countdownTask?.cancel()
        notificationScheduler.cancelPendingTimerCompletion()
        let session = PomodoroSession(focusMinutes: focusMinutes, totalRounds: rounds)
        state = TimerSessionState(
            preset: session.focusMinutes == 25 ? .deepFocus : .focus,
            totalSeconds: session.phaseDurationSeconds,
            remainingSeconds: session.phaseDurationSeconds,
            targetDate: nil, isRunning: false, pomodoro: session
        )
        store.save(state)
        widgetTimelineReloader.reloadAllTimelines()
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
            // The app may have been suspended past one or more phase boundaries.
            // Resolve the current phase before deciding whether it can be paused.
            syncWithCurrentTime()
            if state.isRunning { pause() }
        } else {
            if let session = state.pomodoro, session.isCompleted {
                configurePomodoro(focusMinutes: session.focusMinutes, rounds: session.totalRounds)
            }
            start()
        }
    }

    func reset() {
        countdownTask?.cancel()
        notificationScheduler.cancelPendingTimerCompletion()
        if let session = state.pomodoro {
            setPomodoro(focusMinutes: session.focusMinutes, rounds: session.totalRounds)
            return
        }
        updateState {
            $0.remainingSeconds = $0.totalSeconds
            $0.targetDate = nil
            $0.isRunning = false
        }
        store.save(state)
        widgetTimelineReloader.reloadAllTimelines()
    }

    func reloadFromStore() {
        countdownTask?.cancel()
        state = store.load()
        refreshRunningState()
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
        let settings = settingsStore.load()
        let notificationChanged = settings.notificationEnabled != appliedTimerSettings.notificationEnabled ||
            settings.vibrationEnabled != appliedTimerSettings.vibrationEnabled
        appliedTimerSettings = settings
        displayMode = settings.displayMode
        guard notificationChanged, state.isRunning else { return }
        if !settings.notificationEnabled {
            notificationScheduler.cancelPendingTimerCompletion()
        } else {
            syncWithCurrentTime()
            if state.isRunning {
                notificationScheduler.requestAuthorizationIfNeeded()
                scheduleNotifications(settings: settings)
            }
        }
    }

    var progressFraction: Double {
        guard state.totalSeconds > 0 else { return 0 }
        return Double(state.remainingSeconds) / Double(state.totalSeconds)
    }

    func syncWithCurrentTime() {
        guard state.isRunning, let targetDate = state.targetDate else { return }
        let currentTime = now()
        if var session = state.pomodoro, targetDate <= currentTime {
            var deadline = targetDate
            repeat {
                session.completePhase()
                if !session.isCompleted {
                    deadline = deadline.addingTimeInterval(TimeInterval(session.phaseDurationSeconds))
                }
            } while !session.isCompleted && deadline <= currentTime
            updateState {
                $0.pomodoro = session
                $0.totalSeconds = session.phaseDurationSeconds
                $0.remainingSeconds = session.isCompleted ? session.phaseDurationSeconds : max(0, Int(deadline.timeIntervalSince(currentTime)))
                $0.preset = session.phase == .rest ? .shortBreak : (session.focusMinutes == 25 ? .deepFocus : .focus)
                $0.targetDate = session.isCompleted ? nil : deadline
                $0.isRunning = !session.isCompleted
            }
            if session.isCompleted {
                countdownTask?.cancel()
            }
            store.save(state)
            widgetTimelineReloader.reloadAllTimelines()
            return
        }
        let remaining = max(0, Int(ceil(targetDate.timeIntervalSince(currentTime))))
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
        if timerSettings.notificationEnabled {
            scheduleNotifications(settings: timerSettings)
        }
        store.save(state)
        widgetTimelineReloader.reloadAllTimelines()
        startCountdownLoop()
    }

    private func scheduleNotifications(settings: TimerSettings) {
        guard let targetDate = state.targetDate else { return }
        if let session = state.pomodoro {
            let events = session.completionSchedule(firstDeadline: targetDate).map {
                TimerNotificationEvent(date: $0.deadline, presetTitle: $0.phase == .focus ? "집중" : "쉬는 시간")
            }
            notificationScheduler.scheduleTimerCompletions(events, vibrationEnabled: settings.vibrationEnabled)
        } else {
            notificationScheduler.scheduleTimerCompletion(at: targetDate, presetTitle: state.preset.title,
                                                           vibrationEnabled: settings.vibrationEnabled)
        }
    }

    private func pause() {
        countdownTask?.cancel()
        notificationScheduler.cancelPendingTimerCompletion()
        updateState {
            if let targetDate = $0.targetDate {
                $0.remainingSeconds = max(0, Int(ceil(targetDate.timeIntervalSince(now()))))
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
