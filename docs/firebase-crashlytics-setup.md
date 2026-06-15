# Firebase Crashlytics 설정 체크리스트

## 목표

Android와 iOS 앱에서 Firebase Crashlytics가 빌드에 포함되고, Firebase 콘솔 설정 파일이 추가되면 실제 크래시 리포트가 Firebase 대시보드로 전송되도록 유지합니다.

## 현재 코드 상태

### Android

- Firebase BoM: `34.14.1`
- Google Services Gradle Plugin: `4.4.4`
- Firebase Crashlytics Gradle Plugin: `3.0.7`
- SDK 의존성:
  - `com.google.firebase:firebase-crashlytics`
  - `com.google.firebase:firebase-analytics`
- 설정 파일 위치:
  - `android/app/google-services.json`
- 활성화 방식:
  - `google-services.json`이 있을 때만 Google Services Plugin과 Crashlytics Plugin을 적용합니다.
  - 설정 파일이 없는 로컬 환경에서도 Android 빌드가 깨지지 않습니다.

### iOS

- Firebase Apple SDK: Swift Package Manager, `firebase-ios-sdk` `12.14.0` 이상 `13.0.0` 미만
- 연결 제품:
  - `FirebaseCore`
  - `FirebaseCrashlytics`
- 설정 파일 위치:
  - `ios/SchoolHelper/Resources/GoogleService-Info.plist`
- 활성화 방식:
  - 앱 시작 시 `GoogleService-Info.plist`가 번들에 있을 때만 `FirebaseApp.configure()`를 실행합니다.
  - Xcode 빌드 중 설정 파일이 있으면 앱 번들로 복사합니다.
  - Crashlytics dSYM 업로드 Run Script는 Firebase SDK checkout과 앱 번들 설정 파일이 모두 있을 때만 실행합니다.

## Firebase 콘솔에서 해야 할 일

1. Firebase 프로젝트를 생성하거나 기존 프로젝트를 선택합니다.
2. Android 앱을 추가합니다.
   - 패키지명: `com.lbs.schoolhelper`
   - 받은 `google-services.json`을 `android/app/google-services.json`에 둡니다.
3. iOS 앱을 추가합니다.
   - Bundle ID: `com.lbs.shcoolhelper`
   - 받은 `GoogleService-Info.plist`를 `ios/SchoolHelper/Resources/GoogleService-Info.plist`에 둡니다.
4. Firebase 콘솔의 Crashlytics 안내에 따라 각 플랫폼 앱을 1회 실행합니다.
5. 테스트 크래시를 발생시킨 뒤 Firebase Crashlytics 대시보드에서 수신을 확인합니다.

## 검증 명령

### 공통 정적 설정 점검

```bash
scripts/check_firebase_crashlytics.sh
```

Firebase 콘솔 설정 파일까지 필수로 확인하려면 다음 명령을 사용합니다.

```bash
scripts/check_firebase_crashlytics.sh --require-config
```

### Android

```bash
cd android
./gradlew :app:compileDebugKotlin
./gradlew lintDebug
```

설정 파일 추가 후에는 릴리즈 빌드/배포 파이프라인에서 Crashlytics Plugin이 매핑 파일 업로드를 실행하는지 확인합니다.

### iOS

현재 CLI 환경에서 전체 Xcode가 선택되어 있어야 Xcode 프로젝트 빌드가 가능합니다.

```bash
cd ios
xcodebuild -resolvePackageDependencies -project SchoolHelper.xcodeproj -scheme SchoolHelper
xcodebuild -project SchoolHelper.xcodeproj -scheme SchoolHelper -destination 'generic/platform=iOS Simulator' build
```

Command Line Tools만 선택된 환경에서는 다음 오류가 날 수 있습니다.

```text
xcrun: error: unable to find utility "xcodebuild", not a developer tool or in PATH
```

이 경우 Xcode 설치 후 `xcode-select`를 전체 Xcode 경로로 변경해야 합니다.
