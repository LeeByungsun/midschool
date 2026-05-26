import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel: SettingsViewModel

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

                Section("학교 검색") {
                    TextField("학교 이름", text: $viewModel.searchQuery)
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
                    TextField("학년", text: $viewModel.draftProfile.grade)
                    TextField("반", text: $viewModel.draftProfile.classroom)
                }

                Section {
                    Button("설정 저장") {
                        if let profile = viewModel.buildProfileForSave() {
                            appState.saveProfile(profile)
                        }
                    }
                }
            }
            .navigationTitle("설정")
            .task {
                viewModel.draftProfile = appState.profile
                viewModel.searchQuery = appState.profile.schoolName
                viewModel.selectedSchool = appState.profile.schoolInfo
            }
        }
    }
}
