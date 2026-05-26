import SwiftUI

struct ScheduleView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = ScheduleViewModel()

    var body: some View {
        NavigationStack {
            List(viewModel.items) { item in
                VStack(alignment: .leading) {
                    Text(item.title)
                        .font(.headline)
                    Text(item.date)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(item.description)
                }
            }
                .navigationTitle("일정")
                .task {
                    await viewModel.load(profile: appState.profile)
                }
        }
    }
}
