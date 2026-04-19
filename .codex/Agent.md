# Codex Automation Asset Guide

이 문서는 저장소의 **자동화 자산 배치 기준**을 설명합니다.  
제품 코드별 상세 작업 규칙은 각 영역의 `AGENTS.md`를 우선 봅니다.

## 1. 어디를 먼저 봐야 하나

- 저장소 전체 공통 규칙: `AGENTS.md`
- Android 코드 전용 규칙: `android/AGENTS.md`
- Web 코드 전용 규칙: `web/AGENTS.md`

즉, 코드 작업 시에는 `.codex/Agent.md`보다 **스코프에 맞는 `AGENTS.md`**가 우선입니다.

## 2. `.codex/`의 역할

`.codex/`는 제품 코드 디렉터리가 아니라 **자동화 자산 디렉터리**입니다.

### 포함되는 것
- `.codex/skills/`
  - 반복 작업 흐름
  - 초기화, 점검, 품질 게이트, 변환 절차
- `.codex/prompts/`
  - 역할형 전문가 프롬프트
  - 설계, 구현, 리뷰, 검증 역할 지시문
- `.codex/agents/`
  - 에이전트 정의/메타데이터

### 포함하지 않는 것
- 실제 Android 앱 코드
- 실제 Web 앱 코드
- 제품 기능 구현 규칙 자체

## 3. 권장 배치 규칙

### 제품 코드
- Android 앱 코드: `android/`
- Web 앱 코드: `web/`

### 자동화 자산
- 실행 스킬: `.codex/skills/<skill-name>/SKILL.md`
- 역할 프롬프트: `.codex/prompts/<prompt-name>.md`
- 에이전트 정의: `.codex/agents/`

### 사람이 읽는 운영 문서
- 구조/기획/운영 문서: `docs/`
- 자동화 정리 문서 예시: `docs/skills/`

## 4. 현재 운영 기준

- Android 전용 규칙은 `android/AGENTS.md`에서 관리합니다.
- Web 전용 규칙은 `web/AGENTS.md`에서 관리합니다.
- Android/Web 실행 스킬의 운영 위치는 `.codex/skills/` 입니다.
- 웹 자동화 자산 구성 예시는 `docs/skills/web-automation-map.md`에 정리합니다.
- 새 자동화 흐름을 추가할 때는 코드 규칙 문서와 자동화 문서를 혼동하지 않도록 주의합니다.
- 기존 `.github/skills/` 아래 자산은 **legacy 위치**로 보고, 유지가 필요하면 `.codex/skills/` 기준으로 정리합니다.

## 5. 문서 갱신 규칙

- 제품 코드 규칙이 바뀌면 해당 영역의 `AGENTS.md`를 갱신합니다.
- 자동화 자산 구조가 바뀌면 `.codex/Agent.md`와 관련 `docs/skills/*.md`를 갱신합니다.

이 문서는 자동화 자산의 **위치와 역할 구분**을 설명하는 문서로 유지합니다.
