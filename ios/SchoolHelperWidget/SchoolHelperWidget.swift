import SwiftUI
import AppIntents
import WidgetKit

struct SchoolHelperWidgetConfigurationIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "학교도우미 위젯 설정"
    static var description = IntentDescription("위젯에서 내일 시간표를 보여줄지 선택합니다.")

    @Parameter(title: "내일 시간표 표시", default: true)
    var showTomorrowTimetable: Bool
}

struct SchoolHelperWidgetEntry: TimelineEntry {
    let date: Date
    let snapshot: HomeWidgetSnapshot
}

struct SchoolHelperWidgetProvider: AppIntentTimelineProvider {
    typealias Intent = SchoolHelperWidgetConfigurationIntent
    private let loader = HomeWidgetSnapshotLoader()
    private let timerStore = TimerPreferencesStore()

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

    func snapshot(for configuration: SchoolHelperWidgetConfigurationIntent, in context: Context) async -> SchoolHelperWidgetEntry {
        let snapshot = await loader.load(showTomorrow: configuration.showTomorrowTimetable)
        return SchoolHelperWidgetEntry(date: Date(), snapshot: snapshot)
    }

    func timeline(for configuration: SchoolHelperWidgetConfigurationIntent, in context: Context) async -> Timeline<SchoolHelperWidgetEntry> {
        let now = Date()
        let snapshot = await loader.load(now: now, showTomorrow: configuration.showTomorrowTimetable)
        let entry = SchoolHelperWidgetEntry(date: now, snapshot: snapshot)
        let refreshDate = HomeWidgetTimelinePlanner.nextRefreshDate(
            now: now,
            timerState: timerStore.load()
        )
        return Timeline(entries: [entry], policy: .after(refreshDate))
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

@main
struct SchoolHelperWidget: Widget {
    let kind = "SchoolHelperWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: SchoolHelperWidgetConfigurationIntent.self,
            provider: SchoolHelperWidgetProvider()
        ) { entry in
            SchoolHelperWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("학교도우미")
        .description("오늘/내일 시간표와 타이머 요약을 보여줍니다.")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}
