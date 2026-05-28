import Foundation

enum AppLaunchOverrides {
    static func seededProfile(from environment: [String: String] = ProcessInfo.processInfo.environment) -> StudentProfile? {
        if let raw = environment["SCHOOLHELPER_SEED_PROFILE_JSON"], !raw.isEmpty {
            guard let data = raw.data(using: .utf8) else { return nil }
            return try? JSONDecoder().decode(StudentProfile.self, from: data)
        }

        if environment["SCHOOLHELPER_SEED_PROFILE"] == "fixture" {
            return StudentProfile(
                grade: "1",
                classroom: "2",
                schoolName: "미사중학교",
                officeCode: "J10",
                schoolCode: "7692129",
                schoolKind: "중학교"
            )
        }

        return nil
    }

    static func shouldSkipNotificationRequest(_ environment: [String: String] = ProcessInfo.processInfo.environment) -> Bool {
        let value = environment["SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST"]?.lowercased()
        return value == "1" || value == "true" || value == "yes"
    }

    static func shouldResetProfile(_ environment: [String: String] = ProcessInfo.processInfo.environment) -> Bool {
        truthy(environment["SCHOOLHELPER_RESET_PROFILE"])
    }

    static func initialRoute(_ environment: [String: String] = ProcessInfo.processInfo.environment) -> AppRoute? {
        guard let rawValue = environment["SCHOOLHELPER_INITIAL_ROUTE"]?.lowercased() else {
            return nil
        }
        return AppRoute(rawValue: rawValue)
    }

    static func referenceDate(_ environment: [String: String] = ProcessInfo.processInfo.environment) -> Date? {
        guard let rawValue = environment["SCHOOLHELPER_REFERENCE_DATE"]?.trimmingCharacters(in: .whitespacesAndNewlines),
              !rawValue.isEmpty
        else {
            return nil
        }

        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyyMMdd"
        formatter.isLenient = false
        return formatter.date(from: rawValue)
    }

    static func shouldScheduleTimerNotification(
        from environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> Bool {
        truthy(environment["SCHOOLHELPER_SCHEDULE_TIMER_NOTIFICATION"])
    }

    static func timerState(
        from environment: [String: String] = ProcessInfo.processInfo.environment,
        now: Date = Date()
    ) -> TimerSessionState? {
        guard
            let rawPreset = environment["SCHOOLHELPER_TIMER_PRESET"],
            let preset = TimerPreset(rawValue: rawPreset)
        else {
            return nil
        }

        let totalSeconds = Int(environment["SCHOOLHELPER_TIMER_TOTAL_SECONDS"] ?? "") ?? preset.durationSeconds
        let remainingSeconds = Int(environment["SCHOOLHELPER_TIMER_REMAINING_SECONDS"] ?? "") ?? totalSeconds
        let isRunning = truthy(environment["SCHOOLHELPER_TIMER_RUNNING"])

        return TimerSessionState(
            preset: preset,
            totalSeconds: totalSeconds,
            remainingSeconds: remainingSeconds,
            targetDate: isRunning ? now.addingTimeInterval(TimeInterval(remainingSeconds)) : nil,
            isRunning: isRunning
        )
    }

    private static func truthy(_ rawValue: String?) -> Bool {
        guard let rawValue else { return false }
        switch rawValue.lowercased() {
        case "1", "true", "yes":
            return true
        default:
            return false
        }
    }
}
