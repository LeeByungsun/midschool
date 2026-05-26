import Foundation
import Combine

@MainActor
final class MealsViewModel: ObservableObject {
    @Published var items: [MealInfo] = []

    private let repository: SchoolRepository

    init(repository: SchoolRepository = DefaultSchoolRepository()) {
        self.repository = repository
    }

    func load(profile: StudentProfile, referenceDate: Date = Date()) async {
        guard profile.isComplete else {
            items = []
            return
        }
        let weekStart = Self.startOfSchoolWeek(containing: referenceDate)
        items = (try? await repository.fetchWeekMeals(for: profile, weekStart: weekStart)) ?? []
    }

    private static func startOfSchoolWeek(containing date: Date) -> Date {
        let calendar = Calendar(identifier: .gregorian)
        let startOfDay = calendar.startOfDay(for: date)
        let weekday = calendar.component(.weekday, from: startOfDay)
        let mondayOffset = (weekday + 5) % 7
        return calendar.date(byAdding: .day, value: -mondayOffset, to: startOfDay) ?? startOfDay
    }
}
