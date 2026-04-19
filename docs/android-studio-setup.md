# Android Studio 사용 안내

이 문서는 `android/README.md`를 보완하는 **Android Studio 열기/동기화 중심 안내**입니다.

일반적인 Android 개발 안내와 자주 쓰는 명령은 아래 문서를 먼저 참고하세요.

- `../android/README.md`

## 핵심 원칙

Android Studio에서는 저장소 루트가 아니라 **`android/` 폴더**를 직접 열어야 합니다.

예:
- `misSchoolApp/android`

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

## 자주 만나는 상황

### Gradle 프로젝트를 못 찾는 경우
- 루트가 아니라 `android/` 폴더를 열었는지 확인하세요.

### `local.properties` 관련 경고가 보이는 경우
- `android/local.properties` 가 존재하는지 확인하세요.
- Android SDK 경로가 바뀌었다면 Android Studio가 다시 생성하도록 유도하거나 수동 수정하세요.

### Run/Debug 설정이 꼬인 경우
- `android/` 를 다시 열고 Gradle Sync를 한 번 더 수행하세요.

## 권장 운영 방식

- 저장소 루트: 문서, 자동화, 멀티플랫폼 자산 관리
- `android/`: Android 앱 개발, 빌드, 테스트, 배포 준비

이 기준을 유지하면 프로젝트가 커져도 역할 분리가 명확해집니다.
