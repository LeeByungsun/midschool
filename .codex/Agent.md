# AGENTS Guidelines for MisSchoolApp

이 저장소는 `중학교도우미` 안드로이드 앱입니다. AI 에이전트가 작업할 때는 아래 기준을 우선합니다.

## 1. 현재 프로젝트 기준
- **앱 이름:** 중학교도우미
- **언어:** Kotlin
- **빌드:** Gradle Kotlin DSL
- **minSdk / targetSdk:** 26 / 36
- **단일 모듈:** 현재는 `:app` 단일 모듈 구조

## 2. 아키텍처 원칙
- **패턴:** MVVM
- **DI:** Hilt
- **상태 관리:** `StateFlow`, `SharedFlow`
- **데이터 접근:** `SchoolRepository`, `PreferencesRepository`
- **원격 데이터:** Retrofit + OkHttp + Gson
- **로컬 저장:** `SharedPreferences`를 `PreferencesRepository`로 감싸서 사용

현재 프로젝트는 Clean Architecture의 일부 개념을 따르지만, 아직 별도 `domain` 모듈이나 UseCase 레이어는 없습니다. 새 기능은 기존 구조를 유지하면서 `ui/`, `data/`, `di/`, `widget/`, `timer/` 패턴에 맞춰 추가합니다.

## 3. UI 개발 기준
- **현재 UI 기준:** XML + DataBinding
- **ViewModel 연동:** Activity는 화면 바인딩과 이벤트 전달 위주로 유지하고, 상태 계산은 ViewModel에 둡니다.
- **새 화면 추가 시:** 가능하면 DataBinding을 사용하고, 문자열은 반드시 `strings.xml`로 분리합니다.
- **하드코딩 금지:** XML의 `text`, `contentDescription`, 안내 문구는 리소스로 관리합니다.
- **Edge-to-edge:** 상단 헤더와 시스템 바 영역 간격을 항상 확인합니다.

Jetpack Compose는 현재 메인 UI 기술이 아니므로, 명시적인 전환 작업이 아닌 이상 새 기능도 XML 기반으로 맞추는 것을 우선합니다.

## 4. 비동기/상태 처리 기준
- ViewModel에서는 `viewModelScope`를 사용합니다.
- UI에서는 `repeatOnLifecycle` 기반으로 상태를 수집합니다.
- Repository는 가능한 한 main-safe 하게 유지합니다.
- `MutableStateFlow`, `MutableSharedFlow`는 외부에 직접 노출하지 않습니다.
- `CancellationException`은 일반 예외 처리에서 삼키지 않습니다.

## 5. 데이터/네트워크 기준
- 나이스(NEIS) API 응답 처리는 Repository에서 일관되게 담당합니다.
- 공통 에러 문구와 API 응답 코드 해석은 UI가 아니라 Repository 쪽에 둡니다.
- 급식/시간표 캐시는 `PreferencesRepository`를 통해 관리합니다.
- 새 저장 값이 필요하면 Activity에서 직접 `SharedPreferences`를 만지지 말고 `PreferencesRepository`에 API를 추가합니다.

## 6. 위젯/타이머 관련 기준
- 위젯은 삼성 런처 호환성을 고려해 기본값으로 바로 추가되는 흐름을 유지합니다.
- 위젯 설정 변경은 추가 후 설정 아이콘을 통해 여는 현재 구조를 우선합니다.
- 타이머는 앱을 떠나도 상태가 유지되어야 하며, 완료 알림과 진동 설정을 따릅니다.

## 7. 문서 반영 규칙
- 기능을 추가하거나 흐름이 바뀌면 `docs/project_specification.md`를 함께 업데이트합니다.
- 저장소 규칙이 바뀌면 이 `Agent.md`와 관련 `SKILL.md`도 같이 갱신합니다.

## 8. 우선 참고할 Skill
- `architecture/android-architecture`: 현재 앱 구조와 Hilt 기준
- `architecture/android-data-layer`: Repository / 캐시 / 저장소 기준
- `architecture/android-viewmodel`: `StateFlow`, `SharedFlow`, UI 상태 기준
- `concurrency_and_networking/android-retrofit`: NEIS API / Retrofit 처리 기준
- `concurrency_and_networking/android-coroutines`: 코루틴 안전성 점검
- `ui/android-accessibility`: XML 화면 접근성 점검

프로젝트 구조나 기술 선택이 바뀌면 이 문서를 먼저 최신화합니다.
