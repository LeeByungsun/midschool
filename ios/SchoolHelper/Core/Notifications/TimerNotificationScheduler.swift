import Foundation
import UserNotifications
import Dispatch

protocol TimerNotificationScheduling {
    func requestAuthorizationIfNeeded()
    func scheduleTimerCompletion(at date: Date, presetTitle: String, vibrationEnabled: Bool)
    func cancelPendingTimerCompletion()
}

enum TimerNotificationPresentationPolicy {
    static let timerCompletionIdentifier = "schoolhelper.timer.complete"
    static let vibrationEnabledKey = "timerVibrationEnabled"

    static func shouldPlaySound(identifier: String) -> Bool {
        identifier == timerCompletionIdentifier
    }

    static func shouldPlayHaptic(identifier: String, userInfo: [AnyHashable: Any]) -> Bool {
        identifier == timerCompletionIdentifier && userInfo[vibrationEnabledKey] as? Bool == true
    }
}

final class TimerNotificationScheduler: TimerNotificationScheduling {
    private let center: UNUserNotificationCenter
    private let environment: [String: String]

    init(
        center: UNUserNotificationCenter = .current(),
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) {
        self.center = center
        self.environment = environment
    }

    func requestAuthorizationIfNeeded() {
        if shouldSkipNotificationRequest() { return }
        center.getNotificationSettings { [center] settings in
            guard settings.authorizationStatus == .notDetermined else { return }
            DispatchQueue.main.async {
                center.requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
            }
        }
    }

    func scheduleTimerCompletion(at date: Date, presetTitle: String, vibrationEnabled: Bool) {
        center.getNotificationSettings { [center] settings in
            let isAllowed: Bool
            switch settings.authorizationStatus {
            case .authorized, .provisional:
                isAllowed = true
#if os(iOS)
            case .ephemeral:
                isAllowed = true
#endif
            default:
                isAllowed = false
            }

            guard isAllowed else {
                self.writeSmokeStatus([
                    "authorizationStatus": Self.authorizationStatusName(settings.authorizationStatus),
                    "scheduled": "false",
                    "reason": "notification authorization is not granted",
                    "identifier": TimerNotificationPresentationPolicy.timerCompletionIdentifier,
                ])
                return
            }

            let remaining = max(1, date.timeIntervalSinceNow)
            let content = UNMutableNotificationContent()
            content.title = "타이머가 끝났어요"
            content.body = "\(presetTitle) 시간이 완료됐어요."
            content.sound = .default
            content.userInfo[TimerNotificationPresentationPolicy.vibrationEnabledKey] = vibrationEnabled
            if #available(iOS 15.0, *) {
                content.interruptionLevel = vibrationEnabled ? .timeSensitive : .active
            }

            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: remaining, repeats: false)
            let request = UNNotificationRequest(
                identifier: TimerNotificationPresentationPolicy.timerCompletionIdentifier,
                content: content,
                trigger: trigger
            )

            center.removePendingNotificationRequests(withIdentifiers: [TimerNotificationPresentationPolicy.timerCompletionIdentifier])
            center.add(request) { [center] error in
                if let error {
                    self.writeSmokeStatus([
                        "authorizationStatus": Self.authorizationStatusName(settings.authorizationStatus),
                        "scheduled": "false",
                        "error": error.localizedDescription,
                        "identifier": TimerNotificationPresentationPolicy.timerCompletionIdentifier,
                    ])
                    return
                }

                center.getPendingNotificationRequests { requests in
                    self.writeSmokeStatus([
                        "authorizationStatus": Self.authorizationStatusName(settings.authorizationStatus),
                        "scheduled": "true",
                        "pending": requests.contains { $0.identifier == TimerNotificationPresentationPolicy.timerCompletionIdentifier } ? "true" : "false",
                        "identifier": TimerNotificationPresentationPolicy.timerCompletionIdentifier,
                        "remainingSeconds": String(Int(remaining.rounded())),
                        "presetTitle": presetTitle,
                    ])
                }
            }
        }
    }

    func cancelPendingTimerCompletion() {
        center.removePendingNotificationRequests(withIdentifiers: [TimerNotificationPresentationPolicy.timerCompletionIdentifier])
    }

    private func shouldSkipNotificationRequest() -> Bool {
        let value = environment["SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST"]?.lowercased()
        return value == "1" || value == "true" || value == "yes"
    }

    private func shouldWriteSmokeStatus() -> Bool {
        let value = environment["SCHOOLHELPER_NOTIFICATION_SMOKE_STATUS"]?.lowercased()
        return value == "1" || value == "true" || value == "yes"
    }

    private func writeSmokeStatus(_ fields: [String: String]) {
        guard shouldWriteSmokeStatus() else { return }
        guard let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }

        var payload = fields
        payload["createdAt"] = ISO8601DateFormatter().string(from: Date())
        if let runID = environment["SCHOOLHELPER_NOTIFICATION_SMOKE_RUN_ID"], !runID.isEmpty {
            payload["runID"] = runID
        }

        do {
            let data = try JSONSerialization.data(withJSONObject: payload, options: [.prettyPrinted, .sortedKeys])
            try data.write(to: documentsURL.appendingPathComponent(Self.smokeStatusFileName), options: .atomic)
        } catch {
            print("Failed to write notification smoke status: \(error.localizedDescription)")
        }
    }

    private static func authorizationStatusName(_ status: UNAuthorizationStatus) -> String {
        switch status {
        case .notDetermined:
            return "notDetermined"
        case .denied:
            return "denied"
        case .authorized:
            return "authorized"
        case .provisional:
            return "provisional"
#if os(iOS)
        case .ephemeral:
            return "ephemeral"
#endif
        @unknown default:
            return "unknown"
        }
    }

    private static let smokeStatusFileName = "schoolhelper-notification-smoke.json"
}
