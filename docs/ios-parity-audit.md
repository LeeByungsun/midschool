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

플랫폼 차이:

- Android의 `TimerBootReceiver`와 같은 재부팅 브로드캐스트는 iOS에 동일 개념이 없다.
- iOS는 저장된 target date 기반 복구와 `UNUserNotificationCenter` 예약으로 대응한다.

남은 수동 확인:

- 실제 iPhone에서 알림 권한 요청 UI, 완료 알림, 소리/진동 체감 확인.

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

상태: 구현 및 코어 검증됨

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
- `ios/scripts/verify_live_school_data.py` 로 live web `/api/notices` BFF item 확인
- UI 테스트 `testLiveSchoolDataDisplaysBackendContent` 로 simulator 홈 화면 렌더링 확인
- 2026-05-28 미사중학교 기준 sample `2026학년도미사 오케스트라 아침 맞이 콘서트일정 안내` 확인

남은 수동 확인:

- 실기기 화면에서 실제 가정통신문 렌더링과 외부 링크 전환 확인.

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

남은 검증:

- 실제 홈 화면에 위젯을 배치하고 오늘/내일 시간표가 표시되는지 확인.
- 위젯 탭 후 `schoolhelper://timetable` 또는 `schoolhelper://settings` 라우팅 확인.
- full App Group 실기기 빌드는 widget provisioning profile에 App Group entitlement가 필요하다.

---

## 3. 실기기 상태

현재 확보된 증거:

- iPhone 15 Pro 실기기에 `device-preview` 모드로 앱 설치/실행 성공.
- `device-preview` 모드는 App Group entitlement를 제외하므로 앱 본체 확인용이다.
- `ios/scripts/verify_device_parity.sh` 로 설치된 실기기 앱의 초기 설정 launch, seeded home, 주요 딥링크, running timer launch smoke를 반복 실행할 수 있다.
- 2026-05-28 `DEVICE_ID=buggyani ROUTE_DELAY_SECONDS=0 ios/scripts/verify_device_parity.sh` 로 주요 딥링크와 running timer launch 명령 성공을 확인했다.
- `ios/scripts/verify_device_notification.sh` 로 최신 설치 앱의 타이머 완료 알림 예약 smoke를 반복 실행할 수 있다.
- 2026-05-28 `DEVICE_ID=00008130-0012603E3CC3001C REMAINING_SECONDS=20 ROUTE_DELAY_SECONDS=1 ios/scripts/verify_device_notification.sh` 로 알림 smoke launch 성공을 확인했다. 실제 알림 배너 도착은 수동 확인 대기다.
- 2026-05-28 `DEVICE_ID=00008130-0012603E3CC3001C REMAINING_SECONDS=8 ROUTE_DELAY_SECONDS=1 ios/scripts/verify_device_notification.sh` 로 알림 smoke launch를 재확인했다.
- 2026-05-28 선택된 학교 표시 개선 후 `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-selected-school-smoke LAUNCH=0 ios/scripts/install_device.sh` 로 실제 iPhone 앱 본체 설치 성공을 확인했다.

제약:

- full App Group 실기기 빌드는 앱 profile과 위젯 profile이 모두 같은 App Group을 가져야 한다.
- 현재 외부 provisioning 상태에서는 위젯 profile의 App Group entitlement가 비어 있어 full App Group 검증이 막힌다.
- 실제 iPhone UI 자동 테스트는 `ios/scripts/test_device_ui.sh` 로 signing override를 적용해 실행한다.
- 2026-05-28 `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-test-script ios/scripts/test_device_ui.sh` 로 실제 iPhone에서 초기 설정 학교 검색/선택/저장 UI 테스트가 `TEST SUCCEEDED` 로 통과했다.
- 2026-05-28 `ONLY_TESTING= TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-all-test ios/scripts/test_device_ui.sh` 로 실제 iPhone 전체 UI 테스트 6개가 모두 통과했다.
- 2026-05-28 simulator 전체 UI 테스트는 시간표/급식/일정 콘텐츠 검증을 포함해 9개가 모두 통과했다.
- 2026-05-28 9개 전체 UI 테스트의 실기기 재시도는 한 번 `Unlock buggyani to Continue`, 한 번 `Timed out while enabling automation mode.` 로 실패했다.
- `ios/scripts/test_device_ui.sh` 는 잠금 상태를 감지하면 `The iPhone is locked...` 메시지와 xcodebuild log 경로를 출력하고 exit 5로 종료한다.
- `ios/scripts/test_device_ui.sh` 는 `Timed out while enabling automation mode` 실패를 기본 1회 자동 재시도한다.
- 2026-05-28 같은 derived data로 재실행한 실제 iPhone 9개 전체 UI 테스트가 `TEST SUCCEEDED` 로 통과했다. xcresult: `/tmp/misschool-ios-device-ui-tabs-test-3/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_14-55-49-+0900.xcresult`
- 2026-05-28 `TEAM_ID=2TJFP5788P DERIVED_DATA_PATH=/tmp/misschool-ios-device-ui-script-retry-smoke ios/scripts/test_device_ui.sh` 로 retry wrapper 기본 경로의 1개 UI 테스트 통과를 확인했다.
- 2026-05-28 `DATE=20260528 MONTH=202605 ios/scripts/verify_live_school_data.py` 로 미사중학교 live NEIS/BFF backend 데이터 계약을 확인했다.
- 2026-05-28 `ios/scripts/test_live_ui.sh` 로 simulator 앱 화면의 live NEIS/BFF 렌더링을 확인했다. xcresult: `/tmp/misschool-ios-live-ui-test/Logs/Test/Test-SchoolHelperIOSUI-2026.05.28_15-24-15-+0900.xcresult`

따라서 현재 완료라고 말할 수 있는 범위:

- iOS 앱 본체의 핵심 기능 구현
- simulator UI 테스트
- 실제 iPhone UI 테스트(9개 자동 테스트 기준)
- SwiftPM 코어 회귀 테스트
- 실기기 앱 본체 설치/실행
- live NEIS/BFF backend 데이터 smoke
- live NEIS/BFF simulator UI 렌더링 smoke

아직 완료라고 말할 수 없는 범위:

- 홈 화면 WidgetKit 실제 배치/탭 end-to-end
- App Group 기반 앱/위젯 공유 데이터 실기기 end-to-end
- 실기기 알림 권한/완료 알림 UX
- 실제 iPhone 화면의 live 데이터 날짜 이동/외부 링크 UX 눈검증

---

## 4. 다음 작업 우선순위

1. Apple Developer / Xcode에서 위젯 extension profile에 `group.com.leebyungsun.schoolhelperios` App Group을 반영한다.
2. `ios/scripts/check_app_group_profiles.py` 결과가 앱/위젯 모두 `OK` 인지 확인한다.
3. `ENTITLEMENTS_MODE=app-groups TEAM_ID=... ios/scripts/install_device.sh` 로 full App Group 빌드를 실행한다.
4. 실제 iPhone 홈 화면에 위젯을 배치해 오늘/내일 시간표와 탭 라우팅을 확인한다.
5. 타이머를 1분 이하로 시작해 실기기 알림 권한 요청과 완료 알림을 확인한다.
6. `verify_live_school_data.py` 로 live backend 계약을 재확인한 뒤, 실제 iPhone 화면에서 날짜 이동/주간 스크롤/외부 링크 UX를 눈으로 확인한다.
