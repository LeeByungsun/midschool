import XCTest
@testable import SchoolHelperIOS

@MainActor
final class SetupViewModelTests: XCTestCase {
    func testSearchSingleResultAutoSelectsSchool() async throws {
        let viewModel = SetupViewModel(initialProfile: StudentProfile())

        viewModel.searchQuery = "미사중"
        await viewModel.searchSchools()

        XCTAssertEqual("미사중학교", viewModel.selectedSchool?.schoolName)
        XCTAssertEqual("미사중학교", viewModel.draftProfile.schoolName)
    }

    func testBuildProfileRequiresSelectedSchool() {
        let viewModel = SetupViewModel(initialProfile: StudentProfile())
        viewModel.updateGrade("2")
        viewModel.updateClassroom("3")

        XCTAssertNil(viewModel.buildProfileForSave())
    }
}
