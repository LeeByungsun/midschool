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
        VStack(alignment: .leading, spacing: 5) {
            header

            Text(snapshot.schoolLabel)
                .font(.caption.weight(.semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.78)

            if let tomorrow = snapshot.tomorrowTimetable {
                HStack(alignment: .top, spacing: 6) {
                    compactDayCard(title: "오늘", text: snapshot.todayTimetable)
                    compactDayCard(title: "내일", text: tomorrow)
                }
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
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Text(snapshot.headerDate)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.75)

            Spacer(minLength: 4)

            Text(snapshot.timerSummary)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.blue)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
    }

    private func timetableCard(title: String, text: String, lineLimit: Int?) -> some View {
        timetableBlock(title: title, text: text, lineLimit: lineLimit, compact: false)
            .padding(8)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func compactDayCard(title: String, text: String) -> some View {
        let lines = timetableLines(from: text)
        let visibleLines = Array(lines.prefix(3))
        let remainingCount = max(0, lines.count - visibleLines.count)

        return VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(.caption2.weight(.bold))
                .foregroundStyle(.secondary)
                .lineLimit(1)

            ForEach(Array(visibleLines.enumerated()), id: \.offset) { _, line in
                Text(line)
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.74)
            }

            if remainingCount > 0 {
                Text("외 \(remainingCount)개")
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
        }
        .padding(6)
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
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
}
