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

### 시간표가 인증키 설정 오류로 표시되는 경우
- NEIS Open API에서 발급받은 키를 `android/local.properties`에만 추가하세요. 이 파일은 Git에 포함하지 않습니다.
  ```properties
  sdk.dir=/Users/계정명/Library/Android/sdk
  NEIS_API_KEY=발급받은_나이스_인증키
  ```
- 키를 넣은 뒤 Gradle Sync 후 앱을 다시 빌드·설치하세요. 키가 비어 있으면 앱은 불완전한 캐시 시간표 대신 설정 오류를 표시합니다.

### 가정통신문 조회 또는 단위 테스트가 `WEB_BASE_URL` 오류로 실패하는 경우
- 가정통신문 API를 제공하는 웹 서버의 **기본 URL**을 `android/local.properties`에 설정하세요. 끝에 `/`가 필요합니다.
  ```properties
  WEB_BASE_URL=https://배포된-웹-서버-주소/
  ```
- 미설정 기본값은 빈 문자열입니다. 이 경우 Retrofit 초기화가 필요한 단위 테스트도 실패할 수 있습니다. 테스트용으로 형식만 맞는 주소를 넣어 통과하더라도 실제 가정통신문 조회가 된다는 뜻은 아닙니다.
- `local.properties`는 Git에 포함하지 않습니다. 출시 빌드에서는 실제 웹 서버 주소로 조회와 외부 링크 열기를 별도 확인하세요.

### Run/Debug 설정이 꼬인 경우
- `android/` 를 다시 열고 Gradle Sync를 한 번 더 수행하세요.

## 권장 운영 방식

- 저장소 루트: 문서, 자동화, 멀티플랫폼 자산 관리
- `android/`: Android 앱 개발, 빌드, 테스트, 배포 준비

이 기준을 유지하면 프로젝트가 커져도 역할 분리가 명확해집니다.
