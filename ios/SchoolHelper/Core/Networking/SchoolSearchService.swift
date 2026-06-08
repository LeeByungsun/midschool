import Foundation

protocol SchoolSearchService {
    func searchSchools(query: String) async throws -> [SchoolInfo]
}

struct MockSchoolSearchService: SchoolSearchService {
    private let sampleSchools: [SchoolInfo] = [
        SchoolInfo(
            officeCode: "J10",
            officeName: "경기도교육청",
            schoolCode: "7692129",
            schoolName: "미사중학교",
            schoolKind: "중학교",
            roadAddress: "경기도 하남시"
        ),
        SchoolInfo(
            officeCode: "J10",
            officeName: "경기도교육청",
            schoolCode: "7531163",
            schoolName: "미사고등학교",
            schoolKind: "고등학교",
            roadAddress: "경기도 하남시"
        )
    ]

    func searchSchools(query: String) async throws -> [SchoolInfo] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else { return [] }
        let normalized = Self.normalizedSchoolName(trimmed)
        return sampleSchools.filter {
            $0.schoolName.localizedCaseInsensitiveContains(trimmed) ||
                Self.normalizedSchoolName($0.schoolName).localizedCaseInsensitiveContains(normalized)
        }
    }

    private static func normalizedSchoolName(_ value: String) -> String {
        value.components(separatedBy: .whitespacesAndNewlines).joined()
    }
}
