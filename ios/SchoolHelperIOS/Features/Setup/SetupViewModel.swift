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
    private var latestSearchRequestID = 0

    init(
        initialProfile: StudentProfile,
        repository: SchoolRepository = DefaultSchoolRepository()
    ) {
        self.draftProfile = initialProfile
        self.searchQuery = initialProfile.schoolName
        self.selectedSchool = initialProfile.schoolInfo
        self.message = Self.initialSchoolMessage(for: initialProfile)
        self.repository = repository
    }

    func updateSchoolQuery(_ query: String) {
        latestSearchRequestID += 1
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

    func searchSchoolsAfterDebounce() async {
        let trimmed = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else { return }
        guard selectedSchool?.schoolName != trimmed else { return }

        do {
            try await Task.sleep(nanoseconds: 450_000_000)
        } catch {
            return
        }
        guard !Task.isCancelled else { return }
        await searchSchools()
    }

    func searchSchools() async {
        let trimmed = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else {
            message = "학교 이름은 두 글자 이상 입력해 주세요."
            searchResults = []
            return
        }

        latestSearchRequestID += 1
        let requestID = latestSearchRequestID
        isSearching = true

        do {
            let schools = try await repository.searchSchools(query: trimmed)
            guard isLatestSearch(requestID: requestID, query: trimmed) else {
                return
            }
            isSearching = false
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
            guard isLatestSearch(requestID: requestID, query: trimmed) else {
                return
            }
            isSearching = false
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
        let trimmedGrade = draftProfile.grade.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedClassroom = draftProfile.classroom.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedGrade.isEmpty, !trimmedClassroom.isEmpty else {
            message = "학년/반을 입력해 주세요."
            return nil
        }

        var profile = draftProfile
        profile.grade = trimmedGrade
        profile.classroom = trimmedClassroom
        profile.schoolName = school.schoolName
        profile.officeCode = school.officeCode
        profile.schoolCode = school.schoolCode
        profile.schoolKind = school.schoolKind
        return profile
    }

    private func isLatestSearch(requestID: Int, query: String) -> Bool {
        requestID == latestSearchRequestID &&
            searchQuery.trimmingCharacters(in: .whitespacesAndNewlines) == query
    }

    private static func initialSchoolMessage(for profile: StudentProfile) -> String {
        let schoolName = profile.schoolName.trimmingCharacters(in: .whitespacesAndNewlines)
        if !schoolName.isEmpty && !profile.hasSchoolSelection {
            return "기존 설정에 학교 코드가 없어 학교를 다시 검색해 선택해 주세요."
        }
        return ""
    }
}
