# Android 프로젝트 코드 리뷰

- **리뷰 일자:** 2026-09-20
- **대상 커밋:** `326f051` (`fix(widget): keep timetable errors out of lesson content`)
- **범위:** `android/` 하위 운영 코드, 리소스, Manifest, Gradle 설정
- **리뷰 방식:** 독립 코드 품질 리뷰 + 독립 아키텍처 리뷰
- **리뷰 결과:** **수정 요청(REQUEST CHANGES)**
- **아키텍처 상태:** **차단(BLOCK)**

## 요약

프로젝트는 Hilt DI, Repository 인터페이스, 사용자 동의 기반 telemetry 구조를 갖추고 있어 기본 경계는 잘 잡혀 있습니다. 다만 출시 안정성 관점에서 아래 네 가지는 우선 해결이 필요합니다.

1. 홈과 상세 화면이 각자 타이머를 실행해 완료 처리 경쟁 조건이 생길 수 있습니다.
2. 백그라운드 AlarmReceiver가 완료 상태를 갱신하지 않고 소리만 재생합니다.
3. 가정통신문의 외부 URL을 검증 없이 실행합니다.
4. 전체 단위 테스트가 Robolectric 오류로 정상 완료되지 않습니다.

## 심각도 요약

| 심각도 | 건수 | 상태 |
| --- | ---: | --- |
| Critical | 0 | - |
| High | 4 | 출시 전 수정 권장 |
| Medium | 4 | 다음 안정화 작업에 포함 |
| Low | 2 | 품질 개선 작업으로 관리 |

---

## High

### 1. 타이머 세션을 여러 ViewModel이 동시에 소유

- **위치:** `ui/timer/TimerViewModel.kt:43-49, 130-208`, `MainActivity.kt:29-30`, `TimerActivity.kt:20-22`
- **문제:** 홈과 상세 화면이 각각 `TimerViewModel`과 `CountDownTimer`를 생성하고 동일한 SharedPreferences, AlarmManager 상태를 갱신합니다.
- **영향:** 완료음 중복, 다음 포모도로 단계 이중 전환, 남은 시간 되돌아감, 홈/상세 화면 불일치가 발생할 수 있습니다.
- **권장 조치:** 앱 범위의 단일 `TimerSessionController`가 카운트다운·저장·단계 전환·알람 예약을 단독 소유하고, 각 화면은 동일한 `StateFlow`를 관찰하도록 변경합니다. 최소한 완료 시 target 시간을 compare-and-claim하여 한 번만 완료 처리해야 합니다.

### 2. AlarmReceiver가 완료 상태를 처리하지 않음

- **위치:** `timer/TimerAlarmReceiver.kt:7-10`, `timer/TimerCompletion.kt:8-15`, `ui/timer/TimerViewModel.kt:178-209`
- **문제:** AlarmReceiver는 완료음을 재생하지만 타이머 상태를 완료 처리하거나 다음 단계를 시작하지 않습니다. `TimerCompletion.complete()`는 운영 경로에서 호출되지 않습니다.
- **영향:** 앱이 백그라운드이거나 종료된 경우 `isRunning=true`인 만료 세션이 남고 자동 단계 전환이 중단될 수 있습니다.
- **권장 조치:** UI 타이머와 AlarmReceiver가 하나의 원자적 `completeIfDue()` 완료 경로를 공유하도록 통합합니다. 이 경로는 완료 claim, 다음 단계 결정, 상태 저장, 알람 예약을 함께 수행해야 합니다.

### 3. 외부 URL을 검증 없이 실행

- **위치:** `util/ExternalUrlOpener.kt:9-28`, `MainActivity.kt:149-151`, `data/repository/SchoolRepositoryImpl.kt:219-227`
- **문제:** 서버에서 받은 가정통신문 URL을 scheme·host 검증 없이 `ACTION_VIEW`로 전달합니다. 처리할 앱이 없는 경우도 안전하게 처리하지 않습니다.
- **영향:** 비정상 또는 침해된 응답이 custom scheme을 실행할 수 있고, URL 처리 앱이 없으면 앱 오류가 발생할 수 있습니다.
- **권장 조치:** HTTPS만 허용하고 필요한 경우 도메인 allowlist를 적용합니다. URL 실행은 `resolveActivity()`와 예외 처리를 갖춘 단일 함수로 통일합니다.

### 4. 전체 단위 테스트 게이트가 정상 완료되지 않음

- **위치:** `src/test/java/com/lbs/schoolhelper/MainActivityNavigationTest.kt:47-55`, `gradle/libs.versions.toml:20`
- **문제:** `testDebugUnitTest` 실행 시 Robolectric의 `NoClassDefFoundError` 및 `ClassReader IllegalArgumentException`이 발생하고 테스트가 정상 종료되지 않습니다.
- **영향:** 전체 회귀 상태를 증명할 수 없으므로 머지 및 출시 안정성을 보장하기 어렵습니다.
- **권장 조치:** 현재 AGP/JDK와 호환되는 Robolectric 버전으로 업데이트하고 Hilt/Application 테스트 설정을 점검합니다. CI에서 전체 단위 테스트를 필수 게이트로 복구합니다.

---

## Medium

### 5. 학사 일정 통신 실패가 ‘일정 없음’으로 표시됨

- **위치:** `ui/schedule/ScheduleViewModel.kt:61-77`
- **문제:** 실패한 `Result`를 빈 목록으로 변환해 네트워크 오류와 정상적인 빈 일정을 구분하지 못합니다.
- **영향:** 사용자는 장애를 인지하거나 재시도할 방법이 없습니다.
- **권장 조치:** `loading / empty / error / success` 상태를 분리하고 실패 시 안내와 재시도 동작을 제공합니다.

### 6. 위젯 갱신 Receiver가 외부 refresh broadcast를 수용

- **위치:** `AndroidManifest.xml:76-89`, `widget/MisSchoolWidgetProvider.kt:59-75, 272-360`
- **문제:** exported 위젯 Provider가 앱 전용 refresh action도 함께 처리하며, widget ID를 포함한 외부 broadcast가 네트워크 작업을 유발할 수 있습니다.
- **영향:** 반복 호출에 따른 배터리·네트워크 사용 증가와 telemetry 오염 가능성이 있습니다.
- **권장 조치:** 앱 내부 refresh는 `exported=false` Receiver로 분리하고, 위젯별 중복 갱신을 debounce·mutex 또는 WorkManager unique work로 제한합니다.

### 7. 학생 설정 및 캐시 데이터가 백업 대상

- **위치:** `AndroidManifest.xml:13-17`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`
- **문제:** telemetry 파일만 백업에서 제외되어 학교·학년·반·시간표·급식·일정 캐시가 클라우드 백업 또는 기기 전송 대상이 될 수 있습니다.
- **영향:** 개인정보 처리방침과 실제 백업 동작이 불일치할 수 있습니다.
- **권장 조치:** 관련 SharedPreferences 파일을 백업 제외하거나, 백업 허용 범위를 개인정보 처리방침에 명시합니다.

### 8. NEIS API 키가 APK에 포함되고 빈 키 처리도 불일치

- **위치:** `app/build.gradle.kts:18-20, 39-41`, `data/remote/NeisApiService.kt`, `data/repository/SchoolRepositoryImpl.kt`
- **문제:** API 키가 BuildConfig와 쿼리 파라미터에 포함됩니다. 빈 키 사전 검증도 시간표 중심으로만 적용됩니다.
- **영향:** APK 분석을 통한 키·쿼터 노출 및 기능별 오류 경험 불일치가 생깁니다.
- **권장 조치:** 가능하면 웹 백엔드를 통한 서버 호출로 키를 이동하고 rate limit을 적용합니다. 클라이언트 키가 불가피하면 제공자 제한과 모든 기능의 공통 설정 검증을 적용합니다.

---

## Low

### 9. 이중 Splash 및 고정 시작 지연

- **위치:** `SplashActivity.kt:17-41`, `ui/splash/SplashViewModel.kt:28-45`
- **문제:** Android 12 이상 시스템 Splash 이후에도 1.2초 고정 자체 Splash가 실행됩니다.
- **권장 조치:** AndroidX SplashScreen API의 keep condition으로 초기 라우팅을 처리하고 고정 지연을 제거합니다.

### 10. Lint 경고 누적

- **위치:** `res/layout/widget_home.xml:43-48, 81-87`, `res/layout/activity_settings.xml:62-71, 247-260`
- **문제:** 접근성 설명 누락, 입력 필드 속성 누락, framework `Switch` 사용 등을 포함한 lint 경고가 남아 있습니다.
- **권장 조치:** 장식 이미지는 접근성 트리에서 제외하고, 입력 필드에는 `inputType` 및 autofill 속성을 부여하며, `MaterialSwitch`로 일관되게 전환합니다.

---

## 아키텍처 개선 방향

### 공통 오류 결과 모델

현재 화면별로 오류·빈 데이터·캐시 데이터를 서로 다르게 처리합니다. `Success / Empty / Stale / Error(ErrorKind)`와 같은 공통 결과 모델을 도입하고, 모든 UI가 리소스 문자열로 변환하도록 권장합니다. 내부 `Throwable.message`는 telemetry에만 사용하고 UI에는 노출하지 않습니다.

### Repository 분리

`SchoolRepositoryImpl.kt`가 학교 검색, 급식, 시간표, 일정, 가정통신문까지 담당합니다. 기능별 data source 또는 use case로 점진 분리하면 테스트 범위와 오류 정책을 단순화할 수 있습니다.

---

## 우선순위 로드맵

| 우선순위 | 작업 | 완료 기준 |
| --- | --- | --- |
| P0 | 타이머 단일 세션 소유권 | 홈·상세·AlarmReceiver가 하나의 완료 상태 머신 사용 |
| P0 | 외부 URL 안전 실행 | HTTPS/도메인 검증 및 실행 실패 처리 테스트 추가 |
| P0 | 전체 테스트 게이트 복구 | `testDebugUnitTest`가 정상 종료·통과 |
| P1 | 위젯 갱신 수명 관리 | 외부 refresh 경계 분리, 중복 요청 방지 |
| P1 | 오류 상태 표준화 | 일정·급식·시간표·공지에 공통 오류/빈 상태 적용 |
| P1 | 백업 정책 정리 | 실제 백업 범위와 개인정보 처리방침 일치 |
| P2 | NEIS 키 호출 구조 개선 | 서버 프록시 또는 제공자 제한·공통 설정 검증 |
| P2 | Splash·Lint 품질 개선 | 시작 지연 제거 및 lint 경고 점진 해소 |

---

## 검증 현황

| 항목 | 결과 | 비고 |
| --- | --- | --- |
| `assembleQa` | 통과 | QA APK 빌드 가능 |
| `lintQa` | 통과 | 오류는 없으나 lint 경고 존재 |
| 최근 위젯 오류 처리 회귀 테스트 | 통과 | 오류 메시지 노출·빈 데이터·정렬 처리 검증 |
| `testDebugUnitTest` 전체 실행 | 실패 | Robolectric 클래스 로딩 오류 및 정상 종료 불가 |

## 최종 권고

현재 상태는 기능 검증용 QA 빌드는 가능하지만, **정식 출시 승인 전 상태는 아닙니다.** P0 항목인 타이머 완료 상태 통합, 외부 URL 검증, 전체 테스트 게이트 복구를 먼저 완료한 뒤 출시 전 재리뷰를 수행하는 것을 권장합니다.
