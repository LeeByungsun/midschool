import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class ProfileEditorViewModelTests: XCTestCase {
    func testSetupViewModelRejectsShortSearchQueries() async {
        let viewModel = SetupViewModel(initialProfile: StudentProfile(), repository: MockSchoolRepository())
        viewModel.searchQuery = "가"

        await viewModel.searchSchools()

        XCTAssertEqual(viewModel.message, "학교 이름은 두 글자 이상 입력해 주세요.")
        XCTAssertTrue(viewModel.searchResults.isEmpty)
    }

    func testSetupViewModelAutoSelectsSingleResultAndBuildsProfile() async {
        var initialProfile = StudentProfile()
        initialProfile.grade = "1"
        initialProfile.classroom = "2"
        let viewModel = SetupViewModel(initialProfile: initialProfile, repository: MockSchoolRepository())
        viewModel.searchQuery = "미사중학교"

        await viewModel.searchSchools()
        let savedProfile = viewModel.buildProfileForSave()

        XCTAssertEqual(viewModel.selectedSchool?.schoolName, "미사중학교")
        XCTAssertEqual(viewModel.message, "학교 1개를 찾았어요.")
        XCTAssertEqual(savedProfile?.schoolName, "미사중학교")
        XCTAssertEqual(savedProfile?.grade, "1")
        XCTAssertEqual(savedProfile?.classroom, "2")
    }

    func testSettingsViewModelSyncRefreshesDraftAndSelection() {
        let viewModel = SettingsViewModel(
            initialProfile: StudentProfile(),
            repository: MockSchoolRepository(),
            notificationAuthorizationProvider: StubNotificationAuthorizationProvider()
        )
        let profile = StudentProfile.fixture()

        viewModel.sync(with: profile)

        XCTAssertEqual(viewModel.draftProfile, profile)
        XCTAssertEqual(viewModel.searchQuery, profile.schoolName)
        XCTAssertEqual(viewModel.selectedSchool?.schoolCode, profile.schoolCode)
        XCTAssertTrue(viewModel.searchResults.isEmpty)
        XCTAssertEqual(viewModel.message, "")
    }
}
