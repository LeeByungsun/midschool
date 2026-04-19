# MisSchoolApp

중학교도우미 안드로이드 앱 프로젝트입니다.

현재 저장소는 **루트는 문서/자동화 자산**, **안드로이드 앱 본체는 `/android`** 아래에 두는 구조로 정리되어 있습니다.

## 빠른 시작

### 1) Android Studio로 열기
- Android Studio에서 이 저장소 루트가 아니라 **`android/` 폴더**를 여세요.
- 즉, `misSchoolApp/android` 를 프로젝트로 열면 됩니다.

자세한 안내는 아래 문서를 참고하세요.
- [프로젝트 구조 문서](docs/project-structure.md)
- [Android Studio 사용 안내](docs/android-studio-setup.md)

### 2) 커맨드라인 빌드
```bash
cd android
./gradlew testDebugUnitTest lintDebug
```

또는 저장소 루트에서:

```bash
./android/gradlew testDebugUnitTest lintDebug
```

## 저장소 구조

```text
misSchoolApp/
├── android/              # Android Gradle 프로젝트 루트
├── .codex/               # Codex/OMX 에이전트 스킬 및 프롬프트
├── docs/                 # 프로젝트 문서
├── .github/              # GitHub 관련 자산
└── AGENTS.md             # 에이전트 작업 규칙
```

안드로이드 앱 내부 구조는 `docs/project-structure.md` 에 정리되어 있습니다.

## 스킬 파일 권장 위치

실행용 스킬 파일은 앱 코드와 분리해서 관리하는 것을 권장합니다.

- 실행 스킬: `.codex/skills/<skill-name>/SKILL.md`
- 설명 문서: `docs/skills/<skill-name>.md`
- GitHub 전용 자산: `.github/skills/`

이렇게 두면 안드로이드 앱 코드(`/android`)와 자동화/문서 자산이 섞이지 않아 유지보수가 쉬워집니다.
