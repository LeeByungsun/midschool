import XCTest
@testable import SchoolHelperIOSCore

@MainActor
final class AppStateTests: XCTestCase {
    func testSaveProfileTriggersWidgetReload() {
        let defaults = UserDefaults(suiteName: #function)!
        defaults.removePersistentDomain(forName: #function)
        let reloader = SpyWidgetTimelineReloader()
        let appState = AppState(
            store: StudentPreferencesStore(defaults: defaults),
            widgetTimelineReloader: reloader
        )

        appState.saveProfile(.fixture())

        XCTAssertEqual(reloader.reloadCount, 1)
    }
}
