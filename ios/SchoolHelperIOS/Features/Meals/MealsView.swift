import SwiftUI

struct MealsView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = MealsViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack {
                        Button("이전 주") {
                            Task { await viewModel.showPreviousWeek() }
                        }
                        Spacer()
                        VStack(spacing: 4) {
                            Text(viewModel.weekTitle)
                                .font(.headline)
                            if !viewModel.statusText.isEmpty {
                                Text(viewModel.statusText)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Spacer()
                        Button("이번 주") {
                            Task { await viewModel.showCurrentWeek() }
                        }
                        Button("다음 주") {
                            Task { await viewModel.showNextWeek() }
                        }
                    }
                }

                ForEach(viewModel.items) { item in
                    VStack(alignment: .leading) {
                        Text(formattedMealDate(item.date))
                            .font(.headline)
                        Text(item.menu)
                        Text(item.calorieInfo)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("급식")
            .task {
                await viewModel.load(profile: appState.profile)
            }
        }
    }

    private func formattedMealDate(_ rawDate: String) -> String {
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
