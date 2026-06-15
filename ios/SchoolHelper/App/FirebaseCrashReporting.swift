import Foundation

#if canImport(FirebaseCore) && canImport(FirebaseCrashlytics)
import FirebaseCore
import FirebaseCrashlytics
#endif

enum FirebaseCrashReporting {
    static func configureIfAvailable() {
        #if canImport(FirebaseCore) && canImport(FirebaseCrashlytics)
        guard FirebaseApp.app() == nil else { return }
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            return
        }

        FirebaseApp.configure()
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        #endif
    }
}
