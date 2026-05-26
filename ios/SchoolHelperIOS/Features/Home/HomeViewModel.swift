import Foundation
import Combine

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var todaySummary: String = "오늘 시간표를 불러오는 중…"
    @Published var mealSummary: String = "오늘 급식을 불러오는 중…"
    @Published var eventSummary: String = "일정을 불러오는 중…"
    @Published var noticeSummary: String = "가정통신문을 불러오는 중…"
    @Published var noticeActionText: String = "가정통신문 열기"
    @Published var noticeActionEnabled: Bool = false

    private let repository: SchoolRepository
    private let now: () -> Date
    private(set) var latestNoticeURL: URL?

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
            noticeActionText = "확인 불가"
            noticeActionEnabled = false
            latestNoticeURL = nil
            return
        }

        let currentDate = now()
        async let timetable = repository.fetchTimetable(for: profile, date: currentDate)
        async let meals = repository.fetchTodayMeals(for: profile, date: currentDate)
        async let events = repository.fetchSchedule(for: profile, month: currentDate)
        async let notices = repository.fetchNotices(for: profile, limit: 3)

        let timetableItems = (try? await timetable) ?? []
        let mealItems = (try? await meals) ?? []
        let eventItems = (try? await events) ?? []
        let noticeItems = (try? await notices) ?? []

        todaySummary = timetableItems.isEmpty ? "오늘 시간표가 없어요." : timetableItems.map { "\($0.period)교시 \($0.subject)" }.joined(separator: "\n")
        mealSummary = mealItems.first?.menu ?? "오늘 급식이 없어요."
        eventSummary = eventItems.isEmpty
            ? "가까운 일정이 없어요."
            : eventItems.prefix(3).map { event in
                if event.description.isEmpty {
                    return "\(formattedEventDate(event.date))  \(event.title)"
                }
                return "\(formattedEventDate(event.date))  \(event.title)\n\(event.description)"
            }.joined(separator: "\n\n")

        if noticeItems.isEmpty {
            noticeSummary = "새 가정통신문이 없어요."
            noticeActionText = "확인 불가"
            noticeActionEnabled = false
            latestNoticeURL = nil
        } else {
            noticeSummary = noticeItems.prefix(3).map { notice in
                "\(notice.date)  \(notice.title)"
            }.joined(separator: "\n")
            latestNoticeURL = URL(string: noticeItems.first?.url ?? "")
            noticeActionText = "가정통신문 열기"
            noticeActionEnabled = latestNoticeURL != nil
        }
    }

    func latestNoticeDestination() -> URL? {
        latestNoticeURL
    }

    private func formattedEventDate(_ rawDate: String) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "yyyyMMdd"
        guard let date = formatter.date(from: rawDate) else {
            return rawDate
        }

        formatter.dateFormat = "M월 d일"
        return formatter.string(from: date)
    }
}
