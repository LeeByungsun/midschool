import Foundation

struct StudentProfile: Codable, Equatable {
    var grade: String = ""
    var classroom: String = ""
    var schoolName: String = ""
    var officeCode: String = ""
    var schoolCode: String = ""
    var schoolKind: String = ""

    var hasSchoolSelection: Bool {
        schoolName.isNotBlank && officeCode.isNotBlank && schoolCode.isNotBlank && schoolKind.isNotBlank
    }

    var isComplete: Bool {
        grade.isNotBlank && classroom.isNotBlank && hasSchoolSelection
    }

    var schoolInfo: SchoolInfo? {
        guard hasSchoolSelection else { return nil }
        return SchoolInfo(
            officeCode: officeCode.trimmed,
            officeName: "",
            schoolCode: schoolCode.trimmed,
            schoolName: schoolName.trimmed,
            schoolKind: schoolKind.trimmed,
            roadAddress: ""
        )
    }
}

private extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var isNotBlank: Bool {
        !trimmed.isEmpty
    }
}
