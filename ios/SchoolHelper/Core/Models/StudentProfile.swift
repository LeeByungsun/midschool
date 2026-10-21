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

    static func validateAcademicPlacement(
        grade: String,
        classroom: String,
        schoolKind: String
    ) -> Result<AcademicPlacement, AcademicPlacementValidationError> {
        let trimmedGrade = grade.trimmed
        let trimmedClassroom = classroom.trimmed
        guard !trimmedGrade.isEmpty, !trimmedClassroom.isEmpty else {
            return .failure(.missing)
        }

        let normalizedSchoolKind = schoolKind.trimmed
        let maximumGrade = normalizedSchoolKind.contains("중학교") || normalizedSchoolKind.contains("고등학교")
            ? 3
            : 6
        guard let gradeNumber = Int(trimmedGrade), (1...maximumGrade).contains(gradeNumber) else {
            return .failure(
                .invalidGrade(
                    schoolKind: normalizedSchoolKind.isEmpty ? "학교" : normalizedSchoolKind,
                    maximum: maximumGrade
                )
            )
        }
        guard let classroomNumber = Int(trimmedClassroom), classroomNumber >= 1 else {
            return .failure(.invalidClassroom)
        }

        return .success(
            AcademicPlacement(
                grade: String(gradeNumber),
                classroom: String(classroomNumber)
            )
        )
    }
}

struct AcademicPlacement: Equatable {
    let grade: String
    let classroom: String
}

enum AcademicPlacementValidationError: Error, Equatable {
    case missing
    case invalidGrade(schoolKind: String, maximum: Int)
    case invalidClassroom

    var message: String {
        switch self {
        case .missing:
            return "학년/반을 입력해 주세요."
        case let .invalidGrade(schoolKind, maximum):
            return "\(schoolKind) 학년은 1~\(maximum) 사이의 숫자로 입력해 주세요."
        case .invalidClassroom:
            return "반은 1 이상의 숫자로 입력해 주세요."
        }
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
