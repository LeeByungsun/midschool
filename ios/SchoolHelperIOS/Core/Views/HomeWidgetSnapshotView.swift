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
        VStack(alignment: .leading, spacing: 7) {
            header

            Text(snapshot.schoolLabel)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.78)

            timetableBlock(
                title: "오늘",
                text: snapshot.todayTimetable,
                lineLimit: 3,
                compact: true
            )

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
                    timetableCard(title: "오늘", text: snapshot.todayTimetable, lineLimit: 6)
                    timetableCard(title: "내일", text: tomorrow, lineLimit: 5)
                }
            } else {
                timetableCard(title: "오늘", text: snapshot.todayTimetable, lineLimit: 8)
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

    private func timetableCard(title: String, text: String, lineLimit: Int) -> some View {
        timetableBlock(title: title, text: text, lineLimit: lineLimit, compact: false)
            .padding(8)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func timetableBlock(title: String, text: String, lineLimit: Int, compact: Bool) -> some View {
        VStack(alignment: .leading, spacing: compact ? 3 : 5) {
            Text(title)
                .font(.caption2.weight(.bold))
                .foregroundStyle(.secondary)
                .lineLimit(1)

            Text(text)
                .font(compact ? .caption : .caption.weight(.medium))
                .foregroundStyle(.primary)
                .lineLimit(lineLimit)
                .fixedSize(horizontal: false, vertical: true)
                .minimumScaleFactor(0.86)
        }
    }
}
