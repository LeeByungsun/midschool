# SchoolApp



## 프로젝트 개요

학교도우미는 학생들이 자주 확인하는 정보를 한곳에서 쉽게 볼 수 있도록 돕는 서비스입니다.

핵심 기능:
- 학교 검색 및 기본 설정 저장
- 시간표 조회
- 급식 조회
- 학사 일정 조회
- 학습용 타이머

현재 저장소는 아래처럼 역할을 나눠 운영합니다.

- `android/` : Android 앱 프로젝트
- `web/` : Next.js 기반 Web 클라이언트
- `docs/` : 스펙/구조/설정 문서
- `.codex/` : 자동화/에이전트 자산

## 현재 구현 상태 요약

### Android
- 기존 기준 플랫폼
- Kotlin + XML + DataBinding + Hilt + MVVM 구조
- 앱 위젯/타이머/NEIS 연동 기반 유지

### Web
- Next.js App Router 기반 단일 웹앱
- 학교 검색(초등학교/중학교) 지원
- 시간표 / 급식 / 학사 일정 실데이터 조회
- 홈 대시보드의 가정통신문 미리보기
- 급식 상세 페이지(`/meals`)
- 공통 상태 UX(로딩/오류/빈 상태/설정 필요/재시도)
- 브라우저 캐시/복구 전략 적용
- 타이머 새로고침 복원 / 오늘 기록 / 브라우저 알림 지원

## 저장소 구조

```text
misSchoolApp/
├── android/              # Android 앱 프로젝트
├── web/                  # 학교도우미 Web 클라이언트
├── docs/                 # 프로젝트 문서
├── .codex/               # Codex/OMX 스킬 및 프롬프트
├── .github/              # GitHub 관련 자산
└── AGENTS.md             # 에이전트 작업 규칙
```

더 자세한 구조는 `docs/project-structure.md`를 참고하세요.

## 시작 가이드

### Android
Android 개발/빌드/실행 안내는 아래 문서를 참고하세요.

- `android/README.md`
- `docs/android-studio-setup.md`

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

- 멀티플랫폼 기능/정책 스펙: `docs/project_specification.md`
- 폴더 구조 문서: `docs/project-structure.md`
- Android Studio 열기 안내: `docs/android-studio-setup.md`
- 웹 자동화/스킬 정리: `docs/skills/web-automation-map.md`
- skill 이름 규칙: `docs/skills/skill-naming-conventions.md`
- 웹 전용 안내: `web/README.md`

## 웹에서 현재 제공하는 주요 화면

- `/` : 홈 대시보드(시간표 / 급식 / 일정 / 가정통신문 / 타이머 요약)
- `/setup` : 초기 설정
- `/settings` : 학교/학년/반 설정 변경
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

## 자동화 자산 위치

- 실행 스킬: `.codex/skills/<skill-name>/SKILL.md`
- 설명 문서: `docs/skills/<skill-name>.md`
- GitHub 워크플로우/메타 자산: `.github/`

이렇게 두면 플랫폼 코드와 자동화/문서 자산이 섞이지 않아 유지보수가 쉬워집니다.
