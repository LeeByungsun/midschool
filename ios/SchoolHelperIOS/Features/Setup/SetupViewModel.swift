import Foundation
import Combine

@MainActor
final class SetupViewModel: ObservableObject {
    @Published var draftProfile: StudentProfile
    @Published var searchQuery: String
    @Published var searchResults: [SchoolInfo] = []
    @Published var selectedSchool: SchoolInfo?
    @Published var message: String = ""
    @Published var isSearching: Bool = false

    private let repository: SchoolRepository

    init(
        initialProfile: StudentProfile,
        repository: SchoolRepository = DefaultSchoolRepository()
    ) {
        self.draftProfile = initialProfile
        self.searchQuery = initialProfile.schoolName
        self.selectedSchool = initialProfile.schoolInfo
        self.repository = repository
    }

    func updateSchoolQuery(_ query: String) {
        searchQuery = query
        searchResults = []

        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if let selectedSchool, selectedSchool.schoolName != trimmed {
            self.selectedSchool = nil
            message = trimmed.isEmpty ? "" : "검색 결과에서 학교를 다시 선택해 주세요."
        } else if trimmed.isEmpty {
            message = ""
        }
    }

    func searchSchools() async {
        let trimmed = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else {
            message = "학교 이름은 두 글자 이상 입력해 주세요."
            searchResults = []
            return
        }

        isSearching = true
        defer { isSearching = false }

        do {
            let schools = try await repository.searchSchools(query: trimmed)
            searchResults = schools
            if schools.count == 1, let school = schools.first {
                selectSchool(school)
                message = "학교 1개를 찾았어요."
            } else if schools.isEmpty {
                message = "검색 결과가 없어요."
            } else {
                message = "검색 결과에서 학교를 선택해 주세요."
            }
        } catch {
            message = error.localizedDescription
        }
    }

    func selectSchool(_ school: SchoolInfo) {
        selectedSchool = school
        draftProfile.schoolName = school.schoolName
        draftProfile.officeCode = school.officeCode
        draftProfile.schoolCode = school.schoolCode
        draftProfile.schoolKind = school.schoolKind
        searchQuery = school.schoolName
        message = "\(school.schoolName)을 선택했어요."
    }

    func updateGrade(_ grade: String) {
        draftProfile.grade = grade
    }

    func updateClassroom(_ classroom: String) {
        draftProfile.classroom = classroom
    }

    func buildProfileForSave() -> StudentProfile? {
        let trimmedQuery = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let school = selectedSchool, school.schoolName == trimmedQuery else {
            message = "학교를 검색 후 다시 선택해 주세요."
            return nil
        }
        guard !draftProfile.grade.isEmpty, !draftProfile.classroom.isEmpty else {
            message = "학년/반을 입력해 주세요."
            return nil
        }

        var profile = draftProfile
        profile.schoolName = school.schoolName
        profile.officeCode = school.officeCode
        profile.schoolCode = school.schoolCode
        profile.schoolKind = school.schoolKind
        return profile
    }
}
