import XCTest
@testable import SchoolHelperIOSCore

final class DiagnosticsConsentStoreTests: XCTestCase {
    func testDiagnosticsConsentDefaultsToDisabled() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)

        let store = DiagnosticsConsentStore(defaults: defaults)

        XCTAssertFalse(store.isEnabled())
    }

    func testDiagnosticsConsentPersistsTheLatestChoice() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let store = DiagnosticsConsentStore(defaults: defaults)

        store.save(isEnabled: true)
        XCTAssertTrue(store.isEnabled())

        store.save(isEnabled: false)
        XCTAssertFalse(store.isEnabled())
    }
}

final class AppVersionLabelTests: XCTestCase {
    func testFormatsVersionNameAndBuildNumber() {
        XCTAssertEqual(
            AppVersionLabel.format(versionName: "1.0", buildNumber: "5"),
            "v1.0(5)"
        )
    }
}
