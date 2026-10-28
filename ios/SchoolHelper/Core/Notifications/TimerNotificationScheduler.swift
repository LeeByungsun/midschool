import Foundation
import UserNotifications
import Dispatch

struct TimerNotificationEvent: Equatable {
    let date: Date
    let presetTitle: String
}

protocol TimerNotificationScheduling {
    func requestAuthorizationIfNeeded()
    func scheduleTimerCompletion(at date: Date, presetTitle: String, vibrationEnabled: Bool)
    func scheduleTimerCompletions(_ events: [TimerNotificationEvent], vibrationEnabled: Bool)
    func cancelPendingTimerCompletion()
}

enum TimerNotificationPresentationPolicy {
    static let timerCompletionIdentifier = "schoolhelper.timer.complete"
    static let vibrationEnabledKey = "timerVibrationEnabled"
    static let timerIdentifiers = [timerCompletionIdentifier] + (1...6).map { "\(timerCompletionIdentifier).\($0)" }

    static func shouldPlaySound(identifier: String) -> Bool {
        timerIdentifiers.contains(identifier)
    }

    static func shouldPlayHaptic(identifier: String, userInfo: [AnyHashable: Any]) -> Bool {
        shouldPlaySound(identifier: identifier) && userInfo[vibrationEnabledKey] as? Bool == true
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

    // All instances share the generation: a settings-screen cancellation also invalidates
    // permission callbacks started by the timer's scheduler instance.
    private static let schedulingLock = NSLock()
    private static var generation = 0

    func scheduleTimerCompletion(at date: Date, presetTitle: String, vibrationEnabled: Bool) {
        scheduleTimerCompletions([TimerNotificationEvent(date: date, presetTitle: presetTitle)], vibrationEnabled: vibrationEnabled)
    }

    func scheduleTimerCompletions(_ events: [TimerNotificationEvent], vibrationEnabled: Bool) {
        Self.schedulingLock.lock()
        Self.generation += 1
        let generation = Self.generation
        center.removePendingNotificationRequests(withIdentifiers: TimerNotificationPresentationPolicy.timerIdentifiers)
        Self.schedulingLock.unlock()

        center.getNotificationSettings { [self] settings in
            switch settings.authorizationStatus {
            case .authorized, .provisional:
                scheduleAuthorized(events, vibrationEnabled: vibrationEnabled, generation: generation)
#if os(iOS)
            case .ephemeral:
                scheduleAuthorized(events, vibrationEnabled: vibrationEnabled, generation: generation)
#endif
            case .notDetermined where !shouldSkipNotificationRequest():
                center.requestAuthorization(options: [.alert, .sound, .badge]) { [self] granted, _ in
                    if granted { scheduleAuthorized(events, vibrationEnabled: vibrationEnabled, generation: generation) }
                }
            default:
                writeSmokeStatus(["authorizationStatus": Self.authorizationStatusName(settings.authorizationStatus),
                                  "scheduled": "false", "reason": "notification authorization is not granted",
                                  "identifier": TimerNotificationPresentationPolicy.timerCompletionIdentifier])
            }
        }
    }

    private func scheduleAuthorized(_ events: [TimerNotificationEvent], vibrationEnabled: Bool, generation: Int) {
        Self.schedulingLock.lock()
        defer { Self.schedulingLock.unlock() }
        guard Self.generation == generation else { return }
        for (index, event) in events.prefix(TimerNotificationPresentationPolicy.timerIdentifiers.count).enumerated() {
            let remaining = event.date.timeIntervalSinceNow
            guard remaining > 0 else { continue }
            let identifier = TimerNotificationPresentationPolicy.timerIdentifiers[index]
            let content = UNMutableNotificationContent()
            content.title = "타이머가 끝났어요"
            content.body = "\(event.presetTitle) 시간이 완료됐어요."
            content.sound = .default
            content.userInfo[TimerNotificationPresentationPolicy.vibrationEnabledKey] = vibrationEnabled
            if #available(iOS 15.0, *) {
                content.interruptionLevel = vibrationEnabled ? .timeSensitive : .active
            }
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, remaining), repeats: false)
            let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)
            center.add(request) { [self] error in
                guard index == 0 else { return }
                if let error {
                    writeSmokeStatus(["scheduled": "false", "error": error.localizedDescription, "identifier": identifier])
                } else {
                    center.getPendingNotificationRequests { [self] requests in
                        writeSmokeStatus(["scheduled": "true", "pending": requests.contains { $0.identifier == identifier } ? "true" : "false",
                                          "identifier": identifier, "remainingSeconds": String(Int(remaining.rounded())), "presetTitle": event.presetTitle])
                    }
                }
            }
        }
    }

    func cancelPendingTimerCompletion() {
        Self.schedulingLock.lock()
        Self.generation += 1
        center.removePendingNotificationRequests(withIdentifiers: TimerNotificationPresentationPolicy.timerIdentifiers)
        Self.schedulingLock.unlock()
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
