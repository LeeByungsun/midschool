import SwiftUI

enum HomeWidgetSnapshotViewMode {
    case compact
    case detailed
    case preview
}

struct HomeWidgetSnapshotView: View {
    let snapshot: HomeWidgetSnapshot
    var mode: HomeWidgetSnapshotViewMode = .preview

    var body: some View {
        switch mode {
        case .compact:
            compactBody
        case .detailed, .preview:
            detailedBody
        }
    }

    private var compactBody: some View {
        VStack(alignment: .leading, spacing: 3) {
            header

            Text(snapshot.schoolLabel)
                .font(.caption.weight(.semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.78)

            if let tomorrow = snapshot.tomorrowTimetable {
                compactTwoDayTimetableGrid(
                    todayText: snapshot.todayTimetable,
                    tomorrowText: tomorrow
                )
            } else {
                compactTimetableBlock(
                    title: "오늘",
                    text: snapshot.todayTimetable
                )
            }

            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private var detailedBody: some View {
        VStack(alignment: .leading, spacing: 8) {
            header

            Text(snapshot.schoolLabel)
                .font(.headline)
                .lineLimit(2)
                .minimumScaleFactor(0.82)

            if let tomorrow = snapshot.tomorrowTimetable {
                HStack(alignment: .top, spacing: 8) {
                    timetableCard(title: "오늘", text: snapshot.todayTimetable, lineLimit: nil)
                    timetableCard(title: "내일", text: tomorrow, lineLimit: nil)
                }
            } else {
                timetableCard(title: "오늘", text: snapshot.todayTimetable, lineLimit: nil)
            }

            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private var header: some View {
        Text(snapshot.headerDate)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(.secondary)
            .lineLimit(1)
            .minimumScaleFactor(0.75)
    }

    private func timetableCard(title: String, text: String, lineLimit: Int?) -> some View {
        timetableBlock(title: title, text: text, lineLimit: lineLimit, compact: false)
            .padding(8)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func compactTimetableBlock(title: String, text: String) -> some View {
        let lines = timetableLines(from: text)
        let splitIndex = compactSplitIndex(for: lines)

        return VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(.caption2.weight(.bold))
                .foregroundStyle(.secondary)
                .lineLimit(1)

            if lines.count > 5 {
                HStack(alignment: .top, spacing: 8) {
                    compactTimetableColumn(Array(lines.prefix(splitIndex)))
                    compactTimetableColumn(Array(lines.dropFirst(splitIndex)))
                }
            } else {
                compactTimetableColumn(lines)
            }
        }
    }

    private func compactTimetableColumn(_ lines: [String]) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                Text(line)
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.68)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func compactTwoDayTimetableGrid(todayText: String, tomorrowText: String) -> some View {
        let todaySubjects = timetableSubjectMap(from: todayText)
        let tomorrowSubjects = timetableSubjectMap(from: tomorrowText)

        return VStack(alignment: .leading, spacing: 1) {
            HStack(spacing: 4) {
                Text("")
                    .frame(width: 13)
                compactGridHeader("오늘")
                compactGridHeader("내일")
            }

            ForEach(1...7, id: \.self) { period in
                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    Text("\(period)")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                        .frame(width: 13, alignment: .trailing)

                    compactGridSubject(todaySubjects[period])
                    compactGridSubject(tomorrowSubjects[period])
                }
            }
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 5)
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private func compactGridHeader(_ title: String) -> some View {
        Text(title)
            .font(.caption2.weight(.bold))
            .foregroundStyle(.secondary)
            .lineLimit(1)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func compactGridSubject(_ subject: String?) -> some View {
        Text(compactSubjectLabel(subject))
            .font(.caption2.weight(.semibold))
            .foregroundStyle(subject == nil ? .secondary : .primary)
            .lineLimit(1)
            .minimumScaleFactor(0.7)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func timetableBlock(title: String, text: String, lineLimit: Int?, compact: Bool) -> some View {
        VStack(alignment: .leading, spacing: compact ? 3 : 5) {
            Text(title)
                .font(.caption2.weight(.bold))
                .foregroundStyle(.secondary)
                .lineLimit(1)

            Text(text)
                .font(compact ? .caption2.weight(.medium) : .caption.weight(.medium))
                .foregroundStyle(.primary)
                .lineLimit(lineLimit)
                .fixedSize(horizontal: false, vertical: true)
                .minimumScaleFactor(compact ? 0.78 : 0.86)
        }
    }

    private func timetableLines(from text: String) -> [String] {
        let lines = text
            .components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return lines.isEmpty ? [text] : lines
    }

    private func compactSplitIndex(for lines: [String]) -> Int {
        max(1, Int(ceil(Double(lines.count) / 2.0)))
    }

    private func timetableSubjectMap(from text: String) -> [Int: String] {
        Dictionary(
            timetableLines(from: text).compactMap { line in
                guard let lesson = parseTimetableLine(line) else { return nil }
                return (lesson.period, lesson.subject)
            },
            uniquingKeysWith: { first, _ in first }
        )
    }

    private func parseTimetableLine(_ line: String) -> (period: Int, subject: String)? {
        let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let range = trimmed.range(of: "교시") else { return nil }

        let periodText = trimmed[..<range.lowerBound]
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let subject = trimmed[range.upperBound...]
            .trimmingCharacters(in: .whitespacesAndNewlines)

        guard let period = Int(periodText), !subject.isEmpty else { return nil }
        return (period, subject)
    }

    private func compactSubjectLabel(_ subject: String?) -> String {
        guard let subject = subject?.trimmingCharacters(in: .whitespacesAndNewlines),
              !subject.isEmpty
        else {
            return "—"
        }

        let normalized = subject
            .replacingOccurrences(of: "(창)", with: "")
            .replacingOccurrences(of: "활동", with: "")

        if normalized.contains("창의적체험") { return "창체" }
        if normalized.contains("동아리") { return "동아리" }
        if normalized.contains("현장체험") { return "체험" }
        if normalized.contains("스포츠") { return "스포츠" }

        return normalized.count > 4 ? String(normalized.prefix(4)) : normalized
    }
}
