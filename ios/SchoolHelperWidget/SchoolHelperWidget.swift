import SwiftUI
import WidgetKit

struct SchoolHelperWidgetEntry: TimelineEntry {
    let date: Date
    let snapshot: HomeWidgetSnapshot
}

struct SchoolHelperWidgetProvider: TimelineProvider {
    private let loader = HomeWidgetSnapshotLoader()

    func placeholder(in context: Context) -> SchoolHelperWidgetEntry {
        SchoolHelperWidgetEntry(
            date: Date(),
            snapshot: HomeWidgetSnapshot(
                headerDate: "📅 5월 26일 (화)",
                schoolLabel: "미사중학교 1학년 2반",
                timerSummary: "집중 • 25:00",
                todayTimetable: "1교시 국어\n2교시 수학",
                tomorrowTimetable: "1교시 영어",
                requiresSetup: false
            )
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (SchoolHelperWidgetEntry) -> Void) {
        Task {
            let snapshot = await loader.load()
            completion(SchoolHelperWidgetEntry(date: Date(), snapshot: snapshot))
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SchoolHelperWidgetEntry>) -> Void) {
        Task {
            let snapshot = await loader.load()
            let entry = SchoolHelperWidgetEntry(date: Date(), snapshot: snapshot)
            let refreshDate = Calendar.current.date(byAdding: .minute, value: 30, to: Date()) ?? Date().addingTimeInterval(1800)
            completion(Timeline(entries: [entry], policy: .after(refreshDate)))
        }
    }
}

struct SchoolHelperWidgetEntryView: View {
    var entry: SchoolHelperWidgetEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(entry.snapshot.headerDate)
                .font(.caption)
                .foregroundStyle(.secondary)

            Text(entry.snapshot.schoolLabel)
                .font(.headline)
                .lineLimit(2)

            Text(entry.snapshot.timerSummary)
                .font(.subheadline)
                .foregroundStyle(.blue)

            Divider()

            VStack(alignment: .leading, spacing: 4) {
                Text("오늘")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text(entry.snapshot.todayTimetable)
                    .font(.caption)
                    .lineLimit(4)
            }

            if let tomorrow = entry.snapshot.tomorrowTimetable {
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
        .padding()
    }
}

struct SchoolHelperWidget: Widget {
    let kind = "SchoolHelperWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SchoolHelperWidgetProvider()) { entry in
            SchoolHelperWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("학교도우미")
        .description("오늘/내일 시간표와 타이머 요약을 보여줍니다.")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}
