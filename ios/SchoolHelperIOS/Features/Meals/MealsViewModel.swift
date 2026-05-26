import Foundation

@MainActor
final class MealsViewModel: ObservableObject {
    @Published var items: [MealInfo] = []

    private let repository: SchoolRepository

    init(repository: SchoolRepository = MockSchoolRepository()) {
        self.repository = repository
    }

    func load(profile: StudentProfile, weekStart: Date = Date()) async {
        guard profile.isComplete else {
            items = []
            return
        }
        items = (try? await repository.fetchWeekMeals(for: profile, weekStart: weekStart)) ?? []
    }
}
