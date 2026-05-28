# 학교도우미 iOS 런타임 검증 메모

기준일: 2026-05-28

이 문서는 `ios/` 앱의 현재 런타임 검증 경로와 확보된 증거를 정리합니다.

---

## 1. 현재 검증 방식

현재 iOS 앱은 아래 3단계로 검증합니다.

1. SwiftPM 코어 회귀 테스트
2. Xcode simulator build
3. `simctl` seeded launch + screenshot

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

- 54 tests passed
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
  test
```

현재 검증하는 실제 상호작용:

- 홈 탭이 `More` 없이 표시됨
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

2026-05-28 기준 실제 확인 결과:

- `com.leebyungsun.schoolhelperios`: App Group 포함
- `com.leebyungsun.schoolhelperios.widget`: App Group 미포함

따라서 현재 full App Group 실기기 빌드는 widget provisioning profile 갱신 전까지 실패합니다.

개발자 프로필 신뢰 오류가 나오면 iPhone에서 다음을 확인합니다.

1. `설정`
2. `일반`
3. `VPN 및 기기 관리`
4. Apple Development 프로필 신뢰

### 2.3 재현 스크립트

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

### 2.3 시뮬레이터 launch override

공통 환경:

```bash
SIMCTL_CHILD_SCHOOLHELPER_SEED_PROFILE=fixture
SIMCTL_CHILD_SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST=1
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
  test
```

확인 내용:
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
3. 실기기 알림/권한 UX

이유:
- `simctl` 기본 도구에는 홈 화면 위젯 배치 전용 명령이 확인되지 않음
- macOS 보조 접근/시스템 UI 자동화 권한 제약으로 시스템 확인 다이얼로그 승인 자동화가 제한됨
- 실기기 권한 팝업/로컬 알림은 simulator 증거만으로 충분히 대체되지 않음

---

## 5. 현재 해석

현재 iOS는 아래를 만족합니다.

- iOS 전용 skill 존재
- iOS 전용 스펙 문서 존재
- 팀 분석/개발 수행됨
- 핵심 기능 화면 구현됨
- 주요 탭 화면이 simulator 에서 직접 확인됨
- 주요 상태 변화(타이머 감소)도 simulator 에서 직접 확인됨
- 위젯 콘텐츠는 앱 안 미리보기로 직접 확인됨

하지만 아래는 아직 미완료입니다.

- 홈 화면 위젯의 실제 배치/탭 동작
- 시스템 확인 다이얼로그 이후 최종 전환
- 실기기 알림/권한 최종 UX

즉, 현재 상태는:

**“핵심 기능은 구현 + 다수의 런타임 증거 확보”** 이지만  
**“시스템 UI/홈 화면 위젯/실기기 검증까지 끝난 최종 완료”** 는 아님.

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

학교 검색은 키 없이도 동작해야 하므로, 실기기 직접 실행에서도 `KEY` 없이 호출합니다.
키가 필요한 운영 요청은 iOS 앱에 키를 내장하지 말고 서버/BFF에서 처리하는 방향이 안전합니다.

실기기 앱 아이콘 직접 실행 시에는 환경변수가 전달되지 않습니다.
그래서 iOS 앱은 키가 없어도 기본 조회가 가능한 경로를 우선 사용해야 합니다.
