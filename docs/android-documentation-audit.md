# Android 구현 기준 문서 현행화 점검

기준: 2026-09-29, `main`의 `ae20ff6`. 이 문서는 Android 구현과 문서의 일치 여부를 점검한 목록이다. 앱 동작·화면의 근거는 `android/app/src/main/` 및 `android/app/build.gradle.kts`이며, 과거 QA 기록과 설계안은 현재 동작의 증거로 간주하지 않는다.

## 구현 기준

| 영역 | 현재 구현 근거 | 문서에 반영할 사실 |
|---|---|---|
| 이름·빌드 | `android/app/src/main/res/values/strings.xml`, `android/app/src/qa/res/values/strings.xml`, `android/app/build.gradle.kts` | 운영 이름 `스쿨온`, QA 이름 `스쿨온 QA`; `versionName=1.0`, `versionCode=4`; QA는 비난독화, release는 R8 적용. |
| 학생 프로필·초기 설정 | `data/profile/`, `SetupActivity.kt`, `ui/setup/SetupViewModel.kt`, `SettingsActivity.kt` | 복수 자녀 프로필을 기기에 저장·전환한다. 최초 설정에서 반 입력 키보드의 `완료`는 저장하지 않고 저장 버튼으로 포커스를 옮긴다. 명시적 저장 뒤 선택형 Analytics/진단 동의 단계로 이동한다. |
| 홈·상세 | `MainActivity.kt`, `activity_main.xml`, `strings.xml` | 홈의 학사 일정과 가정통신문 행동은 제목 우측의 `월간 보기`·`목록 열기`; 타이머는 홈에서 제어하며 상세 진입 버튼은 없다. `TimerActivity` 구현 자체는 남아 있다. |
| 학교 데이터 | `data/repository/SchoolRepositoryImpl.kt`, `ui/home/HomeViewModel.kt`, `util/ScheduleFilters.kt` | NEIS 기반 급식·시간표·학사 일정과 로컬 캐시, 웹 BFF 기반 가정통신문. 홈 학사 일정은 현재 월의 오늘 이후 항목 중 필터를 통과한 최대 3건을 요약한다. |
| 위젯·타이머 | `widget/`, `ui/timer/`, `timer/` | 프로필별 오늘/내일 시간표 위젯과 홈 포모도로 타이머. 완료 시 시스템 설정과 앱 설정을 고려한 시각·소리·진동 피드백을 제공한다. |
| 개인정보·관측 | `telemetry/`, `ui/setup/SetupViewModel.kt`, `ui/settings/SettingsViewModel.kt` | 분석과 오류 진단은 별도 선택이며 기본 수집은 꺼져 있다. 기존 법적 문구나 콘솔/실기기 검증 결과는 코드만 보고 새로 확정하지 않는다. |

## 문서별 조치 목록

| 문서 | 분류 | 판단 근거와 조치 |
|---|---|---|
| `README.md` | 수정 | 멀티플랫폼 프로젝트 이름과 Android 앱의 실제 설치 이름을 구분하고, 복수 프로필·홈 타이머 흐름을 반영한다. |
| `android/README.md` | 수정 | Android 개발 시작점의 옛 앱 이름과 빌드/화면 설명을 현행화한다. |
| `android/AGENTS.md` | 수정 | Android 하위 작업 규칙의 앱 이름·현재 구조 설명을 실제 코드에 맞춘다. 규칙 자체를 기능 변경 명령으로 확대하지 않는다. |
| `docs/project_specification.md` | 수정 | 초기 설정 키보드 행동, 홈 타이머 진입, 프로필·공지·빌드 설명 중 구현과 다른 항목을 갱신한다. 웹/iOS 구현을 Android와 같다고 추측하지 않는다. |
| `docs/project-structure.md` | 수정 | Android 패키지/화면/프로필·관측 경계의 현재 구조를 반영한다. |
| `DESIGN.md` | 수정 | 타이머 상세 버튼 권고와 실제 제거 결정의 충돌을 해소하고, 학사 일정·가정통신문 헤더 행동 및 현재 타이머 조작 위계를 기준으로 갱신한다. |
| `docs/android-ui-refresh-mockup.md` | 역사화/참조 보강 | 초기 제안 목업이 현재 화면의 규범처럼 읽히지 않도록 작성 시점·적용 여부와 `DESIGN.md` 기준을 명시한다. |
| `docs/android-app-todo.md` | 수정 | 구현 완료 항목과 남은 검증 과제를 구분한다. |
| `docs/android-release-readiness.md` | 수정 | 2026-09-15 QA 증거는 보존하되 옛 브랜치/테스트 수를 현재 출시 상태로 오인하지 않게 하고, 이후 변경으로 재검증이 필요한 항목을 표시한다. |
| `docs/android-firebase-observability.md` | 수정 | 현재 코드 대조 날짜와 출시 빌드의 서버 주소·조회 확인 항목을 덧붙인다. 과거 Firebase 콘솔 수신 기록은 그대로 유지한다. |
| `docs/feature-backlog.md`, `docs/platform-gap-todo.md` | 수정 | 이미 구현된 Android 항목을 미구현으로 남겨 두지 않도록 현행화하고 실제 미검증은 유지한다. |
| `docs/privacy/README.md` | 수정 | 이미 문안에 적힌 운영자·문의처를 빈 자리표시자로 취급하지 않도록 게시 절차를 정리한다. 실제 정보·Firebase 보관 기간·출시 URL은 운영 확인이 필요하다. |
| `docs/privacy-collection-summary.ko.md`, `docs/privacy-policy.ko.md` | 검토 후 유지 | 앱 내 수집·로컬 저장 설명과 비교하되, 개인정보처리방침의 법적 약속, 운영자 정보, 게시 URL은 추측으로 바꾸지 않는다. 게시·법무 검토 완료를 뜻하지 않는다. |
| `docs/work-summary.md` | 역사 기록 유지 | 당시 작업 타임라인은 소급 수정하지 않는다. 필요하면 최신 상태 문서 링크를 덧붙인다. |
| `docs/android-code-review.md`, `docs/android-school-selection-review.md` | 역사 기록 유지 | 특정 시점의 검토 결과로 보존한다. 현재 남은 문제라고 단정하지 않도록 기준일·후속 변경 링크를 확인한다. |
| `docs/android-studio-setup.md` | 수정 | 기존 NEIS 키 안내에 더해 `WEB_BASE_URL`의 빈 기본값과 단위 테스트·실제 조회의 차이를 설명한다. |
| `docs/firebase-crashlytics-setup.md` | 수정 | Android의 QA/운영 수집 전제와 별도 동의를 반영하고 설정 파일만으로 전송이 확인된다는 오해를 없앤다. iOS 확인 절차나 과거 콘솔 결과는 추정하지 않는다. |
| `docs/ui-mockups/android-ui-refresh-mockup.md`, `docs/ui-mockups/android-ui-refresh-verification.md` | 역사 자료 유지 | 과거 목업·검증 결과를 소급 수정하지 않는다. 현행 기준은 `DESIGN.md`와 Android XML이다. |
| `docs/mockups/android-ui-refresh/README.md`와 SVG 목업 자산 | 역사 자료 유지 | 당시 시안 자산을 현행 화면처럼 다시 그리지 않는다. |
| `docs/superpowers/plans/`, `docs/superpowers/specs/`의 기존 추적 문서 | 역사 자료 유지 | 당시 계획·설계 결정의 기록이다. 현재 기능 설명은 스펙·디자인 문서를 우선한다. |
| `android/scripts/team-school-selection-ab.sh`, `android/scripts/team-widget-timetable-check.sh` | 범위 밖의 레거시 스크립트 | 이전 패키지 경로를 담은 실행 절차를 현행 `android/README.md`에서 권장하지 않도록 제외했다. 스크립트 자체 수정은 별도 코드·검증 작업으로 남긴다. |
| `docs/ios-*.md`, `ios/README.md`, `web/README.md` | 이번 범위 밖 | Android 구현만으로 iOS/Web의 현재 상태를 변경하지 않는다. Android에 대한 명백한 잘못된 언급이 있을 때만 별도 근거로 수정한다. |

## 보존 및 검증 원칙

- 원래 작업 폴더의 미추적 파일 `docs/android-ios-ui-interaction-comparison.md`, `docs/store-assets/`, `docs/superpowers/plans/2026-09-20-high-risk-review-remediation.md`는 이번 커밋에 포함하지 않는다.
- 문서의 현재 기능 설명은 실제 Android 코드와 일치시킨다. 과거 실기기/콘솔 검사와 미래 계획을 현재 확인된 사실처럼 바꾸지 않는다.
- 통합 후 수정 파일 목록, Markdown 상대 경로, `git diff --check`, 구현과의 주요 불일치를 확인한다. 앱 코드 파일은 변경하지 않는다.
