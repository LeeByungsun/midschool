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

final class OpenSourceLicenseCatalogTests: XCTestCase {
    func testDecodesBundledLicenseMetadata() throws {
        let data = Data(
            """
            [{"name":"Firebase iOS SDK","version":"12.14.0","licenseName":"Apache License 2.0","sourceURL":"https://github.com/firebase/firebase-ios-sdk","licenseText":"License text"}]
            """.utf8
        )

        let licenses = try OpenSourceLicenseCatalog.decode(data: data)

        XCTAssertEqual(licenses.count, 1)
        XCTAssertEqual(licenses.first?.name, "Firebase iOS SDK")
        XCTAssertEqual(licenses.first?.version, "12.14.0")
        XCTAssertEqual(licenses.first?.licenseName, "Apache License 2.0")
        XCTAssertEqual(licenses.first?.licenseText, "License text")
    }
}
