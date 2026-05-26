import Foundation

struct SchoolInfo: Codable, Equatable, Identifiable {
    var id: String { "\(officeCode)-\(schoolCode)" }

    var officeCode: String
    var officeName: String
    var schoolCode: String
    var schoolName: String
    var schoolKind: String
    var roadAddress: String
}
