import SwiftUI

struct TimetableView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = TimetableViewModel()

    var body: some View {
        NavigationStack {
            List(viewModel.items) { item in
                VStack(alignment: .leading) {
                    Text("\(item.period)교시")
                        .font(.headline)
                    Text(item.subject)
                }
            }
                .navigationTitle("시간표")
                .task {
                    await viewModel.load(profile: appState.profile)
                }
        }
    }
}
