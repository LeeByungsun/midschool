import Foundation

struct MealInfo: Codable, Equatable, Identifiable {
    var id: String { "\(date)-\(mealType)" }

    var date: String
    var mealType: String
    var menu: String
    var calorieInfo: String
}
