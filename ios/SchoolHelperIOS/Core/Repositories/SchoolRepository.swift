import Foundation

protocol SchoolRepository {
    func searchSchools(query: String) async throws -> [SchoolInfo]
    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo]
    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo]
    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem]
    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent]
    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview]
}

struct MockSchoolRepository: SchoolRepository {
    func searchSchools(query: String) async throws -> [SchoolInfo] {
        try await MockSchoolSearchService().searchSchools(query: query)
    }

    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] {
        [MealInfo(date: "20260526", mealType: "점심", menu: "비빔밥\n미역국", calorieInfo: "712 kcal")]
    }

    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd"
        formatter.locale = Locale(identifier: "ko_KR")
        return (0..<5).map { offset in
            let day = Calendar.current.date(byAdding: .day, value: offset, to: weekStart) ?? weekStart
            return MealInfo(
                date: formatter.string(from: day),
                mealType: "점심",
                menu: "샘플 급식 \(offset + 1)",
                calorieInfo: "\(680 + offset * 10) kcal"
            )
        }
    }

    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd"
        return [
            TimetableItem(date: formatter.string(from: date), period: "1", subject: "국어", grade: profile.grade, classroom: profile.classroom),
            TimetableItem(date: formatter.string(from: date), period: "2", subject: "수학", grade: profile.grade, classroom: profile.classroom),
            TimetableItem(date: formatter.string(from: date), period: "3", subject: "영어", grade: profile.grade, classroom: profile.classroom)
        ]
    }

    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] {
        [
            SchoolEvent(date: "20260526", title: "체육대회", description: "운동장"),
            SchoolEvent(date: "20260528", title: "중간고사", description: "교실별 시험")
        ]
    }

    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] {
        Array([
            NoticePreview(id: "1", title: "현장학습 안내", date: "2026-05-26", author: "교무실", url: "https://example.com/notices/1"),
            NoticePreview(id: "2", title: "학부모 공지", date: "2026-05-25", author: "행정실", url: "https://example.com/notices/2")
        ].prefix(limit))
    }
}

struct DefaultSchoolRepository: SchoolRepository {
    private let neisClient: NEISClient
    private let noticesClient: NoticesClient
    private let fallback: MockSchoolRepository

    init(
        neisClient: NEISClient = NEISClient(),
        noticesClient: NoticesClient = NoticesClient(),
        fallback: MockSchoolRepository = MockSchoolRepository()
    ) {
        self.neisClient = neisClient
        self.noticesClient = noticesClient
        self.fallback = fallback
    }

    func searchSchools(query: String) async throws -> [SchoolInfo] {
        (try? await neisClient.searchSchools(query: query)) ?? fallback.searchSchools(query: query)
    }

    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd"
        let key = formatter.string(from: date)
        return (try? await neisClient.fetchMeals(
            officeCode: profile.officeCode,
            schoolCode: profile.schoolCode,
            date: key
        )) ?? fallback.fetchTodayMeals(for: profile, date: date)
    }

    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] {
        (try? await fallback.fetchWeekMeals(for: profile, weekStart: weekStart)) ?? []
    }

    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd"
        let key = formatter.string(from: date)
        return (try? await neisClient.fetchTimetable(
            officeCode: profile.officeCode,
            schoolCode: profile.schoolCode,
            schoolKind: profile.schoolKind,
            grade: profile.grade,
            classroom: profile.classroom,
            date: key
        )) ?? fallback.fetchTimetable(for: profile, date: date)
    }

    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMM"
        let key = formatter.string(from: month)
        return (try? await neisClient.fetchSchedule(
            officeCode: profile.officeCode,
            schoolCode: profile.schoolCode,
            month: key
        )) ?? fallback.fetchSchedule(for: profile, month: month)
    }

    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] {
        (try? await noticesClient.fetchNotices(
            officeCode: profile.officeCode,
            schoolCode: profile.schoolCode,
            limit: limit
        )) ?? fallback.fetchNotices(for: profile, limit: limit)
    }
}
