import Foundation
import UserNotifications

enum NotificationAuthorizationStatus: Equatable {
    case notDetermined
    case denied
    case authorized
}

protocol NotificationAuthorizationProviding {
    func authorizationStatus() async -> NotificationAuthorizationStatus
    func requestAuthorization() async -> Bool
}

final class NotificationAuthorizationProvider: NotificationAuthorizationProviding {
    private let center: UNUserNotificationCenter

    init(center: UNUserNotificationCenter = .current()) {
        self.center = center
    }

    func authorizationStatus() async -> NotificationAuthorizationStatus {
        await withCheckedContinuation { continuation in
            center.getNotificationSettings { settings in
                let status: NotificationAuthorizationStatus
                switch settings.authorizationStatus {
                case .authorized, .provisional:
                    status = .authorized
#if os(iOS)
                case .ephemeral:
                    status = .authorized
#endif
                case .denied:
                    status = .denied
                default:
                    status = .notDetermined
                }
                continuation.resume(returning: status)
            }
        }
    }

    func requestAuthorization() async -> Bool {
        await withCheckedContinuation { continuation in
            center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
                continuation.resume(returning: granted)
            }
        }
    }
}
