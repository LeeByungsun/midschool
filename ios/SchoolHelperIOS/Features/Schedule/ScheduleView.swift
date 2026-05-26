import SwiftUI

struct ScheduleView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = ScheduleViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack {
                        Button("이전 달") {
                            Task { await viewModel.showPreviousMonth() }
                        }
                        Spacer()
                        VStack(spacing: 4) {
                            Text(viewModel.monthTitle)
                                .font(.headline)
                            if !viewModel.statusText.isEmpty {
                                Text(viewModel.statusText)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Spacer()
                        Button("이번 달") {
                            Task { await viewModel.showCurrentMonth() }
                        }
                        Button("다음 달") {
                            Task { await viewModel.showNextMonth() }
                        }
                    }
                }

                ForEach(viewModel.items) { item in
                    VStack(alignment: .leading) {
                        Text(item.title)
                            .font(.headline)
                        Text(formattedScheduleDate(item.date))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text(item.description)
                    }
                }
            }
            .navigationTitle("일정")
            .task {
                await viewModel.load(profile: appState.profile)
            }
        }
    }

    private func formattedScheduleDate(_ rawDate: String) -> String {
        let parser = DateFormatter()
        parser.locale = Locale(identifier: "ko_KR")
        parser.dateFormat = "yyyyMMdd"
        guard let date = parser.date(from: rawDate) else {
            return rawDate
        }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "M월 d일 EEEE"
        return formatter.string(from: date)
    }
}
