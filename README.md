# MisSchoolApp

`MisSchoolApp`은 **학교도우미** 멀티플랫폼 프로젝트 저장소입니다.

현재 저장소는 **루트는 문서/자동화 자산**, **안드로이드 앱 본체는 `/android`**, **웹 클라이언트는 `/web`** 아래에 두는 구조로 정리되어 있습니다.

## 현재 구현 상태 요약

- **Android**
  - 기존 기준 플랫폼
  - Activity + XML + Hilt + MVVM 구조 유지
- **Web**
  - Next.js App Router 기반 단일 웹앱
  - 학교 검색(초등학교/중학교)
  - 시간표 / 급식 / 학사 일정 실데이터 조회
  - 급식 상세 페이지(`/meals`)
  - 공통 상태 UX(로딩/오류/빈 상태/재시도)
  - 브라우저 캐시/복구 전략

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

### 3) 웹 실행
```bash
cd web
npm install
npm run dev
```

품질 확인:

```bash
cd web
npm run lint
npm run typecheck
npm run build
```

## 저장소 구조

```text
misSchoolApp/
├── android/              # Android Gradle 프로젝트 루트
├── web/                  # 학교도우미 Web 클라이언트
├── .codex/               # Codex/OMX 에이전트 스킬 및 프롬프트
├── docs/                 # 프로젝트 문서
├── .github/              # GitHub 관련 자산
└── AGENTS.md             # 에이전트 작업 규칙
```

문서 안내:
- 안드로이드/웹 폴더 구조: `docs/project-structure.md`
- 멀티플랫폼 기능/정책 스펙: `docs/project_specification.md`
- Android Studio 열기 안내: `docs/android-studio-setup.md`
- 웹 자동화/스킬 정리: `docs/skills/web-automation-map.md`
- skill 이름 규칙: `docs/skills/skill-naming-conventions.md`

## 웹에서 현재 제공하는 주요 화면

- `/` : 홈 대시보드
- `/setup` : 초기 설정
- `/settings` : 학교/학년/반 설정 변경
- `/timetable` : 날짜별 시간표 조회
- `/schedule` : 월간 학사 일정 조회
- `/meals` : 날짜별 급식 상세 조회
- `/timer` : 타이머 영역(현재는 기본 UI 중심)

## 웹 데이터 정책 요약

- 외부 NEIS API는 브라우저에서 직접 호출하지 않고 `app/api/**/route.ts` BFF를 통해 호출
- 학교 검색 시 학교 코드 / 교육청 코드 / 학교 종류를 저장
- 급식 / 시간표 / 일정 조회는 브라우저 캐시를 사용
- 최신 요청 실패 시 마지막 성공 데이터를 fallback으로 재사용

## 스킬 파일 권장 위치

실행용 스킬 파일은 앱 코드와 분리해서 관리하는 것을 권장합니다.

- 실행 스킬: `.codex/skills/<skill-name>/SKILL.md`
- 설명 문서: `docs/skills/<skill-name>.md`
- GitHub 워크플로우/메타 자산: `.github/`

이렇게 두면 안드로이드 앱 코드(`/android`)와 자동화/문서 자산이 섞이지 않아 유지보수가 쉬워집니다.
