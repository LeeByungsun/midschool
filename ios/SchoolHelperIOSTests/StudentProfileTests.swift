import XCTest
@testable import SchoolHelperIOS

final class StudentProfileTests: XCTestCase {
    func testProfileIsCompleteWhenSchoolIdentityAndClassroomExist() {
        let profile = StudentProfile(
            grade: "2",
            classroom: "3",
            schoolName: "미사중학교",
            officeCode: "J10",
            schoolCode: "1234567",
            schoolKind: "중학교"
        )

        XCTAssertTrue(profile.hasSchoolSelection)
        XCTAssertTrue(profile.isComplete)
    }

    func testProfileIsIncompleteWhenSchoolIdentityIsMissing() {
        let profile = StudentProfile(
            grade: "2",
            classroom: "3",
            schoolName: "미사중학교"
        )

        XCTAssertFalse(profile.hasSchoolSelection)
        XCTAssertFalse(profile.isComplete)
    }
}
