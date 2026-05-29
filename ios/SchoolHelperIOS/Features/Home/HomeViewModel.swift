import Foundation
import Combine

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var dateLabel: String = ""
    @Published var todaySummary: String = "오늘 시간표를 불러오는 중…"
    @Published var mealSummary: String = "오늘 급식을 불러오는 중…"
    @Published var mealMeta: String = ""
    @Published var eventSummary: String = "일정을 불러오는 중…"
    @Published var noticeSummary: String = "가정통신문을 불러오는 중…"
    @Published var noticeActionText: String = "가정통신문 열기"
    @Published var noticeActionEnabled: Bool = false
    @Published var noticeRequiresSetup: Bool = false
    @Published var timerSummary: String = ""

    private let repository: SchoolRepository
    private let timerStateProvider: () -> TimerSessionState
    private let now: () -> Date
    private(set) var latestNoticeURL: URL?

    init(
        repository: SchoolRepository = DefaultSchoolRepository(),
        timerStateProvider: @escaping () -> TimerSessionState = { TimerPreferencesStore().load() },
        now: @escaping () -> Date = { AppLaunchOverrides.referenceDate() ?? Date() }
    ) {
        self.repository = repository
        self.timerStateProvider = timerStateProvider
        self.now = now
    }

    func load(profile: StudentProfile) async {
        let currentDate = now()
        dateLabel = formattedCurrentDate(currentDate)
        timerSummary = formatTimerSummary(timerStateProvider())

        guard profile.isComplete else {
            todaySummary = "학교와 학년/반을 먼저 설정해 주세요."
            mealSummary = "학교 설정이 필요해요."
            mealMeta = ""
            eventSummary = "학교 설정이 필요해요."
            noticeSummary = "학교 설정 후 최근 가정통신문을 확인할 수 있어요."
            noticeActionText = "학교 설정하러 가기"
            noticeActionEnabled = true
            noticeRequiresSetup = true
            latestNoticeURL = nil
            return
        }

        async let timetable = repository.fetchTimetable(for: profile, date: currentDate)
        async let meals = repository.fetchTodayMeals(for: profile, date: currentDate)
        async let events = repository.fetchSchedule(for: profile, month: currentDate)
        async let notices = repository.fetchNotices(for: profile, limit: 3)

        let timetableItems = (try? await timetable) ?? []
        let mealItems = (try? await meals) ?? []
        let eventItems = (try? await events) ?? []
        let noticeItems = (try? await notices) ?? []

        todaySummary = timetableItems.isEmpty ? "오늘 시간표가 없어요." : timetableItems.map { "\($0.period)교시 \($0.subject)" }.joined(separator: "\n")
        mealSummary = mealItems.first.map(formatMealMenu) ?? "오늘 급식이 없어요."
        mealMeta = mealItems.first.map(formatMealMeta) ?? ""
        let visibleEventSummaries = eventItems
            .filter { isVisibleSchedule($0) }
            .filter { !isPastSchedule($0, referenceDate: currentDate) }
            .prefix(3)
            .map { event in
                if event.description.isEmpty {
                    return "\(formattedEventDate(event.date))  \(event.title)"
                }
                return "\(formattedEventDate(event.date))  \(event.title)\n\(event.description)"
            }
        eventSummary = visibleEventSummaries.isEmpty
            ? "가까운 일정이 없어요."
            : visibleEventSummaries.joined(separator: "\n\n")

        if noticeItems.isEmpty {
            noticeSummary = "새 가정통신문이 없어요."
            noticeActionText = "확인 불가"
            noticeActionEnabled = false
            noticeRequiresSetup = false
            latestNoticeURL = nil
        } else {
            noticeSummary = noticeItems.prefix(3)
                .map(formatNoticePreviewLine)
                .filter { !$0.isEmpty }
                .joined(separator: "\n")
            latestNoticeURL = URL(string: noticeItems.first?.url ?? "")
            noticeActionText = "가정통신문 열기"
            noticeActionEnabled = latestNoticeURL != nil
            noticeRequiresSetup = false
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

    private func formattedCurrentDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "M월 d일 EEEE"
        return formatter.string(from: date)
    }

    private func formatMealMenu(_ meal: MealInfo) -> String {
        MealMenuFormatter.displayMenu(from: meal.menu, emptyFallback: "오늘 급식이 없어요.")
    }

    private func formatMealMeta(_ meal: MealInfo) -> String {
        [
            meal.mealType.trimmingCharacters(in: .whitespacesAndNewlines),
            meal.calorieInfo.trimmingCharacters(in: .whitespacesAndNewlines)
        ]
        .filter { !$0.isEmpty }
        .joined(separator: " • ")
    }

    private func formatNoticePreviewLine(_ notice: NoticePreview) -> String {
        let title = notice.title.trimmingCharacters(in: .whitespacesAndNewlines)
        let date = notice.date.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !date.isEmpty else {
            return title
        }
        return "\(date)  \(title)"
    }

    private func isVisibleSchedule(_ event: SchoolEvent) -> Bool {
        let blockedKeywords = ["토요휴업일"]
        return blockedKeywords.allSatisfy { keyword in
            !event.title.contains(keyword) && !event.description.contains(keyword)
        }
    }

    private func isPastSchedule(_ event: SchoolEvent, referenceDate: Date) -> Bool {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "yyyyMMdd"
        guard let eventDate = formatter.date(from: event.date) else {
            return false
        }

        let calendar = Calendar(identifier: .gregorian)
        return calendar.startOfDay(for: eventDate) < calendar.startOfDay(for: referenceDate)
    }

    private func formatTimerSummary(_ state: TimerSessionState) -> String {
        let minutes = max(0, state.remainingSeconds) / 60
        let seconds = max(0, state.remainingSeconds) % 60
        let timeText = String(format: "%02d:%02d", minutes, seconds)

        if state.isRunning {
            return "\(state.preset.title) • \(timeText) 남음"
        }
        return "\(state.preset.title) • \(timeText)"
    }
}
