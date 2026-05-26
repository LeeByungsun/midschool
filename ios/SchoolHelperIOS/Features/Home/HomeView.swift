import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = HomeViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section("학생 정보") {
                    Text(appState.profile.schoolName.isEmpty ? "학교 미설정" : appState.profile.schoolName)
                    Text(appState.profile.isComplete ? "\(appState.profile.grade)학년 \(appState.profile.classroom)반" : "학년/반 미완료")
                        .foregroundStyle(.secondary)
                }
                Section("요약") {
                    Text(viewModel.todaySummary)
                    Text(viewModel.mealSummary)
                    Text(viewModel.eventSummary)
                    Text(viewModel.noticeSummary)
                }
            }
            .navigationTitle("학교도우미")
            .task {
                await viewModel.load(profile: appState.profile)
            }
        }
    }
}
