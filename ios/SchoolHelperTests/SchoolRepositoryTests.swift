import XCTest
@testable import SchoolHelperIOS

final class SchoolRepositoryTests: XCTestCase {
    func testDefaultRepositoryPropagatesSchoolSearchFailure() async {
        let repository = DefaultSchoolRepository(
            schoolSearchService: FailingSchoolSearchService(),
            neisService: EmptyNEISService(),
            noticesService: EmptyNoticesService()
        )

        do {
            _ = try await repository.searchSchools(query: "미사중")
            XCTFail("검색 실패가 호출자에게 전달되어야 합니다.")
        } catch {
            XCTAssertTrue(error is ExpectedFailure)
        }
    }
}

private enum ExpectedFailure: Error {
    case search
}

private struct FailingSchoolSearchService: SchoolSearchService {
    func searchSchools(query: String) async throws -> [SchoolInfo] {
        throw ExpectedFailure.search
    }
}

private struct EmptyNEISService: NEISServicing {
    func fetchMeals(officeCode: String, schoolCode: String, date: String) async throws -> [MealInfo] { [] }
    func fetchSchedule(officeCode: String, schoolCode: String, month: String) async throws -> [SchoolEvent] { [] }
    func fetchTimetable(
        officeCode: String,
        schoolCode: String,
        schoolKind: String,
        grade: String,
        classroom: String,
        date: String
    ) async throws -> [TimetableItem] { [] }
}

private struct EmptyNoticesService: NoticesServicing {
    func fetchNotices(officeCode: String, schoolCode: String, limit: Int) async throws -> [NoticePreview] { [] }
}
