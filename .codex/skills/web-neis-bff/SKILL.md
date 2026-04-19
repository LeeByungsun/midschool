---
name: web-neis-bff
description: Design and implement the web-side BFF boundary for NEIS API access using server-side routes, response mapping, and safe configuration handling.
---

# Web NEIS BFF

웹에서 NEIS API를 직접 두드리는 대신, 서버 경계에서 감싸는 작업을 위한 스킬입니다.

## 목적
- 외부 API 호출을 웹 서버 경계로 모은다
- 환경변수와 비밀값을 클라이언트에 노출하지 않는다
- 응답 매핑/에러 처리를 재사용 가능한 형태로 만든다

## 권장 범위
- `app/api/**/route.ts`
- `lib/neis/client.ts`
- `lib/neis/mapper.ts`
- `lib/neis/types.ts`

## 원칙
- 브라우저 직접 호출보다 route handler 우선
- UI에 응답 해석 로직을 흩뿌리지 않음
- 에러 상태는 사용자 메시지와 내부 로그 관점을 분리

Task: {{ARGUMENTS}}
