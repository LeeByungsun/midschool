import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class TimerViewModelTests: XCTestCase {
    func testTimerViewModelTracksRunningStateAgainstCurrentTime() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let store = TimerPreferencesStore(defaults: defaults)

        var currentTime = Date(timeIntervalSince1970: 1_700_000_000)
        let viewModel = TimerViewModel(
            store: store,
            now: { currentTime },
            sleep: { _ in }
        )

        viewModel.selectPreset(.shortBreak)
        viewModel.toggle()
        XCTAssertTrue(viewModel.state.isRunning)
        XCTAssertEqual(viewModel.state.remainingSeconds, TimerPreset.shortBreak.durationSeconds)

        currentTime = currentTime.addingTimeInterval(120)
        viewModel.syncWithCurrentTime()
        XCTAssertEqual(viewModel.state.remainingSeconds, TimerPreset.shortBreak.durationSeconds - 120)

        viewModel.toggle()
        XCTAssertFalse(viewModel.state.isRunning)

        viewModel.reset()
        XCTAssertEqual(viewModel.state.remainingSeconds, TimerPreset.shortBreak.durationSeconds)
        XCTAssertNil(viewModel.state.targetDate)
    }
}
