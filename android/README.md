# 스쿨온 Android

이 폴더는 `misSchoolApp` Gradle 프로젝트의 **스쿨온 Android 앱**입니다. 멀티플랫폼 프로젝트 문서에서는 학교도우미라는 이름을 사용합니다. 일반 빌드의 앱 이름은 `스쿨온`, QA 빌드는 `스쿨온 QA`입니다.

Android 쪽은 현재 서비스의 기준 플랫폼이며, Kotlin + XML + Hilt + MVVM 구조를 사용합니다.

## 기술 스택

- 언어: Kotlin
- UI: XML + DataBinding
- 아키텍처: MVVM
- DI: Hilt
- 비동기 처리: Coroutines / Flow
- 네트워크: Retrofit + OkHttp + Gson
- 로컬 저장: SharedPreferences 기반

## 프로젝트 열기

Android Studio에서는 저장소 루트가 아니라 **`android/` 폴더**를 직접 열어야 합니다.

예:
- `misSchoolApp/android`

자세한 안내:
- `../docs/android-studio-setup.md`

가정통신문 서버 주소는 `android/local.properties`의 `WEB_BASE_URL`에서 빌드 설정으로 주입합니다. 기본값은 빈 문자열이므로 앱을 실행할 환경에서는 `/`로 끝나는 웹 서버 기본 URL을 설정하세요. NEIS 조회에는 같은 파일의 `NEIS_API_KEY`가 필요합니다. `local.properties`는 Git에 포함하지 않습니다.

## 자주 쓰는 명령

### 테스트 / 린트
```bash
cd android
./gradlew testDebugUnitTest lintDebug
```

### 디버그 빌드
```bash
cd android
./gradlew assembleDebug
```

## 폴더 구조 요약

```text
android/
├── app/
│   ├── src/main/java/com/lbs/schoolhelper/
│   │   ├── data/         # 모델, 자녀 프로필, 원격 API, repository
│   │   ├── di/           # Hilt 모듈
│   │   ├── telemetry/    # 선택적 Analytics/오류 진단 경계
│   │   ├── timer/        # 타이머 알람/스케줄링
│   │   ├── ui/           # 화면별 ViewModel / UI state
│   │   ├── util/         # 공용 유틸
│   │   └── widget/       # 앱 위젯 및 커스텀 뷰
│   └── src/main/res/     # layout, drawable, values, xml
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

더 자세한 구조는 `../docs/project-structure.md`를 참고하세요.

## 현재 Android 기준 특징

- 학교 생활 정보 조회의 기준 플랫폼
- 자녀별 프로필과 학교/학년/반 설정
- 앱 위젯 지원
- 타이머 알림/진동 지원
- 홈 카드에서 타이머를 조작하는 XML 기반 화면 구성

## 관련 문서

- 프로젝트 전체 스펙: `../docs/project_specification.md`
- 프로젝트 구조: `../docs/project-structure.md`
- Android Studio 사용 안내: `../docs/android-studio-setup.md`
- 개인정보처리방침: `../docs/privacy-policy.ko.md`
- Android 전용 작업 규칙: `AGENTS.md`
