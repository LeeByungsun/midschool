import Foundation

struct StudentProfile: Codable, Equatable {
    var grade: String = ""
    var classroom: String = ""
    var schoolName: String = ""
    var officeCode: String = ""
    var schoolCode: String = ""
    var schoolKind: String = ""

    var hasSchoolSelection: Bool {
        !schoolName.isEmpty && !officeCode.isEmpty && !schoolCode.isEmpty && !schoolKind.isEmpty
    }

    var isComplete: Bool {
        !grade.isEmpty && !classroom.isEmpty && hasSchoolSelection
    }
}
