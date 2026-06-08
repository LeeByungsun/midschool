import XCTest
@testable import SchoolHelperIOS

final class SchoolRepositoryTests: XCTestCase {
    func testMockRepositoryReturnsWeekMeals() async throws {
        let repository = MockSchoolRepository()
        let profile = StudentProfile(
            grade: "2",
            classroom: "3",
            schoolName: "미사중학교",
            officeCode: "J10",
            schoolCode: "1234567",
            schoolKind: "중학교"
        )

        let meals = try await repository.fetchWeekMeals(for: profile, weekStart: Date())

        XCTAssertEqual(5, meals.count)
    }
}
