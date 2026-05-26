import SwiftUI

struct TimerView: View {
    @StateObject private var viewModel = TimerViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text(viewModel.state.preset.title)
                    .font(.headline)
                Text(timeText)
                    .font(.system(size: 48, weight: .bold, design: .rounded))
                HStack {
                    ForEach(TimerPreset.allCases, id: \.self) { preset in
                        Button(preset.title) {
                            viewModel.selectPreset(preset)
                        }
                    }
                }
                HStack {
                    Button(viewModel.state.isRunning ? "일시정지" : "시작") {
                        viewModel.toggle()
                    }
                    Button("리셋") {
                        viewModel.reset()
                    }
                }
            }
            .navigationTitle("타이머")
            .task {
                viewModel.refreshRunningState()
            }
        }
    }

    private var timeText: String {
        let minutes = viewModel.state.remainingSeconds / 60
        let seconds = viewModel.state.remainingSeconds % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}
