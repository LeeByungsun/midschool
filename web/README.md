# Web workspace

이 폴더는 `MisSchoolApp`의 웹 클라이언트입니다.

현재 기준
- 프레임워크: **Next.js App Router**
- 언어: **TypeScript**
- 스타일: **Tailwind CSS v4**
- 기본 방향: 모바일 퍼스트 대시보드

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

로컬 개발에서는 `web/.env.local` 이 없어도 `android/local.properties` 의
`NEIS_API_KEY` 를 서버 쪽 fallback으로 읽을 수 있습니다.

## 품질 체크

```bash
cd web
npm run lint
npm run typecheck
npm run build
```

## 현재 포함된 기본 골격

- `/` : 대시보드
- `/timetable` : 시간표 스켈레톤
- `/schedule` : 일정 스켈레톤
- `/timer` : 타이머 스켈레톤
- `/settings` : 설정 스켈레톤

## 구현 원칙

- 공통 도메인 요구사항은 `docs/project_specification.md`의 **공통 요구사항**을 기준으로 구현
- 웹 전용 UX/기술 선택은 같은 문서의 **Web 전용 명세**를 기준으로 구체화
- Android 구현 세부사항은 그대로 복붙하지 않고 웹 플랫폼에 맞게 번역
