import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class TimerViewModelTests: XCTestCase {
    func testTimerViewModelTracksRunningStateAgainstCurrentTime() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let store = TimerPreferencesStore(defaults: defaults)
        let settingsStore = TimerSettingsStore(defaults: defaults)
        let scheduler = SpyTimerNotificationScheduler()
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
            now: { currentTime },
            sleep: { _ in }
        )

        XCTAssertEqual(viewModel.displayMode, .ring)
        viewModel.selectPreset(.shortBreak)
        viewModel.toggle()
        XCTAssertTrue(viewModel.state.isRunning)
        XCTAssertEqual(viewModel.state.remainingSeconds, TimerPreset.shortBreak.durationSeconds)
        XCTAssertEqual(scheduler.requestAuthorizationCalls, 1)
        XCTAssertEqual(scheduler.scheduleCalls.count, 1)
        XCTAssertEqual(scheduler.scheduleCalls.first?.presetTitle, TimerPreset.shortBreak.title)
        XCTAssertEqual(scheduler.scheduleCalls.first?.vibrationEnabled, false)

        currentTime = currentTime.addingTimeInterval(120)
        viewModel.syncWithCurrentTime()
        XCTAssertEqual(viewModel.state.remainingSeconds, TimerPreset.shortBreak.durationSeconds - 120)

        viewModel.toggle()
        XCTAssertFalse(viewModel.state.isRunning)
        XCTAssertEqual(scheduler.cancelCalls, 2)

        viewModel.reset()
        XCTAssertEqual(viewModel.state.remainingSeconds, TimerPreset.shortBreak.durationSeconds)
        XCTAssertNil(viewModel.state.targetDate)
        XCTAssertEqual(scheduler.cancelCalls, 3)
    }
}

private final class SpyTimerNotificationScheduler: TimerNotificationScheduling {
    private(set) var requestAuthorizationCalls = 0
    private(set) var cancelCalls = 0
    private(set) var scheduleCalls: [(date: Date, presetTitle: String, vibrationEnabled: Bool)] = []

    func requestAuthorizationIfNeeded() {
        requestAuthorizationCalls += 1
    }

    func scheduleTimerCompletion(at date: Date, presetTitle: String, vibrationEnabled: Bool) {
        scheduleCalls.append((date, presetTitle, vibrationEnabled))
    }

    func cancelPendingTimerCompletion() {
        cancelCalls += 1
    }
}
