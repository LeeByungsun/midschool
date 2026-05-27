import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel: SettingsViewModel
    @State private var widgetPreview: HomeWidgetSnapshot?
    @State private var isLoadingWidgetPreview = false
    private let widgetPreviewLoader = HomeWidgetSnapshotLoader()

    init() {
        _viewModel = StateObject(wrappedValue: SettingsViewModel(initialProfile: StudentProfile()))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("현재 설정") {
                    Text(appState.profile.schoolName.isEmpty ? "학교 미설정" : appState.profile.schoolName)
                    Text(appState.profile.isComplete ? "\(appState.profile.grade)학년 \(appState.profile.classroom)반" : "설정 미완료")
                        .foregroundStyle(.secondary)
                }

                Section("위젯 미리보기") {
                    if let widgetPreview {
                        WidgetPreviewCard(snapshot: widgetPreview)
                    } else if isLoadingWidgetPreview {
                        ProgressView("위젯 미리보기를 불러오는 중…")
                    } else {
                        Text("위젯 미리보기를 준비하지 못했어요.")
                            .foregroundStyle(.secondary)
                    }
                }

                Section("위젯 설정") {
                    Toggle("내일 시간표 표시", isOn: $viewModel.showTomorrowTimetable)
                }

                Section("학교 검색") {
                    TextField("학교 이름", text: Binding(
                        get: { viewModel.searchQuery },
                        set: { viewModel.updateSchoolQuery($0) }
                    ))
                    Button(viewModel.isSearching ? "검색 중..." : "학교 검색") {
                        Task { await viewModel.searchSchools() }
                    }
                    .disabled(viewModel.isSearching)

                    if !viewModel.message.isEmpty {
                        Text(viewModel.message)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }

                    ForEach(viewModel.searchResults) { school in
                        Button {
                            viewModel.selectSchool(school)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(school.schoolName)
                                Text("\(school.schoolKind) • \(school.officeName)")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                Section("학생 정보") {
                    TextField("학년", text: Binding(
                        get: { viewModel.draftProfile.grade },
                        set: { viewModel.updateGrade($0) }
                    ))
                    TextField("반", text: Binding(
                        get: { viewModel.draftProfile.classroom },
                        set: { viewModel.updateClassroom($0) }
                    ))
                }

                Section("타이머 설정") {
                    Picker("표시 모드", selection: $viewModel.timerDisplayMode) {
                        ForEach(TimerDisplayMode.allCases, id: \.self) { mode in
                            Text(mode.title).tag(mode)
                        }
                    }

                    Toggle("알림음 사용", isOn: $viewModel.notificationEnabled)
                    Toggle("진동 사용", isOn: $viewModel.vibrationEnabled)
                        .disabled(!viewModel.notificationEnabled)
                }

                Section("알림 권한") {
                    Text(viewModel.notificationPermissionSummary)
                        .font(.footnote)
                        .foregroundStyle(.secondary)

                    if viewModel.canRequestNotificationPermission {
                        Button("알림 권한 요청") {
                            Task {
                                await viewModel.requestNotificationPermission()
                            }
                        }
                    }
                }

                Section {
                    Button("설정 저장") {
                        if let profile = viewModel.buildProfileForSave() {
                            viewModel.saveTimerSettings()
                            appState.saveProfile(profile)
                            dismiss()
                        }
                    }
                }
            }
            .navigationTitle("설정")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("닫기") {
                        dismiss()
                    }
                }
            }
            .task {
                viewModel.sync(with: appState.profile)
                await viewModel.refreshNotificationPermission()
            }
            .task(id: widgetPreviewTaskKey) {
                await loadWidgetPreview()
            }
        }
    }

    private var widgetPreviewTaskKey: String {
        [
            appState.profile.schoolCode,
            appState.profile.grade,
            appState.profile.classroom,
            viewModel.showTomorrowTimetable ? "1" : "0"
        ].joined(separator: "|")
    }

    private func loadWidgetPreview() async {
        isLoadingWidgetPreview = true
        let snapshot = await widgetPreviewLoader.load(showTomorrow: viewModel.showTomorrowTimetable)
        widgetPreview = snapshot
        isLoadingWidgetPreview = false
    }
}

private struct WidgetPreviewCard: View {
    let snapshot: HomeWidgetSnapshot

    var body: some View {
        HomeWidgetSnapshotView(snapshot: snapshot)
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color(.secondarySystemBackground))
        )
    }
}
