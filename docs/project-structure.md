# 프로젝트 구조

이 저장소는 **루트 작업공간**과 **안드로이드 앱 프로젝트**를 분리한 구조를 사용합니다.

## 최상위 구조

```text
misSchoolApp/
├── android/              # Android Gradle 프로젝트
├── web/                  # Next.js 기반 Web 클라이언트
├── docs/                 # 기획/구조/설정 문서
├── .codex/               # Codex/OMX 스킬, 프롬프트, 에이전트 자산
├── .github/              # GitHub 관련 자산
├── AGENTS.md             # 에이전트 작업 지침
└── README.md             # 저장소 시작 안내
```

## `android/` 내부 구조

```text
android/
├── app/                  # Android application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/bsbarron/midschoolapp/
│   │   │   │   ├── data/         # 모델, 원격 API, repository
│   │   │   │   ├── di/           # Hilt 모듈
│   │   │   │   ├── timer/        # 타이머 알람/스케줄링
│   │   │   │   ├── ui/           # 화면별 ViewModel / UI state
│   │   │   │   ├── util/         # 공용 유틸
│   │   │   │   └── widget/       # 앱 위젯 및 커스텀 뷰
│   │   │   ├── res/              # layout, drawable, values, xml
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                 # 로컬 단위 테스트
│   │   └── androidTest/          # 계측 테스트
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties
├── gradlew
└── gradlew.bat
```

## 왜 `android/` 아래로 분리했나요?

이 구조는 다음 목적에 유리합니다.

- 저장소 루트를 문서/자동화/도구 자산과 함께 운영하기 쉬움
- 이후 iOS, backend, web 같은 다른 플랫폼 폴더를 추가하기 쉬움
- Codex/OMX 자산과 안드로이드 앱 소스를 역할별로 분리 가능

## 자동화 자산 위치

- 실행 스킬: `.codex/skills/`
- 역할 프롬프트: `.codex/prompts/`
- 에이전트 메타데이터: `.codex/agents/`
- 코드 전용 규칙:
  - Android: `android/AGENTS.md`
  - Web: `web/AGENTS.md`

## `web/` 내부 주요 구조

```text
web/
├── app/
│   ├── page.tsx               # 홈 대시보드
│   ├── setup/page.tsx         # 초기 설정
│   ├── settings/page.tsx      # 설정
│   ├── timetable/page.tsx     # 시간표
│   ├── schedule/page.tsx      # 학사 일정
│   ├── meals/page.tsx         # 급식 상세
│   ├── timer/page.tsx         # 타이머
│   └── api/                   # NEIS BFF route handlers
├── components/                # 화면/카드/상태 UI 컴포넌트
├── hooks/                     # 브라우저 상태 구독 훅
├── lib/
│   ├── neis/                  # NEIS 타입/매퍼/클라이언트
│   ├── storage/               # preferences/cache/browser storage
│   └── *.ts                   # 날짜/사이트 데이터/도메인 유틸
├── package.json
└── AGENTS.md
```

## 작업 규칙

- 안드로이드 빌드/테스트 명령은 기본적으로 `android/` 안에서 실행
- 웹 빌드/테스트 명령은 기본적으로 `web/` 안에서 실행
- 문서와 자동화 자산은 가능하면 루트 기준 경로 유지
- 실행용 스킬은 `.codex/skills/` 아래에 유지

## 자주 쓰는 명령

```bash
cd android
./gradlew testDebugUnitTest lintDebug
./gradlew assembleDebug

cd ../web
npm run lint
npm run typecheck
npm run build
```
