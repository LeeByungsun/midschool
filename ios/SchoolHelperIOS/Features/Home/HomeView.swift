import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.openURL) private var openURL
    @StateObject private var viewModel = HomeViewModel()
    @StateObject private var timerViewModel = TimerViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section("학생 정보") {
                    Text(viewModel.dateLabel)
                        .font(.headline)
                    Text(appState.profile.schoolName.isEmpty ? "학교 미설정" : appState.profile.schoolName)
                    Text(appState.profile.isComplete ? "\(appState.profile.grade)학년 \(appState.profile.classroom)반" : "학년/반 미완료")
                        .foregroundStyle(.secondary)
                }

                Section("타이머") {
                    VStack(alignment: .leading, spacing: 12) {
                        timerDisplay

                        HStack(spacing: 8) {
                            ForEach(TimerPreset.allCases, id: \.self) { preset in
                                Button {
                                    timerViewModel.selectPreset(preset)
                                } label: {
                                    Text(preset.title)
                                        .font(.caption.weight(.semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(timerViewModel.state.preset == preset ? .blue : .gray.opacity(0.35))
                            }
                        }

                        HStack(spacing: 12) {
                            Button(timerViewModel.state.isRunning ? "일시정지" : "시작") {
                                timerViewModel.toggle()
                            }
                            .buttonStyle(.borderedProminent)

                            Button("리셋") {
                                timerViewModel.reset()
                            }
                            .buttonStyle(.bordered)

                            Spacer()

                            NavigationLink("타이머 전체 보기") {
                                TimerView()
                            }
                        }
                    }
                }

                Section("오늘 시간표") {
                    Text(viewModel.todaySummary)
                    NavigationLink("시간표 전체 보기") {
                        TimetableView()
                            .environmentObject(appState)
                    }
                }

                Section("오늘 급식") {
                    Text(viewModel.mealSummary)
                    if !viewModel.mealMeta.isEmpty {
                        Text(viewModel.mealMeta)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    NavigationLink("주간 급식 보기") {
                        MealsView()
                            .environmentObject(appState)
                    }
                }

                Section("다가오는 일정") {
                    Text(viewModel.eventSummary)
                    NavigationLink("일정 보기") {
                        ScheduleView()
                            .environmentObject(appState)
                    }
                }

                Section("가정통신문") {
                    Text(viewModel.noticeSummary)
                    Button(viewModel.noticeActionText) {
                        if let url = viewModel.latestNoticeDestination() {
                            openURL(url)
                        }
                    }
                    .disabled(!viewModel.noticeActionEnabled)
                }
            }
            .navigationTitle("학교도우미")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("새로고침") {
                        Task { await refreshHome() }
                    }
                }
            }
            .task(id: appState.profile) {
                await refreshHome()
            }
            .onAppear {
                timerViewModel.refreshRunningState()
            }
        }
    }

    private func refreshHome() async {
        await viewModel.load(profile: appState.profile)
        timerViewModel.refreshRunningState()
    }

    private var timerDisplay: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(timerViewModel.state.preset.title)
                .font(.headline)

            if timerViewModel.displayMode == .count {
                Text(timeText)
                    .font(.system(size: 34, weight: .bold, design: .rounded))
                    .monospacedDigit()
            } else {
                HStack(spacing: 16) {
                    ZStack {
                        Circle()
                            .stroke(Color.gray.opacity(0.2), lineWidth: 10)
                            .frame(width: 88, height: 88)
                        Circle()
                            .trim(from: 0, to: timerViewModel.progressFraction)
                            .stroke(Color.blue, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                            .rotationEffect(.degrees(-90))
                            .frame(width: 88, height: 88)
                        Text(timeText)
                            .font(.system(size: 18, weight: .bold, design: .rounded))
                            .monospacedDigit()
                    }

                    Text(timerViewModel.state.isRunning ? "남은 시간" : "준비 완료")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private var timeText: String {
        let minutes = timerViewModel.state.remainingSeconds / 60
        let seconds = timerViewModel.state.remainingSeconds % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}
