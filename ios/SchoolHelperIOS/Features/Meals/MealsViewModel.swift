import Foundation
import Combine

@MainActor
final class MealsViewModel: ObservableObject {
    @Published var weekTitle: String = ""
    @Published var statusText: String = ""
    @Published var items: [MealInfo] = []

    private let repository: SchoolRepository
    private let todayProvider: () -> Date
    private let calendar = Calendar(identifier: .gregorian)
    private var latestProfile = StudentProfile()
    private var referenceDate: Date

    init(
        repository: SchoolRepository = DefaultSchoolRepository(),
        todayProvider: @escaping () -> Date = { AppLaunchOverrides.referenceDate() ?? Date() }
    ) {
        self.repository = repository
        self.todayProvider = todayProvider
        self.referenceDate = todayProvider()
    }

    func load(profile: StudentProfile, referenceDate: Date? = nil) async {
        latestProfile = profile
        let effectiveReferenceDate = referenceDate ?? todayProvider()
        self.referenceDate = effectiveReferenceDate
        guard profile.isComplete else {
            items = []
            weekTitle = formatWeekTitle(start: Self.startOfSchoolWeek(containing: effectiveReferenceDate))
            statusText = "학교와 학년/반을 먼저 설정해 주세요."
            return
        }
        let weekStart = Self.startOfSchoolWeek(containing: effectiveReferenceDate)
        weekTitle = formatWeekTitle(start: weekStart)
        items = ((try? await repository.fetchWeekMeals(for: profile, weekStart: weekStart)) ?? [])
            .map { $0.withDisplayMenu() }
        statusText = items.isEmpty ? "선택한 주 급식이 없어요." : ""
    }

    func showPreviousWeek() async {
        referenceDate = calendar.date(byAdding: .day, value: -7, to: referenceDate) ?? referenceDate
        await load(profile: latestProfile, referenceDate: referenceDate)
    }

    func showCurrentWeek() async {
        referenceDate = todayProvider()
        await load(profile: latestProfile, referenceDate: referenceDate)
    }

    func showNextWeek() async {
        referenceDate = calendar.date(byAdding: .day, value: 7, to: referenceDate) ?? referenceDate
        await load(profile: latestProfile, referenceDate: referenceDate)
    }

    private static func startOfSchoolWeek(containing date: Date) -> Date {
        let calendar = Calendar(identifier: .gregorian)
        let startOfDay = calendar.startOfDay(for: date)
        let weekday = calendar.component(.weekday, from: startOfDay)
        let mondayOffset = (weekday + 5) % 7
        return calendar.date(byAdding: .day, value: -mondayOffset, to: startOfDay) ?? startOfDay
    }

    private func formatWeekTitle(start: Date) -> String {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "M월 d일"
        let end = calendar.date(byAdding: .day, value: 4, to: start) ?? start
        return "\(formatter.string(from: start)) - \(formatter.string(from: end))"
    }
}
