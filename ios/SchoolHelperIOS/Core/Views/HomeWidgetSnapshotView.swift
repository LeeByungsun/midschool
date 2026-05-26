import SwiftUI

struct HomeWidgetSnapshotView: View {
    let snapshot: HomeWidgetSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(snapshot.headerDate)
                .font(.caption)
                .foregroundStyle(.secondary)

            Text(snapshot.schoolLabel)
                .font(.headline)
                .lineLimit(2)

            Text(snapshot.timerSummary)
                .font(.subheadline)
                .foregroundStyle(.blue)

            Divider()

            VStack(alignment: .leading, spacing: 4) {
                Text("오늘")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text(snapshot.todayTimetable)
                    .font(.caption)
                    .lineLimit(4)
            }

            if let tomorrow = snapshot.tomorrowTimetable {
                Divider()
                VStack(alignment: .leading, spacing: 4) {
                    Text("내일")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Text(tomorrow)
                        .font(.caption)
                        .lineLimit(3)
                }
            }
        }
    }
}
