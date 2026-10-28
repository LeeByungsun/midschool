import Foundation
import Combine

@MainActor
final class ScheduleViewModel: ObservableObject {
    @Published var monthTitle: String = ""
    @Published var statusText: String = ""
    @Published var items: [SchoolEvent] = []
    @Published private(set) var hasLoadError = false

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
            hasLoadError = false
            statusText = "학교와 학년/반을 먼저 설정해 주세요."
            return
        }
        let cached = repository.cachedSchedule(for: profile, month: currentMonth)
        if let cached {
            items = filteredAndSorted(cached)
            hasLoadError = false
            statusText = items.isEmpty ? "선택한 달 일정이 없어요." : ""
        } else {
            items = []
            statusText = "일정을 불러오는 중…"
        }
        do {
            items = filteredAndSorted(try await repository.fetchSchedule(for: profile, month: currentMonth))
            hasLoadError = false
            statusText = items.isEmpty ? "선택한 달 일정이 없어요." : ""
        } catch {
            if cached == nil {
                items = []
                hasLoadError = true
                statusText = "일정을 불러오지 못했어요. 다시 시도해 주세요."
            }
        }
    }

    func showPreviousMonth() async {
        currentMonth = calendar.date(byAdding: .month, value: -1, to: currentMonth) ?? currentMonth
        await load(profile: latestProfile, month: currentMonth)
    }

    func showNextMonth() async {
        currentMonth = calendar.date(byAdding: .month, value: 1, to: currentMonth) ?? currentMonth
        await load(profile: latestProfile, month: currentMonth)
    }

    func retry() async {
        await load(profile: latestProfile, month: currentMonth)
    }

    private func refreshMonthTitle() {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "yyyy년 M월"
        monthTitle = formatter.string(from: currentMonth)
    }

    private func filteredAndSorted(_ events: [SchoolEvent]) -> [SchoolEvent] {
        events.filter(isVisibleSchedule).sorted { $0.date < $1.date }
    }

    private func isVisibleSchedule(_ event: SchoolEvent) -> Bool {
        let blockedKeywords = ["토요휴업일"]
        return blockedKeywords.allSatisfy { keyword in
            !event.title.contains(keyword) && !event.description.contains(keyword)
        }
    }
}
