# 스쿨온 iOS 앱 현재 명세

- **기준일:** 2026-10-21
- **구현 대조 기준:** `main@687b6b8` 이후 iOS 오류 처리 개선분
- **기준 플랫폼:** Android 문서의 도메인·데이터 정책을 참고하되, 이 문서는 현재 iOS 구현과 남은 차이를 구분해 기록합니다.

## 1. 현재 범위

iOS 앱은 SwiftUI 기반 iPhone 앱과 WidgetKit 시간표 위젯으로 구성됩니다. 현재 구현은 학교 한 곳과 학년·반을 저장하는 **단일 학생 프로필** 구조입니다. Android의 자녀 표시 이름, 다중 프로필, 선택형 Firebase 동의 흐름은 아직 iOS에 구현되어 있지 않습니다.

현재 제공 기능:

- 초등학교·중학교·고등학교 검색 및 학교/교육청 코드 저장
- 홈의 오늘 시간표·급식·다가오는 일정·가정통신문·타이머 요약
- 날짜 이동형 일간 시간표, 주간 급식, 월간 학사 일정
- 집중/휴식/딥포커스 프리셋 타이머와 숫자/링 표시
- 타이머 상태 복원, 완료 로컬 알림, 완료 시 홈·상세 화면 배경 깜박임
- 작은/중간/큰 오늘·내일 시간표 위젯과 딥링크
- 네트워크 캐시와 가정통신문 BFF 연동

## 2. 화면과 사용자 흐름

### 2.1 초기 설정 (`SetupView`, `SetupViewModel`)

- 학교 이름을 두 글자 이상 입력해 검색 결과에서 학교를 선택합니다.
- 학교명, 교육청 코드, 학교 코드, 학교 종류와 학년·반을 저장합니다.
- 빠른 연속 검색에서는 이전 요청 결과가 최신 검색을 덮어쓰지 않습니다.
- 기존 저장값에 학교 코드가 없거나 검색어가 선택 학교와 달라지면 다시 선택하도록 안내합니다.
- 현재 자녀 표시 이름, 다중 프로필, 개인정보처리방침 링크, Analytics/오류 진단 동의 단계는 없습니다.

### 2.2 메인 탐색 (`RootTabView`)

- 하단 탭은 홈, 시간표, 급식, 일정으로 구성합니다.
- 설정과 타이머는 홈에서 sheet 또는 상세 탐색으로 엽니다.
- 설정이 완료되지 않으면 초기 설정 화면을 표시합니다.
- `schoolhelper://timetable`, `schoolhelper://settings` 등의 딥링크를 처리합니다.

### 2.3 홈 (`HomeView`, `HomeViewModel`)

- 오늘 날짜와 현재 학교·학년·반을 표시합니다.
- 오늘 시간표, 급식 메뉴·식사 구분·칼로리, 가까운 일정 최대 3개, 가정통신문 최대 3개를 요약합니다.
- 상단 `새로고침` 버튼으로 현재 데이터를 다시 요청합니다. Android의 당겨서 새로고침과 달리 pull-to-refresh는 아직 없습니다.
- 가정통신문은 첫 항목의 게시판 목록 URL(`sourceUrl`)을 우선 열고, 없으면 상세 URL을 사용합니다.
- 타이머 프리셋·시작/일시정지·리셋과 `타이머 전체 보기` 진입을 제공합니다.

### 2.4 시간표·급식·일정

- `TimetableView`: 이전/오늘/다음 날짜 이동과 교시별 과목 표시
- `MealsView`: 이전 주/이번 주/다음 주 이동과 평일 5일 급식·칼로리 표시
- `ScheduleView`: 이전 달/이번 달/다음 달 이동과 월간 일정 표시
- 각 ViewModel은 설정이 미완료된 경우 조회 대신 설정 필요 상태를 표시합니다.

### 2.5 설정 (`SettingsView`, `SettingsViewModel`)

- 현재 학교·학년·반 확인 및 단일 프로필 수정
- 학교 재검색/재선택
- 타이머 숫자/링 표시 모드, 로컬 알림 사용, 진동 옵션 저장
- 알림 권한 상태 표시 및 미결정 상태에서 권한 요청
- 위젯 미리보기와 `내일 시간표 표시` 설정
- 현재 Android에 있는 다중 자녀 관리, 개인정보처리방침, 오픈소스 라이선스, 앱 버전 표시는 없습니다.

## 3. 데이터와 네트워크

### 3.1 API

- 학교 검색: `hub/schoolInfo`
- 급식: `hub/mealServiceDietInfo`
- 학사 일정: `hub/SchoolSchedule`
- 초등학교 시간표: `hub/elsTimetable`
- 중학교 시간표: `hub/misTimetable`
- 고등학교 시간표: `hub/hisTimetable`
- 가정통신문: `${WEB_BASE_URL}/api/notices`
- 시간표 BFF fallback: NEIS 키가 없을 때 `${WEB_BASE_URL}/api/timetable`

`NEIS_BASE_URL` 기본값은 `https://open.neis.go.kr/`, `WEB_BASE_URL` 기본값은 `https://midschool.vercel.app/`입니다. NEIS API 키는 실행 환경에서 선택적으로 읽으며 앱 저장소에 저장하지 않습니다.

### 3.2 저장과 캐시

- `StudentPreferencesStore`: 단일 `StudentProfile`
- `TimerPreferencesStore`, `TimerSettingsStore`: 타이머 상태와 표시/알림 설정
- `WidgetSettingsStore`: 내일 시간표 표시 설정
- `SchoolDataCacheStore`: 급식 12시간, 시간표 24시간, 학사 일정 12시간
- 캐시 키는 Android와 같은 학교·학급·날짜/월 문맥을 포함합니다.
- 현재 iOS는 **네트워크 우선**이며 요청 실패 시 신선한 캐시를 사용합니다. Android의 캐시 우선 즉시 표시 후 최신 데이터 갱신 Flow와는 다릅니다.

### 3.3 오류와 빈 상태

`DefaultSchoolRepository`는 원격 조회가 실패하면 급식·시간표·학사 일정의 신선한 캐시를 먼저 확인하고, 캐시도 없으면 원래 오류를 호출부에 전달합니다. 학교 검색과 가정통신문도 실패를 샘플 데이터로 대체하지 않습니다. `MockSchoolRepository`는 테스트에서만 명시적으로 사용합니다.

홈은 기능별 조회 실패와 실제 빈 결과를 다른 문구로 표시하며 상단 새로고침으로 재시도할 수 있습니다. 시간표·급식·일정 화면은 조회 실패 안내와 `다시 시도` 동작을 제공하고, 선택한 날짜·주·월을 유지한 채 다시 조회합니다.

## 4. 타이머와 로컬 알림

- 프리셋: 집중 40분, 휴식 10분, 딥포커스 25분
- 앱을 다시 열면 저장된 목표 시각을 기준으로 남은 시간을 복원합니다.
- 시작 시 알림 사용 설정과 권한이 허용되어 있으면 `UNUserNotificationCenter`에 완료 알림을 예약합니다.
- 일시정지·리셋·알림 OFF 저장 시 예약된 완료 알림을 취소합니다.
- 완료 상태는 홈 타이머 영역과 상세 화면의 배경 깜박임으로도 전달합니다.
- 현재 `진동 사용` 값은 알림의 interruption level 선택에만 사용되고 `content.sound`는 항상 기본 사운드입니다. iOS 시스템 정책상 앱이 소리와 진동을 완전히 독립 제어한다고 보장할 수 없으므로 UI 문구와 실제 체감 동작을 출시 전에 다시 맞춰야 합니다.

## 5. 위젯

- 앱과 위젯은 App Group `group.com.lbs.shcoolhelper`의 `UserDefaults`를 공유합니다.
- 작은 위젯은 오늘 시간표, 중간/큰 위젯은 오늘·내일 시간표를 표시합니다.
- 긴 과목명은 Android 위젯과 같은 규칙으로 줄입니다.
- `WidgetConfigurationIntent`에서 위젯별 `내일 시간표 표시`를 선택합니다.
- 실행 중 타이머가 있으면 분 단위 또는 종료 시점에 맞춰 timeline을 갱신하고, 그 외에는 기본 30분 주기를 사용합니다.
- 위젯을 누르면 설정 미완료 시 설정, 완료 시 시간표 딥링크로 이동합니다.
- 현재 iOS가 단일 프로필이므로 Android처럼 위젯별 자녀 프로필을 선택할 수 없습니다.

## 6. 기술 구조

- 언어/UI: Swift 5.9 도구 체계, SwiftUI
- 최소 배포 대상: iOS 17.0
- 아키텍처: MVVM (`ObservableObject`, `@StateObject`)
- 비동기 처리: Swift Concurrency (`async`/`await`, `Task`, `MainActor`)과 Combine
- 네트워크: `URLSession`
- 저장: App Group 기반 `UserDefaults`
- 앱 프로젝트: `SchoolHelper.xcodeproj`
- Core 회귀 테스트: Swift Package `SchoolHelperIOSCore`

`SchoolHelper` 프로젝트명, bundle id, App Group, `schoolhelper://` 스킴은 기존 설치·서명·저장 호환성을 위한 기술 식별자이며 사용자 노출 서비스명은 `스쿨온`입니다.

## 7. Firebase·개인정보·법적 정보 상태

- iOS 앱은 `FirebaseCore`와 `FirebaseCrashlytics`를 연결합니다.
- 현재 저장소에는 `GoogleService-Info.plist`가 있으며 앱 시작 시 Crashlytics 수집을 명시적으로 활성화합니다.
- iOS에는 Firebase Analytics, 선택 동의 화면, Crashlytics 수집 토글, 개인정보처리방침 링크가 없습니다.
- 공개 개인정보처리방침은 Android의 선택 동의형 Analytics/Crashlytics 및 다중 프로필 동작을 기준으로 작성되어 있어 현재 iOS 동작을 그대로 설명하지 못합니다.
- 따라서 **iOS 배포 전** Android와 같은 사전 선택 동의·기본 OFF 경계를 구현하거나, 실제 수집 동작과 App Store 개인정보 응답·공개 방침을 일치시키는 별도 결정을 완료해야 합니다.

## 8. Android 기준 차이와 우선순위

| 우선순위 | 항목 | 현재 iOS 상태 |
|---|---|---|
| P0 | 개인정보처리방침·Crashlytics 동의 정합성 | Crashlytics 활성화, 앱 내 링크/선택 동의 없음 |
| P1 | 다중 자녀 프로필과 표시 이름 | 단일 프로필만 지원 |
| P1 | 홈 새로고침 UX | 상단 버튼만 제공, pull-to-refresh 없음 |
| P1 | 앱 정보 | 개인정보처리방침·OSS 라이선스·버전 화면 없음 |
| P2 | 캐시 우선 표시 | 네트워크 우선 + 실패 시 cache fallback |
| P2 | 위젯별 자녀 선택 | 단일 프로필이므로 내일 시간표 옵션만 제공 |

## 9. 검증 기준

```bash
cd ios
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer swift test
./scripts/verify_ios_local_readiness.sh
```

- Core 테스트는 프로필 검증, 학교 검색, 캐시, 화면 ViewModel, 타이머, 알림 권한, 위젯 스냅샷·timeline을 포함합니다.
- simulator/실기기, App Group signing, 실제 알림 체감, live NEIS/BFF 검증은 `ios/README.md`의 전용 스크립트를 사용합니다.
- 과거 실기기·live 검증 기록은 `docs/internal/ios-runtime-verification.md`에 보존하되, 현재 코드의 재검증 결과와 구분합니다.
