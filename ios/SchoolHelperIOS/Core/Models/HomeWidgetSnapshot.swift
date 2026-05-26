import Foundation

struct HomeWidgetSnapshot: Equatable {
    var headerDate: String
    var schoolLabel: String
    var timerSummary: String
    var todayTimetable: String
    var tomorrowTimetable: String?
    var requiresSetup: Bool
}
