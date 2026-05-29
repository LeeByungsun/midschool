import Foundation

enum MealMenuFormatter {
    static func displayMenu(from rawMenu: String, emptyFallback: String = "급식 정보가 없어요.") -> String {
        let formatted = rawMenu
            .replacingOccurrences(
                of: "<br\\s*/?>",
                with: "\n",
                options: [.regularExpression, .caseInsensitive]
            )
            .replacingOccurrences(of: "&nbsp;", with: " ")
            .replacingOccurrences(of: "&amp;", with: "&")
            .replacingOccurrences(of: "&lt;", with: "<")
            .replacingOccurrences(of: "&gt;", with: ">")
            .replacingOccurrences(
                of: "<[^>]+>",
                with: "",
                options: [.regularExpression, .caseInsensitive]
            )
            .replacingOccurrences(
                of: "[ \\t]+",
                with: " ",
                options: .regularExpression
            )
            .components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .map(formatMealLine)
            .joined(separator: "\n")

        return formatted.isEmpty ? emptyFallback : formatted
    }

    private static func formatMealLine(_ line: String) -> String {
        let pattern = #"^(.*?)(\(([^)]*)\))?$"#
        guard
            let regex = try? NSRegularExpression(pattern: pattern),
            let match = regex.firstMatch(
                in: line,
                range: NSRange(location: 0, length: line.utf16.count)
            ),
            let nameRange = Range(match.range(at: 1), in: line)
        else {
            return line.trimmingCharacters(in: .whitespacesAndNewlines)
        }

        let name = line[nameRange].trimmingCharacters(in: .whitespacesAndNewlines)
        guard
            match.numberOfRanges > 3,
            let allergyRange = Range(match.range(at: 3), in: line)
        else {
            return name
        }

        let allergy = line[allergyRange].trimmingCharacters(in: .whitespacesAndNewlines)
        return allergy.isEmpty ? name : "\(name) (\(allergy))"
    }
}

extension MealInfo {
    var displayMenu: String {
        MealMenuFormatter.displayMenu(from: menu)
    }

    func withDisplayMenu() -> MealInfo {
        MealInfo(
            date: date,
            mealType: mealType,
            menu: displayMenu,
            calorieInfo: calorieInfo
        )
    }
}
