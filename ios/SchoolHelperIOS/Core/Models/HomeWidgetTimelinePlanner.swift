import Foundation

struct HomeWidgetTimelinePlanner {
    static func nextRefreshDate(
        now: Date,
        timerState: TimerSessionState,
        defaultInterval: TimeInterval = 30 * 60,
        runningInterval: TimeInterval = 60
    ) -> Date {
        guard
            timerState.isRunning,
            let targetDate = timerState.targetDate,
            targetDate > now
        else {
            return now.addingTimeInterval(defaultInterval)
        }

        let nextTick = now.addingTimeInterval(runningInterval)
        return min(nextTick, targetDate)
    }
}
