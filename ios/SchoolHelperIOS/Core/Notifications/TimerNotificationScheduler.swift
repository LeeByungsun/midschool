import Foundation
import UserNotifications

protocol TimerNotificationScheduling {
    func requestAuthorizationIfNeeded()
    func scheduleTimerCompletion(at date: Date, presetTitle: String)
    func cancelPendingTimerCompletion()
}

final class TimerNotificationScheduler: TimerNotificationScheduling {
    private let center: UNUserNotificationCenter

    init(center: UNUserNotificationCenter = .current()) {
        self.center = center
    }

    func requestAuthorizationIfNeeded() {
        center.getNotificationSettings { [center] settings in
            guard settings.authorizationStatus == .notDetermined else { return }
            center.requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
        }
    }

    func scheduleTimerCompletion(at date: Date, presetTitle: String) {
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
                return
            }

            let remaining = max(1, date.timeIntervalSinceNow)
            let content = UNMutableNotificationContent()
            content.title = "타이머가 끝났어요"
            content.body = "\(presetTitle) 시간이 완료됐어요."
            content.sound = .default

            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: remaining, repeats: false)
            let request = UNNotificationRequest(
                identifier: Self.notificationIdentifier,
                content: content,
                trigger: trigger
            )

            center.removePendingNotificationRequests(withIdentifiers: [Self.notificationIdentifier])
            center.add(request)
        }
    }

    func cancelPendingTimerCompletion() {
        center.removePendingNotificationRequests(withIdentifiers: [Self.notificationIdentifier])
    }

    private static let notificationIdentifier = "schoolhelper.timer.complete"
}
