import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class TimerViewModelTests: XCTestCase {
    func testRefreshSettingsReloadsSavedDisplayMode() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let settingsStore = TimerSettingsStore(defaults: defaults)
        let viewModel = TimerViewModel(
            store: TimerPreferencesStore(defaults: defaults),
            settingsStore: settingsStore,
            notificationScheduler: SpyTimerNotificationScheduler(),
            widgetTimelineReloader: SpyWidgetTimelineReloader(),
            sleep: { _ in }
        )
        XCTAssertEqual(viewModel.displayMode, .count)

        settingsStore.save(TimerSettings(displayMode: .ring))
        viewModel.refreshSettings()

        XCTAssertEqual(viewModel.displayMode, .ring)
    }

    func testTimerViewModelTracksRunningStateAgainstCurrentTime() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let store = TimerPreferencesStore(defaults: defaults)
        let settingsStore = TimerSettingsStore(defaults: defaults)
        let scheduler = SpyTimerNotificationScheduler()
        let reloader = SpyWidgetTimelineReloader()
        settingsStore.save(
            TimerSettings(
                displayMode: .ring,
                notificationEnabled: true,
                vibrationEnabled: false
            )
        )

        var currentTime = Date(timeIntervalSince1970: 1_700_000_000)
        let viewModel = TimerViewModel(
            store: store,
            settingsStore: settingsStore,
            notificationScheduler: scheduler,
            widgetTimelineReloader: reloader,
            now: { currentTime },
            sleep: { _ in }
        )

        XCTAssertEqual(viewModel.displayMode, .ring)
        viewModel.selectPreset(.shortBreak)
        XCTAssertEqual(reloader.reloadCount, 1)
        viewModel.toggle()
        XCTAssertTrue(viewModel.state.isRunning)
        XCTAssertEqual(viewModel.state.remainingSeconds, TimerPreset.shortBreak.durationSeconds)
        XCTAssertEqual(scheduler.requestAuthorizationCalls, 1)
        XCTAssertEqual(scheduler.scheduleCalls.count, 1)
        XCTAssertEqual(scheduler.scheduleCalls.first?.presetTitle, TimerPreset.shortBreak.title)
        XCTAssertEqual(scheduler.scheduleCalls.first?.vibrationEnabled, false)
        XCTAssertEqual(reloader.reloadCount, 2)

        currentTime = currentTime.addingTimeInterval(120)
        viewModel.syncWithCurrentTime()
        XCTAssertEqual(viewModel.state.remainingSeconds, TimerPreset.shortBreak.durationSeconds - 120)

        viewModel.toggle()
        XCTAssertFalse(viewModel.state.isRunning)
        XCTAssertEqual(scheduler.cancelCalls, 2)
        XCTAssertEqual(reloader.reloadCount, 3)

        viewModel.reset()
        XCTAssertEqual(viewModel.state.remainingSeconds, TimerPreset.shortBreak.durationSeconds)
        XCTAssertNil(viewModel.state.targetDate)
        XCTAssertEqual(scheduler.cancelCalls, 3)
        XCTAssertEqual(reloader.reloadCount, 4)
    }

    func testTimerViewModelDoesNotScheduleNotificationWhenNotificationsAreDisabled() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let store = TimerPreferencesStore(defaults: defaults)
        let settingsStore = TimerSettingsStore(defaults: defaults)
        let scheduler = SpyTimerNotificationScheduler()
        let reloader = SpyWidgetTimelineReloader()
        settingsStore.save(
            TimerSettings(
                displayMode: .count,
                notificationEnabled: false,
                vibrationEnabled: true
            )
        )

        let viewModel = TimerViewModel(
            store: store,
            settingsStore: settingsStore,
            notificationScheduler: scheduler,
            widgetTimelineReloader: reloader,
            now: { Date(timeIntervalSince1970: 1_700_000_000) },
            sleep: { _ in }
        )

        viewModel.selectPreset(.focus)
        viewModel.toggle()

        XCTAssertTrue(viewModel.state.isRunning)
        XCTAssertEqual(scheduler.requestAuthorizationCalls, 0)
        XCTAssertTrue(scheduler.scheduleCalls.isEmpty)
    }
    func testPomodoroAutomaticallyTransitionsAndCompletesAfterTwoRounds() {
        let suite = "PomodoroTimer." + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        var now = Date(timeIntervalSince1970: 1_700_000_000)
        let store = TimerPreferencesStore(defaults: defaults)
        let viewModel = TimerViewModel(store: store, settingsStore: TimerSettingsStore(defaults: defaults),
            notificationScheduler: SpyTimerNotificationScheduler(), widgetTimelineReloader: SpyWidgetTimelineReloader(),
            now: { now }, sleep: { _ in })
        viewModel.configurePomodoro(focusMinutes: 25, rounds: 2)
        viewModel.toggle()
        now.addTimeInterval(25 * 60)
        viewModel.syncWithCurrentTime()
        XCTAssertTrue(viewModel.state.isRunning)
        XCTAssertEqual(viewModel.state.pomodoro?.phase, .rest)
        XCTAssertEqual(viewModel.state.pomodoro?.completedRounds, 1)
        XCTAssertEqual(viewModel.state.remainingSeconds, 5 * 60)
        now.addTimeInterval(5 * 60)
        viewModel.syncWithCurrentTime()
        XCTAssertEqual(viewModel.state.pomodoro?.phase, .focus)
        XCTAssertTrue(viewModel.state.isRunning)
        now.addTimeInterval(25 * 60)
        viewModel.syncWithCurrentTime()
        XCTAssertFalse(viewModel.state.isRunning)
        XCTAssertEqual(viewModel.state.pomodoro?.completedRounds, 2)
        XCTAssertEqual(viewModel.state.pomodoro?.isCompleted, true)
        XCTAssertEqual(store.load(), viewModel.state)
    }

    func testPomodoroRestoresAcrossMultipleElapsedPhases() {
        let suite = "PomodoroRestore." + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        var now = Date(timeIntervalSince1970: 1_700_000_000)
        let store = TimerPreferencesStore(defaults: defaults)
        let first = TimerViewModel(store: store, settingsStore: TimerSettingsStore(defaults: defaults),
            notificationScheduler: SpyTimerNotificationScheduler(), widgetTimelineReloader: SpyWidgetTimelineReloader(),
            now: { now }, sleep: { _ in })
        first.configurePomodoro(focusMinutes: 40, rounds: 4)
        first.toggle()
        now.addTimeInterval(40 * 60 + 10 * 60 + 3 * 60)
        let restored = TimerViewModel(store: store, settingsStore: TimerSettingsStore(defaults: defaults),
            notificationScheduler: SpyTimerNotificationScheduler(), widgetTimelineReloader: SpyWidgetTimelineReloader(),
            now: { now }, sleep: { _ in })
        XCTAssertEqual(restored.state.pomodoro?.phase, .focus)
        XCTAssertEqual(restored.state.pomodoro?.completedRounds, 1)
        XCTAssertEqual(restored.state.remainingSeconds, 37 * 60)
        XCTAssertTrue(restored.state.isRunning)
    }
    func testPomodoroPauseResumeAndResetPreserveSelection() {
        let suite = "PomodoroPause." + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        var now = Date(timeIntervalSince1970: 1_700_000_000)
        let viewModel = TimerViewModel(store: TimerPreferencesStore(defaults: defaults),
            settingsStore: TimerSettingsStore(defaults: defaults), notificationScheduler: SpyTimerNotificationScheduler(),
            widgetTimelineReloader: SpyWidgetTimelineReloader(), now: { now }, sleep: { _ in })
        viewModel.configurePomodoro(focusMinutes: 25, rounds: 2)
        viewModel.toggle()
        now.addTimeInterval(60)
        viewModel.toggle()
        XCTAssertFalse(viewModel.state.isRunning)
        XCTAssertEqual(viewModel.state.remainingSeconds, 24 * 60)
        viewModel.toggle()
        XCTAssertTrue(viewModel.state.isRunning)
        viewModel.configurePomodoro(focusMinutes: 40, rounds: 4)
        XCTAssertEqual(viewModel.state.pomodoro?.focusMinutes, 25)
        viewModel.reset()
        XCTAssertFalse(viewModel.state.isRunning)
        XCTAssertEqual(viewModel.state.remainingSeconds, 25 * 60)
        XCTAssertEqual(viewModel.state.pomodoro?.totalRounds, 2)
        XCTAssertEqual(viewModel.state.pomodoro?.completedRounds, 0)
    }
    func testNewTimerDefaultsToTwentyFiveMinutesAndFourRounds() {
        let suite = "PomodoroDefault." + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let vm = TimerViewModel(store: TimerPreferencesStore(defaults: defaults), settingsStore: TimerSettingsStore(defaults: defaults),
            notificationScheduler: SpyTimerNotificationScheduler(), widgetTimelineReloader: SpyWidgetTimelineReloader())
        XCTAssertEqual(vm.state.pomodoro?.focusMinutes, 25)
        XCTAssertEqual(vm.state.pomodoro?.totalRounds, 4)
        XCTAssertEqual(vm.state.remainingSeconds, 1500)
    }

    func testSavedLegacyPausedTimerDoesNotGetResetOnInitialization() {
        let suite = "LegacyTimer." + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let store = TimerPreferencesStore(defaults: defaults)
        let legacy = TimerSessionState(preset: .focus, totalSeconds: 2400, remainingSeconds: 850, isRunning: false)
        store.save(legacy)
        let vm = TimerViewModel(store: store, settingsStore: TimerSettingsStore(defaults: defaults),
            notificationScheduler: SpyTimerNotificationScheduler(), widgetTimelineReloader: SpyWidgetTimelineReloader())
        XCTAssertEqual(vm.state, legacy)
    }
    func testPomodoroStartPreplansEveryRemainingPhaseAndPauseCancels() {
        let suite = "PomodoroPlan." + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        var now = Date(timeIntervalSince1970: 1_700_000_000)
        let scheduler = SpyTimerNotificationScheduler()
        let vm = TimerViewModel(store: TimerPreferencesStore(defaults: defaults), settingsStore: TimerSettingsStore(defaults: defaults),
            notificationScheduler: scheduler, widgetTimelineReloader: SpyWidgetTimelineReloader(), now: { now }, sleep: { _ in })
        vm.configurePomodoro(focusMinutes: 25, rounds: 2)
        vm.toggle()
        XCTAssertEqual(scheduler.batchCalls.last?.events.count, 3)
        XCTAssertEqual(scheduler.batchCalls.last?.events.map(\.date), [now.addingTimeInterval(1500), now.addingTimeInterval(1800), now.addingTimeInterval(3300)])
        let cancelCount = scheduler.cancelCalls
        now.addTimeInterval(60)
        vm.toggle()
        XCTAssertGreaterThan(scheduler.cancelCalls, cancelCount)
        now.addTimeInterval(20)
        vm.toggle()
        XCTAssertEqual(scheduler.batchCalls.last?.events.first?.date, now.addingTimeInterval(1440))
        XCTAssertEqual(scheduler.batchCalls.last?.events.count, 3)
    }

    func testPomodoroDoesNotEndEarlyInLastFractionalSecond() {
        let suite = "PomodoroFraction." + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        var now = Date(timeIntervalSince1970: 1_700_000_000)
        let vm = TimerViewModel(store: TimerPreferencesStore(defaults: defaults), settingsStore: TimerSettingsStore(defaults: defaults),
            notificationScheduler: SpyTimerNotificationScheduler(), widgetTimelineReloader: SpyWidgetTimelineReloader(), now: { now }, sleep: { _ in })
        vm.configurePomodoro(focusMinutes: 25, rounds: 2)
        vm.toggle()
        now.addTimeInterval(1499.5)
        vm.syncWithCurrentTime()
        XCTAssertTrue(vm.state.isRunning)
        XCTAssertEqual(vm.state.remainingSeconds, 1)
        now.addTimeInterval(0.5)
        vm.syncWithCurrentTime()
        XCTAssertEqual(vm.state.pomodoro?.phase, .rest)
        XCTAssertTrue(vm.state.isRunning)
    }
    func testEnablingNotificationsDuringRunningSessionReschedulesAllRemainingPhases() {
        let suite = "PomodoroSettings." + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let settings = TimerSettingsStore(defaults: defaults)
        settings.save(TimerSettings(notificationEnabled: false))
        let scheduler = SpyTimerNotificationScheduler()
        let vm = TimerViewModel(store: TimerPreferencesStore(defaults: defaults), settingsStore: settings,
            notificationScheduler: scheduler, widgetTimelineReloader: SpyWidgetTimelineReloader(),
            now: { Date(timeIntervalSince1970: 1_700_000_000) }, sleep: { _ in })
        vm.configurePomodoro(focusMinutes: 25, rounds: 2)
        vm.toggle()
        XCTAssertTrue(scheduler.batchCalls.isEmpty)
        settings.save(TimerSettings(notificationEnabled: true, vibrationEnabled: false))
        vm.refreshSettings()
        XCTAssertEqual(scheduler.batchCalls.last?.events.count, 3)
        XCTAssertEqual(scheduler.batchCalls.last?.vibrationEnabled, false)
        settings.save(TimerSettings(notificationEnabled: false))
        let previousCancelCount = scheduler.cancelCalls
        vm.refreshSettings()
        XCTAssertGreaterThan(scheduler.cancelCalls, previousCancelCount)
    }
    func testPauseAfterBackgroundPhaseChangePausesCurrentRest() {
        let suite = "PomodoroPauseAfterBackground." + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        var now = Date(timeIntervalSince1970: 1_700_000_000)
        let vm = TimerViewModel(store: TimerPreferencesStore(defaults: defaults), settingsStore: TimerSettingsStore(defaults: defaults),
            notificationScheduler: SpyTimerNotificationScheduler(), widgetTimelineReloader: SpyWidgetTimelineReloader(), now: { now }, sleep: { _ in })
        vm.configurePomodoro(focusMinutes: 25, rounds: 2)
        vm.toggle()
        now.addTimeInterval(26 * 60)
        vm.toggle()
        XCTAssertFalse(vm.state.isRunning)
        XCTAssertEqual(vm.state.pomodoro?.phase, .rest)
        XCTAssertEqual(vm.state.pomodoro?.completedRounds, 1)
        XCTAssertEqual(vm.state.remainingSeconds, 4 * 60)
    }

    func testPauseAfterCompletedSessionDoesNotStartNewSession() {
        let suite = "PomodoroPauseAfterCompletion." + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        var now = Date(timeIntervalSince1970: 1_700_000_000)
        let vm = TimerViewModel(store: TimerPreferencesStore(defaults: defaults), settingsStore: TimerSettingsStore(defaults: defaults),
            notificationScheduler: SpyTimerNotificationScheduler(), widgetTimelineReloader: SpyWidgetTimelineReloader(), now: { now }, sleep: { _ in })
        vm.configurePomodoro(focusMinutes: 25, rounds: 2)
        vm.toggle()
        now.addTimeInterval(55 * 60)
        vm.toggle()
        XCTAssertFalse(vm.state.isRunning)
        XCTAssertEqual(vm.state.pomodoro?.isCompleted, true)
    }
    func testStartingCompletedLegacyTimerRestoresDuration() {
        let suite = "LegacyCompleted." + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let store = TimerPreferencesStore(defaults: defaults)
        store.save(TimerSessionState(preset: .shortBreak, totalSeconds: 30, remainingSeconds: 0, isRunning: false))
        let now = Date(timeIntervalSince1970: 1_700_000_000)
        let vm = TimerViewModel(store: store, settingsStore: TimerSettingsStore(defaults: defaults),
            notificationScheduler: SpyTimerNotificationScheduler(), widgetTimelineReloader: SpyWidgetTimelineReloader(),
            now: { now }, sleep: { _ in })
        vm.toggle()
        XCTAssertTrue(vm.state.isRunning)
        XCTAssertEqual(vm.state.remainingSeconds, 30)
        XCTAssertEqual(vm.state.targetDate, now.addingTimeInterval(30))
    }
}
