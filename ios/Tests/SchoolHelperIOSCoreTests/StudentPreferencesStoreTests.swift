import XCTest
@testable import SchoolHelperIOSCore

final class StudentPreferencesStoreTests: XCTestCase {
    func testMigratesLegacySingleProfileIntoActiveProfileCollection() throws {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let legacyProfile = StudentProfile.fixture()
        defaults.set(try JSONEncoder().encode(legacyProfile), forKey: "student_profile")
        let store = StudentPreferencesStore(defaults: defaults)

        let activeProfile = store.load()

        XCTAssertEqual(activeProfile, legacyProfile)
        XCTAssertEqual(store.loadProfiles(), [legacyProfile])
        XCTAssertEqual(store.activeProfileID(), legacyProfile.id)
        XCTAssertNil(defaults.data(forKey: "student_profile"))
        XCTAssertNotNil(defaults.data(forKey: "student_profiles_data"))
    }

    func testAddsAndSelectsMultipleProfilesLocally() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let store = StudentPreferencesStore(defaults: defaults)
        let first = StudentProfile.fixture()
        var second = StudentProfile.fixture()
        second.id = "second-profile"
        second.displayName = "민준"

        store.save(first)
        XCTAssertTrue(store.add(second))

        XCTAssertEqual(store.loadProfiles(), [first, second])
        XCTAssertEqual(store.load(), second)
        XCTAssertTrue(store.selectProfile(id: first.id))
        XCTAssertEqual(store.load(), first)
    }

    func testRejectsDuplicateDisplayNameAndDeletingLastProfile() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let store = StudentPreferencesStore(defaults: defaults)
        let first = StudentProfile.fixture()
        var duplicate = StudentProfile.fixture()
        duplicate.id = "duplicate-profile"

        store.save(first)

        XCTAssertFalse(store.add(duplicate))
        XCTAssertFalse(store.deleteProfile(id: first.id))
        XCTAssertEqual(store.loadProfiles(), [first])
    }

    func testDeletingActiveProfileSelectsRemainingProfile() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let store = StudentPreferencesStore(defaults: defaults)
        let first = StudentProfile.fixture()
        var second = StudentProfile.fixture()
        second.id = "second-profile"
        second.displayName = "민준"
        store.save(first)
        XCTAssertTrue(store.add(second))

        XCTAssertTrue(store.deleteProfile(id: second.id))

        XCTAssertEqual(store.load(), first)
        XCTAssertEqual(store.activeProfileID(), first.id)
    }
}
