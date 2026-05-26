import Foundation
import Combine

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var todaySummary: String = "오늘 시간표를 불러오는 중…"
    @Published var mealSummary: String = "오늘 급식을 불러오는 중…"
    @Published var eventSummary: String = "일정을 불러오는 중…"
    @Published var noticeSummary: String = "가정통신문을 불러오는 중…"

    private let repository: SchoolRepository
    private let now: () -> Date

    init(
        repository: SchoolRepository = DefaultSchoolRepository(),
        now: @escaping () -> Date = Date.init
    ) {
        self.repository = repository
        self.now = now
    }

    func load(profile: StudentProfile) async {
        guard profile.isComplete else {
            todaySummary = "학교와 학년/반을 먼저 설정해 주세요."
            mealSummary = "학교 설정이 필요해요."
            eventSummary = "학교 설정이 필요해요."
            noticeSummary = "학교 설정이 필요해요."
            return
        }

        let currentDate = now()
        async let timetable = repository.fetchTimetable(for: profile, date: currentDate)
        async let meals = repository.fetchTodayMeals(for: profile, date: currentDate)
        async let events = repository.fetchSchedule(for: profile, month: currentDate)
        async let notices = repository.fetchNotices(for: profile, limit: 1)

        let timetableItems = (try? await timetable) ?? []
        let mealItems = (try? await meals) ?? []
        let eventItems = (try? await events) ?? []
        let noticeItems = (try? await notices) ?? []

        todaySummary = timetableItems.isEmpty ? "오늘 시간표가 없어요." : timetableItems.map { "\($0.period)교시 \($0.subject)" }.joined(separator: "\n")
        mealSummary = mealItems.first?.menu ?? "오늘 급식이 없어요."
        eventSummary = eventItems.first?.title ?? "가까운 일정이 없어요."
        noticeSummary = noticeItems.first?.title ?? "새 가정통신문이 없어요."
    }
}
