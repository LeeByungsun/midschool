# Android Studio 사용 안내

안드로이드 프로젝트는 이제 저장소 루트가 아니라 **`android/` 폴더 아래**에 있습니다.

## 프로젝트 열기

### 권장 방법
1. Android Studio 실행
2. **Open** 또는 **Open an Existing Project** 선택
3. 저장소 루트가 아니라 `misSchoolApp/android` 폴더 선택
4. Gradle Sync 완료 대기

## 이미 루트 폴더를 열어 둔 경우

루트(`misSchoolApp`)를 열어 두었다면 다음 중 하나를 사용하면 됩니다.

### 방법 A: 다시 열기
- 현재 프로젝트 닫기
- `misSchoolApp/android` 를 다시 열기

### 방법 B: Android 폴더만 다시 선택
- Android Studio의 Open 기능으로 `android/` 디렉터리를 직접 선택

## 터미널 기준 실행 명령

Android Studio 내장 터미널이나 일반 터미널에서 아래처럼 실행하면 됩니다.

```bash
cd android
./gradlew testDebugUnitTest lintDebug
```

루트에서 바로 실행하려면:

```bash
./android/gradlew testDebugUnitTest lintDebug
```

## 자주 만나는 상황

### Gradle 프로젝트를 못 찾는 경우
- 루트가 아니라 `android/` 폴더를 열었는지 확인하세요.

### `local.properties` 관련 경고가 보이는 경우
- `android/local.properties` 가 존재하는지 확인하세요.
- Android SDK 경로가 바뀌었다면 Android Studio가 다시 생성하도록 유도하거나 수동 수정하세요.

### Run/Debug 설정이 꼬인 경우
- `android/` 를 다시 열고 Gradle Sync를 한 번 더 수행하세요.

## 권장 운영 방식

- 저장소 루트: 문서, 자동화, Codex/OMX 자산 관리
- `android/`: 실제 앱 개발, 빌드, 테스트, 배포 준비

이 기준을 유지하면 프로젝트가 커져도 역할 분리가 명확해집니다.
