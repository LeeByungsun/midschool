import SwiftUI

struct MealsView: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.scenePhase) private var scenePhase
    @State private var today = AppLaunchOverrides.referenceDate() ?? Date()
    @StateObject private var viewModel = MealsViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(viewModel.items) { item in
                        VStack(alignment: .leading) {
                            Text(formattedMealDate(item.date))
                                .font(.headline)
                                .accessibilityLabel(item.isOnDate(today) ? "오늘 \(formattedMealDate(item.date))" : formattedMealDate(item.date))
                            Text(item.menu)
                            Text(item.calorieInfo)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .listRowBackground(item.isOnDate(today) ? Color.blue.opacity(colorScheme == .dark ? 0.25 : 0.12) : nil)
                    }
                } header: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(viewModel.weekTitle)
                            .font(.headline)
                        if !viewModel.statusText.isEmpty {
                            Text(viewModel.statusText)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                if viewModel.hasLoadError {
                    Button("다시 시도") {
                        Task { await viewModel.retry() }
                    }
                }
            }
            .navigationTitle("급식")
            .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged)) { _ in
                today = AppLaunchOverrides.referenceDate() ?? Date()
            }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { today = AppLaunchOverrides.referenceDate() ?? Date() }
            }
            .task(id: appState.profile) {
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
