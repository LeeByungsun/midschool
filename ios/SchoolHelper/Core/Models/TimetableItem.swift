import Foundation

struct TimetableItem: Codable, Equatable, Identifiable {
    var id: String { "\(date)-\(period)-\(classroom)" }

    var date: String
    var period: String
    var subject: String
    var grade: String
    var classroom: String
}
