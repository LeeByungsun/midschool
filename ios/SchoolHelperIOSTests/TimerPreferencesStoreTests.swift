import XCTest
@testable import SchoolHelperIOS

final class TimerPreferencesStoreTests: XCTestCase {
    func testStoreRoundTripsTimerState() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let store = TimerPreferencesStore(defaults: defaults)
        let state = TimerSessionState(
            preset: .deepFocus,
            totalSeconds: 1500,
            remainingSeconds: 1200,
            targetDate: Date(timeIntervalSince1970: 1_700_000_000),
            isRunning: true
        )

        store.save(state)

        XCTAssertEqual(state, store.load())
    }
}
