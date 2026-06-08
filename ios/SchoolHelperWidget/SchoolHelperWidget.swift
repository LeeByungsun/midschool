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
                todayTimetable: "1교시 국어\n2교시 수학\n3교시 영어\n4교시 과학\n5교시 도덕\n6교시 체육\n7교시 창체",
                tomorrowTimetable: "1교시 미술\n2교시 사회\n3교시 국어\n4교시 과학\n5교시 동아리\n6교시 도덕\n7교시 체육",
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
    @Environment(\.widgetFamily) private var family
    var entry: SchoolHelperWidgetEntry

    var body: some View {
        HomeWidgetSnapshotView(snapshot: entry.snapshot, mode: viewMode)
            .padding(contentPadding)
            .containerBackground(for: .widget) {
                Color(.secondarySystemBackground)
            }
            .widgetURL(entry.snapshot.requiresSetup
                ? URL(string: "schoolhelper://settings")
                : URL(string: "schoolhelper://timetable"))
    }

    private var viewMode: HomeWidgetSnapshotViewMode {
        switch family {
        case .systemSmall:
            return .small
        case .systemMedium:
            return .compact
        default:
            return .detailed
        }
    }

    private var contentPadding: CGFloat {
        switch family {
        case .systemSmall:
            return 8
        case .systemMedium:
            return 8
        default:
            return 14
        }
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
        .description("작은 위젯은 오늘 시간표, 중간/큰 위젯은 오늘/내일 시간표를 보여줍니다.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
