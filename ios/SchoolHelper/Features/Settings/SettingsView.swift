import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel: SettingsViewModel
    @State private var widgetPreview: HomeWidgetSnapshot?
    @State private var isLoadingWidgetPreview = false
    @State private var isAddingProfile = false
    @State private var isConfirmingProfileDeletion = false
    private let widgetPreviewLoader = HomeWidgetSnapshotLoader()

    init() {
        _viewModel = StateObject(wrappedValue: SettingsViewModel(initialProfile: StudentProfile()))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("현재 설정") {
                    Text(appState.profile.displayName.isEmpty ? "자녀 이름 미설정" : appState.profile.displayName)
                    Text(appState.profile.schoolName.isEmpty ? "학교 미설정" : appState.profile.schoolName)
                    Text(appState.profile.isComplete ? "\(appState.profile.grade)학년 \(appState.profile.classroom)반" : "설정 미완료")
                        .foregroundStyle(.secondary)
                }

                Section("자녀 관리") {
                    Button("자녀 추가") {
                        isAddingProfile = true
                    }
                    if appState.profiles.count > 1 {
                        Button("현재 자녀 삭제", role: .destructive) {
                            isConfirmingProfileDeletion = true
                        }
                    }
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
                    .submitLabel(.search)
                    .onSubmit {
                        Task { await viewModel.searchSchools() }
                    }
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
                                if !school.roadAddress.isEmpty {
                                    Text(school.roadAddress)
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                }

                if let selectedSchool = viewModel.selectedSchool {
                    Section("선택된 학교") {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(selectedSchool.schoolName)
                                .font(.headline)
                            Text("\(selectedSchool.schoolKind) • \(selectedSchool.officeName)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            if !selectedSchool.roadAddress.isEmpty {
                                Text(selectedSchool.roadAddress)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .accessibilityIdentifier("settings-selected-school-summary")
                    }
                }

                Section("학생 정보") {
                    TextField("자녀 이름 (10자 이하)", text: Binding(
                        get: { viewModel.draftProfile.displayName },
                        set: { viewModel.updateDisplayName($0) }
                    ))
                    .textContentType(.name)
                    TextField("학년", text: Binding(
                        get: { viewModel.draftProfile.grade },
                        set: { viewModel.updateGrade($0) }
                    ))
                    .keyboardType(.numberPad)
                    TextField("반", text: Binding(
                        get: { viewModel.draftProfile.classroom },
                        set: { viewModel.updateClassroom($0) }
                    ))
                    .keyboardType(.numberPad)
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

                Section("개인정보 및 진단") {
                    Text("오류 진단 정보 제공은 선택 사항이며, 동의하지 않아도 앱을 사용할 수 있어요. 끄면 이후 오류 보고 수집을 중단합니다.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    Link("개인정보처리방침 보기", destination: AppLegalResources.privacyPolicyURL)
                    Toggle("[선택] 오류 진단 정보 제공에 동의", isOn: $viewModel.diagnosticsEnabled)
                }

                Section("앱 정보") {
                    NavigationLink("오픈소스 라이선스") {
                        OpenSourceLicensesView()
                    }
                    LabeledContent("버전", value: AppVersionLabel.current())
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

                ToolbarItem(placement: .topBarTrailing) {
                    Button("저장") {
                        saveSettings()
                    }
                    .accessibilityIdentifier("settings-save-button")
                }
            }
            .task {
                viewModel.sync(with: appState.profile)
                await viewModel.refreshNotificationPermission()
            }
            .task(id: widgetPreviewTaskKey) {
                await loadWidgetPreview()
            }
            .task(id: viewModel.searchQuery) {
                await viewModel.searchSchoolsAfterDebounce()
            }
            .onChange(of: appState.profile) { _, profile in
                viewModel.sync(with: profile)
            }
            .sheet(isPresented: $isAddingProfile) {
                SetupView(isAddingProfile: true)
                    .environmentObject(appState)
            }
            .confirmationDialog(
                "현재 자녀를 삭제할까요?",
                isPresented: $isConfirmingProfileDeletion,
                titleVisibility: .visible
            ) {
                Button("삭제", role: .destructive) {
                    _ = appState.deleteProfile(id: appState.profile.id)
                }
                Button("취소", role: .cancel) {}
            } message: {
                Text("삭제하면 이 자녀의 학교·학년·반 설정을 되돌릴 수 없어요.")
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

    private func saveSettings() {
        viewModel.saveDiagnosticsConsent()
        FirebaseCrashReporting.setCollectionEnabled(viewModel.diagnosticsEnabled)
        guard let profile = viewModel.buildProfileForSave() else { return }
        viewModel.saveTimerSettings()
        appState.notifySettingsChanged()
        guard appState.saveProfile(profile) else {
            viewModel.message = "같은 이름의 자녀가 이미 있어요."
            return
        }
        dismiss()
    }
}

private struct OpenSourceLicensesView: View {
    private let licenses = OpenSourceLicenseCatalog.load()

    var body: some View {
        List(licenses) { license in
            NavigationLink {
                OpenSourceLicenseDetailView(license: license)
            } label: {
                VStack(alignment: .leading, spacing: 4) {
                    Text(license.name)
                    Text("\(license.version) · \(license.licenseName)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .overlay {
            if licenses.isEmpty {
                ContentUnavailableView(
                    "라이선스 정보를 불러오지 못했어요.",
                    systemImage: "doc.text.magnifyingglass"
                )
            }
        }
        .navigationTitle("오픈소스 라이선스")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct OpenSourceLicenseDetailView: View {
    let license: OpenSourceLicense

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("\(license.version) · \(license.licenseName)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Link("소스 저장소 열기", destination: license.sourceURL)
                Text(license.licenseText)
                    .font(.system(.caption, design: .monospaced))
                    .textSelection(.enabled)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
        }
        .navigationTitle(license.name)
        .navigationBarTitleDisplayMode(.inline)
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
