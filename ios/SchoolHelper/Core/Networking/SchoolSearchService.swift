import Foundation

protocol SchoolSearchService {
    func searchSchools(query: String) async throws -> [SchoolInfo]
}
