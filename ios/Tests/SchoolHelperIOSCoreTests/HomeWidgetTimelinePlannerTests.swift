import XCTest
@testable import SchoolHelperIOSCore

final class HomeWidgetTimelinePlannerTests: XCTestCase {
    func testPlannerUsesDefaultIntervalWhenTimerIsIdle() {
        let now = fixtureDate(year: 2026, month: 5, day: 26)
        let refreshDate = HomeWidgetTimelinePlanner.nextRefreshDate(
            now: now,
            timerState: TimerSessionState(
                preset: .focus,
                totalSeconds: 2400,
                remainingSeconds: 2400,
                targetDate: nil,
                isRunning: false
            )
        )

        XCTAssertEqual(refreshDate, now.addingTimeInterval(1800))
    }

    func testPlannerRefreshesEveryMinuteWhileTimerRuns() {
        let now = fixtureDate(year: 2026, month: 5, day: 26)
        let refreshDate = HomeWidgetTimelinePlanner.nextRefreshDate(
            now: now,
            timerState: TimerSessionState(
                preset: .focus,
                totalSeconds: 2400,
                remainingSeconds: 300,
                targetDate: now.addingTimeInterval(300),
                isRunning: true
            )
        )

        XCTAssertEqual(refreshDate, now.addingTimeInterval(60))
    }

    func testPlannerCapsRefreshAtTimerTargetDate() {
        let now = fixtureDate(year: 2026, month: 5, day: 26)
        let refreshDate = HomeWidgetTimelinePlanner.nextRefreshDate(
            now: now,
            timerState: TimerSessionState(
                preset: .shortBreak,
                totalSeconds: 600,
                remainingSeconds: 20,
                targetDate: now.addingTimeInterval(20),
                isRunning: true
            )
        )

        XCTAssertEqual(refreshDate, now.addingTimeInterval(20))
    }
}
