import Foundation

protocol SchoolRepository {
    func searchSchools(query: String) async throws -> [SchoolInfo]
    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo]
    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo]
    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem]
    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent]
    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview]
}

protocol NEISServicing {
    func fetchMeals(
        officeCode: String,
        schoolCode: String,
        date: String
    ) async throws -> [MealInfo]
    func fetchSchedule(
        officeCode: String,
        schoolCode: String,
        month: String
    ) async throws -> [SchoolEvent]
    func fetchTimetable(
        officeCode: String,
        schoolCode: String,
        schoolKind: String,
        grade: String,
        classroom: String,
        date: String
    ) async throws -> [TimetableItem]
}

protocol NoticesServicing {
    func fetchNotices(
        officeCode: String,
        schoolCode: String,
        limit: Int
    ) async throws -> [NoticePreview]
}

extension NEISClient: SchoolSearchService, NEISServicing {}
extension NoticesClient: NoticesServicing {}

struct MockSchoolRepository: SchoolRepository {
    func searchSchools(query: String) async throws -> [SchoolInfo] {
        try await MockSchoolSearchService().searchSchools(query: query)
    }

    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd"
        formatter.locale = Locale(identifier: "ko_KR")
        return [
            MealInfo(
                date: formatter.string(from: date),
                mealType: "점심",
                menu: "비빔밥\n미역국",
                calorieInfo: "712 kcal"
            )
        ]
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
    private let schoolSearchService: SchoolSearchService
    private let neisService: NEISServicing
    private let noticesService: NoticesServicing
    private let fallback: SchoolRepository
    private let calendar: Calendar

    init(
        schoolSearchService: SchoolSearchService? = nil,
        neisClient: NEISClient = NEISClient(),
        noticesService: NoticesServicing = NoticesClient(),
        fallback: SchoolRepository = MockSchoolRepository(),
        calendar: Calendar = .current
    ) {
        self.schoolSearchService = schoolSearchService ?? neisClient
        self.neisService = neisClient
        self.noticesService = noticesService
        self.fallback = fallback
        self.calendar = calendar
    }

    init(
        schoolSearchService: SchoolSearchService,
        neisService: NEISServicing,
        noticesService: NoticesServicing,
        fallback: SchoolRepository = MockSchoolRepository(),
        calendar: Calendar = .current
    ) {
        self.schoolSearchService = schoolSearchService
        self.neisService = neisService
        self.noticesService = noticesService
        self.fallback = fallback
        self.calendar = calendar
    }

    func searchSchools(query: String) async throws -> [SchoolInfo] {
        do {
            return try await schoolSearchService.searchSchools(query: query)
        } catch {
            return try await fallback.searchSchools(query: query)
        }
    }

    func fetchTodayMeals(for profile: StudentProfile, date: Date) async throws -> [MealInfo] {
        let key = formattedString(from: date, format: "yyyyMMdd")
        do {
            return try await neisService.fetchMeals(
                officeCode: profile.officeCode,
                schoolCode: profile.schoolCode,
                date: key
            )
            .filter { $0.date == key }
        } catch {
            return try await fallback.fetchTodayMeals(for: profile, date: date)
        }
    }

    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] {
        let dates = weekDates(startingAt: weekStart)
        var meals: [MealInfo] = []

        for date in dates {
            let dayMeals = try await fetchTodayMeals(for: profile, date: date)
            meals.append(contentsOf: dayMeals)
        }

        return meals.sorted {
            if $0.date == $1.date {
                return $0.mealType < $1.mealType
            }
            return $0.date < $1.date
        }
    }

    func fetchTimetable(for profile: StudentProfile, date: Date) async throws -> [TimetableItem] {
        let key = formattedString(from: date, format: "yyyyMMdd")
        do {
            return try await neisService.fetchTimetable(
                officeCode: profile.officeCode,
                schoolCode: profile.schoolCode,
                schoolKind: profile.schoolKind,
                grade: profile.grade,
                classroom: profile.classroom,
                date: key
            )
        } catch {
            return try await fallback.fetchTimetable(for: profile, date: date)
        }
    }

    func fetchSchedule(for profile: StudentProfile, month: Date) async throws -> [SchoolEvent] {
        let key = formattedString(from: month, format: "yyyyMM")
        do {
            return try await neisService.fetchSchedule(
                officeCode: profile.officeCode,
                schoolCode: profile.schoolCode,
                month: key
            )
        } catch {
            return try await fallback.fetchSchedule(for: profile, month: month)
        }
    }

    func fetchNotices(for profile: StudentProfile, limit: Int) async throws -> [NoticePreview] {
        do {
            return try await noticesService.fetchNotices(
                officeCode: profile.officeCode,
                schoolCode: profile.schoolCode,
                limit: limit
            )
        } catch {
            return try await fallback.fetchNotices(for: profile, limit: limit)
        }
    }

    private func formattedString(from date: Date, format: String) -> String {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.dateFormat = format
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = calendar.timeZone
        return formatter.string(from: date)
    }

    private func weekDates(startingAt weekStart: Date) -> [Date] {
        let startOfDay = calendar.startOfDay(for: weekStart)
        return (0..<5).compactMap { offset in
            calendar.date(byAdding: .day, value: offset, to: startOfDay)
        }
    }
}
