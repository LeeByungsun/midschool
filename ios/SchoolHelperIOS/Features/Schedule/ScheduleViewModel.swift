import Foundation

@MainActor
final class ScheduleViewModel: ObservableObject {
    @Published var items: [SchoolEvent] = []

    private let repository: SchoolRepository

    init(repository: SchoolRepository = MockSchoolRepository()) {
        self.repository = repository
    }

    func load(profile: StudentProfile, month: Date = Date()) async {
        guard profile.isComplete else {
            items = []
            return
        }
        items = (try? await repository.fetchSchedule(for: profile, month: month)) ?? []
    }
}
