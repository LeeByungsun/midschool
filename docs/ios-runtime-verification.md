# 학교도우미 iOS 런타임 검증 메모

기준일: 2026-05-27

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

### 3.5 딥링크

- `timetable-after-deeplink-success.png`

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
