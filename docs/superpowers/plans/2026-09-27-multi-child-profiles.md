# 다자녀 프로필 지원 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Android 앱에서 자녀 프로필을 여러 개 등록하고 홈에서 활성 자녀를 전환하며, 모든 학교 데이터와 위젯이 올바른 프로필을 사용하도록 구현한다.

**Architecture:** `student_profiles.xml`의 버전 있는 단일 JSON 문서를 담당하는 저장소와 `StateFlow` 기반 `StudentProfileRepository`를 새로 둔다. 학교 데이터 조회는 전역 환경설정을 암묵적으로 읽지 않고 호출자가 전달한 `StudentInfo` 스냅샷을 사용하며, 화면은 활성 프로필 변경 시 이전 작업을 취소하고 새 프로필을 다시 로드한다. 자녀 이름과 UUID는 로컬에서만 사용하고 Firebase·로그·네트워크·Android 백업에서 제외한다.

**Tech Stack:** Kotlin, Android XML/DataBinding, ViewModel/StateFlow/Coroutines, SharedPreferences, Gson, Hilt, Retrofit, Firebase Analytics/Crashlytics, JUnit/Robolectric.

**Spec:** `docs/superpowers/specs/2026-09-27-multi-child-profiles-design.md`

## Global Constraints

- 현재 Android의 MVVM/Hilt/XML/DataBinding 구조를 유지하고 Compose나 Room을 추가하지 않는다.
- 프로필 이름 또는 별칭은 필수이며, trim 후 1~10자이고 trim 후 동일한 이름은 중복으로 허용하지 않는다.
- `displayName`과 `profileId`는 Firebase, 로그, NEIS, 가정통신문 서버 요청에 절대 포함하지 않는다.
- `student_profiles.xml`은 클라우드 백업과 기기 간 전송에서 모두 제외한다.
- 타이머 상태·타이머 설정·Firebase 동의는 앱 전역 설정으로 유지한다.
- 캐시는 기존 `officeCode + schoolCode + grade + classroom + date` 문맥을 유지하고 프로필 ID를 캐시 키에 추가하지 않는다.
- 기존 사용자의 학교·학년·반은 이름 입력 전까지 초안으로 보존하고 새 JSON 저장 성공 후에만 기존 학교 키를 삭제한다.
- 기존 작업자의 미커밋 변경을 되돌리거나 덮어쓰지 않는다. 구현 시작 전 현재 변경을 보존한 별도 작업 트리 또는 정리된 브랜치를 사용한다.

## Review Focus

1. JSON이 손상됐거나 `activeProfileId`가 없는 프로필을 가리킬 때 앱이 종료되지 않고 유효한 첫 프로필 또는 초기 설정으로 복구되는지 Task 1·2 테스트로 고정한다.
2. 자녀 전환 직전에 시작한 느린 네트워크 응답이 전환 후 화면을 덮지 않는지 Task 6 테스트로 고정한다.
3. 공백·10자 초과·trim 후 중복인 이름이 저장되지 않는지 Task 2·5 테스트로 고정한다.
4. 위젯이 가리키는 프로필이 삭제됐을 때 현재 활성 프로필로 안전하게 재연결되는지 Task 8 테스트로 고정한다.
5. 자녀 이름과 UUID가 Firebase 매개변수 및 Android 백업 규칙으로 새지 않는지 Task 9 테스트로 고정한다.

---

## 파일 구조

### 새 프로필 계층

- `data/profile/StudentProfile.kt`: 프로필, 저장 문서, 검증 오류 모델
- `data/profile/StudentProfileStore.kt`: JSON 저장 경계
- `data/profile/SharedPreferencesStudentProfileStore.kt`: `student_profiles.xml` 읽기·쓰기
- `data/profile/StudentProfileRepository.kt`: 화면과 기능이 사용하는 프로필 계약
- `data/profile/StudentProfileRepositoryImpl.kt`: CRUD, 활성 선택, 정규화, 기존 데이터 이전

### 기존 계층의 책임 변경

- `PreferencesRepository`: 타이머·동의·캐시·위젯 설정만 유지
- `UserPreferences`: 기존 단일 학생 정보 읽기·삭제는 마이그레이션 전용으로 축소
- `SchoolRepository`: 모든 학교 데이터 조회에서 명시적인 `StudentInfo`를 입력받음
- 화면 ViewModel: `StudentProfileRepository.activeProfile`을 관찰하고 해당 스냅샷으로 조회
- 위젯 설정: `profileId`를 저장해 위젯마다 특정 자녀에 고정

---

### Task 1: 프로필 모델과 전용 JSON 저장소

**Files:**
- Create: `android/app/src/main/java/com/lbs/schoolhelper/data/profile/StudentProfile.kt`
- Create: `android/app/src/main/java/com/lbs/schoolhelper/data/profile/StudentProfileStore.kt`
- Create: `android/app/src/main/java/com/lbs/schoolhelper/data/profile/SharedPreferencesStudentProfileStore.kt`
- Test: `android/app/src/test/java/com/lbs/schoolhelper/data/profile/SharedPreferencesStudentProfileStoreTest.kt`

**Interfaces:**
- Produces: `StudentProfile(id: String, displayName: String, studentInfo: StudentInfo)`
- Produces: `StudentProfilesData(schemaVersion: Int = 1, activeProfileId: String, profiles: List<StudentProfile>)`
- Produces: `StudentProfileStore.read(): StudentProfilesData?`
- Produces: `StudentProfileStore.write(data: StudentProfilesData): Boolean`
- Produces: `StudentProfileStore.clear(): Boolean`
- Produces: `PROFILE_SCHEMA_VERSION = 1`, `MAX_PROFILE_NAME_LENGTH = 10`

- [ ] **Step 1: 저장소 실패 테스트 작성**

```kotlin
@Test fun writeAndRead_roundTripsProfilesAndActiveId()
@Test fun read_whenJsonIsMalformed_returnsNull()
@Test fun read_whenFileDoesNotExist_returnsNull()
@Test fun write_replacesTheWholeDocumentWithoutOrphanedProfiles()
```

첫 테스트는 두 프로필과 활성 ID가 정확히 복원되는지, 마지막 테스트는 두 명을 저장한 뒤 한 명만 저장했을 때 삭제된 프로필이 다시 나타나지 않는지 단언한다.

- [ ] **Step 2: 실패 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*SharedPreferencesStudentProfileStoreTest' --console=plain
```

Expected: 새 타입과 저장소가 없어 컴파일 또는 테스트 실패.

- [ ] **Step 3: 모델과 저장소 구현**

`SharedPreferencesStudentProfileStore`는 `context.getSharedPreferences("student_profiles", MODE_PRIVATE)`와 단일 키 `profiles_data`를 사용한다. Gson 직렬화 결과를 `commit()`으로 저장해 성공 여부를 반환하고, 읽기 예외는 `null`로 격리한다.

- [ ] **Step 4: 저장소 테스트 통과 확인**

Run: Step 2와 동일.

Expected: 4 tests PASS.

- [ ] **Step 5: 커밋**

```bash
git add android/app/src/main/java/com/lbs/schoolhelper/data/profile \
        android/app/src/test/java/com/lbs/schoolhelper/data/profile/SharedPreferencesStudentProfileStoreTest.kt
git commit -m "feat: add student profile storage"
```

### Task 2: 프로필 Repository, 검증 및 기존 데이터 이전

**Files:**
- Create: `android/app/src/main/java/com/lbs/schoolhelper/data/profile/StudentProfileRepository.kt`
- Create: `android/app/src/main/java/com/lbs/schoolhelper/data/profile/StudentProfileRepositoryImpl.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/UserPreferences.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/data/repository/UserPreferencesStore.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/di/AppModule.kt`
- Create: `android/app/src/test/java/com/lbs/schoolhelper/data/profile/StudentProfileRepositoryImplTest.kt`
- Create: `android/app/src/test/java/com/lbs/schoolhelper/test/FakeStudentProfileRepository.kt`
- Modify: `android/app/src/test/java/com/lbs/schoolhelper/UserPreferencesTest.kt`

**Interfaces:**
- Consumes: Task 1의 `StudentProfileStore`, `StudentProfile`, `StudentProfilesData`
- Produces: `StateFlow<List<StudentProfile>> profiles`
- Produces: `StateFlow<StudentProfile?> activeProfile`
- Produces: `addProfile(displayName: String, studentInfo: StudentInfo): ProfileMutationResult<StudentProfile>`
- Produces: `updateProfile(profile: StudentProfile): ProfileMutationResult<Unit>`
- Produces: `selectProfile(id: String): ProfileMutationResult<Unit>`
- Produces: `deleteProfile(id: String): ProfileMutationResult<Unit>`
- Produces: `requiresInitialSetup(): Boolean`
- Produces: `ProfileMutationResult.Success<T>` and `ProfileMutationResult.Failure(ProfileMutationFailure)`
- Produces: `ProfileMutationFailure` values `BLANK_NAME`, `NAME_TOO_LONG`, `DUPLICATE_NAME`, `INCOMPLETE_STUDENT_INFO`, `PROFILE_NOT_FOUND`, `LAST_PROFILE`, `PERSISTENCE_FAILED`

- [ ] **Step 1: CRUD와 이름 검증 실패 테스트 작성**

```kotlin
@Test fun addProfile_firstProfileBecomesActive()
@Test fun addProfile_trimsDisplayName()
@Test fun addProfile_blankNameReturnsBlankNameFailure()
@Test fun addProfile_elevenCharactersReturnsTooLongFailure()
@Test fun addProfile_sameNameAfterTrimmingReturnsDuplicateFailure()
@Test fun updateProfile_preservesIdAndRegistrationOrder()
@Test fun selectProfile_unknownIdReturnsNotFoundWithoutChangingActiveProfile()
@Test fun deleteProfile_onlyProfileReturnsLastProfileFailure()
@Test fun deleteProfile_activeProfileSelectsFirstRemainingProfile()
@Test fun initialize_missingActiveIdRepairsToFirstValidProfile()
@Test fun initialize_filtersIncompleteOrMalformedEntries()
```

- [ ] **Step 2: 기존 사용자 이전 실패 테스트 작성**

```kotlin
@Test fun initialize_withLegacyStudentInfo_createsUnnamedPendingProfile()
@Test fun initialize_pendingProfileRequiresInitialSetup()
@Test fun updateMigratedProfile_afterSuccessfulWriteClearsLegacyStudentKeys()
@Test fun updateMigratedProfile_whenWriteFailsKeepsLegacyStudentKeys()
@Test fun initialize_withoutNewOrLegacyDataStartsEmpty()
```

고정 UUID 생성기를 테스트 생성자에 전달해 정확한 활성 ID를 단언한다. 불완전한 기존 정보도 초안의 같은 필드에 보존되는지 확인한다.

- [ ] **Step 3: 실패 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*StudentProfileRepositoryImplTest' --tests '*UserPreferencesTest' --console=plain
```

Expected: Repository와 이전 API가 없어 실패.

- [ ] **Step 4: 이전 전용 API와 Repository 구현**

`UserPreferencesStore`에 다음 마이그레이션 전용 메서드를 추가한다.

```kotlin
fun getLegacyStudentInfo(): StudentInfo
fun hasLegacyStudentInfo(): Boolean
fun clearLegacyStudentInfo(): Boolean
```

`StudentProfileRepositoryImpl`은 저장 문서 정규화 후 Flow를 초기화한다. 새 문서가 없고 기존 값이 하나라도 있으면 이름이 빈 초안을 만든다. 새 문서 저장이 `true`일 때만 기존 키를 삭제한다. Hilt는 `StudentProfileStore`와 `StudentProfileRepository` 구현을 Singleton으로 바인딩한다.

- [ ] **Step 5: Fake와 테스트 통과 확인**

`FakeStudentProfileRepository`는 Flow와 모든 변경 호출 기록을 제공해 이후 ViewModel 테스트가 저장 구현에 의존하지 않게 한다.

Run: Step 3과 동일.

Expected: 모든 프로필 Repository와 기존 환경설정 테스트 PASS.

- [ ] **Step 6: 커밋**

```bash
git add android/app/src/main/java/com/lbs/schoolhelper/data/profile \
        android/app/src/main/java/com/lbs/schoolhelper/UserPreferences.kt \
        android/app/src/main/java/com/lbs/schoolhelper/data/repository/UserPreferencesStore.kt \
        android/app/src/main/java/com/lbs/schoolhelper/di/AppModule.kt \
        android/app/src/test/java/com/lbs/schoolhelper/data/profile \
        android/app/src/test/java/com/lbs/schoolhelper/test/FakeStudentProfileRepository.kt \
        android/app/src/test/java/com/lbs/schoolhelper/UserPreferencesTest.kt
git commit -m "feat: manage multiple student profiles"
```

### Task 3: 학교 데이터 조회에 프로필 스냅샷 명시

**Files:**
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/data/repository/SchoolRepository.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/data/repository/SchoolRepositoryImpl.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/home/HomeViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/meal/MealViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/schedule/ScheduleViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/timetable/TimetableViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/widget/MisSchoolWidgetProvider.kt`
- Modify: `android/app/src/test/java/com/lbs/schoolhelper/data/repository/SchoolRepositoryImplTest.kt`
- Modify: `android/app/src/test/java/com/lbs/schoolhelper/test/FakeSchoolRepository.kt`
- Modify: 관련 ViewModel 및 위젯 단위 테스트

**Interfaces:**
- Produces: `getMeals(student: StudentInfo, date: String? = null)` and `observeMeals(student, date)`
- Produces: `getSchedules(student: StudentInfo, date: String? = null)` and `observeSchedules(student, date)`
- Produces: `getNotices(student: StudentInfo, limit: Int = 3)`
- Produces: `getTimetable(student: StudentInfo, date: String? = null)` and `observeTimetable(student, date)`
- Preserves: `searchSchools(query: String)` without profile input

- [ ] **Step 1: 명시적 문맥 실패 테스트 작성**

```kotlin
@Test fun getMeals_usesThePassedStudentInsteadOfCurrentPreferences()
@Test fun getSchedules_usesThePassedStudentForCacheAndNetwork()
@Test fun getNotices_usesThePassedSchoolCode()
@Test fun getTimetable_usesGradeAndClassroomFromPassedStudent()
@Test fun dataLoadedTelemetry_usesTheCapturedRequestStudent()
```

테스트의 환경설정 학생과 메서드 인자의 학생을 서로 다르게 만들어 API 인자, 캐시 키, 텔레메트리 문맥이 메서드 인자와 일치하는지 검증한다.

- [ ] **Step 2: 실패 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*SchoolRepositoryImplTest' --console=plain
```

Expected: 새 메서드 서명이 없어 실패.

- [ ] **Step 3: Repository와 모든 호출부를 한 번에 변경**

`SchoolRepositoryImpl.selectedStudentInfo()`와 학생 정보용 `PreferencesRepository` 의존성을 제거한다. 기존 ViewModel과 위젯은 이 단계에서는 자신이 읽은 현재 `StudentInfo`를 명시적으로 전달해 컴파일 가능한 상태를 유지한다. 시간표의 중복 `grade`, `classroom` 인자는 제거한다.

- [ ] **Step 4: Repository와 전체 단위 테스트 통과 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 커밋**

```bash
git add android/app/src/main/java/com/lbs/schoolhelper/data/repository/SchoolRepository.kt \
        android/app/src/main/java/com/lbs/schoolhelper/data/repository/SchoolRepositoryImpl.kt \
        android/app/src/main/java/com/lbs/schoolhelper/ui \
        android/app/src/main/java/com/lbs/schoolhelper/widget/MisSchoolWidgetProvider.kt \
        android/app/src/test
git commit -m "refactor: pass student context to school queries"
```

### Task 4: 초기 설정과 기존 사용자 이름 입력 흐름

**Files:**
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/splash/SplashDestination.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/splash/SplashViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/SplashActivity.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/setup/SetupUiState.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/setup/SetupViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/SetupActivity.kt`
- Modify: `android/app/src/main/res/layout/activity_setup.xml`
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `android/app/src/test/java/com/lbs/schoolhelper/ui/splash/SplashViewModelTest.kt`
- Modify: `android/app/src/test/java/com/lbs/schoolhelper/ui/setup/SetupViewModelTest.kt`
- Modify: `android/app/src/test/java/com/lbs/schoolhelper/StudentInfoFocusPersistenceTest.kt`

**Interfaces:**
- Consumes: `StudentProfileRepository.activeProfile`, `requiresInitialSetup`, `addProfile`, `updateProfile`
- Produces: `SetupUiState.displayName: String`
- Produces: `SetupViewModel.updateDisplayName(value: String)`
- Produces: 초기 사용자 추가와 이름 없는 이전 프로필 업데이트 경로

- [ ] **Step 1: Splash와 Setup 실패 테스트 작성**

```kotlin
@Test fun splash_withReadyProfileAndConsent_navigatesMain()
@Test fun splash_withUnnamedMigratedProfile_navigatesSetup()
@Test fun setup_loadsLegacyDraftAndLeavesDisplayNameBlank()
@Test fun saveProfile_blankNameEmitsNameRequiredMessage()
@Test fun saveProfile_validInputCompletesMigratedProfileWithoutShowingConsentAgain()
@Test fun saveProfile_firstNewProfileShowsConsentStep()
@Test fun editingNameDoesNotLoseFocusedSchoolGradeOrClassDraft()
```

- [ ] **Step 2: 실패 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*SplashViewModelTest' --tests '*SetupViewModelTest' --tests '*StudentInfoFocusPersistenceTest' --console=plain
```

Expected: 이름 상태와 프로필 Repository 연결이 없어 실패.

- [ ] **Step 3: 초기 설정 UI와 저장 동작 구현**

학교 선택 카드 앞에 `자녀 이름 또는 별칭` 입력을 배치한다. 문자열은 실명 강제가 아님을 설명한다. ViewModel은 이름이 없는 활성 초안이 있으면 그 프로필을 수정하고, 없으면 첫 프로필을 추가한다. 프로필 변경이 성공했을 때만 기존 동의 완료 여부에 따라 동의 단계 또는 홈으로 이동한다.

- [ ] **Step 4: 초기 설정 테스트와 리소스 컴파일 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*SplashViewModelTest' --tests '*SetupViewModelTest' --tests '*StudentInfoFocusPersistenceTest' :app:assembleDebug --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 커밋**

```bash
git add android/app/src/main/java/com/lbs/schoolhelper/SplashActivity.kt \
        android/app/src/main/java/com/lbs/schoolhelper/SetupActivity.kt \
        android/app/src/main/java/com/lbs/schoolhelper/ui/splash \
        android/app/src/main/java/com/lbs/schoolhelper/ui/setup \
        android/app/src/main/res/layout/activity_setup.xml \
        android/app/src/main/res/values/strings.xml \
        android/app/src/test/java/com/lbs/schoolhelper/ui/splash \
        android/app/src/test/java/com/lbs/schoolhelper/ui/setup \
        android/app/src/test/java/com/lbs/schoolhelper/StudentInfoFocusPersistenceTest.kt
git commit -m "feat: collect local child profile names"
```

### Task 5: 설정 화면의 자녀 추가·수정·삭제

**Files:**
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/settings/SettingsUiState.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/settings/SettingsViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/SettingsActivity.kt`
- Modify: `android/app/src/main/res/layout/activity_settings.xml`
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `android/app/src/test/java/com/lbs/schoolhelper/ui/settings/SettingsViewModelTest.kt`

**Interfaces:**
- Produces: `SettingsUiState.profiles`, `editingProfileId`, `displayName`, `canDeleteProfile`, `hasUnsavedProfileChanges`
- Produces: `selectEditingProfile(id)`, `startAddingProfile()`, `updateDisplayName(value)`, `saveEditingProfile()`, `deleteEditingProfile()`
- Produces: 저장·버리기·취소 결정을 요청하는 일회성 UI 이벤트

- [ ] **Step 1: 관리 동작 실패 테스트 작성**

```kotlin
@Test fun loadSettings_selectsActiveProfileForEditing()
@Test fun startAddingProfile_clearsOnlyProfileFormAndPreservesGlobalSettings()
@Test fun saveNewProfile_makesNewProfileActive()
@Test fun saveExistingInactiveProfile_doesNotChangeActiveProfile()
@Test fun saveProfile_duplicateTrimmedNameShowsDuplicateError()
@Test fun switchEditingProfile_withDirtyFormRequestsSaveDiscardOrCancel()
@Test fun deleteProfile_whenOnlyOneDisablesDeletion()
@Test fun deleteActiveProfile_selectsRepositoryFallbackAndRefreshesForm()
```

- [ ] **Step 2: 실패 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*SettingsViewModelTest' --console=plain
```

Expected: 다중 프로필 상태와 관리 메서드가 없어 실패.

- [ ] **Step 3: Settings ViewModel과 UI 구현**

기존 학생 정보 영역 상단에 편집 프로필 선택기, `자녀 추가`, 이름 입력, 조건부 `이 자녀 삭제`를 추가한다. 프로필 변경 전에 더러운 입력이 있으면 `저장`, `버리기`, `취소` 대화상자를 표시한다. 삭제는 프로필 이름을 포함한 확인 문구를 보여 주되 그 이름을 로그나 텔레메트리로 전달하지 않는다.

- [ ] **Step 4: 설정 테스트와 빌드 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*SettingsViewModelTest' :app:assembleDebug --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 커밋**

```bash
git add android/app/src/main/java/com/lbs/schoolhelper/SettingsActivity.kt \
        android/app/src/main/java/com/lbs/schoolhelper/ui/settings \
        android/app/src/main/res/layout/activity_settings.xml \
        android/app/src/main/res/values/strings.xml \
        android/app/src/test/java/com/lbs/schoolhelper/ui/settings/SettingsViewModelTest.kt
git commit -m "feat: manage child profiles in settings"
```

### Task 6: 홈 프로필 전환과 오래된 응답 차단

**Files:**
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/data/model/HomeUiState.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/home/HomeViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/MainActivity.kt`
- Modify: `android/app/src/main/res/layout/activity_main.xml`
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `android/app/src/test/java/com/lbs/schoolhelper/ui/home/HomeViewModelTest.kt`
- Modify: `android/app/src/test/java/com/lbs/schoolhelper/MainActivityNavigationTest.kt`

**Interfaces:**
- Consumes: `StudentProfileRepository.profiles`, `activeProfile`, `selectProfile(id)`
- Produces: `HomeUiState.activeProfileId`, `activeProfileName`, `profileChoices`
- Produces: `HomeViewModel.selectProfile(id)`
- Produces: 홈 프로필 선택 대화상자와 설정의 추가 모드 진입

- [ ] **Step 1: 전환 실패 테스트 작성**

```kotlin
@Test fun initialState_showsActiveProfileNameSchoolAndClass()
@Test fun selectProfile_updatesHeaderAndReloadsAllHomeSections()
@Test fun selectProfile_cancelsPreviousProfileLoad()
@Test fun stalePreviousProfileResponse_doesNotOverwriteNewProfileState()
@Test fun profileSwitch_doesNotEmitSchoolChangedTelemetry()
@Test fun addProfileAction_opensSettingsInAddMode()
```

느린 FakeSchoolRepository가 첫 프로필 결과를 나중에 반환하도록 제어하고, 최종 상태가 두 번째 프로필 데이터만 포함하는지 단언한다.

- [ ] **Step 2: 실패 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*HomeViewModelTest' --tests '*MainActivityNavigationTest' --console=plain
```

Expected: 프로필 상태와 전환 메서드가 없어 실패.

- [ ] **Step 3: 홈 전환 구현**

홈 히어로에 48dp 터치 영역의 이름 칩과 아래 화살표를 추가한다. ViewModel은 활성 프로필 Flow를 수집하고 기존 `loadHomeJob`을 취소한 다음 캡처한 `StudentInfo`로 공지·급식·일정을 다시 요청한다. 대화상자 각 행에는 `이름`과 `학교 · 학년 반`을 표시하고 마지막 행은 `자녀 추가`로 설정을 연다.

- [ ] **Step 4: 홈 테스트와 빌드 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*HomeViewModelTest' --tests '*MainActivityNavigationTest' :app:assembleDebug --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 커밋**

```bash
git add android/app/src/main/java/com/lbs/schoolhelper/MainActivity.kt \
        android/app/src/main/java/com/lbs/schoolhelper/data/model/HomeUiState.kt \
        android/app/src/main/java/com/lbs/schoolhelper/ui/home/HomeViewModel.kt \
        android/app/src/main/res/layout/activity_main.xml \
        android/app/src/main/res/values/strings.xml \
        android/app/src/test/java/com/lbs/schoolhelper/ui/home/HomeViewModelTest.kt \
        android/app/src/test/java/com/lbs/schoolhelper/MainActivityNavigationTest.kt
git commit -m "feat: switch active child from home"
```

### Task 7: 급식·시간표·학사 일정의 활성 프로필 반영

**Files:**
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/meal/MealUiState.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/meal/MealViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/schedule/ScheduleUiState.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/schedule/ScheduleViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/timetable/TimetableUiState.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/timetable/TimetableViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/MealActivity.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ScheduleActivity.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/TimetableActivity.kt`
- Modify: `android/app/src/main/res/layout/activity_meal.xml`
- Modify: `android/app/src/main/res/layout/activity_schedule.xml`
- Modify: `android/app/src/main/res/layout/activity_timetable.xml`
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: 해당 ViewModel 테스트 3개

**Interfaces:**
- Consumes: 활성 `StudentProfile`과 Task 3의 명시적 `SchoolRepository` 메서드
- Produces: 각 UiState의 `profileContextText`
- Produces: 활성 프로필 변경 시 이전 로드 취소와 현재 날짜·월 기준 재조회

- [ ] **Step 1: 상세 화면 실패 테스트 작성**

```kotlin
@Test fun meal_activeProfileChangeReloadsSameWeekForNewSchool()
@Test fun timetable_activeProfileChangeUsesNewGradeAndClassroom()
@Test fun schedule_activeProfileChangeReloadsCurrentMonth()
@Test fun detailStates_showProfileNameAsContext()
@Test fun missingActiveProfile_showsSetupRequiredWithoutNetworkCall()
```

- [ ] **Step 2: 실패 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*MealViewModelTest' --tests '*TimetableViewModelTest' --tests '*ScheduleViewModelTest' --console=plain
```

Expected: 활성 프로필 관찰과 문맥 표시가 없어 실패.

- [ ] **Step 3: 상세 화면 Flow와 보조 라벨 구현**

각 ViewModel은 활성 프로필이 바뀌면 현재 주·날짜·월을 유지한 채 기존 Job을 취소하고 재조회한다. 화면 제목 아래에 `민준 · 한빛중학교` 형식의 보조 라벨을 추가한다. 상세 화면 내부에 별도 전환 버튼은 추가하지 않는다.

- [ ] **Step 4: 상세 테스트와 리소스 빌드 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*MealViewModelTest' --tests '*TimetableViewModelTest' --tests '*ScheduleViewModelTest' :app:assembleDebug --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 커밋**

```bash
git add android/app/src/main/java/com/lbs/schoolhelper/MealActivity.kt \
        android/app/src/main/java/com/lbs/schoolhelper/ScheduleActivity.kt \
        android/app/src/main/java/com/lbs/schoolhelper/TimetableActivity.kt \
        android/app/src/main/java/com/lbs/schoolhelper/ui/meal \
        android/app/src/main/java/com/lbs/schoolhelper/ui/schedule \
        android/app/src/main/java/com/lbs/schoolhelper/ui/timetable \
        android/app/src/main/res/layout/activity_meal.xml \
        android/app/src/main/res/layout/activity_schedule.xml \
        android/app/src/main/res/layout/activity_timetable.xml \
        android/app/src/main/res/values/strings.xml \
        android/app/src/test/java/com/lbs/schoolhelper/ui/meal \
        android/app/src/test/java/com/lbs/schoolhelper/ui/schedule \
        android/app/src/test/java/com/lbs/schoolhelper/ui/timetable
git commit -m "feat: reload school screens for active child"
```

### Task 8: 위젯별 자녀 선택과 삭제 fallback

**Files:**
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/data/repository/PreferencesRepository.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/data/repository/PreferencesRepositoryImpl.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/widget/WidgetConfigUiState.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/widget/WidgetConfigViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/widget/WidgetConfigActivity.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/widget/MisSchoolWidgetProvider.kt`
- Modify: `android/app/src/main/res/layout/activity_widget_config.xml`
- Modify: `android/app/src/main/res/values/strings.xml`
- Create: `android/app/src/test/java/com/lbs/schoolhelper/ui/widget/WidgetConfigViewModelTest.kt`
- Create: `android/app/src/test/java/com/lbs/schoolhelper/widget/WidgetProfileResolverTest.kt`
- Modify: `android/app/src/test/java/com/lbs/schoolhelper/test/FakePreferencesRepository.kt`

**Interfaces:**
- Changes: `WidgetSettings(profileId: String = "", showTomorrowTimetable: Boolean = true)`
- Produces: `WidgetConfigUiState.profiles`, `selectedProfileId`
- Produces: `WidgetProfileResolver.resolve(settings, profiles, activeProfile): StudentProfile?`

- [ ] **Step 1: 위젯 프로필 실패 테스트 작성**

```kotlin
@Test fun config_loadsSavedWidgetProfile()
@Test fun config_newWidgetDefaultsToActiveProfile()
@Test fun config_savePersistsProfileIdAndTomorrowFlag()
@Test fun resolver_missingSavedProfileFallsBackToActiveProfile()
@Test fun resolver_withoutAnyProfileReturnsNull()
@Test fun provider_usesWidgetProfileInsteadOfGlobalActiveProfile()
```

- [ ] **Step 2: 실패 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*WidgetConfigViewModelTest' --tests '*WidgetProfileResolverTest' --console=plain
```

Expected: `profileId`와 resolver가 없어 실패.

- [ ] **Step 3: 위젯 설정과 Provider 구현**

위젯 설정 화면에 프로필 단일 선택 목록을 추가한다. 저장된 ID가 비어 있거나 삭제된 경우 활성 프로필로 대체하고 그 ID를 위젯 설정에 다시 저장한다. 프로필 수정·삭제는 `requestAllWidgetUpdates`를 호출하지만 단순 홈 프로필 선택은 고정된 위젯 프로필을 바꾸지 않는다.

- [ ] **Step 4: 위젯 테스트와 QA 리소스 빌드 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*Widget*Test' :app:assembleQa --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 커밋**

```bash
git add android/app/src/main/java/com/lbs/schoolhelper/data/repository \
        android/app/src/main/java/com/lbs/schoolhelper/ui/widget \
        android/app/src/main/java/com/lbs/schoolhelper/widget \
        android/app/src/main/res/layout/activity_widget_config.xml \
        android/app/src/main/res/values/strings.xml \
        android/app/src/test/java/com/lbs/schoolhelper/ui/widget \
        android/app/src/test/java/com/lbs/schoolhelper/widget \
        android/app/src/test/java/com/lbs/schoolhelper/test/FakePreferencesRepository.kt
git commit -m "feat: bind widgets to child profiles"
```

### Task 9: 개인정보 경계, 기존 단일 API 제거 및 문서 갱신

**Files:**
- Modify: `android/app/src/main/res/xml/backup_rules.xml`
- Modify: `android/app/src/main/res/xml/data_extraction_rules.xml`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/telemetry/AppTelemetry.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/telemetry/TelemetryReporter.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/di/TelemetryModule.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/data/repository/PreferencesRepository.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/data/repository/UserPreferencesStore.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/UserPreferences.kt`
- Modify: `android/app/src/test/java/com/lbs/schoolhelper/telemetry/TelemetryReporterTest.kt`
- Create: `android/app/src/test/java/com/lbs/schoolhelper/StudentProfilePrivacyBoundaryTest.kt`
- Modify: `docs/privacy/README.md`
- Modify: `docs/privacy/index.html`
- Modify: `docs/project_specification.md`
- Modify: `DESIGN.md`

**Interfaces:**
- Changes: `AppTelemetry.schoolSaved(previous: StudentInfo, current: StudentInfo)`
- Removes: 일반 호출용 `PreferencesRepository.getStudentInfo`, `saveStudentInfo`, `hasStudentInfo`
- Preserves: 마이그레이션 전용 legacy 접근은 `StudentProfileRepositoryImpl` 내부 경계에서만 사용

- [ ] **Step 1: 개인정보 실패 테스트 작성**

```kotlin
@Test fun schoolSaved_parametersNeverContainDisplayNameOrProfileId()
@Test fun switchingProfilesDoesNotEmitSchoolChanged()
@Test fun backupRulesExcludeStudentProfilesFromCloudBackup()
@Test fun extractionRulesExcludeStudentProfilesFromCloudAndDeviceTransfer()
```

XML 테스트는 Robolectric 리소스 파서로 `sharedpref/student_profiles.xml` 제외 항목을 읽는다. TelemetrySink의 이벤트·문맥·예외 매개변수 키와 값 전체에 테스트 이름과 UUID가 없는지 확인한다.

- [ ] **Step 2: 실패 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*TelemetryReporterTest' --tests '*StudentProfilePrivacyBoundaryTest' --console=plain
```

Expected: 백업 제외와 새 텔레메트리 경계가 없어 실패.

- [ ] **Step 3: 백업·텔레메트리·legacy 정리 구현**

두 백업 규칙에 다음 제외를 추가한다.

```xml
<exclude domain="sharedpref" path="student_profiles.xml" />
```

TelemetryModule은 `activeProfile.value?.studentInfo ?: StudentInfo()`를 문맥 공급자로 사용한다. 프로필 저장은 명시적인 이전·현재 `StudentInfo`만 `schoolSaved`에 전달하고, 선택 동작은 이 이벤트를 호출하지 않는다. 모든 새 호출부 전환 후 기존 단일 학생용 `PreferencesRepository` 메서드를 제거한다.

- [ ] **Step 4: 개인정보 처리방침과 제품 문서 갱신**

자녀 이름 또는 별칭은 단말 내부에서 프로필 구분에만 사용되고 서버·Firebase·백업으로 전송되지 않는다는 문구를 한국어 개인정보 처리방침에 추가한다. `docs/project_specification.md`와 `DESIGN.md`에는 전역 활성 자녀, 홈 전환 칩, 위젯별 프로필 원칙을 반영한다.

- [ ] **Step 5: 개인정보 및 전체 단위 테스트 확인**

Run:

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: 커밋**

```bash
git add android/app/src/main/res/xml/backup_rules.xml \
        android/app/src/main/res/xml/data_extraction_rules.xml \
        android/app/src/main/java/com/lbs/schoolhelper/telemetry \
        android/app/src/main/java/com/lbs/schoolhelper/di/TelemetryModule.kt \
        android/app/src/main/java/com/lbs/schoolhelper/data/repository \
        android/app/src/main/java/com/lbs/schoolhelper/UserPreferences.kt \
        android/app/src/test/java/com/lbs/schoolhelper/telemetry \
        android/app/src/test/java/com/lbs/schoolhelper/StudentProfilePrivacyBoundaryTest.kt \
        docs/privacy/README.md docs/privacy/index.html docs/project_specification.md DESIGN.md
git commit -m "privacy: keep child profile identity on device"
```

### Task 10: 전체 검증과 실제 단말 확인

**Files:**
- Modify: 검증에서 발견된 결함과 직접 관련된 파일만 수정

**Interfaces:**
- Consumes: Tasks 1~9의 완성된 다자녀 흐름
- Produces: 테스트·린트·빌드·실기기 검증 증거

- [ ] **Step 1: 정적 검증과 전체 단위 테스트**

Run:

```bash
git diff --check
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew \
  :app:testDebugUnitTest \
  :app:lintQa \
  :app:assembleQa \
  :app:assembleRelease \
  --console=plain
```

Expected: BUILD SUCCESSFUL, lint errors 0.

- [ ] **Step 2: Firebase와 네트워크 정적 누출 검사**

Run:

```bash
rg -n 'displayName|profileId' app/src/main/java/com/lbs/schoolhelper/telemetry app/src/main/java/com/lbs/schoolhelper/data/remote
rg -n 'student_profiles.xml' app/src/main/res/xml/backup_rules.xml app/src/main/res/xml/data_extraction_rules.xml
```

Expected: 첫 명령은 자녀 식별자를 전송하는 코드가 없어 결과가 비어 있고, 둘째 명령은 두 백업 파일의 제외 항목을 출력한다.

- [ ] **Step 3: 연결 단말에 QA 설치**

Run:

```bash
adb devices
adb install -r app/build/outputs/apk/qa/*.apk
```

Expected: 연결된 기기 1대 이상과 `Success`. 출력 파일명이 커스텀되어 wildcard가 여러 파일과 일치하면 최신 QA APK 하나를 선택한다.

- [ ] **Step 4: 실제 단말 다자녀 시나리오 확인**

- 새 설치: 첫 자녀 이름·학교·학년·반 입력 후 홈 진입
- 업그레이드 데이터: 기존 학교 정보가 채워지고 이름만 입력하면 홈 진입
- 서로 다른 학교 자녀 두 명을 전환해 급식·일정·공지 변경 확인
- 같은 학교의 다른 학년·반을 전환해 시간표가 섞이지 않는지 확인
- 자녀별 위젯 두 개를 배치하고 새로고침·재부팅·자정 갱신 확인
- 활성 자녀 삭제 후 남은 프로필과 위젯 fallback 확인
- 글자 크기 1.0배·1.3배와 TalkBack에서 전환 칩·대화상자·설정 입력 확인

- [ ] **Step 5: 최종 회귀 수정과 재검증**

실패한 시나리오가 있으면 해당 Task의 단위 테스트를 먼저 추가한 뒤 수정하고 Step 1~4 중 영향을 받은 검증만 다시 실행한다.

- [ ] **Step 6: 최종 커밋**

```bash
git add <검증으로 수정한 파일만>
git commit -m "test: verify multi-child profile flows"
```

최종 변경이 없으면 빈 커밋을 만들지 않는다.
