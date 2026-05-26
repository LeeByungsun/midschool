import Foundation

struct HomeWidgetSnapshotLoader {
    private let profileStore: StudentPreferencesStore
    private let timerStore: TimerPreferencesStore
    private let widgetSettingsStore: WidgetSettingsStore
    private let repository: SchoolRepository
    private let calendar: Calendar

    init(
        profileStore: StudentPreferencesStore = StudentPreferencesStore(),
        timerStore: TimerPreferencesStore = TimerPreferencesStore(),
        widgetSettingsStore: WidgetSettingsStore = WidgetSettingsStore(),
        repository: SchoolRepository = DefaultSchoolRepository(),
        calendar: Calendar = Calendar(identifier: .gregorian)
    ) {
        self.profileStore = profileStore
        self.timerStore = timerStore
        self.widgetSettingsStore = widgetSettingsStore
        self.repository = repository
        self.calendar = calendar
    }

    func load(now: Date = Date(), showTomorrow: Bool? = nil) async -> HomeWidgetSnapshot {
        let profile = profileStore.load()
        let timerState = timerStore.load()
        let widgetSettings = widgetSettingsStore.load()
        let shouldShowTomorrow = showTomorrow ?? widgetSettings.showTomorrowTimetable
        let schoolLabel = profile.isComplete
            ? "\(profile.schoolName) \(profile.grade)학년 \(profile.classroom)반"
            : "학교와 학년/반 설정 필요"

        let headerDate = Self.headerDateText(for: now, calendar: calendar)
        let timerSummary = Self.timerSummary(from: timerState)

        guard profile.isComplete else {
            return HomeWidgetSnapshot(
                headerDate: headerDate,
                schoolLabel: schoolLabel,
                timerSummary: timerSummary,
                todayTimetable: "시간표를 보려면 설정을 완료해 주세요.",
                tomorrowTimetable: shouldShowTomorrow ? "시간표를 보려면 설정을 완료해 주세요." : nil,
                requiresSetup: true
            )
        }

        let todayLines = (try? await repository.fetchTimetable(for: profile, date: now)).orEmpty
        let tomorrowDate = calendar.date(byAdding: .day, value: 1, to: now) ?? now
        let tomorrowLines = shouldShowTomorrow
            ? (try? await repository.fetchTimetable(for: profile, date: tomorrowDate)).orEmpty
            : []

        return HomeWidgetSnapshot(
            headerDate: headerDate,
            schoolLabel: schoolLabel,
            timerSummary: timerSummary,
            todayTimetable: Self.timetableSummary(from: todayLines, emptyMessage: "오늘 수업이 없어요."),
            tomorrowTimetable: shouldShowTomorrow
                ? Self.timetableSummary(from: tomorrowLines, emptyMessage: "내일 수업이 없어요.")
                : nil,
            requiresSetup: false
        )
    }

    private static func headerDateText(for date: Date, calendar: Calendar) -> String {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "M월 d일 (E)"
        return "📅 \(formatter.string(from: date))"
    }

    private static func timerSummary(from state: TimerSessionState) -> String {
        let minutes = max(0, state.remainingSeconds) / 60
        let seconds = max(0, state.remainingSeconds) % 60
        let timeText = String(format: "%02d:%02d", minutes, seconds)
        return state.isRunning
            ? "\(state.preset.title) • \(timeText) 남음"
            : "\(state.preset.title) • \(timeText)"
    }

    private static func timetableSummary(from items: [TimetableItem], emptyMessage: String) -> String {
        let lines = items
            .sorted {
                (Int($0.period) ?? Int.max) < (Int($1.period) ?? Int.max)
            }
            .compactMap { item -> String? in
                let subject = item.subject.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !subject.isEmpty else { return nil }
                return "\(item.period)교시 \(subject)"
            }

        return lines.isEmpty ? emptyMessage : lines.joined(separator: "\n")
    }
}

private extension Optional where Wrapped == [TimetableItem] {
    var orEmpty: [TimetableItem] { self ?? [] }
}
