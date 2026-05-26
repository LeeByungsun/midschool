# Android 학교선택 기능 검토 메모

이 문서는 **2026-05-26 기준** Android 학교선택 기능(기존 TODO의 A/B 항목)을 다시 검토해,
현재 구현 상태와 남은 위험을 **코드 기준으로 사실대로** 정리한 메모입니다.

## 검토 범위

- `android/app/src/main/res/layout/activity_setup.xml`
- `android/app/src/main/res/layout/activity_settings.xml`
- `android/app/src/main/java/com/bsbarron/midschoolapp/SetupActivity.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/SettingsActivity.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/setup/SetupViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/settings/SettingsViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/UserPreferences.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepository.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepositoryImpl.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/SchoolRepositoryImpl.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/home/HomeViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/splash/SplashViewModel.kt`
- `android/app/src/test/java/com/bsbarron/midschoolapp/ui/setup/SetupViewModelTest.kt`
- `android/app/src/test/java/com/bsbarron/midschoolapp/ui/settings/SettingsViewModelTest.kt`
- `android/app/src/test/java/com/bsbarron/midschoolapp/data/repository/SchoolRepositoryImplTest.kt`
- `android/app/src/test/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepositoryImplTest.kt`
- `android/app/src/test/java/com/bsbarron/midschoolapp/UserPreferencesTest.kt`
- `android/app/src/test/java/com/bsbarron/midschoolapp/ui/splash/SplashViewModelTest.kt`

## 현재 Android 구현 상태

1. **Setup/Settings 학교 검색/선택 UI는 이미 구현되어 있습니다.**
   - Setup과 Settings 모두 학교 검색 입력, 검색 버튼, 결과 목록, 라디오 선택, 저장 전 검증 흐름을 가집니다.
   - 단건 검색 결과는 자동 선택되고, 다건 결과는 사용자가 명시적으로 학교를 고르도록 유도합니다.

2. **학교 identity 저장 계약도 이미 구현되어 있습니다.**
   - `StudentInfo`는 `schoolName`, `officeCode`, `schoolCode`, `schoolKind`, `grade`, `classroom`을 함께 다룹니다.
   - `UserPreferences`와 `PreferencesRepository`는 이 값을 그대로 저장/복원합니다.
   - `hasStudentInfo()` / `isComplete()` 는 **학년/반 + 학교 identity 전체**가 모두 있어야 완료로 판단합니다.

3. **실제 조회 기준도 저장된 학교로 동작합니다.**
   - `SchoolRepositoryImpl`은 급식/일정/시간표/가정통신문 조회에 저장된 `officeCode` / `schoolCode`를 사용합니다.
   - 시간표는 `schoolKind`에 따라 `elsTimetable` / `misTimetable` / `hisTimetable`를 분기합니다.

4. **학교별 캐시 분리도 구현되어 있습니다.**
   - 급식 캐시: `officeCode + schoolCode + yyyyMMdd`
   - 시간표 캐시: `officeCode + schoolCode + grade + classroom + yyyyMMdd`
   - 일정 캐시: `officeCode + schoolCode + yyyyMM`

5. **홈/스플래시/위젯의 설정 완료 게이트도 새 계약을 따릅니다.**
   - 홈 헤더는 저장된 `schoolName`을 동적으로 표시합니다.
   - 학교 선택이 끝나지 않은 사용자는 홈에서 Setup 유도 상태를 보게 됩니다.
   - Splash는 완전한 학교 설정이 없으면 Setup으로 보냅니다.

## 이번 검토에서 확인한 핵심 결론

- 기존 TODO의 **A. 학교 검색/선택 기능**, **B. 학교 코드/학교 종류 저장** 자체는 현재 브랜치에서 이미 구현되어 있습니다.
- 현재 Android의 가장 큰 리스크는 “학교를 바꿔도 실제 조회 학교가 안 바뀌는 상태”가 아니라,
  **레거시 사용자 마이그레이션 UX** 와 **검색 상태 회귀** 입니다.

## 남아 있는 주요 위험

1. **레거시 사용자 마이그레이션 안내 부족**
   - 예전처럼 학년/반만 저장된 사용자는 이제 완료 상태가 아니므로 Splash에서 Setup으로 이동합니다.
   - 동작 자체는 안전하지만, 왜 다시 Setup으로 보내는지 설명하는 전용 UX는 아직 없습니다.

2. **동시 검색 응답 역전 가능성**
   - Setup/Settings 검색은 요청 취소나 request token 없이 매번 새 코루틴을 실행합니다.
   - 사용자가 빠르게 연속 검색하면 느린 이전 응답이 더 늦게 도착해 최신 결과를 덮을 수 있습니다.

3. **검색 편집/실패 시 기존 선택 소실 가능성**
   - 검색어를 바꾸거나 실패/빈 결과를 받으면 `selectedSchool` 이 비워집니다.
   - 저장 계약상 안전한 동작이지만, Settings에서 타이머 옵션만 바꾸려던 사용자는 학교를 다시 선택해야 할 수 있습니다.

4. **지원 학교 종류 제한**
   - 현재 검색 결과는 초등학교/중학교/고등학교만 노출합니다.
   - 그 외 학교 종류 지원이 필요하면 검색 필터 정책을 별도로 확장해야 합니다.

## 현재 Android 학교선택 구현 계약

1. **입력 계약**
   - Setup/Settings에서 학교 검색 후 결과 목록에서 학교를 선택해야 저장할 수 있습니다.
   - 학년/반만 있고 학교 identity가 없으면 완료 상태로 보지 않습니다.

2. **저장 계약**
   - 저장 단위는 `grade`, `classroom`, `schoolName`, `officeCode`, `schoolCode`, `schoolKind` 입니다.
   - 레거시 partial prefs는 허용하되, 완료 상태로 취급하지 않습니다.

3. **조회 계약**
   - 급식/일정/시간표/가정통신문은 모두 저장된 학교 identity 기준으로 조회합니다.
   - 시간표 엔드포인트는 `schoolKind` 에 따라 분기합니다.

4. **캐시 계약**
   - 급식/일정/시간표 캐시는 모두 학교 identity를 키에 포함해야 합니다.
   - 학교가 다르면 같은 날짜여도 서로 캐시를 재사용하지 않습니다.

5. **UI 계약**
   - 홈/설정/초기설정은 같은 `StudentInfo` 계약을 공유합니다.
   - 학교 선택이 완료되지 않은 사용자는 Setup 유도 상태를 유지합니다.

## 회귀 검증 초점

- Setup 저장 검증: `SetupViewModelTest`
- Settings 저장/재선택 검증: `SettingsViewModelTest`
- 저장/완료 상태 검증: `UserPreferencesTest`, `SplashViewModelTest`
- 학교별 조회/엔드포인트/캐시 검증: `SchoolRepositoryImplTest`, `PreferencesRepositoryImplTest`
- 홈/설정 반영 검증: `HomeViewModelTest`, `MainActivityNavigationTest`

## 문서 반영 원칙

- Android 학교선택 기능은 이제 “미구현”이 아니라 **기본 구현은 완료**된 상태로 기록합니다.
- 다만 남은 위험(마이그레이션 UX, 검색 응답 경합, 선택 유지 UX)은 **완료된 기능과 별도로** 남은 품질 과제로 문서화합니다.
