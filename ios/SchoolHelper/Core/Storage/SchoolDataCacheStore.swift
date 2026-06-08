import Foundation

final class SchoolDataCacheStore {
    private struct CacheEntry<Value: Codable>: Codable {
        let savedAt: Date
        let value: [Value]
    }

    private let defaults: UserDefaults
    private let now: () -> Date

    private static let mealTTL: TimeInterval = 12 * 60 * 60
    private static let timetableTTL: TimeInterval = 24 * 60 * 60
    private static let scheduleTTL: TimeInterval = 12 * 60 * 60

    init(
        defaults: UserDefaults = AppStorageConfig.userDefaults(),
        now: @escaping () -> Date = Date.init
    ) {
        self.defaults = defaults
        self.now = now
    }

    func saveMeals(_ meals: [MealInfo], officeCode: String, schoolCode: String, date: String) {
        save(meals, key: mealKey(officeCode: officeCode, schoolCode: schoolCode, date: date))
    }

    func getMeals(officeCode: String, schoolCode: String, date: String) -> [MealInfo]? {
        load(key: mealKey(officeCode: officeCode, schoolCode: schoolCode, date: date), ttl: Self.mealTTL)
    }

    func saveTimetable(
        _ items: [TimetableItem],
        officeCode: String,
        schoolCode: String,
        grade: String,
        classroom: String,
        date: String
    ) {
        save(items, key: timetableKey(officeCode: officeCode, schoolCode: schoolCode, grade: grade, classroom: classroom, date: date))
    }

    func getTimetable(
        officeCode: String,
        schoolCode: String,
        grade: String,
        classroom: String,
        date: String
    ) -> [TimetableItem]? {
        load(key: timetableKey(officeCode: officeCode, schoolCode: schoolCode, grade: grade, classroom: classroom, date: date), ttl: Self.timetableTTL)
    }

    func saveSchedule(_ events: [SchoolEvent], officeCode: String, schoolCode: String, month: String) {
        save(events, key: scheduleKey(officeCode: officeCode, schoolCode: schoolCode, month: month))
    }

    func getSchedule(officeCode: String, schoolCode: String, month: String) -> [SchoolEvent]? {
        load(key: scheduleKey(officeCode: officeCode, schoolCode: schoolCode, month: month), ttl: Self.scheduleTTL)
    }

    private func save<Value: Codable>(_ value: [Value], key: String) {
        let entry = CacheEntry(savedAt: now(), value: value)
        guard let data = try? JSONEncoder().encode(entry) else { return }
        defaults.set(data, forKey: key)
    }

    private func load<Value: Codable>(key: String, ttl: TimeInterval) -> [Value]? {
        guard
            let data = defaults.data(forKey: key),
            let entry = try? JSONDecoder().decode(CacheEntry<Value>.self, from: data)
        else {
            return nil
        }

        guard now().timeIntervalSince(entry.savedAt) <= ttl else {
            defaults.removeObject(forKey: key)
            return nil
        }

        return entry.value
    }

    private func mealKey(officeCode: String, schoolCode: String, date: String) -> String {
        key("meal", officeCode, schoolCode, date)
    }

    private func timetableKey(officeCode: String, schoolCode: String, grade: String, classroom: String, date: String) -> String {
        key("timetable", officeCode, schoolCode, grade, classroom, date)
    }

    private func scheduleKey(officeCode: String, schoolCode: String, month: String) -> String {
        key("schedule", officeCode, schoolCode, month)
    }

    private func key(_ components: String...) -> String {
        (["school_data_cache"] + components).joined(separator: "|")
    }
}
