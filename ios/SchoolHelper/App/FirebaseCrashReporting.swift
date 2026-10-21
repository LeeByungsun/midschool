import Foundation

#if canImport(FirebaseCore) && canImport(FirebaseCrashlytics)
import FirebaseCore
import FirebaseCrashlytics
#endif

enum FirebaseCrashReporting {
    static func configureIfAvailable(
        diagnosticsEnabled: Bool = DiagnosticsConsentStore().isEnabled()
    ) {
        #if canImport(FirebaseCore) && canImport(FirebaseCrashlytics)
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            return
        }

        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        applyCollectionSetting(diagnosticsEnabled)
        #endif
    }

    static func setCollectionEnabled(_ enabled: Bool) {
        #if canImport(FirebaseCore) && canImport(FirebaseCrashlytics)
        guard FirebaseApp.app() != nil else { return }
        applyCollectionSetting(enabled)
        #endif
    }

    #if canImport(FirebaseCore) && canImport(FirebaseCrashlytics)
    private static func applyCollectionSetting(_ enabled: Bool) {
        let crashlytics = Crashlytics.crashlytics()
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
        if !enabled {
            crashlytics.deleteUnsentReports()
        }
    }
    #endif
}
