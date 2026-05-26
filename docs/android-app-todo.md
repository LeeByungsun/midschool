# 학교도우미 Android 앱 앞으로 할 일

기준일: 2026-05-26

이 문서는 현재 저장소의 **Android 앱만** 기준으로 구현 상태를 정리하고, 다음 작업 우선순위를 명확히 남기기 위한 문서입니다.

참고 기준:

- `android/README.md`
- `docs/project_specification.md`
- `docs/android-school-selection-review.md`
- `android/app/src/main/java/com/bsbarron/midschoolapp/**`

---

## 1. 현재 상태 요약

Android 앱은 현재 서비스의 기준 플랫폼이며, 아래 축은 이미 갖춰져 있습니다.

- Kotlin + XML + DataBinding + Hilt + MVVM 구조
- 메인/급식/일정/설정/초기설정/스플래시/시간표 화면 구성
- NEIS 기반 급식 / 시간표 / 학사 일정 조회
- 타이머 완료 알림 / 진동 / 상태 유지
- 홈 화면 위젯
- 홈 대시보드 가정통신문 preview 카드
- Repository / Preferences 기반의 기본 캐시 구조

추가로, **학교 선택 A/B(검색/선택 UI + 학교 identity 저장)** 도 현재 브랜치 기준 구현되어 있습니다.

지금 남은 일은 주로 **운영 검증**, **추가 회귀 검증**, **실기기 확인**입니다.

---

## 2. 이미 구현된 항목

### 화면/흐름

- [x] 스플래시 화면
- [x] 초기 설정 화면
- [x] 설정 화면
- [x] 메인 대시보드
- [x] 주간 급식 화면
- [x] 일간 시간표 화면
- [x] 월간 학사 일정 화면

### 데이터/아키텍처

- [x] Hilt 기반 DI
- [x] `SchoolRepository`
- [x] `PreferencesRepository`
- [x] ViewModel + UiState 분리
- [x] SharedPreferences 기반 저장/캐시
- [x] Retrofit + OkHttp + Gson 기반 원격 연동

### 학교 선택 / 저장 계약

- [x] Setup 화면 학교 검색 입력/결과 선택 UI
- [x] Settings 화면 학교 변경 흐름
- [x] 학교 선택 완료 전 Setup 미완료 처리
- [x] `officeCode` 저장
- [x] `schoolCode` 저장
- [x] `schoolKind` 저장
- [x] 저장된 학교 기준 급식/일정/시간표 조회
- [x] `schoolKind` 기반 시간표 엔드포인트 분기
- [x] 홈 학교명 동적 표시

### 타이머/위젯

- [x] 타이머 ViewModel/UiState
- [x] 완료 알림/진동
- [x] 위젯 Provider
- [x] 위젯 설정 화면
- [x] 커스텀 링 뷰

### 가정통신문

- [x] 홈 preview 카드 추가
- [x] Android가 학교 홈페이지를 직접 스크래핑하지 않고 web BFF를 재사용
- [x] notices 응답 모델/원격 서비스 연결

---

## 3. 지금 남은 핵심 과제

### A. 레거시 사용자 마이그레이션 UX

- [x] 학년/반만 저장된 기존 사용자가 왜 Setup으로 돌아가는지 안내 문구 추가
- [x] partial prefs를 reset할지 재입력으로 유도할지 UX 정책 확정

### B. 학교 검색 상태 안정성

- [x] 빠른 연속 검색 시 오래된 응답이 최신 결과를 덮지 않도록 정리
- [x] 검색 실패/빈 결과 시 기존 선택을 유지할지 초기화할지 UX 정책 정리

### C. 회귀 검증 보강

- [x] Setup 검색 결과 상태 테스트 추가 보강
- [x] Settings 검색 결과 상태 테스트 추가 보강
- [x] persistence/setup-complete 규칙 테스트 유지

### D. 운영 검증

- [ ] Android 실기기에서 production notices 수동 확인
- [ ] 에뮬레이터에서 production notices 수동 확인
- [ ] 대구교육청(`*.dge.ms.kr`) 경로 대응 가능성 조사
- [ ] 미지원 학교용 fallback 문구/버튼 흐름 점검

---

## 4. 우선순위별 앞으로 할 일

## 1순위 — 운영 검증과 수동 확인

완료 기준:

- school selection과 notices가 실기기/에뮬레이터에서 기대대로 동작
- 수동 검증으로 남은 UX 리스크를 줄임

세부 작업:

- [ ] Android 실기기에서 school selection 저장/복원 수동 확인
- [ ] Android 에뮬레이터에서 school selection 저장/복원 수동 확인
- [ ] Android 실기기에서 production notices 수동 확인
- [ ] 에뮬레이터에서 production notices 수동 확인

## 2순위 — 회귀 검증 유지

완료 기준:

- 학교 선택/저장/완료 상태 핵심 흐름이 테스트로 보호됨

세부 작업:

- [x] SetupViewModel 저장 검증 테스트
- [x] SettingsViewModel 저장/재선택 검증 테스트
- [x] Repository 학교 코드/학교 종류 분기 테스트
- [x] 홈 학교명 동기화 테스트
- [x] persistence/setup-complete 규칙 테스트
- [x] 경쟁 검색/선택 유지 UX 테스트 보강

## 3순위 — 운영 검증

완료 기준:

- notices와 학교 선택 흐름이 실기기/에뮬레이터에서 안정적으로 동작

세부 작업:

- [ ] Android 실기기에서 production notices 수동 확인
- [ ] 에뮬레이터에서 production notices 수동 확인
- [ ] 미지원 학교 fallback 문구/버튼 흐름 점검

---

## 5. 추천 작업 순서

다음 스프린트는 아래 순서가 가장 안전합니다.

1. school selection 실기기/에뮬레이터 수동 확인
2. notices 실기기 검증
3. 남은 미지원 학교 fallback UX 정리

---

## 6. 지금 당장 가장 중요한 한 가지

지금 Android에서 가장 큰 제품 리스크는:

**코드 미구현보다 실기기 검증이 아직 부족하다는 점**입니다.

즉, 다음 단계의 핵심은 새 기능 추가보다,

**수동 검증 + 운영 확인 + 남은 fallback UX 점검**을 묶어서 닫는 것입니다.

---

## 7. 관련 파일 빠른 진입점

학교 선택 기능 후속 작업 시 먼저 볼 파일:

- `android/app/src/main/java/com/bsbarron/midschoolapp/SetupActivity.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/SettingsActivity.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/setup/SetupViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/settings/SettingsViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/UserPreferences.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepository.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepositoryImpl.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/SchoolRepositoryImpl.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/home/HomeViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/splash/SplashViewModel.kt`
- `android/app/src/main/res/layout/activity_setup.xml`
- `android/app/src/main/res/layout/activity_settings.xml`

---

## 8. 팀 명령어

Android 앞으로 할 일을 바로 병렬 실행할 수 있도록 아래 팀 명령 스크립트를 둡니다.

### 학교 선택 A/B 구현

- `android/scripts/team-school-selection-ab.sh`

대상:

- 학교 검색/선택 UI와 저장 계약 관련 후속 정리

### 위젯 시간표 자동 업데이트 확인

- `android/scripts/team-widget-timetable-check.sh`
