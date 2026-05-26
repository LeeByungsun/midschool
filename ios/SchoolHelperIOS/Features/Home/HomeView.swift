import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        NavigationStack {
            List {
                Section("학생 정보") {
                    Text(appState.profile.schoolName.isEmpty ? "학교 미설정" : appState.profile.schoolName)
                    Text(appState.profile.isComplete ? "\(appState.profile.grade)학년 \(appState.profile.classroom)반" : "학년/반 미완료")
                        .foregroundStyle(.secondary)
                }
                Section("요약") {
                    Text("오늘 시간표")
                    Text("오늘 급식")
                    Text("다가오는 일정")
                    Text("타이머")
                }
            }
            .navigationTitle("학교도우미")
        }
    }
}
