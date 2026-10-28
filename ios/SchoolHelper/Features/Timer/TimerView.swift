import SwiftUI

struct TimerView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var viewModel: TimerViewModel
    @State private var completionBlinkOn = false
    private let showsDismissButton: Bool

    init(showsDismissButton: Bool = false) {
        self.showsDismissButton = showsDismissButton
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                TimerControls(viewModel: viewModel, ringSize: 240)
                    .padding(24)
                    .frame(maxWidth: .infinity)
                    .background(timerCompletionBackground, in: RoundedRectangle(cornerRadius: 28))
                    .overlay(RoundedRectangle(cornerRadius: 28).stroke(Color(.separator).opacity(0.18)))
                    .padding(24)
            }
            .animation(.easeInOut(duration: 0.35), value: completionBlinkOn)
            .task(id: isTimerCompleted) {
                await updateCompletionBlink()
            }
            .navigationTitle("집중력 타이머")
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

    private var isTimerCompleted: Bool {
        viewModel.state.pomodoro?.isCompleted ?? (!viewModel.state.isRunning && viewModel.state.remainingSeconds == 0)
    }

    private var timerCompletionBackground: Color {
        if isTimerCompleted && completionBlinkOn {
            return Color.orange.opacity(0.22)
        }
        return Color(.systemBackground)
    }

    private func updateCompletionBlink() async {
        guard isTimerCompleted else {
            completionBlinkOn = false
            return
        }

        completionBlinkOn = true
        while !Task.isCancelled {
            try? await Task.sleep(nanoseconds: 550_000_000)
            if Task.isCancelled { break }
            completionBlinkOn.toggle()
        }
    }

 }

private let timerNavy = Color(red: 23 / 255, green: 59 / 255, blue: 102 / 255)
private let timerBlue = Color(red: 63 / 255, green: 130 / 255, blue: 214 / 255)

struct TimerControls: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var viewModel: TimerViewModel
    let ringSize: CGFloat

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if viewModel.displayMode == .count {
                Text(timeText)
                    .font(.system(size: 42, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(colorScheme == .dark ? Color.white : timerNavy)
                    .accessibilityIdentifier("timer-\(Int(ringSize))-time")
            } else {
                TimerSegmentRing(progress: viewModel.progressFraction, timeText: timeText)
                    .frame(width: ringSize, height: ringSize)
                    .accessibilityIdentifier("timer-\(Int(ringSize))-time")
                    .frame(maxWidth: .infinity)
            }
            Text(phaseTitle)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            HStack(spacing: 8) {
                choiceButton("25분 집중", selected: viewModel.state.pomodoro?.focusMinutes == 25, id: "preset-deepFocus") {
                    viewModel.configurePomodoro(focusMinutes: 25, rounds: viewModel.state.pomodoro?.totalRounds ?? 4)
                }
                choiceButton("40분 집중", selected: viewModel.state.pomodoro?.focusMinutes == 40, id: "preset-focus") {
                    viewModel.configurePomodoro(focusMinutes: 40, rounds: viewModel.state.pomodoro?.totalRounds ?? 4)
                }
                choiceButton("2회", selected: viewModel.state.pomodoro?.totalRounds == 2, id: "rounds-2") {
                    viewModel.configurePomodoro(focusMinutes: viewModel.state.pomodoro?.focusMinutes ?? (viewModel.state.preset == .deepFocus ? 25 : 40), rounds: 2)
                }
                choiceButton("4회", selected: viewModel.state.pomodoro?.totalRounds == 4, id: "rounds-4") {
                    viewModel.configurePomodoro(focusMinutes: viewModel.state.pomodoro?.focusMinutes ?? (viewModel.state.preset == .deepFocus ? 25 : 40), rounds: 4)
                }
            }
            HStack(spacing: 12) {
                Button { viewModel.toggle() } label: {
                    Text(primaryTitle)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .background(timerBlue, in: RoundedRectangle(cornerRadius: 16))
                        .foregroundStyle(.white)
                }
                .accessibilityIdentifier("timer-\(Int(ringSize))-primary")
                Button { viewModel.reset() } label: {
                    Text("초기화")
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .foregroundStyle(colorScheme == .dark ? Color.white : timerNavy)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(.separator).opacity(0.5)))
                }
            }
            .font(.subheadline.weight(.semibold))
            .buttonStyle(.plain)
        }
    }

    private var phaseTitle: String {
        guard let session = viewModel.state.pomodoro else {
            switch viewModel.state.preset {
            case .deepFocus: return "25분 집중"
            case .focus: return "40분 집중"
            case .shortBreak: return "쉬는 시간"
            }
        }
        let round = session.phase == .rest ? session.completedRounds : min(session.completedRounds + 1, session.totalRounds)
        return "\(session.phase == .focus ? "집중" : "쉬는 시간") \(round)/\(session.totalRounds)"
    }

    private var primaryTitle: String {
        if viewModel.state.isRunning { return "일시정지" }
        if viewModel.state.pomodoro?.isCompleted == true { return "새 세션 시작" }
        return viewModel.state.remainingSeconds > 0 && viewModel.state.remainingSeconds < viewModel.state.totalSeconds ? "이어하기" : "시작"
    }

    private func choiceButton(_ title: String, selected: Bool, id: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.caption.weight(.semibold))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 3)
                .frame(maxWidth: .infinity, minHeight: 48)
                .background(selected ? Color(red: 221 / 255, green: 235 / 255, blue: 1) : Color(.systemBackground), in: RoundedRectangle(cornerRadius: 18))
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color(.separator).opacity(0.3)))
        }
        .buttonStyle(.plain)
        .foregroundStyle(selected ? Color(red: 28 / 255, green: 36 / 255, blue: 48 / 255) : Color.primary)
        .accessibilityAddTraits(selected ? .isSelected : [])
        .accessibilityIdentifier("timer-\(Int(ringSize))-\(id)")
        .disabled(viewModel.state.isRunning)
    }

    private var timeText: String {
        String(format: "%02d:%02d", viewModel.state.remainingSeconds / 60, viewModel.state.remainingSeconds % 60)
    }
}

private struct TimerSegmentRing: View {
    @Environment(\.colorScheme) private var colorScheme
    let progress: Double
    let timeText: String

    var body: some View {
        ZStack {
            Canvas { context, size in
                let center = CGPoint(x: size.width / 2, y: size.height / 2)
                let outer = min(size.width, size.height) / 2 - 12
                let inner = outer - 22
                for index in 0..<60 {
                    let angle = Double(index) * .pi / 30 - .pi / 2
                    var path = Path()
                    path.move(to: CGPoint(x: center.x + cos(angle) * inner, y: center.y + sin(angle) * inner))
                    path.addLine(to: CGPoint(x: center.x + cos(angle) * outer, y: center.y + sin(angle) * outer))
                    let isActive = Double(index) + 0.5 < max(0, min(1, progress)) * 60
                    context.stroke(path, with: .color(isActive ? timerBlue : Color.gray.opacity(0.25)), lineWidth: 6)
                }
            }
            VStack(spacing: 8) {
                Text(timeText).font(.system(size: 34, weight: .bold, design: .rounded)).monospacedDigit().foregroundStyle(colorScheme == .dark ? Color.white : timerNavy)
                Text("남은 시간").font(.caption).foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("남은 시간 \(timeText)")
    }
}
