# Android 코드 리뷰 결과

이 문서에는 안드로이드 프로젝트([android/](file:///Users/byungsunlee/Project/misSchoolApp/android))의 전체 소스 코드 분석을 통해 발견된 6가지 주요 문제점 및 개선 요소가 포함되어 있습니다.

현재 기준 메모:

- 2026-05-26: **1번 NEIS 에러 핸들링 항목은 수정 완료**
- 2026-05-26: **2번 POST_NOTIFICATIONS 권한 요청 항목도 수정 완료**
- 2026-05-26: **3번 타이머 per-tick 저장 항목도 수정 완료**
- 2026-05-26: **4번 주간 급식 순차 호출 항목도 수정 완료**
- 2026-05-26: **5번 재부팅 후 타이머 복구 누락 항목도 수정 완료**
- 현재 남은 활성 항목은 **6번**

---

## 1. 나이스(NEIS) API 에러 핸들링 논리 오류 (✅ 2026-05-26 해결)
*   **위치:** [`SchoolRepositoryImpl.kt`](file:///Users/byungsunlee/Project/misSchoolApp/android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/SchoolRepositoryImpl.kt#L268-L295) 및 [`NeisResponses.kt`](file:///Users/byungsunlee/Project/misSchoolApp/android/app/src/main/java/com/bsbarron/midschoolapp/data/remote/dto/NeisResponses.kt)
*   **상황:** API Key가 다르거나(인증 실패 `ERROR-300`), 일일 트래픽 초과(`ERROR-336`), 파라미터 오류(`INFO-100`) 등이 발생할 때 NEIS Open API는 데이터 배열을 반환하지 않고 **루트 레벨에 `RESULT` 객체만 담아 반환**합니다. (예: `{"RESULT": {"CODE": "ERROR-300", "MESSAGE": "..."}}`)
*   **원인 및 문제점:**
    *   현재 `NeisResponse<T>` DTO 클래스는 루트 레벨의 `RESULT` 필드가 선언되어 있지 않고, `mealServiceDietInfo` 같은 섹션 필드만 정의되어 있습니다.
    *   에러 응답 시 `mealServiceDietInfo` 등은 `null`이 되며, `extractRows()`는 `sections.orEmpty()`를 수행하여 **빈 리스트**를 반환합니다.
    *   결과적으로 에러 검증 함수인 `validateResult(result, ...)`에는 `null`이 전달되고, `result?.code`가 빈 문자열(`""`)이 되면서 `code.isBlank()` 가 `true`를 반환해 **에러가 감지되지 않고 정상 성공(`Result.success(emptyList())`)으로 처리**됩니다.
*   **영향:** API 키 불일치나 서버 에러 등 실제 네트워크 에러가 발생해도, 앱은 에러 상태를 전혀 감지하지 못하고 화면에 **그저 정보가 없는 날(성공 + 빈 데이터)로 노출**하게 되며 로컬 캐시 복구 전략(Fallback)도 작동하지 않습니다.
*   **현재 상태:** `NeisResponse<T>` 에 루트 레벨 `RESULT` 파싱을 추가하고, `SchoolRepositoryImpl.extractRows()` 가 루트 `RESULT` 또는 섹션 `head.RESULT` 를 모두 검증하도록 수정해 해결했습니다. 관련 회귀 테스트도 추가했습니다.

---

## 2. 알림 권한(POST_NOTIFICATIONS) 런타임 요청 부재 (✅ 2026-05-26 해결)
*   **위치:** [`TimerAlarmReceiver.kt`](file:///Users/byungsunlee/Project/misSchoolApp/android/app/src/main/java/com/bsbarron/midschoolapp/timer/TimerAlarmReceiver.kt#L39-L47) 및 [`MainActivity.kt`](file:///Users/byungsunlee/Project/misSchoolApp/android/app/src/main/java/com/bsbarron/midschoolapp/MainActivity.kt)
*   **상황:** Android 13(Tiramisu, API 33) 이상 환경에서는 `POST_NOTIFICATIONS`가 런타임 권한이 되어 사용자의 명시적인 허용이 필요합니다.
*   **문제점:** `TimerAlarmReceiver`에서 해당 권한이 없을 경우 조기 반환(`return`)하도록 방어 코드는 들어가 있으나, 정작 앱 내의 `MainActivity`나 `SettingsActivity` 등 진입점 화면 어디에서도 **사용자에게 이 알림 권한을 요청(Request)하는 로직이 없습니다.**
*   **영향:** Android 13 이상 기기에서 타이머 완료 알림이 절대 발생하지 않고 차단됩니다.
*   **현재 상태:** `MainActivity` 진입 시 알림 설정이 켜져 있고 권한이 없으면 `POST_NOTIFICATIONS` 를 요청하도록 수정했고, `SettingsActivity` 에서 알림 스위치를 켤 때도 같은 권한 요청이 발생하도록 보강했습니다. `MainActivityNavigationTest` 기준 회귀 테스트도 추가했습니다.

---

## 3. 타이머 동작 시 매 초마다 SharedPreferences 디스크 쓰기 수행 (성능 이슈) (✅ 2026-05-26 해결)
*   **위치:** [`TimerViewModel.kt`](file:///Users/byungsunlee/Project/misSchoolApp/android/app/src/main/java/com/bsbarron/midschoolapp/ui/timer/TimerViewModel.kt#L88-L102)
*   **상황:** 타이머가 흐르는 동안 `CountDownTimer.onTick` 콜백이 매 초(1000ms)마다 호출됩니다.
*   **문제점:** 매 초마다 `saveTimerState`를 호출하여 `System.currentTimeMillis() + millisUntilFinished`로 계산한 값을 SharedPreferences에 `apply()` 하고 있습니다.
    *   타이머 시작 시점(Start)에 완료 목표 절대 시간인 `targetAtMillis` 값을 **최초 1회만 고정해서 저장**해 두면, 앱이 강제 종료되거나 다시 켜졌을 때 `targetAtMillis - 현재_시스템_시간` 만 계산해도 남은 시간을 알아낼 수 있어 매 초마다 저장할 필요가 전혀 없습니다.
*   **영향:** `apply()`는 비동기식으로 동작하지만, 매 초마다 SharedPreferences 메모리 캐시 변경 및 디스크 파일 쓰기 큐를 채우게 되므로 불필요한 GC(Garbage Collection) 유발, 배터리 소모 및 파일 입출력 오버헤드를 일으킵니다.
*   **현재 상태:** `TimerViewModel` 에서 러닝 시작 시점에만 `targetAtMillis` 중심으로 저장하고, `onTick` 마다 저장하던 호출을 제거했습니다. pause 시에는 마지막 `remainingMillis` 만 한 번 저장하도록 유지했고, `TimerViewModelTest` 회귀 테스트로 검증했습니다.

---

## 4. 주간 급식 조회 시 API 순차(Sequential) 호출 (성능 이슈) (✅ 2026-05-26 해결)
*   **위치:** [`MealViewModel.kt`](file:///Users/byungsunlee/Project/misSchoolApp/android/app/src/main/java/com/bsbarron/midschoolapp/ui/meal/MealViewModel.kt#L60-L65)
*   **상황:** 주간 급식 탭에 진입하면 월요일부터 금요일까지 5일 치의 급식 데이터를 가져옵니다.
*   **문제점:** `viewModelScope.launch` 내에서 `(0L..4L).map` 루프를 사용해 하루씩 `schoolRepository.getMeals(day)`를 직접 호출하고 있습니다. 이는 5번의 네트워크 요청이 **병렬이 아닌 순차적으로(하나가 끝나야 다음 날 요청 시작) 실행**됨을 의미합니다.
*   **영향:** 네트워크 환경이 지연될 경우(예: 한 번의 요청에 300ms 소요 시) 총 로딩 시간이 `5 * 300ms = 1.5초` 이상으로 누적되어 화면 진입 로딩이 매우 느려집니다. 코루틴의 `async`와 `awaitAll`을 이용해 5개의 요청을 동시에 던지도록 처리해야 합니다.
*   **현재 상태:** `MealViewModel.loadWeekMeals()` 를 요일별 `async` + `awaitAll()` 구조로 바꿔 5일 조회를 병렬화했습니다. `MealViewModelTest` 에서 느린 첫 요청이 있어도 주간 날짜 5개가 모두 즉시 요청되는 회귀 케이스를 추가해 검증했습니다.

---

## 5. 디바이스 재부팅 시 타이머 복구 처리 누락 (정책 누락) (✅ 2026-05-26 해결)
*   **위치:** [`AndroidManifest.xml`](file:///Users/byungsunlee/Project/misSchoolApp/android/app/src/main/AndroidManifest.xml#L7)
*   **상황:** 디바이스가 재부팅되면 OS에 등록된 AlarmManager의 알람이 모두 삭제됩니다.
*   **문제점:** Manifest에는 `RECEIVE_BOOT_COMPLETED` 권한이 선언되어 있고 위젯 리시버에서 이 이벤트를 필터링하고 있지만, 정작 **타이머 알람을 복구하는 전용 BroadcastReceiver가 존재하지 않습니다.**
*   **영향:** 타이머가 동작 중인 상태에서 사용자의 스마트폰이 재부팅되면 알람이 완전히 해제되며, 사용자는 지정된 시간(집중 시간 완료 등)에 알림을 받지 못하게 됩니다.
*   **현재 상태:** `TimerBootReceiver` 와 `TimerBootRestorer` 를 추가해 `BOOT_COMPLETED` 수신 시 저장된 타이머 상태를 읽고, 아직 유효한 `targetAtMillis` 가 있으면 다시 스케줄하도록 보강했습니다. 이미 만료된 타이머 상태는 정리하도록 했고, `TimerBootRestorerTest` 로 회귀 검증했습니다.

---

## 6. 의존성 주입(DI) 아키텍처 결합도 이슈
*   **위치:** [`PreferencesRepositoryImpl.kt`](file:///Users/byungsunlee/Project/misSchoolApp/android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepositoryImpl.kt#L22-L32)
*   **상황:** Hilt를 통해 Repository 패턴과 의존성 주입을 잘 설계하셨습니다.
*   **문제점:** `PreferencesRepositoryImpl` 내부에서 데이터 저장/조회를 처리할 때, Hilt로 주입받는 인스턴스가 아니라 static Singleton 객체인 `UserPreferences`의 정적 메서드를 직접 호출하고 있습니다.
*   **영향:** 단위 테스트(Unit Test)를 작성할 때 Repository의 Preferences 행위를 모킹(Mocking)하기 어려워져, 테스트 작성 편의성 및 설계의 결합도 관점에서 아쉬운 구조입니다. `UserPreferences` 내부 코드를 `PreferencesRepositoryImpl`로 병합하거나 `UserPreferences`를 인스턴스화하여 의존성 주입하도록 개선하면 테스트 작성이 한결 편리해집니다.
