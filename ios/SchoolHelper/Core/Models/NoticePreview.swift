import Foundation

struct NoticePreview: Codable, Equatable, Identifiable {
    var id: String
    var title: String
    var date: String
    var author: String
    var url: String
    var sourceUrl: String? = nil
}
