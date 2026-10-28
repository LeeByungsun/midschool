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

                if viewModel.hasLoadError {
                    Button("다시 시도") {
                        Task { await viewModel.retry() }
                    }
                }
            }
            .navigationTitle("시간표")
            .task(id: appState.profile) {
                await viewModel.load(profile: appState.profile)
            }
        }
    }
}
