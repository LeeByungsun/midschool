import Foundation

struct NEISClient {
    enum ClientError: LocalizedError {
        case notImplemented(String)

        var errorDescription: String? {
            switch self {
            case .notImplemented(let endpoint):
                return "\(endpoint) 연동은 아직 구현 중입니다."
            }
        }
    }

    func fetchMeals(
        officeCode: String,
        schoolCode: String,
        date: String
    ) async throws -> [MealInfo] {
        throw ClientError.notImplemented("급식")
    }

    func fetchSchedule(
        officeCode: String,
        schoolCode: String,
        month: String
    ) async throws -> [SchoolEvent] {
        throw ClientError.notImplemented("학사 일정")
    }

    func fetchTimetable(
        officeCode: String,
        schoolCode: String,
        schoolKind: String,
        grade: String,
        classroom: String,
        date: String
    ) async throws -> [TimetableItem] {
        throw ClientError.notImplemented("시간표")
    }

    func searchSchools(query: String) async throws -> [SchoolInfo] {
        throw ClientError.notImplemented("학교 검색")
    }
}

struct NoticesClient {
    enum ClientError: LocalizedError {
        case notImplemented

        var errorDescription: String? {
            "가정통신문 연동은 아직 구현 중입니다."
        }
    }

    func fetchNotices(
        officeCode: String,
        schoolCode: String,
        limit: Int
    ) async throws -> [NoticePreview] {
        throw ClientError.notImplemented
    }
}
