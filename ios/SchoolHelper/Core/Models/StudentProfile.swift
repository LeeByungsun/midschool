import Foundation

struct StudentProfile: Codable, Equatable {
    static let maximumDisplayNameLength = 10

    var displayName: String = ""
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
        displayName.isNotBlank && grade.isNotBlank && classroom.isNotBlank && hasSchoolSelection
    }

    init(
        displayName: String = "",
        grade: String = "",
        classroom: String = "",
        schoolName: String = "",
        officeCode: String = "",
        schoolCode: String = "",
        schoolKind: String = ""
    ) {
        self.displayName = displayName
        self.grade = grade
        self.classroom = classroom
        self.schoolName = schoolName
        self.officeCode = officeCode
        self.schoolCode = schoolCode
        self.schoolKind = schoolKind
    }

    private enum CodingKeys: String, CodingKey {
        case displayName, grade, classroom, schoolName, officeCode, schoolCode, schoolKind
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        grade = try container.decodeIfPresent(String.self, forKey: .grade) ?? ""
        classroom = try container.decodeIfPresent(String.self, forKey: .classroom) ?? ""
        schoolName = try container.decodeIfPresent(String.self, forKey: .schoolName) ?? ""
        officeCode = try container.decodeIfPresent(String.self, forKey: .officeCode) ?? ""
        schoolCode = try container.decodeIfPresent(String.self, forKey: .schoolCode) ?? ""
        schoolKind = try container.decodeIfPresent(String.self, forKey: .schoolKind) ?? ""
        displayName = try container.decodeIfPresent(String.self, forKey: .displayName)
            ?? (schoolName.isNotBlank ? "자녀" : "")
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(displayName, forKey: .displayName)
        try container.encode(grade, forKey: .grade)
        try container.encode(classroom, forKey: .classroom)
        try container.encode(schoolName, forKey: .schoolName)
        try container.encode(officeCode, forKey: .officeCode)
        try container.encode(schoolCode, forKey: .schoolCode)
        try container.encode(schoolKind, forKey: .schoolKind)
    }

    static func validateDisplayName(_ displayName: String) -> Result<String, DisplayNameValidationError> {
        let trimmed = displayName.trimmed
        guard !trimmed.isEmpty else {
            return .failure(.missing)
        }
        guard trimmed.count <= maximumDisplayNameLength else {
            return .failure(.tooLong)
        }
        return .success(trimmed)
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

enum DisplayNameValidationError: Error, Equatable {
    case missing
    case tooLong

    var message: String {
        switch self {
        case .missing:
            return "자녀 이름을 입력해 주세요."
        case .tooLong:
            return "자녀 이름은 10자 이하로 입력해 주세요."
        }
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
