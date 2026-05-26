import SwiftUI

struct TimerView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = TimerViewModel()
    private let showsDismissButton: Bool

    init(showsDismissButton: Bool = false) {
        self.showsDismissButton = showsDismissButton
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text(viewModel.state.preset.title)
                    .font(.headline)
                if viewModel.displayMode == .count {
                    Text(timeText)
                        .font(.system(size: 48, weight: .bold, design: .rounded))
                } else {
                    ZStack {
                        Circle()
                            .stroke(Color.gray.opacity(0.2), lineWidth: 16)
                            .frame(width: 180, height: 180)
                        Circle()
                            .trim(from: 0, to: viewModel.progressFraction)
                            .stroke(Color.blue, style: StrokeStyle(lineWidth: 16, lineCap: .round))
                            .rotationEffect(.degrees(-90))
                            .frame(width: 180, height: 180)
                        VStack(spacing: 8) {
                            Text(timeText)
                                .font(.system(size: 36, weight: .bold, design: .rounded))
                            Text("남은 시간")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
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
            .navigationBarTitleDisplayMode(showsDismissButton ? .inline : .automatic)
            .toolbar {
                if showsDismissButton {
                    ToolbarItem(placement: .topBarLeading) {
                        Button("닫기") {
                            dismiss()
                        }
                    }
                }
            }
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
