import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.openURL) private var openURL
    @StateObject private var viewModel = HomeViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section("학생 정보") {
                    Text(appState.profile.schoolName.isEmpty ? "학교 미설정" : appState.profile.schoolName)
                    Text(appState.profile.isComplete ? "\(appState.profile.grade)학년 \(appState.profile.classroom)반" : "학년/반 미완료")
                        .foregroundStyle(.secondary)
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
                        Task { await viewModel.load(profile: appState.profile) }
                    }
                }
            }
            .task {
                await viewModel.load(profile: appState.profile)
            }
        }
    }
}
