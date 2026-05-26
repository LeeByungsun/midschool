import Foundation

struct SchoolEvent: Codable, Equatable, Identifiable {
    var id: String { "\(date)-\(title)" }

    var date: String
    var title: String
    var description: String
}
