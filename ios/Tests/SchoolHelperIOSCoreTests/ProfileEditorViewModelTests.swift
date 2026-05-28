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

    func testSetupViewModelIgnoresStaleSchoolSearchResults() async {
        let repository = DelayedSearchRepository(
            results: [
                "미사": [
                    SchoolInfo(
                        officeCode: "J10",
                        officeName: "경기도교육청",
                        schoolCode: "7531093",
                        schoolName: "미사중학교",
                        schoolKind: "중학교",
                        roadAddress: "경기도 하남시"
                    )
                ],
                "하남": [
                    SchoolInfo(
                        officeCode: "J10",
                        officeName: "경기도교육청",
                        schoolCode: "7531094",
                        schoolName: "하남중학교",
                        schoolKind: "중학교",
                        roadAddress: "경기도 하남시"
                    )
                ]
            ],
            delays: [
                "미사": 120_000_000,
                "하남": 10_000_000
            ]
        )
        let viewModel = SetupViewModel(initialProfile: StudentProfile(), repository: repository)

        viewModel.updateSchoolQuery("미사")
        let firstSearch = Task { await viewModel.searchSchools() }
        try? await Task.sleep(nanoseconds: 20_000_000)
        viewModel.updateSchoolQuery("하남")
        let secondSearch = Task { await viewModel.searchSchools() }

        await firstSearch.value
        await secondSearch.value

        XCTAssertEqual(viewModel.searchQuery, "하남중학교")
        XCTAssertEqual(viewModel.searchResults.map { $0.schoolName }, ["하남중학교"])
        XCTAssertEqual(viewModel.selectedSchool?.schoolCode, "7531094")
        XCTAssertEqual(viewModel.message, "학교 1개를 찾았어요.")
        XCTAssertFalse(viewModel.isSearching)
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

    func testSettingsViewModelRejectsSaveWhenSchoolSelectionNoLongerMatchesQuery() {
        let viewModel = SettingsViewModel(
            initialProfile: StudentProfile.fixture(),
            repository: MockSchoolRepository(),
            notificationAuthorizationProvider: StubNotificationAuthorizationProvider()
        )

        viewModel.updateSchoolQuery("다른학교")
        let savedProfile = viewModel.buildProfileForSave()

        XCTAssertNil(savedProfile)
        XCTAssertEqual(viewModel.message, "학교를 검색 후 다시 선택해 주세요.")
    }
}

private struct DelayedSearchRepository: SchoolRepository {
    let results: [String: [SchoolInfo]]
    let delays: [String: UInt64]

    func searchSchools(query: String) async throws -> [SchoolInfo] {
        if let delay = delays[query] {
            try await Task.sleep(nanoseconds: delay)
        }
        return results[query] ?? []
    }

    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] {
        []
    }

    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] {
        []
    }

    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] {
        []
    }

    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] {
        []
    }

    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] {
        []
    }
}
