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

    static func fromEnvironment(
        _ environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> NotificationAuthorizationProviding {
        guard let overrideStatus = NotificationAuthorizationStatus(launchOverride: environment["SCHOOLHELPER_NOTIFICATION_AUTHORIZATION_STATUS"]) else {
            return NotificationAuthorizationProvider()
        }

        return LaunchOverrideNotificationAuthorizationProvider(
            initialStatus: overrideStatus,
            requestGranted: Self.launchOverrideRequestGranted(from: environment)
        )
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

    private static func launchOverrideRequestGranted(from environment: [String: String]) -> Bool {
        guard let rawValue = environment["SCHOOLHELPER_NOTIFICATION_AUTHORIZATION_REQUEST_GRANTED"]?.lowercased() else {
            return true
        }
        return rawValue == "1" || rawValue == "true" || rawValue == "yes"
    }
}

private actor LaunchOverrideNotificationAuthorizationProvider: NotificationAuthorizationProviding {
    private var status: NotificationAuthorizationStatus
    private let requestGranted: Bool

    init(initialStatus: NotificationAuthorizationStatus, requestGranted: Bool) {
        self.status = initialStatus
        self.requestGranted = requestGranted
    }

    func authorizationStatus() async -> NotificationAuthorizationStatus {
        status
    }

    func requestAuthorization() async -> Bool {
        status = requestGranted ? .authorized : .denied
        return requestGranted
    }
}

private extension NotificationAuthorizationStatus {
    init?(launchOverride rawValue: String?) {
        switch rawValue?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "notdetermined", "not_determined", "undetermined":
            self = .notDetermined
        case "denied":
            self = .denied
        case "authorized", "granted":
            self = .authorized
        default:
            return nil
        }
    }
}
