import Foundation
import Combine

@MainActor
final class TimetableViewModel: ObservableObject {
    @Published var date: Date = Date()
    @Published var items: [TimetableItem] = []

    private let repository: SchoolRepository

    init(repository: SchoolRepository = DefaultSchoolRepository()) {
        self.repository = repository
    }

    func load(profile: StudentProfile) async {
        guard profile.isComplete else {
            items = []
            return
        }
        items = (try? await repository.fetchTimetable(for: profile, date: date)) ?? []
    }
}
