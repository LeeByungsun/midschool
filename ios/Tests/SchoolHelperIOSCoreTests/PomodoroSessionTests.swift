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
    func testLegacyRunningTimerDecodesWithoutNewSessionAndKeepsCountdown() throws {
        let data = Data("""
        {"preset":"focus","totalSeconds":2400,"remainingSeconds":900,"targetDate":12345,"isRunning":true}
        """.utf8)
        let state = try JSONDecoder().decode(TimerSessionState.self, from: data)
        XCTAssertEqual(state.preset, .focus)
        XCTAssertEqual(state.remainingSeconds, 900)
        XCTAssertEqual(state.targetDate, Date(timeIntervalSinceReferenceDate: 12345))
        XCTAssertTrue(state.isRunning)
        XCTAssertNil(state.pomodoro)
    }

    func testNewSessionStateRoundTripsIncludingCompletedRoundAndPhase() throws {
        var session = PomodoroSession(focusMinutes: 25, totalRounds: 2)
        session.completePhase()
        var state = TimerSessionState()
        state.pomodoro = session
        state.totalSeconds = 300
        state.remainingSeconds = 80
        state.isRunning = true
        state.targetDate = Date(timeIntervalSinceReferenceDate: 12345)
        let decoded = try JSONDecoder().decode(TimerSessionState.self, from: JSONEncoder().encode(state))
        XCTAssertEqual(decoded, state)
        XCTAssertEqual(decoded.pomodoro?.phase, .rest)
        XCTAssertEqual(decoded.pomodoro?.completedRounds, 1)
        let suite = "PomodoroSessionTests." + UUID().uuidString
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let store = TimerPreferencesStore(defaults: defaults)
        store.save(state)
        XCTAssertEqual(store.load(), state)
    }
}
