import XCTest

final class SchoolHelperIOSUITests: XCTestCase {
    func testInitialSetupSearchFindsMisaMiddleSchoolFromNEIS() {
        let app = makeFreshSetupApp()
        app.launch()

        let schoolField = app.textFields["학교 이름"]
        XCTAssertTrue(schoolField.waitForExistence(timeout: 5))
        schoolField.tap()
        schoolField.typeText("미사중학교")

        app.buttons["학교 검색"].tap()

        XCTAssertTrue(app.staticTexts["학교 1개를 찾았어요."].waitForExistence(timeout: 20))
        XCTAssertTrue(app.buttons["저장하고 시작하기"].exists)
    }

    func testSeededHomeShowsCoreTabsAndNoMoreTab() {
        let app = makeSeededApp()
        app.launch()

        XCTAssertTrue(app.tabBars.buttons["홈"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.tabBars.buttons["시간표"].exists)
        XCTAssertTrue(app.tabBars.buttons["급식"].exists)
        XCTAssertTrue(app.tabBars.buttons["일정"].exists)
        XCTAssertFalse(app.tabBars.buttons["More"].exists)
    }

    func testSettingsModalCanOpenAndCloseFromHome() {
        let app = makeSeededApp()
        app.launch()

        XCTAssertTrue(app.buttons["설정"].waitForExistence(timeout: 5))
        app.buttons["설정"].tap()

        XCTAssertTrue(app.buttons["닫기"].waitForExistence(timeout: 5))
        app.buttons["닫기"].tap()

        XCTAssertTrue(app.navigationBars.staticTexts["학교도우미"].waitForExistence(timeout: 5))
    }

    func testSettingsSaveDismissesModal() {
        let app = makeSeededApp(initialRoute: "settings")
        app.launch()

        let saveButton = app.buttons["settings-save-button"]
        XCTAssertTrue(saveButton.waitForExistence(timeout: 5))
        saveButton.tap()

        XCTAssertTrue(app.navigationBars.staticTexts["학교도우미"].waitForExistence(timeout: 5))
    }

    func testSettingsModalShowsWidgetPreview() {
        let app = makeSeededApp(initialRoute: "settings")
        app.launch()

        XCTAssertTrue(app.staticTexts["위젯 미리보기"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["미사중학교 1학년 2반"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["오늘"].exists)
        XCTAssertTrue(app.staticTexts["내일"].exists)
    }

    func testTimerModalShowsCloseButtonWhenLaunchedDirectly() {
        let app = makeSeededApp(initialRoute: "timer")
        app.launch()

        XCTAssertTrue(app.buttons["닫기"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.navigationBars.staticTexts["타이머"].exists)
    }

    private func makeSeededApp(initialRoute: String? = nil) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchEnvironment["SCHOOLHELPER_SEED_PROFILE"] = "fixture"
        app.launchEnvironment["SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST"] = "1"
        if let initialRoute {
            app.launchEnvironment["SCHOOLHELPER_INITIAL_ROUTE"] = initialRoute
        }
        return app
    }

    private func makeFreshSetupApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchEnvironment["SCHOOLHELPER_RESET_PROFILE"] = "1"
        app.launchEnvironment["SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST"] = "1"
        return app
    }
}
