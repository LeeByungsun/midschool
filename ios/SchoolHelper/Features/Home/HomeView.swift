import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.openURL) private var openURL
    @StateObject private var viewModel = HomeViewModel()
    @EnvironmentObject private var timerViewModel: TimerViewModel
    @State private var completionBlinkOn = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    profileAndSummaryCard

                    Button {
                        appState.selectedRoute = .meals
                    } label: {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("오늘 급식")
                                    .font(.headline.weight(.bold))
                                    .foregroundStyle(.primary)
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.footnote.weight(.semibold))
                                    .foregroundStyle(.secondary)
                            }
                            if !viewModel.mealMeta.isEmpty {
                                Text(viewModel.mealMeta)
                                    .font(.caption.weight(.semibold))
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color.green.opacity(0.12), in: Capsule())
                            }
                            Text(viewModel.mealSummary)
                                .font(.body)
                                .foregroundStyle(.secondary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .homeCard()
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("home-meals-section")

                    homeSummaryCard(
                        title: "학사 일정",
                        summary: viewModel.eventSummary,
                        actionTitle: "월간 보기",
                        identifier: "home-schedule-section"
                    ) {
                        appState.selectedRoute = .schedule
                    }

                    noticeCard

                    timerCard
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 24)
            }
            .background(Color(.systemGroupedBackground))
            .refreshable {
                await refreshHome()
            }
            .navigationTitle("스쿨온")
            .task(id: appState.profile) {
                await refreshHome()
            }
            .onAppear {
                timerViewModel.refreshRunningState()
            }
            .onChange(of: appState.settingsRevision) { _, _ in
                timerViewModel.refreshSettings()
            }
            .task(id: isTimerCompleted) {
                await updateCompletionBlink()
            }
        }
    }

    private var profileAndSummaryCard: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    profileSelector
                    Text(appState.profile.schoolName.isEmpty ? "학교를 설정해 주세요" : appState.profile.schoolName)
                        .font(.title2.bold())
                        .accessibilityIdentifier("home-school-name")
                    Text(viewModel.dateLabel)
                        .font(.subheadline)
                    Text(appState.profile.isComplete ? "\(appState.profile.grade)학년 \(appState.profile.classroom)반" : "학년과 반을 설정해 주세요")
                        .font(.footnote)
                }
                .foregroundStyle(.white)

                Spacer(minLength: 8)

                Button {
                    appState.selectedRoute = .settings
                } label: {
                    Image(systemName: "gearshape")
                        .font(.system(size: 19, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 48, height: 48)
                        .contentShape(Rectangle())
                }
                .accessibilityLabel("설정 열기")
            }

            VStack(alignment: .leading, spacing: 10) {
                Text("오늘의 학교생활")
                    .font(.headline.bold())
                    .foregroundStyle(.white)
                    .accessibilityIdentifier("home-today-summary")
                Text(viewModel.todaySummary)
                    .font(.subheadline)
                    .foregroundStyle(Color.white.opacity(0.9))
                    .lineLimit(2)
                Button {
                    appState.selectedRoute = .timetable
                } label: {
                    HStack(spacing: 8) {
                        Text("시간표 보러 가기")
                            .font(.subheadline.weight(.bold))
                        Image(systemName: "arrow.right")
                            .font(.caption.weight(.bold))
                    }
                    .foregroundStyle(Color(red: 44 / 255, green: 109 / 255, blue: 176 / 255))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 11)
                    .background(.white, in: Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("home-timetable-action")
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(18)
            .background(Color(red: 59 / 255, green: 128 / 255, blue: 196 / 255), in: RoundedRectangle(cornerRadius: 20))
        }
        .padding(20)
        .background(Color(red: 44 / 255, green: 109 / 255, blue: 176 / 255), in: RoundedRectangle(cornerRadius: 28))
    }

    @ViewBuilder
    private var profileSelector: some View {
        if appState.profiles.count > 1 {
            Menu {
                ForEach(appState.profiles, id: \.id) { profile in
                    Button {
                        appState.selectProfile(id: profile.id)
                    } label: {
                        if profile.id == appState.profile.id {
                            Label(profile.displayName, systemImage: "checkmark")
                        } else {
                            Text(profile.displayName)
                        }
                    }
                }
            } label: {
                Label(appState.profile.displayName.isEmpty ? "자녀 선택" : appState.profile.displayName, systemImage: "person.2")
                    .font(.subheadline.weight(.semibold))
            }
            .accessibilityIdentifier("home-profile-switcher")
        } else {
            Label(appState.profile.displayName.isEmpty ? "자녀 이름 미설정" : appState.profile.displayName, systemImage: "person")
                .font(.subheadline.weight(.semibold))
        }
    }

    private func homeSummaryCard(
        title: String,
        summary: String,
        actionTitle: String,
        identifier: String,
        action: @escaping () -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                Text(title)
                    .font(.headline.weight(.bold))
                    .accessibilityIdentifier(identifier)
                Spacer(minLength: 4)
                Button(actionTitle, action: action)
                    .font(.subheadline.weight(.semibold))
                    .buttonStyle(.bordered)
                    .tint(Color(red: 23 / 255, green: 59 / 255, blue: 102 / 255))
            }
            Text(summary)
                .font(.body)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .homeCard()
    }

    private var noticeCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                Text("가정통신문")
                    .font(.headline.weight(.bold))
                    .accessibilityIdentifier("home-notices-section")
                Spacer(minLength: 4)
                Button(noticeActionTitle) {
                    if let url = viewModel.latestNoticeDestination() {
                        openURL(url)
                    } else if viewModel.noticeRequiresSetup {
                        appState.selectedRoute = .settings
                    }
                }
                .font(.subheadline.weight(.semibold))
                .buttonStyle(.bordered)
                .tint(Color(red: 23 / 255, green: 59 / 255, blue: 102 / 255))
                .disabled(!viewModel.noticeActionEnabled)
            }
            Text(viewModel.noticeSummary)
                .font(.body)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .homeCard()
    }

    private var timerCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("집중력 타이머")
                .font(.headline.weight(.bold))
                .accessibilityIdentifier("home-timer-section")
            TimerControls(viewModel: timerViewModel, ringSize: 220)
        }
        .homeCard(background: timerCompletionRowBackground)
    }

    private var noticeActionTitle: String {
        if viewModel.noticeRequiresSetup {
            return "학교 설정하러 가기"
        }
        return viewModel.noticeActionEnabled ? "목록 열기" : viewModel.noticeActionText
    }

    private func refreshHome() async {
        await viewModel.load(profile: appState.profile)
        timerViewModel.refreshRunningState()
    }

    private var isTimerCompleted: Bool {
        timerViewModel.state.pomodoro?.isCompleted ?? (!timerViewModel.state.isRunning && timerViewModel.state.remainingSeconds == 0)
    }

    private var timerCompletionRowBackground: Color {
        if isTimerCompleted && completionBlinkOn {
            return Color.orange.opacity(0.22)
        }
        return Color(.secondarySystemGroupedBackground)
    }

    private func updateCompletionBlink() async {
        guard isTimerCompleted else {
            completionBlinkOn = false
            return
        }

        completionBlinkOn = true
        while !Task.isCancelled {
            try? await Task.sleep(nanoseconds: 550_000_000)
            if Task.isCancelled { break }
            completionBlinkOn.toggle()
        }
    }


}

private extension View {
    func homeCard(background: Color = Color(.secondarySystemGroupedBackground)) -> some View {
        padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(background, in: RoundedRectangle(cornerRadius: 24))
            .overlay {
                RoundedRectangle(cornerRadius: 24)
                    .stroke(Color(.separator), lineWidth: 1)
            }
    }
}
