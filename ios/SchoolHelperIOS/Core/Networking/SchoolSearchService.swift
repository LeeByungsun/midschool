import Foundation

protocol SchoolSearchService {
    func searchSchools(query: String) async throws -> [SchoolInfo]
}

struct MockSchoolSearchService: SchoolSearchService {
    private let sampleSchools: [SchoolInfo] = [
        SchoolInfo(
            officeCode: "J10",
            officeName: "경기도교육청",
            schoolCode: "7531093",
            schoolName: "미사중학교",
            schoolKind: "중학교",
            roadAddress: "경기도 하남시"
        ),
        SchoolInfo(
            officeCode: "J10",
            officeName: "경기도교육청",
            schoolCode: "7531094",
            schoolName: "미사고등학교",
            schoolKind: "고등학교",
            roadAddress: "경기도 하남시"
        )
    ]

    func searchSchools(query: String) async throws -> [SchoolInfo] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else { return [] }
        return sampleSchools.filter { $0.schoolName.localizedCaseInsensitiveContains(trimmed) }
    }
}
