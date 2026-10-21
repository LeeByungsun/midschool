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
