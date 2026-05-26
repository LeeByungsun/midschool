import SwiftUI

struct MealsView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = MealsViewModel()

    var body: some View {
        NavigationStack {
            List(viewModel.items) { item in
                VStack(alignment: .leading) {
                    Text(item.date)
                        .font(.headline)
                    Text(item.menu)
                    Text(item.calorieInfo)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
                .navigationTitle("급식")
                .task {
                    await viewModel.load(profile: appState.profile)
                }
        }
    }
}
