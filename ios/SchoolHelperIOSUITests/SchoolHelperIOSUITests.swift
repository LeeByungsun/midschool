import XCTest

final class SchoolHelperIOSUITests: XCTestCase {
    func testInitialSetupSearchSelectsSchoolAndSavesProfile() {
        let app = makeFreshSetupApp()
        app.launch()

        let schoolField = app.textFields["학교 이름"]
        XCTAssertTrue(schoolField.waitForExistence(timeout: 5))
        schoolField.tap()
        schoolField.typeText("미사중학교")

        app.buttons["학교 검색"].tap()

        XCTAssertTrue(app.staticTexts["학교 1개를 찾았어요."].waitForExistence(timeout: 20))
        XCTAssertTrue(app.staticTexts["선택된 학교"].waitForExistence(timeout: 5))

        app.textFields["학년"].tap()
        app.textFields["학년"].typeText("1")
        app.textFields["반"].tap()
        app.textFields["반"].typeText("2")

        app.buttons["저장하고 시작하기"].tap()

        XCTAssertTrue(app.tabBars.buttons["홈"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["미사중학교"].waitForExistence(timeout: 5))
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

    func testSeededTimetableShowsCoreContent() {
        let app = makeSeededApp(initialRoute: "timetable")
        app.launch()

        XCTAssertTrue(app.navigationBars.staticTexts["시간표"].waitForExistence(timeout: 5))
        XCTAssertTrue(waitForStaticText(containing: "국어", in: app, timeout: 5))
        XCTAssertTrue(waitForStaticText(containing: "수학", in: app, timeout: 5))
    }

    func testSeededMealsShowsCoreContent() {
        let app = makeSeededApp(initialRoute: "meals")
        app.launch()

        XCTAssertTrue(app.navigationBars.staticTexts["급식"].waitForExistence(timeout: 5))
        XCTAssertTrue(scrollToStaticText(containing: "비빔밥", in: app))
        XCTAssertTrue(scrollToStaticText(containing: "712 kcal", in: app))
    }

    func testSeededScheduleShowsCoreContent() {
        let app = makeSeededApp(initialRoute: "schedule")
        app.launch()

        XCTAssertTrue(app.navigationBars.staticTexts["일정"].waitForExistence(timeout: 5))
        XCTAssertTrue(scrollToStaticText(containing: "체육대회", in: app))
        XCTAssertTrue(scrollToStaticText(containing: "중간고사", in: app))
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

    func testSettingsNotificationPermissionRequestUpdatesSummary() {
        let app = makeSeededApp(initialRoute: "settings")
        app.launchEnvironment["SCHOOLHELPER_NOTIFICATION_AUTHORIZATION_STATUS"] = "not_determined"
        app.launchEnvironment["SCHOOLHELPER_NOTIFICATION_AUTHORIZATION_REQUEST_GRANTED"] = "true"
        app.launch()

        XCTAssertTrue(scrollToStaticText(containing: "타이머 완료 알림을 받으려면 권한이 필요해요.", in: app))
        XCTAssertTrue(scrollToButton(named: "알림 권한 요청", in: app))

        app.buttons["알림 권한 요청"].tap()

        XCTAssertTrue(waitForStaticText(containing: "알림 권한이 허용되어 있어요.", in: app, timeout: 5))
        XCTAssertFalse(app.buttons["알림 권한 요청"].exists)
    }

    func testTimerModalShowsCloseButtonWhenLaunchedDirectly() {
        let app = makeSeededApp(initialRoute: "timer")
        app.launch()

        XCTAssertTrue(app.buttons["닫기"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.navigationBars.staticTexts["타이머"].exists)
    }

    func testNoticeButtonOpensExternalSafariURL() throws {
        #if !EXTERNAL_LINK_TEST_ENABLED
        try XCTSkipUnless(
            false,
            "external notice link UI test is opt-in because it leaves the app and opens Safari"
        )
        #endif

        let app = makeSeededApp()
        app.launch()

        XCTAssertTrue(app.navigationBars.staticTexts["학교도우미"].waitForExistence(timeout: 5))
        XCTAssertTrue(scrollToStaticText(containing: "현장학습 안내", in: app, maxSwipes: 5))
        XCTAssertTrue(scrollToButton(named: "가정통신문 열기", in: app, maxSwipes: 5))
        app.buttons["가정통신문 열기"].tap()

        let safari = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")
        XCTAssertTrue(safari.wait(for: .runningForeground, timeout: 10))
        XCTAssertTrue(
            waitForAnyElement(containing: "example.com", in: safari, timeout: 15)
                || waitForAnyElement(containing: "Example Domain", in: safari, timeout: 15)
        )
    }

    func testLiveNoticeButtonOpensOperatingSafariURL() throws {
        #if !(LIVE_UI_TEST_ENABLED && EXTERNAL_LINK_TEST_ENABLED)
        try XCTSkipUnless(
            false,
            "live external notice link UI test is opt-in because it depends on external services and opens Safari"
        )
        #endif

        let app = makeLiveApp()
        app.launch()

        XCTAssertTrue(app.navigationBars.staticTexts["학교도우미"].waitForExistence(timeout: 10))
        XCTAssertTrue(scrollToStaticText(containing: "오케스트라", in: app, maxSwipes: 8))
        XCTAssertTrue(scrollToButton(named: "가정통신문 열기", in: app, maxSwipes: 3))
        app.buttons["가정통신문 열기"].tap()

        let safari = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")
        XCTAssertTrue(safari.wait(for: .runningForeground, timeout: 10))
        XCTAssertTrue(
            waitForAnyElement(containing: "misaj-m.goegh.kr", in: safari, timeout: 20)
                || waitForAnyElement(containing: "오케스트라", in: safari, timeout: 20)
                || waitForAnyElement(containing: "2026학년도", in: safari, timeout: 20)
        )
    }

    func testLiveSchoolDataDisplaysBackendContent() throws {
        #if !LIVE_UI_TEST_ENABLED
        try XCTSkipUnless(
            false,
            "live NEIS/BFF UI test is opt-in because it depends on external services"
        )
        #endif

        let app = makeLiveApp()
        app.launch()

        XCTAssertTrue(app.navigationBars.staticTexts["학교도우미"].waitForExistence(timeout: 10))
        XCTAssertTrue(waitForStaticText(containing: "수학", in: app, timeout: 30))
        XCTAssertTrue(scrollToStaticText(containing: "발아현미밥", in: app, maxSwipes: 8))
        XCTAssertTrue(scrollToStaticText(containing: "오케스트라", in: app, maxSwipes: 8))

        app.tabBars.buttons["시간표"].tap()
        XCTAssertTrue(app.navigationBars.staticTexts["시간표"].waitForExistence(timeout: 10))
        XCTAssertTrue(waitForStaticText(containing: "수학", in: app, timeout: 30))

        app.tabBars.buttons["급식"].tap()
        XCTAssertTrue(app.navigationBars.staticTexts["급식"].waitForExistence(timeout: 10))
        XCTAssertTrue(scrollToStaticText(containing: "발아현미밥", in: app, maxSwipes: 8))

        app.tabBars.buttons["일정"].tap()
        XCTAssertTrue(app.navigationBars.staticTexts["일정"].waitForExistence(timeout: 10))
        XCTAssertTrue(scrollToStaticText(containing: "노동절", in: app, maxSwipes: 8))
    }

    func testLiveDateNavigationUpdatesTitles() throws {
        #if !LIVE_UI_TEST_ENABLED
        try XCTSkipUnless(
            false,
            "live date navigation UI test is opt-in because it depends on external services"
        )
        #endif

        let app = makeLiveApp()
        app.launch()

        app.tabBars.buttons["시간표"].tap()
        XCTAssertTrue(app.navigationBars.staticTexts["시간표"].waitForExistence(timeout: 10))
        XCTAssertTrue(waitForStaticText(containing: "5월 28일 목요일", in: app, timeout: 30))
        app.buttons["다음"].tap()
        XCTAssertTrue(waitForStaticText(containing: "5월 29일 금요일", in: app, timeout: 10))

        app.tabBars.buttons["급식"].tap()
        XCTAssertTrue(app.navigationBars.staticTexts["급식"].waitForExistence(timeout: 10))
        XCTAssertTrue(waitForStaticText(containing: "5월 25일 - 5월 29일", in: app, timeout: 30))
        app.buttons["다음 주"].tap()
        XCTAssertTrue(waitForStaticText(containing: "6월 1일 - 6월 5일", in: app, timeout: 10))

        app.tabBars.buttons["일정"].tap()
        XCTAssertTrue(app.navigationBars.staticTexts["일정"].waitForExistence(timeout: 10))
        XCTAssertTrue(waitForStaticText(containing: "2026년 5월", in: app, timeout: 30))
        app.buttons["다음 달"].tap()
        XCTAssertTrue(waitForStaticText(containing: "2026년 6월", in: app, timeout: 10))
    }

    private func makeSeededApp(initialRoute: String? = nil) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchEnvironment["SCHOOLHELPER_SEED_PROFILE_JSON"] = seededProfileJSON()
        app.launchEnvironment["SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST"] = "1"
        app.launchEnvironment["NEIS_BASE_URL"] = "http://[::1"
        app.launchEnvironment["WEB_BASE_URL"] = "http://[::1"
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

    private func makeLiveApp() -> XCUIApplication {
        let app = XCUIApplication()
        let environment = ProcessInfo.processInfo.environment
        app.launchEnvironment["SCHOOLHELPER_SEED_PROFILE_JSON"] = liveProfileJSON()
        app.launchEnvironment["SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST"] = "1"
        app.launchEnvironment["SCHOOLHELPER_REFERENCE_DATE"] = environment["SCHOOLHELPER_LIVE_TEST_DATE"] ?? "20260528"
        if let neisBaseURL = environment["NEIS_BASE_URL"] {
            app.launchEnvironment["NEIS_BASE_URL"] = neisBaseURL
        }
        if let webBaseURL = environment["WEB_BASE_URL"] {
            app.launchEnvironment["WEB_BASE_URL"] = webBaseURL
        }
        if let neisAPIKey = environment["NEIS_API_KEY"] {
            app.launchEnvironment["NEIS_API_KEY"] = neisAPIKey
        }
        return app
    }

    private func seededProfileJSON() -> String {
        """
        {"grade":"1","classroom":"2","schoolName":"미사중학교","officeCode":"J10","schoolCode":"ui-\(UUID().uuidString)","schoolKind":"중학교"}
        """
    }

    private func liveProfileJSON() -> String {
        """
        {"grade":"1","classroom":"2","schoolName":"미사중학교","officeCode":"J10","schoolCode":"7692129","schoolKind":"중학교"}
        """
    }

    private func waitForStaticText(
        containing text: String,
        in app: XCUIApplication,
        timeout: TimeInterval
    ) -> Bool {
        let predicate = NSPredicate(format: "label CONTAINS %@", text)
        return app.staticTexts.containing(predicate).firstMatch.waitForExistence(timeout: timeout)
    }

    private func waitForAnyElement(
        containing text: String,
        in app: XCUIApplication,
        timeout: TimeInterval
    ) -> Bool {
        let predicate = NSPredicate(format: "label CONTAINS %@ OR value CONTAINS %@", text, text)
        return app.descendants(matching: .any).containing(predicate).firstMatch.waitForExistence(timeout: timeout)
    }

    private func scrollToButton(
        named label: String,
        in app: XCUIApplication,
        maxSwipes: Int = 5
    ) -> Bool {
        for attempt in 0...maxSwipes {
            if app.buttons[label].waitForExistence(timeout: 1) {
                return true
            }
            if attempt < maxSwipes {
                app.swipeUp()
            }
        }
        return false
    }

    private func scrollToStaticText(
        containing text: String,
        in app: XCUIApplication,
        maxSwipes: Int = 5
    ) -> Bool {
        for attempt in 0...maxSwipes {
            if waitForStaticText(containing: text, in: app, timeout: 1) {
                return true
            }
            if attempt < maxSwipes {
                app.swipeUp()
            }
        }
        return false
    }
}
