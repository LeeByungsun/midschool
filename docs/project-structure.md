# 프로젝트 구조

이 저장소는 **학교도우미** 서비스를 위한 루트 작업공간이며, Android 앱과 Web 클라이언트를 분리한 구조를 사용합니다.

현재 문서는 **푸시된 추적 파일 기준** 저장소 구조를 설명합니다.

## 최상위 구조

```text
misSchoolApp/
├── .gitignore            # 루트 ignore 규칙
├── README.md             # 저장소 시작 안내
├── android/              # Android Gradle 프로젝트
├── docs/                 # 기획/구조/설정 문서
└── web/                  # 학교도우미 Next.js Web 클라이언트
```

## `android/` 내부 구조

```text
android/
├── AGENTS.md             # Android 작업 규칙
├── README.md             # Android 작업 안내
├── app/                  # Android application module
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
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
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
```

## 왜 `android/` 아래로 분리했나요?

이 구조는 다음 목적에 유리합니다.

- 저장소 루트를 문서/자동화/도구 자산과 함께 운영하기 쉬움
- 이후 iOS, backend, web 같은 다른 플랫폼 폴더를 추가하기 쉬움
- Codex/OMX 자산과 안드로이드 앱 소스를 역할별로 분리 가능

## `docs/` 내부 구조

```text
docs/
├── android-studio-setup.md   # Android Studio 열기와 로컬 실행 안내
├── project-structure.md      # 현재 저장소 구조 문서
└── project_specification.md  # 멀티플랫폼 기능/정책 스펙
```

## `web/` 내부 주요 구조

```text
web/
├── AGENTS.md                 # Web 작업 규칙
├── README.md                 # Web 작업 안내
├── app/
│   ├── page.tsx               # 홈 대시보드
│   ├── setup/page.tsx         # 초기 설정
│   ├── settings/page.tsx      # 설정
│   ├── timetable/page.tsx     # 시간표
│   ├── schedule/page.tsx      # 학사 일정
│   ├── meals/page.tsx         # 급식 상세
│   ├── timer/page.tsx         # 타이머
│   └── api/                   # NEIS/가정통신문 BFF route handlers
├── components/                # 화면/카드/상태 UI 컴포넌트
│   ├── home-dashboard.tsx     # 홈 실데이터 대시보드 + 가정통신문 카드 조합
│   ├── home-timer-card.tsx    # 홈 타이머 요약/빠른 제어 카드
│   ├── meal-browser.tsx       # 날짜별 급식 상세 조회
│   ├── timer-panel.tsx        # 타이머 상세 제어 / 알림 / 오늘 기록
│   └── data-state.tsx         # 공통 로딩/오류/빈 상태/설정 필요 UI
├── hooks/                     # 브라우저 상태 구독 훅
│   └── use-study-timer.ts     # 홈/타이머 페이지가 공유하는 타이머 상태 구독
├── lib/
│   ├── neis/                  # NEIS 타입/매퍼/클라이언트
│   ├── notices/               # 가정통신문 수집/파싱/provider 판별
│   ├── storage/               # preferences/cache/browser storage
│   │   ├── preferences.ts     # 학생 설정 저장
│   │   ├── cache.ts           # 급식/시간표/일정 캐시 및 복구 전략
│   │   ├── timer.ts           # 타이머 스냅샷/기록/알림 설정 저장
│   │   └── browser-storage.ts # 브라우저 저장소 래퍼
│   ├── timer.ts               # 타이머 상태 계산/복원용 순수 유틸
│   └── *.ts                   # 날짜/사이트 데이터/도메인 유틸/API 호출 래퍼
├── public/                    # 정적 리소스
├── scripts/                   # node:test 기반 경량 회귀 스크립트
│   ├── test-notices.mjs       # 가정통신문 provider/URL 처리 검증
│   ├── test-notice-errors.mjs # 가정통신문 복구 오류 규칙 검증
│   └── test-timer.mjs         # 타이머 도메인/저장 규칙 검증
├── .env.example               # Web 로컬 환경변수 예시
├── .gitignore
├── eslint.config.mjs
├── next.config.ts
├── package.json
├── package-lock.json
├── postcss.config.mjs
└── tsconfig.json
```

## 작업 규칙

- 안드로이드 빌드/테스트 명령은 기본적으로 `android/` 안에서 실행
- 웹 빌드/테스트 명령은 기본적으로 `web/` 안에서 실행
- 문서는 `docs/`와 각 플랫폼 `README.md`를 기준으로 관리
- 코드 전용 규칙 문서는 각 플랫폼의 `AGENTS.md`를 기준으로 관리
- 사용자/서비스 표기는 문서에서 `학교도우미`를 우선 사용하고, 저장소명 `MisSchoolApp`은 저장소 식별 문맥에서만 사용

## 자주 쓰는 명령

```bash
cd android
./gradlew testDebugUnitTest lintDebug
./gradlew assembleDebug

cd ../web
npm run lint
npm run typecheck
npm test
npm run build
```
