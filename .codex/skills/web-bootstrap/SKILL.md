---
name: web-bootstrap
description: Initialize or restructure the /web workspace for a Next.js-based web client with a clean folder layout, baseline quality gates, and documentation alignment.
---

# Web Bootstrap

`web/` 작업을 시작할 때 사용하는 초기 스캐폴드/정렬용 스킬입니다.

## 목적

- `/web` 아래에 웹 작업 기반을 만든다
- Next.js App Router 기준의 기본 구조를 준비한다
- 문서와 실제 폴더 구조가 어긋나지 않도록 맞춘다
- 과한 추상화 없이 빠르게 “개발 가능한 상태”를 만든다

## 사용 시점

다음 상황에서 사용합니다.

- `/web` 폴더를 처음 초기화할 때
- Next.js 앱을 새로 만들 때
- 웹 폴더 구조가 커져서 재정렬이 필요할 때
- 공통 문서와 실제 웹 구조를 맞출 때

## 기본 원칙

- 기본 프레임워크는 **Next.js + TypeScript + App Router**
- 데이터 연동은 브라우저 직결보다 서버 경계를 우선 검토
- 모바일 퍼스트
- 최소한의 구조로 시작
- 기능을 가로지르는 거대한 추상화는 초기에 만들지 않음

## 권장 결과 구조

```text
web/
├── app/
│   ├── layout.tsx
│   ├── page.tsx
│   ├── timetable/
│   ├── schedule/
│   ├── settings/
│   ├── timer/
│   └── api/
├── components/
├── lib/
│   ├── neis/
│   ├── storage/
│   └── utils/
├── hooks/
├── public/
└── README.md
```

## 작업 절차

1. `/web` 현재 상태 확인
   - 이미 프레임워크가 있는지 확인
   - `package.json`, `app/`, `src/` 여부 확인

2. 프레임워크 결정 확인
   - 기본은 Next.js
   - 사용자가 별도 프레임워크를 명시하지 않았다면 Next.js 기준으로 진행

3. 기본 폴더 구조 준비
   - `app/`
   - `components/`
   - `lib/`
   - `hooks/`
   - `public/`

4. 최소 진입 구조 준비
   - 홈 페이지
   - 공통 레이아웃
   - 초기 README

5. 문서 정합성 확인
   - `docs/project_specification.md`
   - `docs/project-structure.md`
   - 필요 시 `README.md`

6. 품질 게이트 확인
   - 설치/실행 가능 여부
   - lint / typecheck / build 가능한지 점검

## 완료 조건

- `/web` 이 웹 프로젝트 시작점으로 기능함
- 팀이 어디서부터 개발해야 하는지 명확함
- 문서가 실제 구조와 맞음
- 이후 `web-neis-bff`, `web-ui-builder` 같은 후속 스킬이 자연스럽게 이어질 수 있음

## 출력 형식

```text
WEB BOOTSTRAP REPORT
====================

Framework:
- [chosen framework]

Created/Updated:
- [path] - [purpose]

Structure Decisions:
- [decision]

Verification:
- [command] - PASS/FAIL

Next Recommended Skills:
- web-neis-bff
- web-ui-builder
- web-pwa
```

Task: {{ARGUMENTS}}
