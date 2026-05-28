import Foundation
import Combine

@MainActor
final class ScheduleViewModel: ObservableObject {
    @Published var monthTitle: String = ""
    @Published var statusText: String = ""
    @Published var items: [SchoolEvent] = []

    private let repository: SchoolRepository
    private let todayProvider: () -> Date
    private let calendar = Calendar(identifier: .gregorian)
    private var latestProfile = StudentProfile()
    private var currentMonth: Date

    init(
        repository: SchoolRepository = DefaultSchoolRepository(),
        todayProvider: @escaping () -> Date = { AppLaunchOverrides.referenceDate() ?? Date() }
    ) {
        self.repository = repository
        self.todayProvider = todayProvider
        self.currentMonth = todayProvider()
    }

    func load(profile: StudentProfile, month: Date? = nil) async {
        latestProfile = profile
        currentMonth = month ?? todayProvider()
        refreshMonthTitle()
        guard profile.isComplete else {
            items = []
            statusText = "학교와 학년/반을 먼저 설정해 주세요."
            return
        }
        items = ((try? await repository.fetchSchedule(for: profile, month: currentMonth)) ?? [])
            .filter(isVisibleSchedule)
            .sorted { $0.date < $1.date }
        statusText = items.isEmpty ? "선택한 달 일정이 없어요." : ""
    }

    func showPreviousMonth() async {
        currentMonth = calendar.date(byAdding: .month, value: -1, to: currentMonth) ?? currentMonth
        await load(profile: latestProfile, month: currentMonth)
    }

    func showCurrentMonth() async {
        currentMonth = todayProvider()
        await load(profile: latestProfile, month: currentMonth)
    }

    func showNextMonth() async {
        currentMonth = calendar.date(byAdding: .month, value: 1, to: currentMonth) ?? currentMonth
        await load(profile: latestProfile, month: currentMonth)
    }

    private func refreshMonthTitle() {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "yyyy년 M월"
        monthTitle = formatter.string(from: currentMonth)
    }

    private func isVisibleSchedule(_ event: SchoolEvent) -> Bool {
        let blockedKeywords = ["토요휴업일"]
        return blockedKeywords.allSatisfy { keyword in
            !event.title.contains(keyword) && !event.description.contains(keyword)
        }
    }
}
