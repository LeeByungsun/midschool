# 학교도우미 Android

이 폴더는 `MisSchoolApp` 저장소 안의 **학교도우미 Android 앱 프로젝트**입니다.

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

저장소 루트에서 실행하려면:

```bash
./android/gradlew testDebugUnitTest lintDebug
./android/gradlew assembleDebug
```

## 폴더 구조 요약

```text
android/
├── app/
│   ├── src/main/java/com/bsbarron/midschoolapp/
│   │   ├── data/         # 모델, 원격 API, repository
│   │   ├── di/           # Hilt 모듈
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
- 앱 위젯 지원
- 타이머 알림/진동 지원
- XML 기반 화면 구성 유지

## 관련 문서

- 프로젝트 전체 스펙: `../docs/project_specification.md`
- 프로젝트 구조: `../docs/project-structure.md`
- Android Studio 사용 안내: `../docs/android-studio-setup.md`
- Android 전용 작업 규칙: `AGENTS.md`
