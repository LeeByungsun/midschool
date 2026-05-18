# 학교도우미 Web

이 폴더는 `MisSchoolApp` 저장소 안의 **학교도우미 웹 클라이언트**입니다.

Web 클라이언트는 학교 생활 정보를 브라우저에서 빠르게 확인할 수 있도록 만든 Next.js 앱입니다.

## 현재 구현 상태

- Next.js App Router 기반 단일 웹앱
- 학교 검색(초등학교 / 중학교)
- 시간표 / 급식 / 학사 일정 실데이터 조회
- 홈 대시보드 가정통신문 미리보기
- 급식 상세 페이지(`/meals`)
- 공통 상태 UX(로딩/오류/빈 상태/설정 필요/재시도)
- 브라우저 캐시/복구 전략
- 타이머 새로고침 복원 / 오늘 기록 / 브라우저 알림

## 기술 기준

- 프레임워크: **Next.js App Router**
- 언어: **TypeScript**
- 스타일: **Tailwind CSS v4**
- 기본 방향: 모바일 퍼스트 대시보드
- 데이터 경계: **Next.js BFF (`app/api/**/route.ts`)**

## 시작하기

```bash
cd web
npm install
npm run dev
```

브라우저에서 `http://localhost:3000` 을 열면 됩니다.

## 품질 체크

```bash
cd web
npm run lint
npm run typecheck
npm test
npm run build
```

## 환경변수

실제 NEIS 연동을 위해서는 서버 쪽 환경변수가 필요합니다.

- `NEIS_API_KEY` (필수)
- `NEIS_BASE_URL` (선택, 기본값: `https://open.neis.go.kr/`)
- `NEIS_OFFICE_CODE` (선택, 기본값: `J10`)
- `NEIS_SCHOOL_CODE` (선택, 기본값: `7679399`)

`NEIS_API_KEY` 는 클라이언트 코드가 아니라 `app/api/**/route.ts` 경계에서만 사용합니다.

시작할 때는 `web/.env.example` 를 복사해 `.env.local` 로 두고 값을 채우면 됩니다.

## 현재 제공하는 주요 화면

- `/` : 홈 대시보드(시간표 / 급식 / 일정 / 가정통신문 / 타이머 요약)
- `/setup` : 초기 설정
- `/settings` : 학교/학년/반 설정 변경
- `/timetable` : 날짜별 시간표 조회
- `/schedule` : 월간 학사 일정 조회
- `/meals` : 날짜별 급식 상세 조회
- `/timer` : 프리셋 / 복원 / 오늘 기록 / 알림 설정을 포함한 타이머

## 현재 반영된 주요 기능

### 학교 검색 / 설정
- 초등학교 / 중학교 검색 지원
- Enter 키로 검색 가능
- 교육청 코드 / 학교 코드 / 학교 종류 저장

### 시간표
- 날짜 이동형 일간 조회
- 학교 종류에 따라 초등학교/중학교 시간표 엔드포인트 분기

### 급식
- 오늘 급식 요약
- 날짜 이동형 상세 조회
- 알레르기 / 칼로리 / 영양 / 원산지 정보 표시

### 일정
- 월간 일정 조회

### 가정통신문
- 홈 대시보드에서 최근 가정통신문 미리보기
- 학교 홈페이지 URL 패턴 기반 서버 수집
- 일부 교육청/학교 홈페이지는 미지원 또는 제한적 지원

### 타이머
- 집중 / 휴식 / 딥포커스 프리셋
- 새로고침 후 진행 상태 복원
- 오늘 완료 기록 집계
- 브라우저 Notification API 기반 완료 알림
- 홈 요약 카드(`components/home-timer-card.tsx`)와 상세 패널(`components/timer-panel.tsx`)이 같은 저장 상태/구독 훅을 공유

### 공통 UX / 데이터 복구
- 로딩 / 오류 / 빈 상태 / 설정 필요 / 재시도 패턴 공통화
- 급식 12시간 / 시간표 24시간 / 일정 12시간 캐시
- 최신 요청 실패 시 마지막 성공 데이터 fallback

## 주요 폴더

- `app/` : 페이지와 BFF route handlers
- `components/` : 화면 단위/재사용 UI 컴포넌트
- `hooks/` : 브라우저 상태 구독 훅
- `lib/neis/` : NEIS 타입/매퍼/클라이언트
- `lib/notices/` : 가정통신문 수집/파싱 로직
- `lib/storage/` : preferences/cache/browser storage 래퍼
- `lib/timer.ts` + `hooks/use-study-timer.ts` : 홈/타이머 화면이 공유하는 타이머 계산/구독 계층

## 관련 문서

- 프로젝트 전체 개요: `../README.md`
- 전체 스펙 문서: `../docs/project_specification.md`
- 프로젝트 구조 문서: `../docs/project-structure.md`
- 웹 작업 규칙: `AGENTS.md`
- 웹 자동화/스킬 정리: `../docs/skills/web-automation-map.md`
