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
