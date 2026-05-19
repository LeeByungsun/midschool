# Android 학교선택 기능 검토 메모

이 문서는 2026-05-19 기준 Android 학교선택 기능 추가 작업을 검토하면서 확인한 **현재 구현 상태**, **주요 위험**, **문서화가 필요한 구현 계약**을 정리한 메모입니다.

## 검토 범위

- `android/app/src/main/res/layout/activity_setup.xml`
- `android/app/src/main/res/layout/activity_settings.xml`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/setup/SetupViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/settings/SettingsViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/UserPreferences.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepository.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepositoryImpl.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/di/NetworkModule.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/SchoolRepositoryImpl.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/remote/NeisApiService.kt`
- `android/app/src/main/res/layout/activity_main.xml`
- `android/app/src/main/res/values/strings.xml`

## 현재 Android 구현 상태

1. **Setup/Settings 입력 범위**
   - 현재 Android의 Setup/Settings 화면은 학년/반 입력만 제공합니다.
   - 학교 검색/선택 UI, 검색 결과 선택 상태, 학교 재선택 흐름은 아직 없습니다.

2. **저장되는 사용자 설정**
   - 현재 Android는 학년/반만 저장합니다.
   - `officeCode`, `schoolCode`, `schoolKind`를 저장하는 Preferences 계약은 아직 없습니다.

3. **NEIS 조회 기준**
   - Android의 NEIS 조회는 현재 고정값 `officeCode = "J10"`, `schoolCode = "7679399"`에 의존합니다.
   - 즉, UI에서 학교를 선택하더라도 Repository/DI 계층이 함께 바뀌지 않으면 실제 조회 학교는 달라지지 않습니다.

4. **시간표 엔드포인트 선택**
   - Android 시간표 조회는 현재 `hub/misTimetable`에 고정되어 있습니다.
   - `schoolKind`에 따라 `misTimetable` / `elsTimetable`를 분기하는 구조는 아직 없습니다.

5. **학교명 표시**
   - 홈 화면 학교명은 동적 상태가 아니라 문자열 리소스(`다원중학교`)에 고정되어 있습니다.

## 현재 상태에서의 주요 위험

- **학교 선택 UI만 추가하는 불완전 구현 위험**
  - 저장 계층과 DI가 그대로면 사용자가 학교를 바꿔도 실제 NEIS 조회 학교는 바뀌지 않습니다.

- **학교 종류 누락 위험**
  - `schoolKind`가 없으면 초등학교/중학교 시간표 엔드포인트 분기가 불가능합니다.

- **캐시 오염 위험**
  - 현재 급식/시간표 캐시는 학교 identity 없이 저장됩니다.
  - 학교 선택 기능이 추가되면 캐시 키도 학교 기준으로 분리해야 학교 간 데이터 혼선이 생기지 않습니다.

- **문구 불일치 위험**
  - 현재 일부 Android 설명 문구는 “학교 정보 조회 기준”이 바뀐다고 안내하지만, 실제 구현은 동적 학교 조회를 지원하지 않습니다.

## Android 학교선택 기능 구현 계약

Android에서 학교선택 기능을 실제 지원하려면 최소한 아래 계약이 함께 충족되어야 합니다.

1. **입력 계약**
   - Setup/Settings에서 학교 검색과 결과 선택이 가능해야 합니다.
   - 저장 시 학년/반과 함께 `officeCode`, `schoolCode`, `schoolKind`를 함께 확정해야 합니다.

2. **저장 계약**
   - `PreferencesRepository`와 실제 저장소는 학교 identity를 읽고 쓸 수 있어야 합니다.
   - 기존 사용자(학년/반만 저장된 상태)에 대한 마이그레이션 또는 재설정 흐름이 필요합니다.

3. **조회 계약**
   - 급식, 일정, 시간표 조회는 모두 저장된 학교 기준으로 동작해야 합니다.
   - 시간표는 `schoolKind`에 따라 올바른 NEIS 엔드포인트를 선택해야 합니다.

4. **캐시 계약**
   - 급식/시간표/일정 캐시는 날짜만이 아니라 학교 identity까지 포함해 분리해야 합니다.

5. **UI 계약**
   - 홈/설정 화면의 학교명은 저장된 선택 결과를 반영해야 합니다.
   - 학교 선택이 완료되지 않은 사용자는 Setup 완료 상태로 처리하지 않아야 합니다.

## 문서 반영 원칙

- Web은 이미 학교 검색/선택을 지원하지만, Android는 아직 같은 수준의 구현이 아닙니다.
- 따라서 제품 문서에는 **Android 현재 상태**와 **목표 기능 계약**을 분리해서 기록해야 합니다.
- 실제 Android 구현이 머지되기 전에는, 문서가 완료된 기능처럼 보이게 쓰지 않습니다.
