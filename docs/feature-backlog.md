# 학교도우미 필요 기능 리스트

작성일: 2026-06-08

이 문서는 현재 저장소의 구현 상태와 남은 미비점을 기준으로 다음에 필요한 기능을 정리한 백로그입니다.
범위는 Web, Android, iOS, 공통 데이터/품질 기능입니다.

## 정리 기준

- 사용자 가치가 바로 큰 기능을 먼저 둡니다.
- 이미 구현된 핵심 조회 기능은 반복하지 않고, 안정화·운영·확장 기능 위주로 정리합니다.
- 구현 완료와 릴리스 준비는 분리해서 봅니다.

## P0 — 바로 필요한 기능

### 1. Android production notices 수동 검증

목표:
- Android 홈 가정통신문 preview가 실제 환경에서 안정적으로 동작하는지 닫습니다.

필요 기능:
- 실기기 검증 체크리스트
- 에뮬레이터 검증 체크리스트
- 학교 선택 저장/복원 수동 확인
- production `WEB_BASE_URL` 응답 확인
- 외부 링크 열기 확인
- 미지원 학교 fallback 확인

완료 기준:
- 실기기와 에뮬레이터에서 학교 선택 저장/복원과 production notices 흐름이 검증 기록으로 남습니다.

### 2. 공통 fallback UX 정렬

목표:
- Web, Android, iOS에서 오류/미지원/빈 상태 문구를 같은 의미 체계로 맞춥니다.

필요 기능:
- 공통 상태 코드 정의
- 공통 사용자 문구 정의
- unsupported school 문구 통일
- 네트워크 실패 문구 통일
- 캐시 사용/오래된 데이터 표시 문구 통일

완료 기준:
- 같은 상황에서 세 플랫폼이 같은 의미의 안내를 보여줍니다.

## P1 — 안정화 기능

### 3. 접근성 점검과 Android warning cleanup

목표:
- Web과 Android의 남은 접근성 리스크와 lint warning을 줄입니다.

필요 기능:
- Web keyboard navigation 점검
- Web focus 상태 점검
- Web timer control accessibility 점검
- Android widget 장식 이미지 접근성 정리
- Android touch target 점검
- Android `UseKtx` warning 정리 여부 결정
- 주요 화면 screen-reader smoke 기록

완료 기준:
- 주요 화면별 접근성 체크리스트가 완료됩니다.
- 자동 lint warning 중 접근성 항목이 줄어듭니다.
- 남기는 warning은 이유와 보류 기준이 문서화됩니다.

### 4. Web 반응형/PWA/알림/경고 cleanup

목표:
- 브라우저 앱으로서 설치성과 사용성을 높이고, 현재 검증에서 보이는 비실패 경고를 줄입니다.

필요 기능:
- tablet/desktop 레이아웃 보강
- 긴 일정/긴 급식 텍스트 대응
- PWA manifest
- app icon
- offline fallback page
- service worker 도입 여부 결정
- notification permission denied/unsupported fallback
- Node ESM warning 정리
  - `package.json` module type 명시 또는 test/build 설정 조정

완료 기준:
- 모바일, tablet, desktop viewport에서 핵심 화면이 깨지지 않습니다.
- PWA 최소 설치 요건 또는 명시적 보류 결정이 문서화됩니다.
- test 실행 중 반복되는 Node ESM 경고가 제거되거나 의도된 보류로 기록됩니다.

## P2 — 릴리스/품질 기능

### 5. iOS 문서 정합성 및 릴리스 체크리스트

목표:
- iOS 구현 완료 상태와 배포 준비 상태를 분리합니다.

필요 기능:
- 오래된 “수동 검증 필요” 문서 갱신
- local-only evidence 설명
- TestFlight/App Store checklist
- signing/archive 검증 절차
- App Group/profile 확인 절차

완료 기준:
- iOS readiness complete와 release readiness가 문서에서 혼동되지 않습니다.

### 6. 공통 도메인 모델 정렬

목표:
- 플랫폼별로 흩어진 상태값과 데이터 의미를 맞춥니다.

필요 기능:
- School identity 표준화
- Timetable item 표준화
- Meal detail 표준화
- Schedule event 표준화
- Notice status/error code 표준화
- Timer state 표준화
- cache metadata 표준화

완료 기준:
- Web BFF, Android, iOS가 같은 상태 의미와 필드 이름을 공유합니다.

## P3 — 제품 확장 기능

### 7. 사용자 맞춤 홈

필요 기능:
- 홈 카드 순서 변경
- 자주 보는 기능 고정
- 타이머 quick preset
- 오늘 할 일/학습 기록 요약

### 8. 알림 확장

필요 기능:
- 급식/일정 알림
- 시간표 시작 전 알림
- 가정통신문 새 글 알림
- 플랫폼별 알림 권한 안내

### 9. 학습 타이머 기록 고도화

필요 기능:
- 일/주/월 집중 시간 통계
- 프리셋별 통계
- 목표 시간 설정
- 연속 학습 streak

### 10. 학교 데이터 coverage 확장

필요 기능:
- 고등학교 시간표 API 추가 검토
- 교육청별 홈페이지 profile 확대
- SSO 학교 대응 가능성 조사
- 학교 홈페이지 selector/profile registry

## 추천 실행 순서

1. Android production notices와 학교 선택 저장/복원 수동 검증
2. 공통 fallback UX 정렬
3. Web 접근성/반응형/PWA/알림 안정화
4. Android lint warning cleanup
5. iOS 문서 정합성 및 release checklist
6. 공통 도메인 모델 정렬
7. 학교 데이터 coverage 확장
8. 사용자 맞춤 홈
9. 알림 확장
10. 학습 타이머 기록 고도화

## 당장 첫 스프린트 제안

### Sprint 1 목표

앱 사용자에게 직접 영향을 주는 남은 리스크를 먼저 닫습니다.

포함 기능:
- Android 실기기/에뮬레이터 school selection 저장/복원 검증
- Android production notices 검증
- 미지원 학교 fallback 문구 정리
- Web timer/settings 접근성 smoke
- Web tablet/desktop 레이아웃 smoke

성공 기준:
- Android에서 학교 선택과 notices 흐름이 실제 환경 검증 기록으로 남습니다.
- Web/Android/iOS의 미지원/오류/fallback 문구 방향이 같은 의미 체계로 정리됩니다.
- 사용자-facing 기능의 다음 구현 우선순위가 명확해집니다.
