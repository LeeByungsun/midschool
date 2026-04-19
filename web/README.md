# 학교도우미 Web workspace

이 폴더는 `MisSchoolApp` 저장소 안의 **학교도우미 웹 클라이언트**입니다.

현재 기준
- 프레임워크: **Next.js App Router**
- 언어: **TypeScript**
- 스타일: **Tailwind CSS v4**
- 기본 방향: 모바일 퍼스트 대시보드
- 데이터 경계: **Next.js BFF (`app/api/**/route.ts`)**

우선 참고 문서
- `docs/project_specification.md`
- `web/AGENTS.md`
- `docs/skills/web-automation-map.md`

## 시작하기

```bash
cd web
npm run dev
```

브라우저에서 `http://localhost:3000` 을 열면 됩니다.

## 환경변수

실제 NEIS 연동을 위해서는 서버 쪽 환경변수가 필요합니다.

- `NEIS_API_KEY` (필수)
- `NEIS_BASE_URL` (선택, 기본값: `https://open.neis.go.kr/`)
- `NEIS_OFFICE_CODE` (선택, 기본값: `J10`)
- `NEIS_SCHOOL_CODE` (선택, 기본값: `7679399`)

`NEIS_API_KEY` 는 클라이언트 코드가 아니라 `app/api/**/route.ts` 경계에서만 사용합니다.

시작할 때는 `web/.env.example` 를 복사해 `.env.local` 로 두고 값을 채우면 됩니다.

## 품질 체크

```bash
cd web
npm run lint
npm run typecheck
npm run build
```

## 현재 포함된 기본 골격

- `/` : 대시보드
- `/setup` : 초기 설정
- `/meals` : 날짜별 급식 상세 조회
- `/timetable` : 실제 시간표 조회
- `/schedule` : 실제 월간 일정 조회
- `/timer` : 타이머 기본 UI
- `/settings` : 저장된 학교/학년/반 수정

## 현재 반영된 주요 기능

- 학교 검색
  - 초등학교 / 중학교 검색 지원
  - Enter 키로 검색 가능
  - 교육청 코드 / 학교 코드 / 학교 종류 저장
- 시간표
  - 날짜 이동형 일간 조회
  - 학교 종류에 따라 초등학교/중학교 시간표 엔드포인트 분기
- 급식
  - 오늘 급식 요약
  - 날짜 이동형 상세 조회
  - 알레르기 / 칼로리 / 영양 / 원산지 정보 표시
- 일정
  - 월간 일정 조회
- 공통 상태 UX
  - 로딩 / 오류 / 빈 상태 / 설정 필요 / 재시도 패턴 공통화
- 캐시 / 복구 전략
  - 급식 12시간
  - 시간표 24시간
  - 일정 12시간
  - 최신 요청 실패 시 마지막 성공 데이터 fallback

## 주요 폴더

- `app/` : 페이지와 BFF route handlers
- `components/` : 화면 단위/재사용 UI 컴포넌트
- `hooks/` : 브라우저 상태 구독 훅
- `lib/neis/` : NEIS 타입/매퍼/클라이언트
- `lib/storage/` : preferences/cache/browser storage 래퍼

## 구현 원칙

- 공통 도메인 요구사항은 `docs/project_specification.md`의 **공통 요구사항**을 기준으로 구현
- 웹 전용 UX/기술 선택은 같은 문서의 **Web 전용 명세**를 기준으로 구체화
- Android 구현 세부사항은 그대로 복붙하지 않고 웹 플랫폼에 맞게 번역
