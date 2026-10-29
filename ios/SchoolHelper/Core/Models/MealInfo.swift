import Foundation

struct MealInfo: Codable, Equatable, Identifiable {
    var id: String { "\(date)-\(mealType)" }

    var date: String
    var mealType: String
    var menu: String
    var calorieInfo: String
    func isOnDate(_ referenceDate: Date, calendar: Calendar = Calendar(identifier: .gregorian)) -> Bool {
        let components = calendar.dateComponents([.year, .month, .day], from: referenceDate)
        guard let year = components.year, let month = components.month, let day = components.day else { return false }
        return date == String(format: "%04d%02d%02d", year, month, day)
    }
}
