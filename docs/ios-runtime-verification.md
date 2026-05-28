# 학교도우미 iOS 런타임 검증 메모

기준일: 2026-05-28

이 문서는 `ios/` 앱의 현재 런타임 검증 경로와 확보된 증거를 정리합니다.

---

## 1. 현재 검증 방식

현재 iOS 앱은 아래 4단계로 검증합니다.

1. SwiftPM 코어 회귀 테스트
2. Xcode simulator build
3. Xcode simulator UI 테스트
4. `simctl` seeded launch + screenshot

핵심 이유:
- 이 환경에서는 일반적인 앱 빌드/실행은 가능함
- 하지만 시스템 확인 다이얼로그 자동 승인과 홈 화면 위젯 배치는 제한적임
- 그래서 simulator launch override 를 통해 핵심 화면/상태를 직접 재현함

---

## 2. 주요 검증 명령

### 2.1 코어 테스트

```bash
cd ios
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer swift test
```

2026-05-28 최신 확인:

- 56 tests passed
- 가정통신문 BFF query/응답 매핑
- 가정통신문 날짜 누락 시 Android와 동일하게 제목만 표시
- 타이머 알림 OFF 시 권한 요청/완료 알림 예약 생략
- 설정에서 타이머 알림 OFF 저장 시 이미 예약된 완료 알림 취소
- 초기 설정/설정 학교 검색에서 늦게 끝난 이전 검색 결과 무시
- 기존 저장값에 학교명만 있고 학교 코드가 없으면 재검색/재선택 안내
- 학생 설정 완료 판정에서 공백 문자열을 미완료로 처리

### 2.2 앱 빌드

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
xcodebuild \
  -project ios/SchoolHelperIOS.xcodeproj \
  -scheme SchoolHelperIOS \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.3.1' \
  -configuration Debug \
  -derivedDataPath /tmp/misschool-ios-run \
  CODE_SIGNING_ALLOWED=NO \
  build
```

### 2.2.1 Xcode 공유 스킴

- 앱 스킴: `SchoolHelperIOS`
- UI 테스트 스킴: `SchoolHelperIOSUI`
- 위젯 스킴: `SchoolHelperWidget`

위젯 스킴에는 아래 debug env 가 기본으로 들어 있습니다.

```text
_XCWidgetKind=SchoolHelperWidget
_XCWidgetFamily=medium
_XCWidgetDefaultView=timeline
```

### 2.2.2 UI 테스트

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
xcodebuild \
  -project ios/SchoolHelperIOS.xcodeproj \
  -scheme SchoolHelperIOSUI \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.3.1' \
  -derivedDataPath /tmp/misschool-ios-ui-setup-save \
  test
```

2026-05-28 최신 확인:

- 결과: `** TEST SUCCEEDED **`
- `SchoolHelperIOSUITests`: 9 tests, 0 failures
- 확인 범위: 초기 설정 학교 검색/선택/저장, 홈 탭 구조, 시간표 핵심 콘텐츠(`국어`, `수학`), 급식 핵심 콘텐츠(`비빔밥`, `712 kcal`), 일정 핵심 콘텐츠(`체육대회`, `중간고사`), 설정 modal, 설정 저장, 위젯 미리보기, 타이머 modal
- seeded 콘텐츠 테스트는 고유 `SCHOOLHELPER_SEED_PROFILE_JSON` 과 invalid `NEIS_BASE_URL`/`WEB_BASE_URL` 로 mock fallback을 결정적으로 사용한다.
- xcresult: `/tmp/misschool-ios-full-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_14-30-04-+0900.xcresult`

2026-05-28 주요 탭 콘텐츠 UI 테스트 재확인:

- 명령: `xcodebuild -project ios/SchoolHelperIOS.xcodeproj -scheme SchoolHelperIOSUI -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.3.1' -configuration Debug -derivedDataPath /tmp/misschool-ios-feature-tabs-ui-test -only-testing:SchoolHelperIOSUITests/SchoolHelperIOSUITests/testSeededTimetableShowsCoreContent -only-testing:SchoolHelperIOSUITests/SchoolHelperIOSUITests/testSeededMealsShowsCoreContent -only-testing:SchoolHelperIOSUITests/SchoolHelperIOSUITests/testSeededScheduleShowsCoreContent test`
- 결과: `** TEST SUCCEEDED **`
- `SchoolHelperIOSUITests`: 3 tests, 0 failures
- 확인 범위: 시간표/급식/일정 탭 직접 진입과 핵심 표시 콘텐츠
- xcresult: `/tmp/misschool-ios-feature-tabs-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_14-28-09-+0900.xcresult`

2026-05-28 초기 설정 학교 검색 재확인:

- 명령: `xcodebuild -project ios/SchoolHelperIOS.xcodeproj -scheme SchoolHelperIOSUI -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.3.1' -configuration Debug -derivedDataPath /tmp/misschool-ios-setup-search-test -only-testing:SchoolHelperIOSUITests/SchoolHelperIOSUITests/testInitialSetupSearchSelectsSchoolAndSavesProfile test`
- 결과: `** TEST SUCCEEDED **`
- 확인 범위: `미사중학교` 검색, `학교 1개를 찾았어요.` 표시, 1학년 2반 저장, 홈 화면 `미사중학교` 표시

2026-05-28 초기 설정 선택 상태 표시 재확인:

- 변경 목적: 정확히 1개 학교가 검색되면 자동 선택까지 완료되지만, 사용자가 검색 결과가 사라진 것으로 오해할 수 있어 `선택된 학교` 섹션을 명시 표시한다.
- 명령: `xcodebuild -project ios/SchoolHelperIOS.xcodeproj -scheme SchoolHelperIOSUI -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.3.1' -configuration Debug -derivedDataPath /tmp/misschool-ios-selected-school-ui-test -only-testing:SchoolHelperIOSUITests/SchoolHelperIOSUITests/testInitialSetupSearchSelectsSchoolAndSavesProfile test`
- 결과: `** TEST SUCCEEDED **`
- 확인 범위: `미사중학교` 검색, `학교 1개를 찾았어요.` 표시, `선택된 학교` 섹션 표시, 1학년 2반 저장, 홈 화면 진입
- xcresult: `/tmp/misschool-ios-selected-school-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_13-49-36-+0900.xcresult`

2026-05-28 초기 설정 공백 포함 학교명 검색 재확인:

- 변경 목적: 실제 입력에서 `미사 중학교`처럼 학교명 중간에 공백이 들어가면 NEIS가 0건을 반환하므로, 첫 검색이 비어 있을 때 공백 제거 검색어로 1회 재시도한다.
- 명령: `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer swift test --package-path ios --filter NEISClientTests/testSearchSchoolsRetriesWithoutWhitespaceWhenFirstSearchIsEmpty`
- 결과: 통과, 1 test, 0 failures
- 확인 범위: 첫 요청 `SCHUL_NM=미사 중학교`가 비면 두 번째 요청 `SCHUL_NM=미사중학교`로 재시도하고 `미사중학교` 결과를 반환함

2026-05-28 실제 iPhone 초기 설정 UI 테스트 시도:

- 명령: `xcodebuild -project ios/SchoolHelperIOS.xcodeproj -scheme SchoolHelperIOSUI -destination 'platform=iOS,id=00008130-0012603E3CC3001C' -configuration Debug -derivedDataPath /tmp/misschool-ios-real-device-setup-search-test -only-testing:SchoolHelperIOSUITests/SchoolHelperIOSUITests/testInitialSetupSearchSelectsSchoolAndSavesProfile DEVELOPMENT_TEAM=2TJFP5788P ENTITLEMENTS_MODE=device-preview test`
- 결과: 테스트 실행 전 `SchoolHelperIOSUITests-Runner` 설치 단계에서 중단
- 원인: test runner code signature verification 실패, `0xe8008018 (The identity used to sign the executable is no longer valid.)`
- 앱 본체 설치 확인: `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-selected-school-smoke LAUNCH=0 ios/scripts/install_device.sh` 는 `BUILD SUCCEEDED` 및 `App installed`

2026-05-28 실제 iPhone UI 테스트 signing override 재시도:

- 반복 스크립트: `ios/scripts/test_device_ui.sh`
- 동등 명령: `xcodebuild -project ios/SchoolHelperIOS.xcodeproj -scheme SchoolHelperIOSUI -destination 'platform=iOS,id=00008130-0012603E3CC3001C' -configuration Debug -derivedDataPath /tmp/misschool-ios-real-device-setup-search-signed-test -only-testing:SchoolHelperIOSUITests/SchoolHelperIOSUITests/testInitialSetupSearchSelectsSchoolAndSavesProfile DEVELOPMENT_TEAM=2TJFP5788P ENTITLEMENTS_MODE=device-preview CODE_SIGNING_ALLOWED=YES CODE_SIGNING_REQUIRED=YES CODE_SIGN_STYLE=Automatic CODE_SIGN_ENTITLEMENTS= -allowProvisioningUpdates test`
- 개선: test runner와 XCTest framework들이 `Apple Development: buggyani@hanmail.net (Q4Y5ZKMUHT)` 로 서명되고 기기에서 `SchoolHelperIOSUITests-Runner` 실행까지 진행됨
- 1차 결과: UI testing 초기화 단계에서 `LocalAuthentication Code=-4 "인증이 취소되었습니다."` 로 실패
- 2차 결과: `ios/scripts/test_device_ui.sh` 실행으로 `** TEST SUCCEEDED **`
- 성공 명령: `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-test-script ios/scripts/test_device_ui.sh`
- 확인 범위: 실제 iPhone에서 `미사중학교` 검색, `학교 1개를 찾았어요.` 표시, `선택된 학교` 표시, 1학년 2반 저장, 홈 화면 `미사중학교` 표시
- xcresult: `/tmp/misschool-ios-device-ui-test-script/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_14-00-31-+0900.xcresult`
- 해석: 기존 signing blocker는 `test_device_ui.sh` 의 signing override로 해소했다. 실기기 초기 설정 검색/선택/저장 자동 UI 테스트는 통과했다.

2026-05-28 실제 iPhone 전체 UI 테스트:

- 명령: `ONLY_TESTING= TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-all-test ios/scripts/test_device_ui.sh`
- 결과: `** TEST SUCCEEDED **`
- `SchoolHelperIOSUITests`: 6 tests, 0 failures
- 확인 범위: 초기 설정 학교 검색/선택/저장, 홈 탭 구조, 설정 modal 열기/닫기, 설정 저장, 설정 안 위젯 미리보기, 타이머 modal 직접 실행
- xcresult: `/tmp/misschool-ios-device-ui-all-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_14-05-55-+0900.xcresult`

2026-05-28 실제 iPhone 전체 UI 테스트 재시도:

- 명령: `ONLY_TESTING= TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-tabs-test ios/scripts/test_device_ui.sh`
- 결과: 테스트 시작 직후 중단
- 원인: 기기가 잠겨 있어 Xcode가 `Unlock buggyani to Continue` 상태로 대기함
- 해석: 잠금 상태에서는 9개 전체 UI 테스트를 시작할 수 없었다. 이후 잠금 해제 상태에서 재시도해 통과했다.

2026-05-28 실제 iPhone 전체 UI 테스트 잠금 감지 스크립트 재시도:

- 명령: `ONLY_TESTING= TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-tabs-test-2 ios/scripts/test_device_ui.sh`
- 결과: 앱/테스트 러너 빌드 후 기기 잠금 감지로 exit 5
- 감지 메시지: `The iPhone is locked. Unlock the device and rerun this script.`
- xcodebuild log: `/tmp/misschool-ios-device-ui-tabs-test-2/test_device_ui.xcodebuild.log`
- 해석: 장시간 대기 대신 명확한 실패 메시지와 로그 경로를 남기도록 스크립트를 보강했다.
- 후속 보강: `UNLOCK_WAIT_SECONDS=120` 처럼 설정하면 잠금 감지 직후 종료하지 않고 지정 시간 동안 unlock을 기다린다.

2026-05-28 실제 iPhone 9개 전체 UI 테스트 최종 재시도:

- 1차 명령: `ONLY_TESTING= TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-tabs-test-3 ios/scripts/test_device_ui.sh`
- 1차 결과: UI testing 초기화 단계에서 `Timed out while enabling automation mode.` 로 실패
- 후속 조치: `test_device_ui.sh` 에 automation mode timeout 기본 1회 자동 재시도를 추가
- 2차 명령: `ONLY_TESTING= TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-tabs-test-3 ios/scripts/test_device_ui.sh`
- 2차 결과: `** TEST SUCCEEDED **`
- `SchoolHelperIOSUITests`: 9 tests, 0 failures
- 확인 범위: 초기 설정 학교 검색/선택/저장, 홈 탭 구조, 시간표 핵심 콘텐츠, 급식 핵심 콘텐츠, 일정 핵심 콘텐츠, 설정 modal, 설정 저장, 위젯 미리보기, 타이머 modal
- xcresult: `/tmp/misschool-ios-device-ui-tabs-test-3/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_14-55-49-+0900.xcresult`
- retry wrapper smoke: `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-script-retry-smoke ios/scripts/test_device_ui.sh`
- retry wrapper smoke 결과: `** TEST SUCCEEDED **`, 1 test, 0 failures

실제 iPhone에서 live NEIS/BFF 화면 렌더링만 검증하려면 아래 전용 경로를 사용합니다.

```bash
LIVE_UI_TEST=1 TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh
```

`LIVE_UI_TEST=1` 은 UI test target에 `-DLIVE_UI_TEST_ENABLED` Swift flag를 주입하고,
외부 서비스 의존 테스트인 `testLiveSchoolDataDisplaysBackendContent` 만 실행합니다.
기본 전체 UI 테스트에서는 live 테스트가 skip 처리되므로 seeded 회귀 테스트가 외부 API 상태에 흔들리지 않습니다.

2026-05-28 실제 iPhone live NEIS/BFF UI 전용 테스트:

- 명령: `LIVE_UI_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-live-ui-test ios/scripts/test_device_ui.sh`
- 결과: `** TEST SUCCEEDED **`
- `SchoolHelperIOSUITests/testLiveSchoolDataDisplaysBackendContent`: 1 test, 0 failures
- 확인 범위: 홈/시간표/급식/일정 탭에서 live `수학`, `발아현미밥`, `오케스트라`, `노동절` 렌더링
- xcresult: `/tmp/misschool-ios-device-live-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_15-40-03-+0900.xcresult`

live 날짜 이동 제목 갱신만 검증하려면 아래 opt-in 경로를 사용합니다.

```bash
ios/scripts/test_live_navigation_ui.sh
LIVE_NAVIGATION_TEST=1 TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh
```

`LIVE_NAVIGATION_TEST=1` 은 UI test target에 `-DLIVE_UI_TEST_ENABLED` Swift flag를 주입하고,
외부 서비스 의존 테스트인 `testLiveDateNavigationUpdatesTitles` 만 실행합니다.

2026-05-28 simulator live 날짜 이동 UI 전용 테스트:

- 명령: `ios/scripts/test_live_navigation_ui.sh`
- 결과: `** TEST SUCCEEDED **`
- `SchoolHelperIOSUITests/testLiveDateNavigationUpdatesTitles`: 1 test, 0 failures
- 확인 범위: 시간표 `5월 28일 목요일` → `5월 29일 금요일`, 급식 `5월 25일 - 5월 29일` → `6월 1일 - 6월 5일`, 일정 `2026년 5월` → `2026년 6월` 제목 갱신
- xcresult: `/tmp/misschool-ios-live-navigation-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-15-26-+0900.xcresult`

2026-05-28 실제 iPhone live 날짜 이동 UI 전용 테스트:

- 명령: `LIVE_NAVIGATION_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-live-navigation-ui-test ios/scripts/test_device_ui.sh`
- 결과: 기기 잠금 상태로 테스트 시작 전 중단
- 감지 메시지: `The iPhone is locked. Unlock the device and rerun this script.`
- xcodebuild log: `/tmp/misschool-ios-device-live-navigation-ui-test/test_device_ui.xcodebuild.log`
- xcresult: `/tmp/misschool-ios-device-live-navigation-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-17-42-+0900.xcresult`
- 재시도 명령: `LIVE_NAVIGATION_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-live-navigation-ui-test-rerun ios/scripts/test_device_ui.sh`
- 재시도 결과: 동일하게 기기 잠금 상태로 테스트 시작 전 중단
- 재시도 log: `/tmp/misschool-ios-device-live-navigation-ui-test-rerun/test_device_ui.xcodebuild.log`
- 해석: 앱/테스트 빌드와 signing은 진행됐지만, 실제 iPhone 잠금 때문에 UI 실행 증거는 아직 미확보. 다음 재시도는 `UNLOCK_WAIT_SECONDS=120` 을 함께 지정해 unlock 대기 가능.
- unlock 대기 후 통과 명령: `UNLOCK_WAIT_SECONDS=20 LIVE_NAVIGATION_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-live-navigation-ui-test-ready ios/scripts/test_device_ui.sh`
- unlock 대기 후 결과: `** TEST SUCCEEDED **`
- `SchoolHelperIOSUITests/testLiveDateNavigationUpdatesTitles`: 1 test, 0 failures
- 통과 확인 범위: 실제 iPhone에서 시간표 `5월 28일 목요일` → `5월 29일 금요일`, 급식 `5월 25일 - 5월 29일` → `6월 1일 - 6월 5일`, 일정 `2026년 5월` → `2026년 6월` 제목 갱신
- 통과 xcresult: `/tmp/misschool-ios-device-live-navigation-ui-test-ready/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-34-27-+0900.xcresult`

2026-05-28 실제 iPhone 초기 설정 검색 재확인:

- 명령: `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-initial-search-check ios/scripts/test_device_ui.sh`
- 결과: `** TEST SUCCEEDED **`
- 확인 범위: `미사중학교` 검색 → `학교 1개를 찾았어요.` → `선택된 학교` 표시 → 학년/반 저장 → 홈 진입
- xcresult: `/tmp/misschool-ios-device-initial-search-check/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_15-38-27-+0900.xcresult`

가정통신문 외부 링크 전환만 검증하려면 아래 opt-in 경로를 사용합니다.

```bash
ios/scripts/test_external_link_ui.sh
EXTERNAL_LINK_TEST=1 TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh
```

`EXTERNAL_LINK_TEST=1` 은 UI test target에 `-DEXTERNAL_LINK_TEST_ENABLED` Swift flag를 주입하고,
seeded fallback notice의 `가정통신문 열기` 버튼이 Safari를 foreground로 전환하고, URL/페이지 텍스트에 `example.com` 또는 `Example Domain` 이 노출되는지 확인합니다.
기본 UI 회귀 테스트에서는 외부 앱 전환 테스트가 skip 처리되므로 Safari 상태에 흔들리지 않습니다.

2026-05-28 simulator 가정통신문 외부 링크 전용 테스트:

- 명령: `ios/scripts/test_external_link_ui.sh`
- 결과: `** TEST SUCCEEDED **`
- `SchoolHelperIOSUITests/testNoticeButtonOpensExternalSafariURL`: 1 test, 0 failures
- 확인 범위: seeded 홈 `현장학습 안내` 표시 → `가정통신문 열기` 탭 → Safari foreground 전환 → `example.com` URL/페이지 텍스트 확인
- xcresult: `/tmp/misschool-ios-external-link-url-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-27-48-+0900.xcresult`

2026-05-28 실제 iPhone 가정통신문 외부 링크 전용 테스트:

- 명령: `EXTERNAL_LINK_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-external-link-test ios/scripts/test_device_ui.sh`
- 결과: `** TEST SUCCEEDED **`
- `SchoolHelperIOSUITests/testNoticeButtonOpensExternalSafariURL`: 1 test, 0 failures
- 확인 범위: seeded 홈 `현장학습 안내` 표시 → `가정통신문 열기` 탭 → Safari foreground 전환
- xcresult: `/tmp/misschool-ios-device-external-link-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_15-52-21-+0900.xcresult`

현재 검증하는 실제 상호작용:

- 초기 설정에서 `미사중학교`를 입력해 NEIS 공개 학교 검색 결과를 찾고 학년/반 저장 후 홈으로 진입함
- 홈 탭이 `More` 없이 표시됨
- 시간표 탭이 `국어`, `수학`을 표시함
- 급식 탭이 `비빔밥`, `712 kcal`를 표시함
- 일정 탭이 `체육대회`, `중간고사`를 표시함
- 홈에서 설정 modal 진입/닫기
- 설정 modal 저장 버튼으로 홈 복귀
- 설정 modal 안 위젯 미리보기 섹션 표시
- 타이머 modal 진입 시 닫기 버튼 표시


### 2.4 실제 iPhone 설치/실행

실제 기기 검증은 아래 조건이 필요합니다.

- iPhone Developer Mode 활성화
- Mac과 iPhone 페어링
- Xcode Apple Development signing identity
- `TEAM_ID` 환경변수

기본 설치 명령:

```bash
TEAM_ID=YOUR_TEAM_ID ios/scripts/install_device.sh
```

기본값은 `ENTITLEMENTS_MODE=device-preview` 입니다.
이 모드는 실기기에서 앱 화면을 빠르게 보기 위해 App Group entitlement를 빌드 명령에서 제외합니다.
따라서 앱 본체 확인은 가능하지만 홈 화면 위젯의 App Group 데이터 공유는 완전 검증이 아닙니다.

App Group까지 검증하려면 Apple Developer portal/provisioning profile에
`group.com.leebyungsun.schoolhelperios` capability가 반영되어 있어야 합니다.
먼저 로컬 profile 상태를 확인합니다.

```bash
ios/scripts/check_app_group_profiles.py
```

앱과 위젯 bundle id가 모두 `OK` 여야 합니다.
그 후 아래처럼 실행합니다.

```bash
ENTITLEMENTS_MODE=app-groups TEAM_ID=YOUR_TEAM_ID ios/scripts/install_device.sh
```

기본 `APP_GROUP_PROFILE_CHECK=strict` 는 로컬 profile precheck 실패 시 즉시 중단합니다.
Apple Developer portal에서 capability를 반영했지만 로컬 profile만 오래된 상태라면,
아래처럼 `warn` 모드로 `xcodebuild -allowProvisioningUpdates` 갱신 시도를 실행할 수 있습니다.

```bash
APP_GROUP_PROFILE_CHECK=warn ENTITLEMENTS_MODE=app-groups TEAM_ID=YOUR_TEAM_ID ios/scripts/install_device.sh
```

2026-05-28 기준 실제 확인 결과:

- `com.leebyungsun.schoolhelperios`: App Group 포함
- `com.leebyungsun.schoolhelperios.widget`: App Group 미포함
- `APP_GROUP_PROFILE_CHECK=warn ENTITLEMENTS_MODE=app-groups TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-app-groups-refresh-attempt LAUNCH=0 ios/scripts/install_device.sh` 로 Xcode profile 갱신/빌드를 시도했지만 widget profile mismatch로 실패함
  - 오류: `Provisioning profile "iOS Team Provisioning Profile: com.leebyungsun.schoolhelperios.widget" doesn't match the entitlements file's value for the com.apple.security.application-groups entitlement.`
- 최신 device-preview 설치 확인:
  - 커밋: `4ed3c2f`
  - 명령: `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-latest-4ed3c2f LAUNCH=1 ios/scripts/install_device.sh`
  - 기기: `00008130-0012603E3CC3001C`
  - 결과: `BUILD SUCCEEDED`, `App installed`, `Launched application`
  - 설치 bundle id: `com.leebyungsun.schoolhelperios`

따라서 현재 full App Group 실기기 빌드는 widget identifier에 App Group capability가 반영된 provisioning profile 갱신 전까지 실패합니다.

개발자 프로필 신뢰 오류가 나오면 iPhone에서 다음을 확인합니다.

1. `설정`
2. `일반`
3. `VPN 및 기기 관리`
4. Apple Development 프로필 신뢰

### 2.4.1 실기기 parity smoke

이미 설치된 앱에서 주요 딥링크/런치 상태를 빠르게 확인하려면 아래 스크립트를 사용합니다. 기본값은 현재 저장된 앱 데이터를 유지합니다.

```bash
ios/scripts/verify_device_parity.sh
```

초기 설정 화면과 fixture 프로필 저장까지 포함해 결정적으로 확인하려면 다음처럼 실행합니다.

```bash
RUN_FRESH_SETUP=1 SEED_PROFILE=1 ios/scripts/verify_device_parity.sh
```

설치부터 다시 수행하려면 다음처럼 실행합니다.

```bash
INSTALL=1 TEAM_ID=YOUR_TEAM_ID SEED_PROFILE=1 ios/scripts/verify_device_parity.sh
```

이 smoke는 앱 실행과 라우팅 성공을 자동화하지만, 사람이 화면을 보며 최종 UX를 확인해야 합니다.
홈 화면 WidgetKit 배치/탭과 타이머 완료 알림 수신은 별도 수동 검증 대상입니다.

2026-05-28 확인:

- 명령: `DEVICE_ID=buggyani ROUTE_DELAY_SECONDS=0 ios/scripts/verify_device_parity.sh`
- 결과: timetable/meals/schedule/settings/timer 딥링크 launch 및 running timer launch 성공
- 제한: 화면 내용은 사람이 직접 확인해야 하며, WidgetKit 홈 화면 배치/탭과 알림 완료 UX는 이 smoke로 증명하지 않음

### 2.4.2 실기기 타이머 알림 smoke

앱이 최신 코드로 설치된 상태에서 launch override 기반 타이머 완료 알림을 예약하려면 아래 스크립트를 사용합니다.

```bash
ios/scripts/verify_device_notification.sh
```

처음 실행 시 iOS 권한 팝업이 뜨면 허용한 뒤 한 번 더 실행합니다.

```bash
REMAINING_SECONDS=20 ios/scripts/verify_device_notification.sh
```

이 smoke는 `SCHOOLHELPER_SCHEDULE_TIMER_NOTIFICATION=1` 테스트 전용 환경변수로 실행 중 타이머 상태를 저장하고 알림 예약을 시도합니다.
단, 실제 알림 도착 여부는 iPhone을 잠그거나 앱을 백그라운드로 보낸 뒤 사람이 확인해야 합니다.

2026-05-28 확인:

- 사전 설치: `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-notification-smoke LAUNCH=0 ios/scripts/install_device.sh`
- 설치 결과: iPhone `00008130-0012603E3CC3001C` 에 `com.leebyungsun.schoolhelperios` 설치 성공
- smoke 명령: `DEVICE_ID=00008130-0012603E3CC3001C REMAINING_SECONDS=20 ROUTE_DELAY_SECONDS=1 ios/scripts/verify_device_notification.sh`
- smoke 결과: `Launched application with com.leebyungsun.schoolhelperios bundle identifier.`
- 2026-05-28 재확인 명령: `DEVICE_ID=00008130-0012603E3CC3001C REMAINING_SECONDS=8 ROUTE_DELAY_SECONDS=1 ios/scripts/verify_device_notification.sh`
- 재확인 결과: `Launched application with com.leebyungsun.schoolhelperios bundle identifier.`
- 제한: 실제 알림 배너 도착은 iPhone 잠금/백그라운드 상태에서 사람이 확인해야 하므로 최종 UX 검증은 아직 수동 확인 대기

알림 권한 설정 화면의 안내/요청 버튼 상태는 시스템 권한 팝업에 의존하지 않도록
`SCHOOLHELPER_NOTIFICATION_AUTHORIZATION_STATUS` launch override로 검증합니다.

2026-05-28 알림 권한 설정 UI 자동 검증:

- simulator 명령: `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild -project ios/SchoolHelperIOS.xcodeproj -scheme SchoolHelperIOSUI -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.3.1' -configuration Debug -derivedDataPath /tmp/misschool-ios-notification-permission-ui-test-final '-only-testing:SchoolHelperIOSUITests/SchoolHelperIOSUITests/testSettingsNotificationPermissionRequestUpdatesSummary' test`
- simulator 결과: `** TEST SUCCEEDED **`
- simulator xcresult: `/tmp/misschool-ios-notification-permission-ui-test-final/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-04-02-+0900.xcresult`
- 실제 iPhone 명령: `ONLY_TESTING=SchoolHelperIOSUITests/SchoolHelperIOSUITests/testSettingsNotificationPermissionRequestUpdatesSummary TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-notification-permission-ui-test ios/scripts/test_device_ui.sh`
- 실제 iPhone 결과: `** TEST SUCCEEDED **`
- 실제 iPhone xcresult: `/tmp/misschool-ios-device-notification-permission-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_16-01-31-+0900.xcresult`
- 확인 범위: 설정 화면의 `타이머 완료 알림을 받으려면 권한이 필요해요.` 안내 → `알림 권한 요청` 버튼 표시 → 탭 후 `알림 권한이 허용되어 있어요.` 안내로 갱신
- 제한: 실제 iOS 시스템 권한 팝업과 완료 알림 배너 도착은 여전히 별도 수동 확인 대상

### 2.4.3 live NEIS/BFF 데이터 smoke

iOS 앱이 사용하는 live 데이터 계약을 앱 외부에서 반복 검증하려면 아래 스크립트를 사용합니다.

```bash
DATE=20260528 MONTH=202605 ios/scripts/verify_live_school_data.py
```

확인 범위:

- NEIS `schoolInfo` 에서 `미사중학교` / `J10` / `7692129` 확인
- NEIS `mealServiceDietInfo` 급식 row 확인
- NEIS `misTimetable` 1학년 2반 시간표 row 확인
- NEIS `SchoolSchedule` 월간 일정 row 확인
- web `/api/notices` BFF live item 확인
- 첫 notice 상세 URL HTTP 200 및 제목 매칭 확인

2026-05-28 확인:

- 명령: `DATE=20260528 MONTH=202605 ios/scripts/verify_live_school_data.py`
- 결과: `{"status":"ok"}` JSON 출력
- sample: 급식 `발아현미밥`, 시간표 `수학`, 일정 `노동절`, 가정통신문 `2026학년도미사 오케스트라 아침 맞이 콘서트일정 안내`

live backend 응답이 실제 앱 화면까지 렌더링되는지 simulator에서 확인하려면 아래 opt-in UI 테스트를 사용합니다.

```bash
ios/scripts/test_live_ui.sh
```

2026-05-28 확인:

- 명령: `ios/scripts/test_live_ui.sh`
- 결과: `** TEST SUCCEEDED **`
- `SchoolHelperIOSUITests/testLiveSchoolDataDisplaysBackendContent`: 1 test, 0 failures
- 확인 범위: 홈/시간표/급식/일정 화면에서 live sample `수학`, `발아현미밥`, `오케스트라`, `노동절` 렌더링
- xcresult: `/tmp/misschool-ios-live-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_15-36-53-+0900.xcresult`

2026-05-28 실제 iPhone 확인:

- 명령: `LIVE_UI_TEST=1 TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-live-ui-test ios/scripts/test_device_ui.sh`
- 결과: `** TEST SUCCEEDED **`
- `SchoolHelperIOSUITests/testLiveSchoolDataDisplaysBackendContent`: 1 test, 0 failures
- 확인 범위: 홈/시간표/급식/일정 화면에서 live sample `수학`, `발아현미밥`, `오케스트라`, `노동절` 렌더링
- xcresult: `/tmp/misschool-ios-device-live-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_15-40-03-+0900.xcresult`
- 제한: 실제 iPhone live 데이터 렌더링은 자동 검증됐지만, 날짜 이동/외부 링크 전환 UX를 눈으로 증명하지는 않는다.

### 2.5 재현 스크립트

```bash
ios/scripts/capture_runtime_screens.sh
```

기본적으로 아래 화면을 순서대로 재생성합니다.

- home
- timetable
- meals
- schedule
- settings
- timer
- timer-running-1 / timer-running-2

출력 경로 기본값은 `/tmp/misschool-ios-captures` 입니다.

딥링크 확인 다이얼로그 재현:

```bash
ios/scripts/capture_deeplink_prompt.sh
```

기본 출력 경로는 `/tmp/deeplink-confirm.png` 입니다.

### 2.6 시뮬레이터 launch override

공통 환경:

```bash
SIMCTL_CHILD_SCHOOLHELPER_SEED_PROFILE=fixture
SIMCTL_CHILD_SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST=1
```

초기 설정을 강제로 다시 열기:

```bash
SIMCTL_CHILD_SCHOOLHELPER_RESET_PROFILE=1
```

탭 직접 검증:

```bash
SIMCTL_CHILD_SCHOOLHELPER_INITIAL_ROUTE=home
SIMCTL_CHILD_SCHOOLHELPER_INITIAL_ROUTE=timetable
SIMCTL_CHILD_SCHOOLHELPER_INITIAL_ROUTE=meals
SIMCTL_CHILD_SCHOOLHELPER_INITIAL_ROUTE=schedule
SIMCTL_CHILD_SCHOOLHELPER_INITIAL_ROUTE=timer
SIMCTL_CHILD_SCHOOLHELPER_INITIAL_ROUTE=settings
```

실행 중 타이머 검증:

```bash
SIMCTL_CHILD_SCHOOLHELPER_TIMER_PRESET=shortBreak
SIMCTL_CHILD_SCHOOLHELPER_TIMER_REMAINING_SECONDS=123
SIMCTL_CHILD_SCHOOLHELPER_TIMER_RUNNING=1
```

---

## 3. 확보된 simulator 증거

### 3.1 초기 설정 / 홈

- `setup-after-reset.png`
- `home-no-more.png`

확인 내용:
- 초기 설정 화면 진입
- 홈 대시보드 표시
- 설정 버튼 표시
- `More` 없는 핵심 탭 구조

### 3.2 시간표 / 급식 / 일정

- `timetable-route-override.png`
- `meals-route-final.png`
- `schedule-route-reinstalled.png`

확인 내용:
- 시간표 일간 화면
- 급식 주간 화면
- 일정 월간 화면
- 급식 날짜 중복 버그 수정 반영
- 일정 날짜 포맷 개선 반영

### 3.3 타이머

- `home-timer-controls-clean-2.png`
- `timer-close-button-inline.png`
- `timer-running-1.png`
- `timer-running-2.png`

확인 내용:
- 홈 quick controls
- 타이머 modal 닫기 버튼
- 실행 중 countdown 감소

### 3.4 설정 / 위젯 미리보기

- `settings-close-inline.png`
- `settings-widget-preview-shared.png`

확인 내용:
- 설정 modal 닫기 버튼
- 위젯 미리보기 카드
- 위젯/앱 미리보기 공통 렌더 경로

### 3.5 UI 자동 검증

검증 명령:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
xcodebuild \
  -project ios/SchoolHelperIOS.xcodeproj \
  -scheme SchoolHelperIOSUI \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.3.1' \
  -derivedDataPath /tmp/misschool-ios-ui-setup-save \
  test
```

2026-05-28 최신 결과:

- `** TEST SUCCEEDED **`
- 실제 iPhone 9개 전체 UI 테스트 기준 `Executed 9 tests, with 0 failures`

확인 내용:
- 초기 설정 NEIS 학교 검색/저장/홈 진입
- 홈 탭 기본 구조
- 설정 modal 열기/닫기
- 설정 안 위젯 미리보기
- 타이머 modal 닫기 버튼

### 3.6 딥링크

- `timetable-after-deeplink-success.png`
- `/tmp/deeplink-confirm.png` (`ios/scripts/capture_deeplink_prompt.sh` 로 재현 가능)

확인 내용:
- `schoolhelper://timetable` 호출 시
- 시스템이 앱 열기 확인 다이얼로그를 표시함
- 즉 URL scheme 등록은 정상

---

## 4. 현재 hard blocker

아직 직접 자동 검증이 어려운 항목:

1. 홈 화면에 실제 위젯 배치
2. 시스템 딥링크 확인 다이얼로그의 `열기` 승인 이후 최종 화면 전환
3. 실기기 시스템 권한 팝업/완료 알림 배너 UX

이유:
- `simctl` 기본 도구에는 홈 화면 위젯 배치 전용 명령이 확인되지 않음
- macOS 보조 접근/시스템 UI 자동화 권한 제약으로 시스템 확인 다이얼로그 승인 자동화가 제한됨
- 실기기 권한 팝업/로컬 알림 배너는 launch override 기반 앱 내부 UI 테스트만으로 충분히 대체되지 않음

---

## 5. 현재 해석

현재 iOS는 아래를 만족합니다.

- iOS 전용 skill 존재
- iOS 전용 스펙 문서 존재
- 팀 분석/개발 수행됨
- 핵심 기능 화면 구현됨
- 주요 탭 화면이 simulator 와 실제 iPhone 9개 UI 테스트에서 직접 확인됨
- 주요 상태 변화(타이머 감소)도 simulator 에서 직접 확인됨
- 위젯 콘텐츠는 앱 안 미리보기로 직접 확인됨
- live NEIS/BFF backend 데이터 계약과 첫 notice 상세 URL HTTP 로드는 `verify_live_school_data.py` 로 직접 확인됨
- live NEIS/BFF 데이터의 simulator 앱 화면 렌더링은 `test_live_ui.sh` 로 직접 확인됨
- live NEIS/BFF 데이터의 simulator 날짜 이동 UX는 `test_live_navigation_ui.sh` 로 직접 확인됨
- live NEIS/BFF 데이터의 실제 iPhone 앱 화면 렌더링은 `test_device_ui.sh` + `LIVE_UI_TEST=1` 로 직접 확인됨
- live NEIS/BFF 데이터의 실제 iPhone 날짜 이동 UX는 `test_device_ui.sh` + `LIVE_NAVIGATION_TEST=1` 로 직접 확인됨
- 가정통신문 외부 링크의 Safari 전환은 simulator 및 실제 iPhone에서 `EXTERNAL_LINK_TEST=1` 로 직접 확인됨. simulator에서는 URL/페이지 텍스트까지 확인됨
- 알림 권한 설정 화면의 안내/요청 버튼 상태 변화는 simulator 및 실제 iPhone에서 launch override 기반 UI 테스트로 직접 확인됨

하지만 아래는 아직 미완료입니다.

- 홈 화면 위젯의 실제 배치/탭 동작
- 시스템 확인 다이얼로그 이후 최종 전환
- 실기기 시스템 권한 팝업/완료 알림 배너 최종 UX
- 실제 iPhone Safari에서 운영 notice 웹페이지 콘텐츠 렌더링 눈검증

즉, 현재 상태는:

**“앱 본체 핵심 기능 parity 구현 및 simulator/실제 iPhone 자동 UI 검증, live NEIS/BFF backend/notice URL/simulator/실기기 UI smoke, simulator/실기기 live 날짜 이동 UX, 가정통신문 외부 링크 전환 smoke, 알림 권한 설정 UI smoke 완료”** 이지만
**“시스템 UI/홈 화면 위젯/App Group/알림 권한 UX까지 끝난 최종 완료”** 는 아님.

---

## 6. NEIS API 키 관리

iOS 앱은 NEIS API 키를 앱 번들에 저장하지 않습니다.

현재 구현:

- `NEISClient.Config.fromEnvironment()` 가 `ProcessInfo.processInfo.environment` 에서 값을 읽음
- 읽는 키:
  - `NEIS_API_KEY`
  - `NEIS_BASE_URL`
  - `WEB_BASE_URL`
- `NEIS_API_KEY` 가 있으면 `KEY` query item을 붙임
- `NEIS_API_KEY` 가 없으면 `KEY` query item을 생략함
- 학교 검색 결과가 없고 검색어 안에 공백이 있으면 공백 제거 검색어로 1회 재시도함
- 네트워크 오류 등으로 mock fallback을 사용할 때도 학교명 공백을 제거해 `미사 중학교` → `미사중학교` 검색을 허용함

학교 검색은 키 없이도 동작해야 하므로, 실기기 직접 실행에서도 `KEY` 없이 호출합니다.
키가 필요한 운영 요청은 iOS 앱에 키를 내장하지 말고 서버/BFF에서 처리하는 방향이 안전합니다.

실기기 앱 아이콘 직접 실행 시에는 환경변수가 전달되지 않습니다.
그래서 iOS 앱은 키가 없어도 기본 조회가 가능한 경로를 우선 사용해야 합니다.
