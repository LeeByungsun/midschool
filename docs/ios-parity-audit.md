# iOS 기능 Parity 감사

기준일: 2026-05-28

이 문서는 Android 앱의 현재 기능 계약을 기준으로 `ios/` 구현이 어디까지 같은 사용자 기능을 제공하는지 검증한 감사 기록입니다.
완료 선언용 문서가 아니라, 남은 검증/외부 의존성을 명확히 분리하기 위한 현재 상태 문서입니다.

---

## 1. 감사 기준

권위 기준:

- 공통/Android 스펙: `docs/project_specification.md`
- iOS 스펙: `docs/ios-project-specification.md`
- iOS 런타임 검증: `docs/ios-runtime-verification.md`
- iOS 아키텍처 규칙: `.codex/skills/ios-architecture/SKILL.md`
- 현재 코드: `ios/SchoolHelperIOS`, `ios/SchoolHelperWidget`, `ios/Tests`, `ios/SchoolHelperIOSUITests`

검증 원칙:

- 화면 이름이 아니라 같은 학생 기능과 데이터 의미를 기준으로 비교한다.
- iOS 정책상 Android와 1:1로 같을 수 없는 기능은 플랫폼 차이로 분리한다.
- 자동 테스트가 덮지 않는 시스템 UI/실기기 항목은 완료가 아니라 수동 검증 대기로 기록한다.
- `ios/scripts/verify_ios_local_readiness.sh` 는 현재 자동/로컬로 재현 가능한 iOS 검증 묶음을 순서대로 실행한다.
- `ios/scripts/audit_ios_goal_readiness.py` 는 이 목표의 완료 준비도를 JSON으로 재감사하며, 미완료/외부 blocker가 있으면 exit `20` 으로 종료한다.
- 시스템 수동 증거는 `ios/system-evidence.template.json` → `ios/system-evidence.local.json` 형식으로 기록하고 `ios/scripts/validate_ios_system_evidence.py` 로 검증한다.

---

## 2. 요구사항별 현재 상태

### 2.1 iOS 작업공간/스킬/스펙

상태: 완료

증거:

- iOS 전용 agent skill: `.codex/skills/ios-architecture/SKILL.md`
- iOS 작업공간: `ios/`
- iOS 앱 프로젝트: `ios/SchoolHelperIOS.xcodeproj`
- iOS 전용 스펙: `docs/ios-project-specification.md`
- 실행/검증 문서: `docs/ios-runtime-verification.md`

비고:

- SwiftUI + ViewModel + Repository/Client + UserDefaults wrapper 구조를 iOS skill 기준에 맞춰 구성했다.

### 2.2 초기 설정/학교 검색

상태: 구현 및 자동 검증됨

Android 기준:

- 학교 검색/선택
- 학년/반 입력
- 학교 identity와 학년/반이 모두 있어야 설정 완료
- 불완전 저장값은 설정 미완료로 처리

현재 iOS 증거:

- `SetupView`, `SetupViewModel`
- `StudentProfile.isComplete`
- `StudentPreferencesStore`
- `SchoolSearchService`, `NEISClient`
- UI 테스트 `testInitialSetupSearchSelectsSchoolAndSavesProfile`
- 코어 테스트에서 공백/학교 코드 누락/검색 응답 경합을 검증
- 2026-05-28 `NEISClientTests/testSearchSchoolsRetriesWithoutWhitespaceWhenFirstSearchIsEmpty` 로 `미사 중학교` 입력 시 공백 제거 재검색을 확인
- 2026-05-28 simulator UI 테스트에서 `미사중학교` 검색 후 `학교 1개를 찾았어요.` 와 `선택된 학교` 섹션 표시를 확인

메모:

- `NEIS_API_KEY`는 앱 번들에 저장하지 않는다.
- 키가 없으면 `KEY` 없이 요청한다.
- 실기기 아이콘 실행에서는 환경변수가 전달되지 않으므로 키 없이 동작 가능한 학교 검색 경로를 우선한다.
- 정확히 1개 학교가 검색되면 자동 선택되며, iOS 초기 설정/설정 화면은 선택 상태를 `선택된 학교` 섹션으로 별도 표시한다.
- `미사 중학교`처럼 중간 공백이 들어간 입력은 0건일 때 공백 제거 검색어로 한 번 더 조회한다.
- 네트워크 오류 등으로 mock fallback을 사용할 때도 학교명 공백을 제거해 `미사 중학교` 입력을 처리한다.

### 2.3 홈 대시보드

상태: 구현 및 자동 검증됨

Android 기준:

- 학교/학년/반 표시
- 급식, 일정, 가정통신문 preview, 타이머 요약
- 주요 화면 진입

현재 iOS 증거:

- `HomeView`, `HomeViewModel`
- `RootTabView`
- `NoticePreview`
- UI 테스트 `testSeededHomeShowsCoreTabsAndNoMoreTab`

비고:

- iOS는 Android Activity 구조 대신 SwiftUI Tab + modal 구조를 사용한다.
- 홈에서 타이머/설정은 modal로 연다.

### 2.4 시간표

상태: 구현 및 자동 검증됨

Android 기준:

- 날짜 이동형 일간 시간표
- 교시별 과목 표시
- 학교/학년/반 기준 조회
- 캐시 fallback

현재 iOS 증거:

- `TimetableView`, `TimetableViewModel`
- `SchoolRepository.fetchTimetable`
- `SchoolDataCacheStore`
- `DefaultSchoolRepositoryTests`
- UI 테스트 `testSeededTimetableShowsCoreContent`
- `ios/scripts/verify_live_school_data.py` 로 live NEIS `misTimetable` row 확인
- UI 테스트 `testLiveSchoolDataDisplaysBackendContent` 로 simulator 화면 렌더링 확인
- 2026-05-28 미사중학교 1학년 2반 기준 sample `수학` 확인

남은 수동 확인:

- 실기기 화면에서 실제 날짜 이동 UX와 live NEIS 응답 렌더링을 눈으로 확인.

### 2.5 급식

상태: 구현 및 자동 검증됨

Android 기준:

- 주간 급식
- 날짜별 메뉴/칼로리 표시
- 캐시 fallback

현재 iOS 증거:

- `MealsView`, `MealsViewModel`
- `SchoolRepository.fetchMeals`
- `SchoolDataCacheStore`
- `DefaultSchoolRepositoryTests`
- UI 테스트 `testSeededMealsShowsCoreContent`
- `ios/scripts/verify_live_school_data.py` 로 live NEIS `mealServiceDietInfo` row 확인
- UI 테스트 `testLiveSchoolDataDisplaysBackendContent` 로 simulator 화면 렌더링 확인
- 2026-05-28 미사중학교 기준 sample `발아현미밥` 포함 급식 확인

남은 수동 확인:

- 실기기 화면에서 live NEIS 기준 실제 주간 리스트 스크롤/표시 확인.

### 2.6 학사 일정

상태: 구현 및 자동 검증됨

Android 기준:

- 월간 일정 목록
- `토요휴업일` 필터링
- 캐시 fallback

현재 iOS 증거:

- `ScheduleView`, `ScheduleViewModel`
- `HomeViewModel`과 `ScheduleViewModel`의 필터링
- `SchoolRepository.fetchSchedule`
- `DefaultSchoolRepositoryTests`
- UI 테스트 `testSeededScheduleShowsCoreContent`
- `ios/scripts/verify_live_school_data.py` 로 live NEIS `SchoolSchedule` row 확인
- UI 테스트 `testLiveSchoolDataDisplaysBackendContent` 로 simulator 화면 렌더링 확인
- 2026-05-28 미사중학교 2026년 5월 기준 sample `노동절` 확인

남은 수동 확인:

- 실기기 화면에서 live NEIS 기준 월 이동/목록 표시 확인.

### 2.7 타이머

상태: 구현 및 자동 검증됨

Android 기준:

- 집중/휴식 프리셋
- 숫자/링 표시
- 시작/일시정지/재시작/리셋
- 앱 재진입 시 상태 복구
- 완료 알림/진동

현재 iOS 증거:

- `TimerView`, `TimerViewModel`
- `TimerPreferencesStore`, `TimerSettingsStore`
- `TimerNotificationScheduler`
- `NotificationPermissionCoordinator`
- `TimerViewModelTests`
- `NotificationPermissionCoordinatorTests`
- UI 테스트 `testTimerModalShowsCloseButtonWhenLaunchedDirectly`
- UI 테스트 `testSettingsNotificationPermissionRequestUpdatesSummary`

플랫폼 차이:

- Android의 `TimerBootReceiver`와 같은 재부팅 브로드캐스트는 iOS에 동일 개념이 없다.
- iOS는 저장된 target date 기반 복구와 `UNUserNotificationCenter` 예약으로 대응한다.

2026-05-28 추가 검증:

- `SCHOOLHELPER_NOTIFICATION_AUTHORIZATION_STATUS` launch override로 설정 화면의 알림 권한 안내/요청 버튼 상태를 deterministic하게 검증한다.
- simulator `testSettingsNotificationPermissionRequestUpdatesSummary` 통과. xcresult: `/tmp/misschool-ios-notification-permission-ui-test-final/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-04-02-+0900.xcresult`
- 실제 iPhone `testSettingsNotificationPermissionRequestUpdatesSummary` 통과. xcresult: `/tmp/misschool-ios-device-notification-permission-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-01-31-+0900.xcresult`

남은 수동 확인:

- 실제 iOS 시스템 권한 팝업, 완료 알림 배너, 소리/진동 체감 확인.

### 2.8 설정

상태: 구현 및 자동 검증됨

Android 기준:

- 학년/반 수정
- 학교 검색/변경
- 타이머 옵션
- 위젯 옵션

현재 iOS 증거:

- `SettingsView`, `SettingsViewModel`
- `ProfileEditorViewModelTests`
- `SettingsViewModelTests`
- UI 테스트 `testSettingsModalCanOpenAndCloseFromHome`
- UI 테스트 `testSettingsSaveDismissesModal`
- UI 테스트 `testSettingsModalShowsWidgetPreview`

비고:

- 알림 OFF 저장 시 예약된 타이머 완료 알림 취소를 테스트로 검증한다.

### 2.9 가정통신문 preview

상태: 구현 및 simulator/실기기 전환 smoke 검증됨

Android 기준:

- 앱이 학교 홈페이지를 직접 스크래핑하지 않고 web `/api/notices` BFF 사용
- 홈 카드 최대 3개 표시
- 날짜가 없으면 제목만 표시
- URL이 있을 때 외부 링크 열기

현재 iOS 증거:

- `NEISClient.fetchNotices`
- `NoticePreview`
- `HomeViewModel`
- `NEISClientTests`
- `FeatureViewModelTests`
- `ios/scripts/verify_live_school_data.py` 로 live web `/api/notices` BFF item 및 첫 notice 상세 URL HTTP 200/제목 매칭 확인
- UI 테스트 `testLiveSchoolDataDisplaysBackendContent` 로 simulator 홈 화면 렌더링 확인
- 2026-05-28 미사중학교 기준 sample `2026학년도미사 오케스트라 아침 맞이 콘서트일정 안내` 확인
- UI 테스트 `testNoticeButtonOpensExternalSafariURL` 로 seeded notice의 `가정통신문 열기` 버튼이 Safari를 foreground로 전환하고 URL/페이지 텍스트에 `example.com` 또는 `Example Domain` 이 노출되는지 확인
- 2026-05-28 `ios/scripts/test_external_link_ui.sh` 로 simulator 외부 링크 전환 및 URL/페이지 텍스트 smoke 통과. xcresult: `/tmp/misschool-ios-external-link-url-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-27-48-+0900.xcresult`
- 2026-05-28 `EXTERNAL_LINK_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-external-link-test ios/scripts/test_device_ui.sh` 로 실제 iPhone 외부 링크 전환 smoke 통과. xcresult: `/tmp/misschool-ios-device-external-link-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_15-52-21-+0900.xcresult`

남은 수동 확인:

- 실제 운영 notice 웹페이지 콘텐츠 자체의 실기기 Safari 렌더링 눈검증.

### 2.10 홈 화면 위젯

상태: 구현됨, 시스템 배치/탭 실기기 검증은 미완료

Android 기준:

- 오늘/내일 시간표 표시
- 본문 탭 시 앱 진입
- 설정에서 내일 시간표 표시 여부 제어
- 작은 위젯 대응/긴 과목명 제한

현재 iOS 증거:

- `SchoolHelperWidget` WidgetKit target/source
- `SchoolHelperWidgetConfigurationIntent`
- `HomeWidgetSnapshotLoader`
- `HomeWidgetTimelinePlanner`
- `HomeWidgetSnapshotView`
- `WidgetSettingsStore`
- `HomeWidgetSnapshotLoaderTests`
- `HomeWidgetTimelinePlannerTests`
- UI 테스트의 앱 내 위젯 미리보기 확인
- `ios/scripts/test_widget_sim.sh` 로 simulator 위젯 패키징/route/App Group config smoke 고정
- `ios/scripts/verify_widget_app_group_readiness.sh` 로 simulator smoke → profile check → 선택적 full App Group device build/install 경로 고정
- `ios/scripts/refresh_app_group_profiles.sh` 로 local stale provisioning profile 백업/제거와 Xcode refresh 재시도 경로 고정
- `ios/scripts/generate_xcodeproj.py` 와 `SchoolHelperIOS.xcodeproj` 에 app/widget target App Groups `SystemCapabilities` metadata 고정
- 2026-05-28 초기 refresh 시에는 widget profile App Group이 비어 있었으나, Xcode/Apple provisioning 갱신 후 앱/위젯 profile 모두 같은 App Group을 포함하도록 해소됨
- `AppStateTests/testWidgetDeepLinksRouteToSetupOrTimetable` 로 위젯 URL(`schoolhelper://settings`, `schoolhelper://timetable`) 라우팅 고정
- 2026-05-28 `DERIVED_DATA_PATH=/tmp/misschool-ios-widget-sim-test-quiet ios/scripts/test_widget_sim.sh` 통과

남은 검증:

- 실제 홈 화면에 위젯을 배치하고 오늘/내일 시간표가 표시되는지 확인.
- 위젯 탭 후 `schoolhelper://timetable` 또는 `schoolhelper://settings` 라우팅 확인.
- full App Group 실기기 빌드/설치는 앱/위젯 profile App Group 갱신 후 통과했다.

---

## 3. 실기기 상태

현재 확보된 증거:

- 2026-05-28 `ios/scripts/verify_ios_local_readiness.sh` 로 로컬 통합 검증 경로를 고정했다.
- 2026-05-28 `RUN_LIVE_BACKEND=1 DERIVED_DATA_PATH=/tmp/misschool-ios-local-readiness-live-final ios/scripts/verify_ios_local_readiness.sh` 로 Python/Shell 문법, SwiftPM 62 tests, 위젯 simulator packaging, live NEIS/BFF/notice URL smoke, goal audit expected incomplete를 한 번에 확인했다.
- 2026-05-28 `ios/scripts/audit_ios_goal_readiness.py` 는 agent skill/spec/workspace/core feature/verifier artifact와 full App Group profiles를 `pass` 로 확인했지만, 시스템 수동 증거 대기로 `complete=false`, exit `20` 을 반환했다.
- `ios/scripts/validate_ios_system_evidence.py` 는 실제 iPhone 홈 화면 위젯/App Group/알림 UX 수동 증거 JSON이 모두 채워졌는지 검증한다.
- `ios/scripts/record_ios_system_evidence.py` 는 실제로 눈검증한 UX 항목만 명시적 flag로 `ios/system-evidence.local.json` 에 기록한다.
- iPhone 15 Pro 실기기에 `device-preview` 모드로 앱 설치/실행 성공.
- `device-preview` 모드는 App Group entitlement를 제외하므로 앱 본체 확인용이다.
- 2026-05-28 `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-2tj-device-preview ENTITLEMENTS_MODE=device-preview ios/scripts/install_device.sh` 로 현재 Xcode 계정의 Team ID와 signing-required 설정을 재확인했다. 결과는 `BUILD SUCCEEDED`, `App installed`; `codesign -vvv --strict` 통과. 앱 실행은 기기 잠금으로 `RequestDenied`/`Locked` 상태에서 중단됐다.
- 2026-05-28 `RUN_DEVICE_BUILD=1 TEAM_ID=2TJFP5788P DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer DERIVED_DATA_PATH=/tmp/misschool-ios-widget-app-group-readiness-now ios/scripts/verify_widget_app_group_readiness.sh` 로 simulator 위젯 smoke, App Group profile precheck, full App Group 실기기 build/install을 통과했다.
- 앱/위젯 산출물은 `codesign -vvv --strict` 를 통과했고, 둘 다 `com.apple.security.application-groups=[group.com.leebyungsun.schoolhelperios]` entitlement를 포함한다.
- 2026-05-28 `RUN_LIVE_BACKEND=1 DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer DERIVED_DATA_PATH=/tmp/misschool-ios-local-readiness-post-appgroup ios/scripts/verify_ios_local_readiness.sh` 로 SwiftPM 62 tests, 위젯 simulator packaging, App Group profile precheck, live NEIS/BFF smoke를 통합 재검증했다.
- 2026-05-29 `RUN_LIVE_BACKEND=1 RUN_SWIFT_TESTS=0 RUN_WIDGET_READINESS=1 DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer DERIVED_DATA_PATH=/tmp/misschool-ios-local-readiness-20260529-fixed ios/scripts/verify_ios_local_readiness.sh` 로 위젯 simulator packaging, App Group profile precheck, live NEIS/BFF smoke를 재검증했다. `20260529` 급식 row는 비어 있어 주변 날짜 fallback으로 `20260528` 급식 row를 확인했고, `20260529` 시간표는 `현장체험학습` row를 확인했다.
- `ios/scripts/verify_device_app_group_data.sh` 로 실제 iPhone App Group container의 `student_profile` 저장 여부를 복사/검증할 수 있게 고정했다.
- 2026-05-28 `DEVICE_ID=00008130-0012603E3CC3001C UNLOCK_WAIT_SECONDS=30 OUTPUT_DIR=/tmp/misschool-ios-device-app-group-data-resumed ios/scripts/verify_device_app_group_data.sh` 로 실제 iPhone App Group container의 `student_profile` 복사/검증을 통과했다.
- 2026-05-28 `DEVICE_ID=00008130-0012603E3CC3001C REMAINING_SECONDS=20 ROUTE_DELAY_SECONDS=1 STATUS_WAIT_SECONDS=3 STATUS_OUTPUT_DIR=/tmp/misschool-ios-device-notification-resumed SMOKE_RUN_ID=notification-resumed-1779963848 ios/scripts/verify_device_notification.sh` 로 알림 예약 상태 파일의 `authorized`/`scheduled=true`/`pending=true` 를 재확인했다.
- `ios/scripts/verify_device_parity.sh` 로 설치된 실기기 앱의 초기 설정 launch, seeded home, 주요 딥링크, running timer launch smoke를 반복 실행할 수 있다.
- 2026-05-28 `DEVICE_ID=buggyani ROUTE_DELAY_SECONDS=0 ios/scripts/verify_device_parity.sh` 로 주요 딥링크와 running timer launch 명령 성공을 확인했다.
- `ios/scripts/verify_device_notification.sh` 로 최신 설치 앱의 타이머 완료 알림 예약 smoke를 반복 실행할 수 있다.
- 2026-05-28 `DEVICE_ID=00008130-0012603E3CC3001C REMAINING_SECONDS=20 ROUTE_DELAY_SECONDS=1 ios/scripts/verify_device_notification.sh` 로 알림 smoke launch 성공을 확인했다. 실제 알림 배너 도착은 수동 확인 대기다.
- 2026-05-28 `DEVICE_ID=00008130-0012603E3CC3001C REMAINING_SECONDS=8 ROUTE_DELAY_SECONDS=1 ios/scripts/verify_device_notification.sh` 로 알림 smoke launch를 재확인했다.
- 2026-05-28 알림 smoke에 sandbox 상태 파일 검증을 추가했다. `SMOKE_RUN_ID=notification-status-v5 UNLOCK_WAIT_SECONDS=6 DEVICE_ID=00008130-0012603E3CC3001C REMAINING_SECONDS=20 ROUTE_DELAY_SECONDS=1 STATUS_WAIT_SECONDS=3 STATUS_OUTPUT_DIR=/tmp/misschool-ios-device-notification-status-v5 ios/scripts/verify_device_notification.sh` 는 기기 잠금 상태로 launch 거절을 감지하고 대기/재시도 후 exit 5로 종료했다.
- 2026-05-28 잠금 해제 후 `SMOKE_RUN_ID=notification-status-final-1779955391 UNLOCK_WAIT_SECONDS=20 DEVICE_ID=00008130-0012603E3CC3001C REMAINING_SECONDS=20 ROUTE_DELAY_SECONDS=1 STATUS_WAIT_SECONDS=3 STATUS_OUTPUT_DIR=/tmp/misschool-ios-device-notification-status-final TEAM_ID=2TJFP5788P ios/scripts/verify_device_notification.sh` 로 sandbox 상태 파일의 `scheduled=true`/`pending=true`/`authorizationStatus=authorized`/`runID` 일치를 확인했다. 상태 파일: `/tmp/misschool-ios-device-notification-status-final/schoolhelper-notification-smoke.json`.
- 2026-05-28 선택된 학교 표시 개선 후 `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-selected-school-smoke LAUNCH=0 ios/scripts/install_device.sh` 로 실제 iPhone 앱 본체 설치 성공을 확인했다.

제약:

- full App Group 실기기 빌드는 앱 profile과 위젯 profile이 모두 같은 App Group을 가져야 한다.
- 2026-05-28 `ios/scripts/check_app_group_profiles.py` 결과 앱/위젯 profile 모두 `OK` 다.
- 이전 widget profile App Group 누락 blocker는 해소됐다.
- 남은 제약은 실제 iPhone 홈 화면 위젯 배치/탭, App Group 공유 데이터의 눈검증, 시스템 알림 배너 UX 수동 증거다.
- 실제 iPhone UI 자동 테스트는 `ios/scripts/test_device_ui.sh` 로 signing override를 적용해 실행한다.
- 2026-05-28 `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-test-script ios/scripts/test_device_ui.sh` 로 실제 iPhone에서 초기 설정 학교 검색/선택/저장 UI 테스트가 `TEST SUCCEEDED` 로 통과했다.
- 2026-05-28 `ONLY_TESTING= TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-all-test ios/scripts/test_device_ui.sh` 로 실제 iPhone 전체 UI 테스트 6개가 모두 통과했다.
- 2026-05-28 simulator 전체 UI 테스트는 시간표/급식/일정 콘텐츠 검증을 포함해 9개가 모두 통과했다.
- 2026-05-28 9개 전체 UI 테스트의 실기기 재시도는 한 번 `Unlock buggyani to Continue`, 한 번 `Timed out while enabling automation mode.` 로 실패했다.
- `ios/scripts/test_device_ui.sh` 는 잠금 상태를 감지하면 `The iPhone is locked...` 메시지와 xcodebuild log 경로를 출력하고 exit 5로 종료한다. `UNLOCK_WAIT_SECONDS` 를 지정하면 잠금 해제를 일정 시간 기다릴 수 있다.
- `ios/scripts/test_device_ui.sh` 는 `Timed out while enabling automation mode` 실패를 기본 1회 자동 재시도한다.
- 2026-05-28 같은 derived data로 재실행한 실제 iPhone 9개 전체 UI 테스트가 `TEST SUCCEEDED` 로 통과했다. xcresult: `/tmp/misschool-ios-device-ui-tabs-test-3/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_14-55-49-+0900.xcresult`
- 2026-05-28 `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-script-retry-smoke ios/scripts/test_device_ui.sh` 로 retry wrapper 기본 경로의 1개 UI 테스트 통과를 확인했다.
- 2026-05-28 `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-setup-search-test UNLOCK_WAIT_SECONDS=120 ios/scripts/test_device_ui.sh` 로 실제 iPhone 초기 설정 학교 검색/선택/저장 흐름을 다시 확인했다. xcresult: `/tmp/misschool-ios-device-setup-search-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_17-56-15-+0900.xcresult`
- 2026-05-28 `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-preview-install ios/scripts/install_device.sh` 로 실제 iPhone에 device-preview 앱을 설치/실행했다.
- 2026-05-28 `DATE=20260528 MONTH=202605 ios/scripts/verify_live_school_data.py` 로 미사중학교 live NEIS/BFF backend 데이터 계약과 첫 notice 상세 URL HTTP 200/제목 매칭을 확인했다.
- 2026-05-28 `ios/scripts/test_live_ui.sh` 로 simulator 앱 화면의 live NEIS/BFF 렌더링을 재확인했다. xcresult: `/tmp/misschool-ios-live-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_15-36-53-+0900.xcresult`
- 2026-05-28 `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-initial-search-check ios/scripts/test_device_ui.sh` 로 실제 iPhone 초기 설정 학교 검색/저장 흐름을 재확인했다. xcresult: `/tmp/misschool-ios-device-initial-search-check/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_15-38-27-+0900.xcresult`
- 2026-05-28 `LIVE_UI_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-live-ui-test ios/scripts/test_device_ui.sh` 로 실제 iPhone 앱 화면의 live NEIS/BFF 렌더링을 확인했다. xcresult: `/tmp/misschool-ios-device-live-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_15-40-03-+0900.xcresult`
- 2026-05-28 `ios/scripts/test_live_navigation_ui.sh` 로 simulator live 날짜 이동 UX를 확인했다. 시간표/급식/일정 제목이 각각 다음 날/다음 주/다음 달로 갱신됐다. xcresult: `/tmp/misschool-ios-live-navigation-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-15-26-+0900.xcresult`
- 2026-05-28 `LIVE_NAVIGATION_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-live-navigation-ui-test ios/scripts/test_device_ui.sh` 는 앱/테스트 빌드와 signing 후 실제 iPhone 잠금 상태로 중단됐다. log: `/tmp/misschool-ios-device-live-navigation-ui-test/test_device_ui.xcodebuild.log`
- 2026-05-28 같은 실기기 live 날짜 이동 테스트 재시도도 잠금 상태로 중단됐다. 다음 재시도는 `UNLOCK_WAIT_SECONDS=120` 으로 unlock 대기 가능. log: `/tmp/misschool-ios-device-live-navigation-ui-test-rerun/test_device_ui.xcodebuild.log`
- 2026-05-28 `UNLOCK_WAIT_SECONDS=20 LIVE_NAVIGATION_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-live-navigation-ui-test-ready ios/scripts/test_device_ui.sh` 로 실제 iPhone live 날짜 이동 UX를 확인했다. 시간표/급식/일정 제목이 각각 다음 날/다음 주/다음 달로 갱신됐다. xcresult: `/tmp/misschool-ios-device-live-navigation-ui-test-ready/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-34-27-+0900.xcresult`
- 2026-05-28 `ios/scripts/test_external_link_ui.sh` 로 simulator 가정통신문 외부 링크 전환과 URL/페이지 텍스트를 확인했다. xcresult: `/tmp/misschool-ios-external-link-url-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-27-48-+0900.xcresult`
- 2026-05-28 `EXTERNAL_LINK_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-external-link-test ios/scripts/test_device_ui.sh` 로 실제 iPhone 가정통신문 외부 링크 전환을 확인했다. xcresult: `/tmp/misschool-ios-device-external-link-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_15-52-21-+0900.xcresult`
- 2026-05-28 `ios/scripts/test_live_external_link_ui.sh` 로 simulator 운영 가정통신문 외부 링크 전환과 `misaj-m.goegh.kr`/공지 텍스트 노출을 확인했다. xcresult: `/tmp/misschool-ios-live-external-link-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_17-04-55-+0900.xcresult`
- 2026-05-28 `LIVE_EXTERNAL_LINK_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-live-external-link-ui-test ios/scripts/test_device_ui.sh` 로 실제 iPhone 운영 가정통신문 외부 링크 전환과 `misaj-m.goegh.kr`/공지 텍스트 노출을 확인했다. xcresult: `/tmp/misschool-ios-device-live-external-link-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_17-07-23-+0900.xcresult`
- 2026-05-28 `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild -project ios/SchoolHelperIOS.xcodeproj -scheme SchoolHelperIOSUI -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.3.1' -configuration Debug -derivedDataPath /tmp/misschool-ios-notification-permission-ui-test-final '-only-testing:SchoolHelperIOSUITests/SchoolHelperIOSUITests/testSettingsNotificationPermissionRequestUpdatesSummary' test` 로 simulator 알림 권한 설정 UI를 확인했다. xcresult: `/tmp/misschool-ios-notification-permission-ui-test-final/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-04-02-+0900.xcresult`
- 2026-05-28 `ONLY_TESTING=SchoolHelperIOSUITests/SchoolHelperIOSUITests/testSettingsNotificationPermissionRequestUpdatesSummary TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-notification-permission-ui-test ios/scripts/test_device_ui.sh` 로 실제 iPhone 알림 권한 설정 UI를 확인했다. xcresult: `/tmp/misschool-ios-device-notification-permission-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-01-31-+0900.xcresult`

따라서 현재 완료라고 말할 수 있는 범위:

- iOS 앱 본체의 핵심 기능 구현
- simulator UI 테스트
- 실제 iPhone UI 테스트(9개 자동 테스트 기준)
- SwiftPM 코어 회귀 테스트
- 실기기 앱 본체 설치/실행
- live NEIS/BFF backend 데이터 및 첫 notice 상세 URL smoke
- live NEIS/BFF simulator UI 렌더링 smoke
- live NEIS/BFF simulator 날짜 이동 UX smoke
- live NEIS/BFF 실제 iPhone UI 렌더링 smoke
- live NEIS/BFF 실제 iPhone 날짜 이동 UX smoke
- seeded/live 가정통신문 외부 링크 Safari 전환 및 simulator/실제 iPhone URL·페이지 텍스트 smoke
- 알림 권한 설정 화면 안내/요청 버튼 UI smoke

아직 완료라고 말할 수 없는 범위:

- 홈 화면 WidgetKit 실제 배치/탭 end-to-end
- App Group 기반 앱/위젯 공유 데이터는 자동 smoke로 통과했고, 홈 화면 WidgetKit 렌더링 눈검증은 별도 수동 확인 필요
- 실기기 시스템 권한 팝업/완료 알림 배너 UX
- 실제 iPhone Safari에서 운영 notice 웹페이지 콘텐츠 렌더링 눈검증

---

## 4. 다음 작업 우선순위

1. 실제 iPhone 홈 화면에 위젯을 배치해 오늘/내일 시간표와 탭 라우팅을 확인한다.
2. 타이머를 1분 이하로 시작해 실기기 알림 권한 요청과 완료 알림을 확인한다.
3. 확인 결과를 `ios/system-evidence.local.json` 에 기록하고 `ios/scripts/validate_ios_system_evidence.py` 를 실행한다.
4. `verify_live_school_data.py` 로 live backend 계약을 재확인한 뒤, 실제 iPhone Safari에서 운영 notice 웹페이지 렌더링을 눈으로 확인한다.
