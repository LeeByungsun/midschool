# 학교도우미

학교도우미는 학생들이 자주 확인하는 정보를 한곳에서 쉽게 볼 수 있도록 돕는 멀티플랫폼 프로젝트입니다.

현재 푸시 기준 저장소는 Android 앱과 Web 클라이언트를 함께 관리합니다.

## 핵심 기능

- 학생 기본 설정 저장
- 시간표 조회
- 급식 조회
- 학사 일정 조회
- 학습용 타이머

## 현재 구현 상태 요약

### Android

- Kotlin + XML + DataBinding + Hilt + MVVM 구조
- NEIS 기반 시간표 / 급식 / 학사 일정 조회
- 타이머 및 앱 위젯 기능 유지
- `android/README.md`와 `docs/android-studio-setup.md` 기준으로 개발

### Web

- Next.js App Router 기반 단일 웹앱
- 학교 검색(초등학교 / 중학교)
- 시간표 / 급식 / 학사 일정 실데이터 조회
- 홈 대시보드의 가정통신문 미리보기
- 급식 상세 페이지(`/meals`)
- 공통 상태 UX(로딩 / 오류 / 빈 상태 / 설정 필요 / 재시도)
- 브라우저 캐시 / 복구 전략
- 타이머 새로고침 복원 / 오늘 기록 / 브라우저 알림

## 현재 푸시 기준 저장소 구조

```text
misSchoolApp/
├── .gitignore
├── README.md
├── android/
│   ├── AGENTS.md
│   ├── README.md
│   ├── app/
│   │   ├── build.gradle.kts
│   │   ├── proguard-rules.pro
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/bsbarron/midschoolapp/
│   │       │   │   ├── data/
│   │       │   │   ├── di/
│   │       │   │   ├── timer/
│   │       │   │   ├── ui/
│   │       │   │   ├── util/
│   │       │   │   └── widget/
│   │       │   └── res/
│   │       ├── test/
│   │       └── androidTest/
│   ├── gradle/
│   ├── build.gradle.kts
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   └── settings.gradle.kts
├── docs/
│   ├── android-studio-setup.md
│   ├── project-structure.md
│   └── project_specification.md
└── web/
    ├── AGENTS.md
    ├── README.md
    ├── app/
    │   ├── api/
    │   ├── meals/
    │   ├── schedule/
    │   ├── settings/
    │   ├── setup/
    │   ├── timer/
    │   ├── timetable/
    │   ├── globals.css
    │   ├── layout.tsx
    │   └── page.tsx
    ├── components/
    ├── hooks/
    ├── lib/
    │   ├── neis/
    │   ├── notices/
    │   └── storage/
    ├── public/
    ├── scripts/
    ├── eslint.config.mjs
    ├── next.config.ts
    ├── package.json
    ├── postcss.config.mjs
    └── tsconfig.json
```

더 자세한 구조 설명은 `docs/project-structure.md`를 참고하세요.

## 시작 가이드

### Android

Android 개발 / 빌드 / 실행 안내:

- `android/README.md`
- `docs/android-studio-setup.md`

대표 명령:

```bash
cd android
./gradlew testDebugUnitTest lintDebug
./gradlew assembleDebug
```

### Web

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
npm test
npm run build
```

## 문서 안내

- 멀티플랫폼 기능 / 정책 스펙: `docs/project_specification.md`
- 현재 저장소 구조 문서: `docs/project-structure.md`
- Android Studio 열기 안내: `docs/android-studio-setup.md`
- 웹 전용 안내: `web/README.md`
- Android 전용 안내: `android/README.md`

## 웹에서 현재 제공하는 주요 화면

- `/` : 홈 대시보드(시간표 / 급식 / 일정 / 가정통신문 / 타이머 요약)
- `/setup` : 초기 설정
- `/settings` : 학교 / 학년 / 반 설정 변경
- `/timetable` : 날짜별 시간표 조회
- `/schedule` : 월간 학사 일정 조회
- `/meals` : 날짜별 급식 상세 조회
- `/timer` : 프리셋 / 복원 / 오늘 기록 / 알림 설정을 포함한 타이머

## 웹 데이터 정책 요약

- 외부 NEIS API는 브라우저에서 직접 호출하지 않고 `app/api/**/route.ts` BFF를 통해 호출
- 학교 검색 시 학교 코드 / 교육청 코드 / 학교 종류를 저장
- 급식 / 시간표 / 일정 조회는 브라우저 캐시를 사용
- 최신 요청 실패 시 마지막 성공 데이터를 fallback으로 재사용
- 가정통신문은 학교 홈페이지를 서버에서 해석해 최근 항목을 수집
