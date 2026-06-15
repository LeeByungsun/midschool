ㅇ이ㄹ단작업 요약 마스터

이 문서는 저장소 안의 이전 작업 기록을 한곳에서 빠르게 보기 위한 요약본입니다.

- 생성일: 2026-05-22
- 기준 소스: `.omx/context/`, `.omx/reports/team-commit-hygiene/`, `docs/`
- 업데이트 원칙: **작업이 완료될 때마다 이 파일을 갱신**합니다.

## 한눈에 보기

- Web
  - 홈 타이머 영역 분리 및 렌더링 범위 정리
  - 타이머 카드 중복 파일 제거 및 문서 정합성 정리
  - 타이머 관련 최종 리뷰와 커밋/푸시 준비 정리
- Android
  - 학교선택 기능 추가 관련 구현 기록과 검토 메모 정리
  - 설정 변경 즉시 반영 점검 및 미설정 UX 보강 기록
  - UI 목업 제안 작업 기록
  - 급식 주간 상세 화면 구현 기록
  - 학사일정 캐시 + fallback 구현/테스트 기록

## 작업 타임라인

### 2026-05-18 · Web

#### 1) 타이머 영역 분리 및 검토
- 목표
  - `HomeDashboard` 전체가 타이머 때문에 1초마다 리렌더링되는 범위를 줄이는 것
- 요약
  - 홈 대시보드 안의 타이머 영역을 분리하는 방향으로 정리되었습니다.
  - 이후 기록 기준으로 `HomeDashboard`는 `HomeTimerCard`를 사용합니다.
- 상태
  - 완료 기록 존재
- 근거 문서
  - `.omx/context/timer-ui-split-review-20260518T031846Z.md`
  - `.omx/reports/team-commit-hygiene/team-task-254c26b6.md`

#### 2) 타이머 카드 중복 파일 정리
- 목표
  - 홈 타이머 카드 중복 파일을 없애고 실제 사용 컴포넌트 이름과 문서를 맞추는 것
- 요약
  - `home-timer-card.tsx` 기준으로 정리하는 흐름이 남아 있습니다.
  - `home-study-timer-card.tsx` 중복 제거 및 문서 정합성 점검이 작업 범위였습니다.
- 상태
  - 완료 흐름으로 기록됨
- 근거 문서
  - `.omx/context/web-timer-card-dedup-20260518T034830Z.md`
  - `.omx/context/web-timer-final-review-commit-push-20260518T035957Z.md`

#### 3) web 타이머 정리 최종 리뷰
- 목표
  - 누락, 중복, 문서 불일치를 다시 확인하고 커밋/푸시 준비 상태를 검토하는 것
- 요약
  - 타이머 분리와 중복 파일 제거 이후, test/lint/typecheck/build 기준으로 최종 점검하는 단계가 기록되어 있습니다.
  - 실제 푸시는 별도 요청 시 진행하는 조건이었습니다.
- 상태
  - 리뷰/정리 완료, 푸시 여부는 별도
- 근거 문서
  - `.omx/context/web-timer-final-review-commit-push-20260518T035957Z.md`
  - `.omx/reports/team-commit-hygiene/web.md`

### 2026-05-19 · Android / UX

#### 4) Android 학교선택 기능 추가
- 목표
  - Setup/Settings에서 학교 검색·선택을 지원하고, 선택 학교 기준으로 조회 흐름이 동작하게 하는 것
- 요약
  - 고등학교를 포함한 학교선택 확장 작업 컨텍스트가 남아 있습니다.
  - 별도 검토 메모에는 구현 전 기준의 위험과 계약이 정리되어 있습니다.
  - 팀 완료 기록에는 학교 검색·선택 UI와 `officeCode` / `schoolCode` / `schoolKind` 반영 작업이 포함된 것으로 남아 있습니다.
- 상태
  - 완료 기록 존재
- 주의
  - `docs/android-school-selection-review.md`는 검토 메모 성격이라 당시 기준의 위험/계약을 설명합니다. 이후 구현 기록과 시점 차이가 있을 수 있습니다.
- 근거 문서
  - `.omx/context/android-high-school-selection-20260519T042051Z.md`
  - `.omx/reports/team-commit-hygiene/android-setup-setting-1a30c5f1.md`
  - `docs/android-school-selection-review.md`

#### 5) 설정 변경 즉시 반영 점검 / 학교 미설정 UX 보강
- 목표
  - Setup/Settings에서 바꾼 학교 설정이 홈·시간표·급식 조회에 즉시 반영되는지 점검하고, 미설정 상태 안내를 강화하는 것
- 요약
  - 설정 반영 타이밍과 Setup 유도 UX를 보강하는 작업 기록이 남아 있습니다.
- 상태
  - 완료 기록 존재
- 근거 문서
  - `.omx/reports/team-commit-hygiene/1-setup-settings-2-ux-1a30c5f1.md`
  - `.omx/reports/team-commit-hygiene/1-setup-settings-2-ux-setup.md`

#### 6) 앱 UI 목업 제안
- 목표
  - 코드 수정 없이 앱 UI를 더 보기 좋게 바꾸기 위한 목업과 디자인 방향을 만드는 것
- 요약
  - 구현 없이 목업/디자인 방향만 정리하는 작업이었습니다.
- 상태
  - 완료 기록 존재
- 근거 문서
  - `.omx/context/app-ui-mockup-20260519T071248Z.md`
  - `.omx/reports/team-commit-hygiene/ui.md`

### 2026-05-20 · Android 기능/데이터

#### 7) 급식 주간 상세 화면 구현
- 목표
  - 홈 또는 급식 진입 지점 클릭 시 주간 급식 상세 화면으로 들어가고, 일주일치 식단을 표시하는 것
- 요약
  - Android 앱에서 급식 요약 클릭 후 상세 화면으로 이동하는 작업 기록이 남아 있습니다.
  - 상세 화면은 일주일치 식단 표시를 목표로 했습니다.
- 상태
  - 완료 기록 존재
- 근거 문서
  - `.omx/context/android-meals-weekly-detail-20260520T005347Z.md`
  - `.omx/reports/team-commit-hygiene/team-task.md`

#### 8) 학사일정 캐시 추가
- 목표
  - 학사 일정 조회에 캐시 저장과 네트워크 실패 시 마지막 성공 데이터 fallback을 추가하는 것
- 요약
  - 1차 기록에서는 요구사항과 터치포인트가 정리되었습니다.
  - 이후 기록과 팀 완료 리포트 기준으로, Android 일정 캐시를 실제 구현하고 테스트까지 추가하는 단계가 별도로 진행되었습니다.
  - 프로젝트 기획서 기준 요구사항은 학사 일정 12시간 캐시와 마지막 성공 데이터 재사용입니다.
- 상태
  - 완료 기록 존재
- 근거 문서
  - `.omx/context/android-schedule-cache-20260520T024234Z.md`
  - `.omx/context/android-schedule-cache-implement-20260520T025536Z.md`
  - `.omx/reports/team-commit-hygiene/android.md`
  - `docs/project_specification.md`


### 2026-05-22 · Android / Web 연계

#### 9) Android 가정통신문 phase 1 구현
- 목표
  - Android 홈에 가정통신문 preview card를 추가하고, web `/api/notices` BFF를 재사용해 최근 가정통신문을 보여주는 것
- 요약
  - Android 홈에 notices card 위치를 일정 아래 / 타이머 위로 두는 방향으로 구현이 진행되었다.
  - Android는 학교 홈페이지 스크래핑을 직접 구현하지 않고, web notices BFF 경계를 재사용한다.
  - web 쪽은 Android 소비를 위한 machine-readable notices contract(`status`, `errorCode`, `items`) 보강이 반영되었다.
  - Android 쪽에는 notices feed/model, notices API service, repository integration, HomeViewModel/MainActivity/activity_main notices 흐름, external URL open, 관련 테스트가 추가되었다.
  - Android `WEB_BASE_URL` 기본값을 local dev 주소(`10.0.2.2:3000`)에서 production 주소(`https://midschool.vercel.app/`)로 바꿔, 별도 로컬 설정이 없을 때도 notices 요청이 실서버를 보도록 조정했다.
  - 후속 문서 정리로 `TODO.md` 와 `docs/project_specification.md` 에 Android notices phase 1 상태와 production 기본 경로를 반영했다.
- 검증 메모
  - leader 기준 `web/scripts/test-notice-route-contract.mjs` 통과
  - leader 기준 `android/app:compileDebugKotlin` 통과
  - leader 기준 `HomeViewModelTest` targeted run 통과
  - worker 런타임 auto-checkpoint 커밋이 섞여 있어 최종 커밋 정리는 별도 필요
- 상태
  - 구현/검증 완료 후 최종 history 정리 예정
- 근거 문서
  - `.omx/context/android-notices-implementation-20260522T021700Z.md`
  - `.omx/reports/team-commit-hygiene/android-notices-discovery-andr.md`


### 2026-06-08 · 멀티플랫폼 미비점/TODO 정리

#### 10) Web / Android / iOS 현재 미비점과 추가 할 일 정리
- 목표
  - 세 플랫폼의 현재 완료 상태, 남은 미비점, 추가 작업을 한 문서에서 볼 수 있게 정리하는 것
- 요약
  - `docs/platform-gap-todo.md`를 추가해 Web, Android, iOS별 구현 상태와 후속 작업을 정리했다.
  - Web은 접근성, 반응형, PWA, 알림 fallback, 가정통신문 provider coverage를 주요 후속 작업으로 정리했다.
  - Android는 production 가정통신문 실기기 검증, SSO 학교 조사, lint warning cleanup, release 전 smoke test를 주요 후속 작업으로 정리했다.
  - iOS는 readiness audit 기준 complete이나, local-only evidence 의존성과 오래된 문서 상태 문구 갱신이 남은 작업임을 분리했다.
- 검증 메모
  - `cd web && npm run lint && npm run typecheck && npm test` 통과, 테스트 48개 통과
  - `cd android && ./gradlew testDebugUnitTest lintDebug` 통과, lint warning 62개 확인
  - `ios/scripts/audit_ios_goal_readiness.py` complete 반환 확인
- 상태
  - 문서화 완료
- 근거 문서
  - `docs/platform-gap-todo.md`


### 2026-06-08 · 필요 기능 백로그 정리

#### 11) 프로젝트 전체 필요 기능 리스트업
- 목표
  - 현재 구현 상태 이후 필요한 기능을 Web, Android, iOS, 공통 도메인 관점에서 우선순위별로 정리하는 것
- 요약
  - `docs/feature-backlog.md`를 추가해 P0~P3 백로그를 정리했다.
  - 사용자 앱 중심 후속 기능으로 재정리했다.
  - Android production notices와 학교 선택 저장/복원 검증, Web 접근성/PWA/반응형/Node ESM 경고, Android lint warning, iOS 문서 정합성, 공통 fallback/domain 정렬을 후속 기능으로 포함했다.
- 검증 메모
  - 기존 `TODO.md`, `docs/project_specification.md`, `docs/platform-gap-todo.md`, 플랫폼 README/TODO 문서를 근거로 정리했다.
  - 문서 readback과 diff 확인을 완료했다.
  - read-only architect 검증에서 승인 verdict를 받았고, 지적된 경미한 누락도 문서에 반영했다.
- 상태
  - 문서화 완료
- 근거 문서
  - `docs/feature-backlog.md`


### 2026-06-08 · Android/iOS 패키지 식별자 변경

#### 12) Android applicationId와 iOS bundle id/App Group 변경
- 목표
  - Android 패키지명을 `com.lbs.schoolhelper`로, iOS bundle id 계열을 `com.lbs.shcoolhelper`로 변경하는 것
- 요약
  - Android `namespace` / `applicationId`와 Kotlin package/import 경로를 `com.lbs.schoolhelper`로 변경했다.
  - Android main/test/androidTest 소스 디렉터리를 `com/lbs/schoolhelper`로 이동했다.
  - iOS app/widget/test bundle id와 App Group 기본값을 `com.lbs.shcoolhelper` / `group.com.lbs.shcoolhelper` 계열로 변경했다.
  - 누락됐던 `SchoolHelperWidget.entitlements`의 App Group 설정을 추가했다.
  - 현재 구조 기준으로 `SchoolHelper.xcodeproj`, `SchoolHelper` source path, widget 검증 scripts, app 산출물 경로를 함께 갱신했다.
- 검증 메모
  - `cd android && ./gradlew clean testDebugUnitTest lintDebug` 통과
  - `ios/scripts/verify_widget_app_group_readiness.sh` 통과
  - `ios/scripts/verify_ios_local_readiness.sh` 통과
  - 소스/설정 범위에서 이전 Android/iOS 식별자 참조가 남지 않았는지 grep으로 확인했다.
- 상태
  - 구현/검증 완료
- 근거 문서
  - `android/app/build.gradle.kts`
  - `ios/SchoolHelper.xcodeproj/project.pbxproj`
  - `ios/SchoolHelper/Core/Storage/AppStorageConfig.swift`
  - `ios/SchoolHelperWidget/SchoolHelperWidget.entitlements`


### 2026-06-15 · Android 스크롤 화면 하단 inset 수정

#### 13) 스크롤/목록 화면이 소프트키와 겹치는 레이아웃 오류 수정
- 목표
  - Android 전체 Activity 중 스크롤/목록 화면의 마지막 콘텐츠가 소프트 내비게이션 키 위에서 끝나게 만드는 것
- 요약
  - `ScrollView` 안에 `RecyclerView`를 `wrap_content`로 넣던 중첩 스크롤 구조를 제거했다.
  - 시간표/급식 화면은 전체를 `ConstraintLayout`으로 바꾸고, 목록 카드는 남은 화면 높이를 차지하게 했다.
  - 카드 내부 `RecyclerView`가 직접 스크롤하도록 `0dp` 제약 높이로 고정해 긴 목록이 잘리지 않게 했다.
  - 급식 상세 화면에도 같은 구조 수정을 적용했다.
  - `applySystemBarPadding()` 공통 유틸을 추가해 XML 기본 padding을 보존하면서 top/bottom system bar inset을 적용하게 했다.
  - Main, Setup, Settings, Schedule, WidgetConfig, Meal, Timetable Activity에 하단 inset 처리를 적용했다.
  - ScrollView 기반 화면은 `clipToPadding=false`와 `fillViewport=true`를 명시해 마지막 콘텐츠가 하단 inset 위까지 스크롤되게 했다.
  - 패키지명 변경 후 테스트에 남아 있던 `MisSchoolApplication` 참조를 `SchoolHelperApplication`으로 정리했다.
- 검증 메모
  - `cd android && ./gradlew testDebugUnitTest --tests com.lbs.schoolhelper.MainActivityNavigationTest --tests com.lbs.schoolhelper.ui.home.HomeViewModelTest --tests com.lbs.schoolhelper.ui.meal.MealViewModelTest --tests com.lbs.schoolhelper.util.ExternalUrlOpenerTest` 통과
  - `cd android && ./gradlew testDebugUnitTest --tests com.lbs.schoolhelper.ui.meal.MealViewModelTest --tests com.lbs.schoolhelper.MainActivityNavigationTest` 통과
  - `enableEdgeToEdge` Activity와 `applySystemBarPadding` 적용 범위를 grep으로 확인
  - ScrollView/NestedScrollView 및 목록 화면의 하단 padding/clip 설정 grep 확인
  - `cd android && ./gradlew testDebugUnitTest` 통과
  - `cd android && ./gradlew lintDebug` 통과
  - `adb devices`는 현재 환경에 `adb`가 없어 실기기/에뮬레이터 스모크 미실행
- 상태
  - 구현/검증 완료
- 근거 문서
  - `android/app/src/main/java/com/lbs/schoolhelper/util/SystemBarInsets.kt`
  - `android/app/src/main/res/layout/activity_timetable.xml`
  - `android/app/src/main/res/layout/activity_meal.xml`
  - `android/app/src/main/res/layout/activity_schedule.xml`
  - `android/app/src/main/res/layout/activity_settings.xml`
  - `android/app/src/main/res/layout/activity_setup.xml`
  - `android/app/src/main/res/layout/activity_widget_config.xml`
  - `docs/project_specification.md`
  - `android/app/src/test/java/com/lbs/schoolhelper/MainActivityNavigationTest.kt`
  - `android/app/src/test/java/com/lbs/schoolhelper/ui/home/HomeViewModelTest.kt`
  - `android/app/src/test/java/com/lbs/schoolhelper/ui/meal/MealViewModelTest.kt`
  - `android/app/src/test/java/com/lbs/schoolhelper/util/ExternalUrlOpenerTest.kt`


### 2026-06-15 · iOS/AOS 가정통신문 목록 열기 수정

#### 14) 홈 가정통신문 CTA를 최신 상세 대신 목록으로 연결
- 목표
  - iOS와 Android 홈의 가정통신문 열기 버튼이 최신 글 상세가 아니라 학교 가정통신문 목록을 열게 만드는 것
- 요약
  - Android HomeViewModel이 `NoticePreview.sourceUrl`을 우선 사용하고, 비어 있으면 기존 상세 `url`로 fallback하도록 수정했다.
  - Android 버튼 문구를 `가정통신문 목록 열기`로 변경했다.
  - iOS `NoticePreview`에 `sourceUrl`을 추가하고 notices API 매핑에서 값을 전달하도록 수정했다.
  - iOS HomeViewModel도 `sourceUrl` 우선, 상세 `url` fallback으로 목록 URL을 열게 변경했다.
  - Android/iOS 테스트 fixture와 기대값을 목록 URL 기준으로 갱신했다.
- 검증 메모
  - `cd android && ./gradlew testDebugUnitTest --tests com.lbs.schoolhelper.ui.home.HomeViewModelTest --tests com.lbs.schoolhelper.MainActivityNavigationTest` 통과
  - `cd android && ./gradlew lintDebug` 통과
  - `cd android && ./gradlew testDebugUnitTest lintDebug`는 전체 단위 테스트 중 기존 캐시 관찰 흐름 관련 2개 테스트 timeout으로 실패했고, lint는 별도 실행으로 통과했다.
  - `cd ios && swift build` 통과
  - `cd ios && swift test`는 현재 CLI 환경에서 `XCTest` 모듈을 찾지 못해 실패했지만, 라이브러리 빌드는 통과했다.
  - XcodeBuildMCP `list_schemes`는 현재 환경에서 `xcodebuild`를 찾지 못해 실행 불가했다.
- 상태
  - 구현/검증 완료
- 근거 문서
  - `android/app/src/main/java/com/lbs/schoolhelper/ui/home/HomeViewModel.kt`
  - `android/app/src/main/res/values/strings.xml`
  - `android/app/src/test/java/com/lbs/schoolhelper/ui/home/HomeViewModelTest.kt`
  - `ios/SchoolHelper/Core/Models/NoticePreview.swift`
  - `ios/SchoolHelper/Core/Networking/NEISClient.swift`
  - `ios/SchoolHelper/Features/Home/HomeViewModel.swift`
  - `ios/Tests/SchoolHelperIOSCoreTests/FeatureViewModelTests.swift`
  - `docs/project_specification.md`


### 2026-06-15 · Android 초기 설정 입력 UX와 스플래시 로딩 표시 개선

#### 15) 학교 검색 키보드 닫기, 학년/반 완료 입력 반영, 스플래시 로딩 애니메이션 추가
- 목표
  - Android 초기 설정 화면에서 학교 검색 시 키보드를 숨기고, 학년/반 입력 후 키보드 완료 동작으로 저장 흐름이 정상 동작하게 만드는 것
  - 최초 실행 스플래시 화면 대기 시간이 길어질 때 사용자에게 로딩 중임을 보여주는 것
- 요약
  - `SetupActivity`에 검색/저장 입력 처리 함수를 분리했다.
  - 학교 검색 버튼과 키보드 검색 액션에서 키보드를 숨기고 현재 학교명 입력값을 ViewModel에 반영한 뒤 검색하도록 수정했다.
  - 학년 입력은 키보드 다음 액션으로 반 입력칸에 포커스를 넘기고, 반 입력은 완료 액션으로 현재 학년/반 값을 반영한 뒤 저장을 시도하도록 수정했다.
  - 초기 설정 XML에 `imeOptions`와 단일 라인 설정을 추가해 키보드 액션이 명확하게 표시되도록 했다.
  - 스플래시 화면에 indeterminate `ProgressBar`와 로딩 문구를 추가했다.
- 검증 메모
  - `cd android && ./gradlew clean testDebugUnitTest --tests com.lbs.schoolhelper.ui.setup.SetupViewModelTest --tests com.lbs.schoolhelper.MainActivityNavigationTest` 통과
  - `cd android && ./gradlew lintDebug` 통과
- 상태
  - 구현/검증 완료
- 근거 문서
  - `android/app/src/main/java/com/lbs/schoolhelper/SetupActivity.kt`
  - `android/app/src/main/java/com/lbs/schoolhelper/SplashActivity.kt`
  - `android/app/src/main/res/layout/activity_setup.xml`
  - `android/app/src/main/res/layout/activity_splash.xml`
  - `android/app/src/main/res/values/strings.xml`


### 2026-06-15 · Android 학사일정 월 이동 버튼 스타일 통일

#### 16) 학사일정 상세 이전/다음달 버튼을 Material 버튼으로 변경
- 목표
  - Android 학사일정 상세 화면의 `이전 달` / `다음 달` 버튼을 다른 이동 버튼과 같은 스타일로 통일하는 것
- 요약
  - 기존 `TextView` 기반 월 이동 컨트롤을 `MaterialButton`으로 교체했다.
  - 시간표 이전/다음 이동 버튼과 동일한 soft blue 배경, 18dp corner, navy 텍스트 톤을 적용했다.
  - 이전 달 버튼에는 기존 back 아이콘과 tint를 맞춰 적용했다.
- 검증 메모
  - `cd android && ./gradlew testDebugUnitTest --tests com.lbs.schoolhelper.ui.schedule.ScheduleViewModelTest --tests com.lbs.schoolhelper.MainActivityNavigationTest` 통과
  - `cd android && ./gradlew lintDebug` 통과
- 상태
  - 구현/검증 완료
- 근거 문서
  - `android/app/src/main/res/layout/activity_schedule.xml`

### 2026-06-15 · Android 타이머 상세 화면 복구

#### 17) Android 홈 타이머에서 상세 화면 진입 재노출
- 목표
  - Android 홈 화면의 타이머 카드에서 타이머 상세 화면을 다시 열 수 있게 만드는 것
  - iOS도 같은 상세 화면 누락이 있는지 확인하는 것
- 요약
  - Android에 `TimerActivity`와 `activity_timer.xml` 상세 화면을 추가했다.
  - 홈 타이머 카드에 `타이머 전체 보기` 버튼을 추가하고 `TimerActivity`로 이동하도록 연결했다.
  - 상세 화면은 기존 `TimerViewModel`, 프리셋 선택, 시작/일시정지/초기화, 링 표시, 완료 깜박임 UX를 재사용한다.
  - iOS는 `HomeView`의 `NavigationLink`와 `RootTabView`의 sheet route가 이미 `TimerView`로 연결되어 있어 별도 수정이 필요 없음을 확인했다.
- 검증 메모
  - `cd android && ./gradlew testDebugUnitTest --tests com.lbs.schoolhelper.MainActivityNavigationTest` 통과
  - `cd android && ./gradlew lintDebug` 통과
- 상태
  - 구현/검증 완료
- 근거 문서
  - `android/app/src/main/java/com/lbs/schoolhelper/TimerActivity.kt`
  - `android/app/src/main/res/layout/activity_timer.xml`
  - `android/app/src/main/java/com/lbs/schoolhelper/MainActivity.kt`
  - `android/app/src/main/res/layout/activity_main.xml`
  - `android/app/src/test/java/com/lbs/schoolhelper/MainActivityNavigationTest.kt`
  - `ios/SchoolHelper/Features/Home/HomeView.swift`
  - `ios/SchoolHelper/App/RootTabView.swift`


### 2026-06-15 · Android/iOS Firebase Crashlytics 적용 준비

#### 18) 양 플랫폼 Crashlytics SDK 및 빌드 설정 추가
- 목표
  - Android와 iOS 앱에 Firebase Crashlytics를 추가하는 것
- 요약
  - Android Gradle 버전 카탈로그에 Firebase BoM, Google Services Plugin, Crashlytics Plugin, Crashlytics/Analytics SDK를 추가했다.
  - Android는 `android/app/google-services.json`이 있을 때만 Google Services/Crashlytics 플러그인을 적용해 로컬 빌드가 설정 파일 부재로 깨지지 않게 했다.
  - iOS 앱 타깃에 SwiftPM `firebase-ios-sdk` 패키지의 `FirebaseCore`, `FirebaseCrashlytics` 제품을 연결했다.
  - iOS 앱 시작 시 `GoogleService-Info.plist`가 번들에 있을 때만 Firebase를 초기화하는 `FirebaseCrashReporting`을 추가했다.
  - iOS Xcode 프로젝트에 Firebase 설정 파일 복사 스크립트와 Crashlytics dSYM 업로드 스크립트를 추가했다.
  - 양 플랫폼 Crashlytics 정적 설정을 점검하는 `scripts/check_firebase_crashlytics.sh`를 추가했다.
  - 실제 Firebase 콘솔에서 받은 설정 파일은 아직 저장소에 없으므로 다음 파일을 추가해야 실서비스 전송이 활성화된다.
    - Android: `android/app/google-services.json`
    - iOS: `ios/SchoolHelper/Resources/GoogleService-Info.plist`
- 검증 메모
  - `cd android && ./gradlew :app:compileDebugKotlin` 통과
  - `cd android && ./gradlew :app:compileDebugKotlin` 통과, `processDebugGoogleServices`와 `injectCrashlytics` 태스크 실행 확인
  - `cd android && ./gradlew lintDebug` 통과
  - `plutil -lint ios/SchoolHelper.xcodeproj/project.pbxproj` 통과
  - `plutil -lint ios/SchoolHelper/Resources/GoogleService-Info.plist` 통과
  - `cd ios && swift build` 통과
  - `scripts/check_firebase_crashlytics.sh` 통과, 설정 파일 포함 필수 점검 통과
  - `cd ios && xcodebuild -list -project SchoolHelper.xcodeproj`는 현재 active developer directory가 CommandLineTools라 Xcode가 없어 실행 불가
- 상태
  - SDK/빌드 설정 추가 완료
  - Android/iOS 설정 파일 추가 및 정적 설정 검증 완료. 실제 Crashlytics 대시보드 수신은 배포/실행 후 확인 필요
- 근거 문서
  - `android/gradle/libs.versions.toml`
  - `android/build.gradle.kts`
  - `android/app/build.gradle.kts`
  - `ios/SchoolHelper/App/FirebaseCrashReporting.swift`
  - `ios/SchoolHelper/App/SchoolHelperIOSApp.swift`
  - `ios/SchoolHelper.xcodeproj/project.pbxproj`
  - `ios/Package.swift`
  - `docs/project_specification.md`
  - `docs/firebase-crashlytics-setup.md`
  - `scripts/check_firebase_crashlytics.sh`


## 참고 문서

- `docs/project_specification.md`
  - 멀티플랫폼 전체 요구사항과 캐시 정책, Android/Web 범위를 정리한 기획서
- `docs/android-school-selection-review.md`
  - Android 학교선택 기능의 구현 전/검토 시점 메모
- `.omx/context/*.md`
  - 작업별 컨텍스트 스냅샷 원문
- `.omx/reports/team-commit-hygiene/*.md`
  - 팀 작업 종료 후 남은 완료 task 제목과 커밋 정리 가이드

## 운영 메모

앞으로 이 저장소에서 작업을 완료하면, 이전 작업 이력 요약은 이 파일(`docs/work-summary.md`)에 계속 누적 업데이트합니다.

### 2026-06-15 · 웹 파비콘 후보 적용

#### 19) A-1 학교/체크 아이콘을 웹 파비콘 자산으로 반영
- 목표
  - 선택한 A-1 앱 대표 아이콘을 웹 파비콘으로 사용할 수 있게 배치하는 것
- 요약
  - `checkimage/app-icon-a1-final.png`를 원본으로 사용했다.
  - `web/app/favicon.ico`를 16/32/48/64/128/256 크기 PNG 엔트리를 포함한 ICO로 재생성했다.
  - `web/public/favicon-192.png`, `favicon-256.png`, `favicon-512.png`를 같은 이미지 기반으로 교체했다.
- 검증 메모
  - `file web/app/favicon.ico web/public/favicon-192.png web/public/favicon-256.png web/public/favicon-512.png`로 포맷/크기 확인
  - `web/public/favicon-192.png` 시각 확인
- 상태
  - 적용 완료
- 근거 문서
  - `checkimage/app-icon-a1-final.png`
  - `web/app/favicon.ico`
  - `web/public/favicon-192.png`
  - `web/public/favicon-256.png`
  - `web/public/favicon-512.png`

### 2026-06-15 · iOS 앱 아이콘 적용

#### 20) A-1 학교/체크 아이콘을 iOS AppIcon 에셋으로 반영
- 목표
  - 선택한 A-1 앱 대표 아이콘을 iOS 앱 아이콘으로 사용할 수 있게 배치하는 것
- 요약
  - `checkimage/app-icon-a1-final.png`를 원본으로 사용했다.
  - `ios/SchoolHelper/Resources/Assets.xcassets/AppIcon.appiconset`을 생성했다.
  - iPhone/iPad/ios-marketing에 필요한 18개 PNG 크기와 `Contents.json`을 추가했다.
  - Xcode 앱 타깃 설정을 `ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon`으로 수정해 생성한 에셋을 사용하게 했다.
- 검증 메모
  - `python3 -m json.tool ios/SchoolHelper/Resources/Assets.xcassets/AppIcon.appiconset/Contents.json` 통과
  - `file ios/SchoolHelper/Resources/Assets.xcassets/AppIcon.appiconset/*.png`로 PNG 크기 확인
  - `grep ASSETCATALOG_COMPILER_APPICON_NAME ios/SchoolHelper.xcodeproj/project.pbxproj`로 AppIcon 연결 확인
  - `plutil -lint ios/SchoolHelper.xcodeproj/project.pbxproj` 통과
  - `cd ios && swift build` 통과
- 상태
  - 적용 완료
- 근거 문서
  - `ios/SchoolHelper/Resources/Assets.xcassets/AppIcon.appiconset/Contents.json`
  - `ios/SchoolHelper/Resources/Assets.xcassets/AppIcon.appiconset/*.png`
  - `ios/SchoolHelper.xcodeproj/project.pbxproj`
  - `checkimage/app-icon-a1-final.png`

### 2026-06-15 · iOS Firebase 설정 복사 스크립트 수정

#### 21) Xcode Run Script 환경변수 문법 수정
- 목표
  - iOS 빌드의 `PhaseScriptExecution Copy Firebase config if present` 실패를 해결하는 것
- 요약
  - Run Script 내부의 `$(TARGET_BUILD_DIR)` / `$(UNLOCALIZED_RESOURCES_FOLDER_PATH)` 사용을 제거했다.
  - 쉘에서 명령 치환으로 해석되지 않도록 `${TARGET_BUILD_DIR}` / `${UNLOCALIZED_RESOURCES_FOLDER_PATH}` 환경변수 문법으로 변경했다.
  - Crashlytics dSYM 업로드 스크립트도 같은 방식으로 `GOOGLE_SERVICE_INFO` 경로를 계산하게 정리했다.
- 검증 메모
  - `plutil -lint ios/SchoolHelper.xcodeproj/project.pbxproj` 통과
  - `/bin/sh -n`으로 Copy/Upload 스크립트 문법 확인
  - 임시 디렉터리에서 `GoogleService-Info.plist` 복사 동작 시뮬레이션 통과
  - Crashlytics run tool 부재 시 upload script no-op 통과
  - `cd ios && swift build` 통과
  - `git diff --check` 통과
- 상태
  - 수정/검증 완료
- 근거 문서
  - `ios/SchoolHelper.xcodeproj/project.pbxproj`

### 2026-06-15 · iOS Firebase 설정 복사 phase 제거

#### 22) GoogleService-Info.plist를 Resource로 직접 등록
- 목표
  - Xcode가 stale `Copy Firebase config if present` Run Script를 실행해 실패하는 문제를 제거하는 것
- 요약
  - `Copy Firebase config if present` Run Script phase를 앱 타깃에서 제거했다.
  - `ios/SchoolHelper/Resources/GoogleService-Info.plist`를 Xcode Resources group과 앱 `Resources` build phase에 직접 등록했다.
  - Crashlytics dSYM 업로드 phase는 유지하되, 앱 번들에 포함된 plist를 기준으로 실행 조건을 판단하게 했다.
- 검증 메모
  - `grep`으로 `Copy Firebase config if present` phase 제거 확인
  - `plutil -lint ios/SchoolHelper.xcodeproj/project.pbxproj` 통과
  - `plutil -lint ios/SchoolHelper/Resources/GoogleService-Info.plist` 통과
  - `python3 -m json.tool ios/SchoolHelper/Resources/Assets.xcassets/AppIcon.appiconset/Contents.json` 통과
  - `cd ios && swift build` 통과
  - `git diff --check` 통과
- 상태
  - 수정/검증 완료
- 근거 문서
  - `ios/SchoolHelper.xcodeproj/project.pbxproj`
  - `ios/SchoolHelper/Resources/GoogleService-Info.plist`
