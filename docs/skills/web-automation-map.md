# Web 자동화 맵

웹 작업과 관련된 에이전트/스킬을 어디에 두고 어떻게 나눌지 정리한 문서입니다.

## 1. 정리 원칙

- **제품 코드**는 `web/` 아래에 둡니다.
- **자동화 자산**은 `.codex/` 아래에 둡니다.
- **웹 코드 전용 작업 규칙**은 `web/AGENTS.md` 에 둡니다.
- **사람이 읽는 설명 문서**는 `docs/skills/` 아래에 둡니다.
- skill 이름 규칙은 `docs/skills/skill-naming-conventions.md` 를 따릅니다.

즉:

```text
web/           -> 실제 웹 앱 코드
.codex/        -> 스킬 / 프롬프트 / 자동화 자산
docs/skills/   -> 사람이 읽는 운영 문서
```

---

## 2. 추천 skill 구조

실행 가능한 반복 작업은 `.codex/skills/<name>/SKILL.md` 로 정리합니다.

### 추천 스킬 목록

#### `web-bootstrap`
- 목적: `/web` 초기 세팅, 기본 폴더 구조, 환경 체크
- 범위: Next.js 앱 시작, 기본 디렉터리 스캐폴드, 초기 품질 게이트

#### `web-neis-bff`
- 목적: NEIS API를 웹 서버/BFF 경계로 감싸는 작업
- 범위: route handler, 환경변수 사용, 응답 매핑, 에러 표준화

#### `web-ui-builder`
- 목적: 대시보드/상세 페이지/공통 카드 UI 스캐폴드
- 범위: 레이아웃, 컴포넌트 분리, 모바일 퍼스트 화면 구성

#### `web-pwa`
- 목적: 웹앱 설치성과 오프라인/PWA 준비
- 범위: manifest, install UX, 캐시 전략, 오프라인 대응 체크리스트

---

## 3. 추천 prompt / agent 역할

역할이 강한 전문가는 `.codex/prompts/` 에 두는 것을 권장합니다.

### 추천 프롬프트 목록

#### `nextjs-app-router-expert`
- 목적: App Router 구조, 서버/클라이언트 컴포넌트 분리, route handler 설계

#### `web-frontend-architect`
- 목적: 웹 폴더 구조, 도메인 분리, 상태/데이터 흐름 설계

#### `web-accessibility-reviewer`
- 목적: 시맨틱 구조, 폼 접근성, 키보드 탐색, 모바일 가독성 점검

---

## 4. 추천 위치 요약

```text
.codex/
├── skills/
│   ├── web-bootstrap/
│   │   └── SKILL.md
│   ├── web-neis-bff/
│   │   └── SKILL.md
│   ├── web-ui-builder/
│   │   └── SKILL.md
│   └── web-pwa/
│       └── SKILL.md
└── prompts/
    ├── nextjs-app-router-expert.md
    ├── web-frontend-architect.md
    └── web-accessibility-reviewer.md
```

---

## 5. 운영 원칙

- **웹 구현 규칙**은 `web/AGENTS.md` 를 먼저 본다.
- **반복 작업 흐름**은 skill로 만든다.
- **전문 역할 지시문**은 prompt로 만든다.
- **공통 요구사항 변경**은 `docs/project_specification.md` 를 먼저 고친다.

---

## 6. 시작 순서 추천

1. `web-bootstrap`
2. `nextjs-app-router-expert`
3. `web-neis-bff`
4. `web-ui-builder`
5. `web-accessibility-reviewer`
6. `web-pwa`

이 순서로 가면 웹의 기반 구조 → 데이터 경계 → UI → 품질 → 설치성 순으로 자연스럽게 확장할 수 있습니다.
