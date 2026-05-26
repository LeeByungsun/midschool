import SwiftUI

struct SetupView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel: SetupViewModel

    init() {
        _viewModel = StateObject(wrappedValue: SetupViewModel(initialProfile: StudentProfile()))
    }

    var body: some View {
        NavigationStack {
            Form {
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

                Section {
                    Button("저장하고 시작하기") {
                        if let profile = viewModel.buildProfileForSave() {
                            appState.saveProfile(profile)
                        }
                    }
                }
            }
            .navigationTitle("초기 설정")
        }
    }
}
