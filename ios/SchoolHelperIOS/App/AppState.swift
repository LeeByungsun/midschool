import Foundation

final class AppState: ObservableObject {
    @Published var isSetupComplete: Bool = false
}
