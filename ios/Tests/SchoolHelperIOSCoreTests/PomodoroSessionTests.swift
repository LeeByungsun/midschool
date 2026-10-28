import XCTest
@testable import SchoolHelperIOSCore

final class PomodoroSessionTests: XCTestCase {
    func testTwentyFiveMinuteTwoRoundSessionTransitionsAndStopsWithoutFinalBreak() {
        var session = PomodoroSession(focusMinutes: 25, totalRounds: 2)
        XCTAssertEqual(session.phase, .focus)
        XCTAssertEqual(session.phaseDurationSeconds, 25 * 60)
        session.completePhase()
        XCTAssertEqual(session.phase, .rest)
        XCTAssertEqual(session.completedRounds, 1)
        XCTAssertEqual(session.phaseDurationSeconds, 5 * 60)
        session.completePhase()
        XCTAssertEqual(session.phase, .focus)
        XCTAssertEqual(session.completedRounds, 1)
        session.completePhase()
        XCTAssertTrue(session.isCompleted)
        XCTAssertEqual(session.completedRounds, 2)
        XCTAssertEqual(session.phase, .focus)
        session.completePhase()
        XCTAssertEqual(session.completedRounds, 2)
    }

    func testFortyMinuteFourRoundSessionUsesTenMinuteBreak() {
        var session = PomodoroSession(focusMinutes: 40, totalRounds: 4)
        XCTAssertEqual(session.phaseDurationSeconds, 40 * 60)
        for round in 1...3 {
            session.completePhase()
            XCTAssertEqual(session.completedRounds, round)
            XCTAssertEqual(session.phase, .rest)
            XCTAssertEqual(session.phaseDurationSeconds, 10 * 60)
            session.completePhase()
            XCTAssertEqual(session.phase, .focus)
        }
        session.completePhase()
        XCTAssertTrue(session.isCompleted)
        XCTAssertEqual(session.completedRounds, 4)
    }
}
