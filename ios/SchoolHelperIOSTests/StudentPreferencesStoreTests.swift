import XCTest
@testable import SchoolHelperIOS

final class StudentPreferencesStoreTests: XCTestCase {
    func testStoreRoundTripsProfile() {
        let defaults = UserDefaults(suiteName: #file)!
        defaults.removePersistentDomain(forName: #file)
        let store = StudentPreferencesStore(defaults: defaults)
        let profile = StudentProfile(
            grade: "1",
            classroom: "4",
            schoolName: "미사중학교",
            officeCode: "J10",
            schoolCode: "1234567",
            schoolKind: "중학교"
        )

        store.save(profile)

        XCTAssertEqual(profile, store.load())
    }
}
