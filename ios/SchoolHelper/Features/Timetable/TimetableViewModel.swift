import Foundation
import Combine

@MainActor
final class TimetableViewModel: ObservableObject {
    @Published var date: Date
    @Published var dateTitle: String = ""
    @Published var statusText: String = ""
    @Published var items: [TimetableItem] = []
    @Published private(set) var hasLoadError = false

    private let repository: SchoolRepository
    private let todayProvider: () -> Date
    private let calendar = Calendar(identifier: .gregorian)
    private var latestProfile = StudentProfile()

    init(
        repository: SchoolRepository = DefaultSchoolRepository(),
        todayProvider: @escaping () -> Date = { AppLaunchOverrides.referenceDate() ?? Date() }
    ) {
        self.repository = repository
        self.todayProvider = todayProvider
        self.date = todayProvider()
        refreshDateTitle()
    }

    func load(profile: StudentProfile) async {
        latestProfile = profile
        refreshDateTitle()
        guard profile.isComplete else {
            items = []
            hasLoadError = false
            statusText = "학교와 학년/반을 먼저 설정해 주세요."
            return
        }
        do {
            items = try await repository.fetchTimetable(for: profile, date: date)
            hasLoadError = false
            statusText = items.isEmpty ? "해당 날짜 시간표가 없어요." : ""
        } catch {
            items = []
            hasLoadError = true
            statusText = "시간표를 불러오지 못했어요. 다시 시도해 주세요."
        }
    }

    func showPreviousDay() async {
        date = calendar.date(byAdding: .day, value: -1, to: date) ?? date
        await load(profile: latestProfile)
    }

    func showToday() async {
        date = todayProvider()
        await load(profile: latestProfile)
    }

    func showNextDay() async {
        date = calendar.date(byAdding: .day, value: 1, to: date) ?? date
        await load(profile: latestProfile)
    }

    func retry() async {
        await load(profile: latestProfile)
    }

    private func refreshDateTitle() {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "M월 d일 EEEE"
        dateTitle = formatter.string(from: date)
    }
}
