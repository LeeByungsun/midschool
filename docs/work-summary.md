ㅇ이ㄹ단작업 요약 마스터

이 문서는 저장소 안의 이전 작업 기록을 한곳에서 빠르게 보기 위한 요약본입니다.

- 생성일: 2026-05-22
- 기준 소스: `.omx/context/`, `.omx/reports/team-commit-hygiene/`, `docs/`
- 업데이트 원칙: **작업이 완료될 때마다 이 파일을 갱신**합니다.

## 한눈에 보기

- Web
  - 홈 타이머 영역 분리 및 렌더링 범위 정리
  - 타이머 카드 중복 파일 제거 및 문서 정합성 정리
  - 타이머 관련 최종 리뷰와 커밋/푸시 준비 정리
- Android
  - 학교선택 기능 추가 관련 구현 기록과 검토 메모 정리
  - 설정 변경 즉시 반영 점검 및 미설정 UX 보강 기록
  - UI 목업 제안 작업 기록
  - 급식 주간 상세 화면 구현 기록
  - 학사일정 캐시 + fallback 구현/테스트 기록

## 작업 타임라인

### 2026-05-18 · Web

#### 1) 타이머 영역 분리 및 검토
- 목표
  - `HomeDashboard` 전체가 타이머 때문에 1초마다 리렌더링되는 범위를 줄이는 것
- 요약
  - 홈 대시보드 안의 타이머 영역을 분리하는 방향으로 정리되었습니다.
  - 이후 기록 기준으로 `HomeDashboard`는 `HomeTimerCard`를 사용합니다.
- 상태
  - 완료 기록 존재
- 근거 문서
  - `.omx/context/timer-ui-split-review-20260518T031846Z.md`
  - `.omx/reports/team-commit-hygiene/team-task-254c26b6.md`

#### 2) 타이머 카드 중복 파일 정리
- 목표
  - 홈 타이머 카드 중복 파일을 없애고 실제 사용 컴포넌트 이름과 문서를 맞추는 것
- 요약
  - `home-timer-card.tsx` 기준으로 정리하는 흐름이 남아 있습니다.
  - `home-study-timer-card.tsx` 중복 제거 및 문서 정합성 점검이 작업 범위였습니다.
- 상태
  - 완료 흐름으로 기록됨
- 근거 문서
  - `.omx/context/web-timer-card-dedup-20260518T034830Z.md`
  - `.omx/context/web-timer-final-review-commit-push-20260518T035957Z.md`

#### 3) web 타이머 정리 최종 리뷰
- 목표
  - 누락, 중복, 문서 불일치를 다시 확인하고 커밋/푸시 준비 상태를 검토하는 것
- 요약
  - 타이머 분리와 중복 파일 제거 이후, test/lint/typecheck/build 기준으로 최종 점검하는 단계가 기록되어 있습니다.
  - 실제 푸시는 별도 요청 시 진행하는 조건이었습니다.
- 상태
  - 리뷰/정리 완료, 푸시 여부는 별도
- 근거 문서
  - `.omx/context/web-timer-final-review-commit-push-20260518T035957Z.md`
  - `.omx/reports/team-commit-hygiene/web.md`

### 2026-05-19 · Android / UX

#### 4) Android 학교선택 기능 추가
- 목표
  - Setup/Settings에서 학교 검색·선택을 지원하고, 선택 학교 기준으로 조회 흐름이 동작하게 하는 것
- 요약
  - 고등학교를 포함한 학교선택 확장 작업 컨텍스트가 남아 있습니다.
  - 별도 검토 메모에는 구현 전 기준의 위험과 계약이 정리되어 있습니다.
  - 팀 완료 기록에는 학교 검색·선택 UI와 `officeCode` / `schoolCode` / `schoolKind` 반영 작업이 포함된 것으로 남아 있습니다.
- 상태
  - 완료 기록 존재
- 주의
  - `docs/android-school-selection-review.md`는 검토 메모 성격이라 당시 기준의 위험/계약을 설명합니다. 이후 구현 기록과 시점 차이가 있을 수 있습니다.
- 근거 문서
  - `.omx/context/android-high-school-selection-20260519T042051Z.md`
  - `.omx/reports/team-commit-hygiene/android-setup-setting-1a30c5f1.md`
  - `docs/android-school-selection-review.md`

#### 5) 설정 변경 즉시 반영 점검 / 학교 미설정 UX 보강
- 목표
  - Setup/Settings에서 바꾼 학교 설정이 홈·시간표·급식 조회에 즉시 반영되는지 점검하고, 미설정 상태 안내를 강화하는 것
- 요약
  - 설정 반영 타이밍과 Setup 유도 UX를 보강하는 작업 기록이 남아 있습니다.
- 상태
  - 완료 기록 존재
- 근거 문서
  - `.omx/reports/team-commit-hygiene/1-setup-settings-2-ux-1a30c5f1.md`
  - `.omx/reports/team-commit-hygiene/1-setup-settings-2-ux-setup.md`

#### 6) 앱 UI 목업 제안
- 목표
  - 코드 수정 없이 앱 UI를 더 보기 좋게 바꾸기 위한 목업과 디자인 방향을 만드는 것
- 요약
  - 구현 없이 목업/디자인 방향만 정리하는 작업이었습니다.
- 상태
  - 완료 기록 존재
- 근거 문서
  - `.omx/context/app-ui-mockup-20260519T071248Z.md`
  - `.omx/reports/team-commit-hygiene/ui.md`

### 2026-05-20 · Android 기능/데이터

#### 7) 급식 주간 상세 화면 구현
- 목표
  - 홈 또는 급식 진입 지점 클릭 시 주간 급식 상세 화면으로 들어가고, 일주일치 식단을 표시하는 것
- 요약
  - Android 앱에서 급식 요약 클릭 후 상세 화면으로 이동하는 작업 기록이 남아 있습니다.
  - 상세 화면은 일주일치 식단 표시를 목표로 했습니다.
- 상태
  - 완료 기록 존재
- 근거 문서
  - `.omx/context/android-meals-weekly-detail-20260520T005347Z.md`
  - `.omx/reports/team-commit-hygiene/team-task.md`

#### 8) 학사일정 캐시 추가
- 목표
  - 학사 일정 조회에 캐시 저장과 네트워크 실패 시 마지막 성공 데이터 fallback을 추가하는 것
- 요약
  - 1차 기록에서는 요구사항과 터치포인트가 정리되었습니다.
  - 이후 기록과 팀 완료 리포트 기준으로, Android 일정 캐시를 실제 구현하고 테스트까지 추가하는 단계가 별도로 진행되었습니다.
  - 프로젝트 기획서 기준 요구사항은 학사 일정 12시간 캐시와 마지막 성공 데이터 재사용입니다.
- 상태
  - 완료 기록 존재
- 근거 문서
  - `.omx/context/android-schedule-cache-20260520T024234Z.md`
  - `.omx/context/android-schedule-cache-implement-20260520T025536Z.md`
  - `.omx/reports/team-commit-hygiene/android.md`
  - `docs/project_specification.md`


### 2026-05-22 · Android / Web 연계

#### 9) Android 가정통신문 phase 1 구현
- 목표
  - Android 홈에 가정통신문 preview card를 추가하고, web `/api/notices` BFF를 재사용해 최근 가정통신문을 보여주는 것
- 요약
  - Android 홈에 notices card 위치를 일정 아래 / 타이머 위로 두는 방향으로 구현이 진행되었다.
  - Android는 학교 홈페이지 스크래핑을 직접 구현하지 않고, web notices BFF 경계를 재사용한다.
  - web 쪽은 Android 소비를 위한 machine-readable notices contract(`status`, `errorCode`, `items`) 보강이 반영되었다.
  - Android 쪽에는 notices feed/model, notices API service, repository integration, HomeViewModel/MainActivity/activity_main notices 흐름, external URL open, 관련 테스트가 추가되었다.
  - Android `WEB_BASE_URL` 기본값을 local dev 주소(`10.0.2.2:3000`)에서 production 주소(`https://midschool.vercel.app/`)로 바꿔, 별도 로컬 설정이 없을 때도 notices 요청이 실서버를 보도록 조정했다.
- 검증 메모
  - leader 기준 `web/scripts/test-notice-route-contract.mjs` 통과
  - leader 기준 `android/app:compileDebugKotlin` 통과
  - leader 기준 `HomeViewModelTest` targeted run 통과
  - worker 런타임 auto-checkpoint 커밋이 섞여 있어 최종 커밋 정리는 별도 필요
- 상태
  - 구현/검증 완료 후 최종 history 정리 예정
- 근거 문서
  - `.omx/context/android-notices-implementation-20260522T021700Z.md`
  - `.omx/reports/team-commit-hygiene/android-notices-discovery-andr.md`

## 참고 문서

- `docs/project_specification.md`
  - 멀티플랫폼 전체 요구사항과 캐시 정책, Android/Web 범위를 정리한 기획서
- `docs/android-school-selection-review.md`
  - Android 학교선택 기능의 구현 전/검토 시점 메모
- `.omx/context/*.md`
  - 작업별 컨텍스트 스냅샷 원문
- `.omx/reports/team-commit-hygiene/*.md`
  - 팀 작업 종료 후 남은 완료 task 제목과 커밋 정리 가이드

## 운영 메모

앞으로 이 저장소에서 작업을 완료하면, 이전 작업 이력 요약은 이 파일(`docs/work-summary.md`)에 계속 누적 업데이트합니다.
