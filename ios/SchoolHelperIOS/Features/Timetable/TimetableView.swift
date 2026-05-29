import SwiftUI

struct TimetableView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = TimetableViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack {
                        Button("이전") {
                            Task { await viewModel.showPreviousDay() }
                        }
                        Spacer()
                        VStack(spacing: 4) {
                            Text(viewModel.dateTitle)
                                .font(.headline)
                            if !viewModel.statusText.isEmpty {
                                Text(viewModel.statusText)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Spacer()
                        Button("오늘") {
                            Task { await viewModel.showToday() }
                        }
                        Button("다음") {
                            Task { await viewModel.showNextDay() }
                        }
                    }
                    .buttonStyle(.borderless)
                }

                ForEach(viewModel.items) { item in
                    VStack(alignment: .leading) {
                        Text("\(item.period)교시")
                            .font(.headline)
                        Text(item.subject)
                    }
                }
            }
            .navigationTitle("시간표")
            .task {
                await viewModel.load(profile: appState.profile)
            }
        }
    }
}
