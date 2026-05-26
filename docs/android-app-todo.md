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

즉, **기본 앱 구조와 핵심 조회 기능은 이미 구현되어 있고**, 지금 남은 일은 주로 **학교 선택 흐름 완성**, **조회 기준의 동적화**, **가정통신문 검증**, **품질 보강**입니다.

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

## 3. 핵심 미완 항목

아래는 현재 Android에서 가장 큰 미완 영역입니다.

### A. 학교 검색/선택 기능

- [ ] Setup 화면에 학교 검색 입력/결과 선택 UI 추가
- [ ] Settings 화면에 학교 변경 흐름 추가
- [ ] 학교 선택 완료 전에는 설정 완료로 처리하지 않도록 정리

### B. 학교 코드/학교 종류 저장

- [ ] `officeCode` 저장
- [ ] `schoolCode` 저장
- [ ] `schoolKind` 저장
- [ ] 기존 사용자 데이터 마이그레이션 또는 재설정 흐름 정의

### C. 실제 조회 기준 동적화

- [ ] 급식 조회가 저장된 학교 기준으로 동작
- [ ] 일정 조회가 저장된 학교 기준으로 동작
- [ ] 시간표 조회가 저장된 학교 기준으로 동작
- [ ] `schoolKind`에 따라 `misTimetable` / `elsTimetable` 분기

### D. 홈 표시값 동기화

- [ ] 홈 학교명을 저장된 학교 기준으로 표시
- [ ] Setup/Settings/홈이 같은 사용자 설정 계약을 사용하도록 정리

### E. 캐시 키 정리

- [ ] 급식 캐시에 학교 identity 포함
- [ ] 시간표 캐시에 학교 identity 포함
- [ ] 일정 캐시에 학교 identity 포함

현재는 학교 선택 기능이 완전히 연결되지 않으면, UI와 실제 조회 학교가 어긋날 위험이 큽니다.

---

## 4. 우선순위별 앞으로 할 일

## 1순위 — 학교 선택 기능을 실제 동작까지 완성

이 단계가 가장 중요합니다.

완료 기준:

- 사용자가 학교를 검색하고 선택할 수 있음
- 선택한 학교의 `officeCode`, `schoolCode`, `schoolKind`가 저장됨
- 이후 급식/시간표/일정이 모두 선택한 학교 기준으로 조회됨

세부 작업:

- [ ] `SetupActivity` / `SetupViewModel` 학교 검색 흐름 추가
- [ ] `SettingsActivity` / `SettingsViewModel` 학교 변경 흐름 추가
- [ ] `PreferencesRepository`에 학교 identity 읽기/쓰기 API 추가
- [ ] `UserPreferences` 모델 확장
- [ ] 설정 완료 조건 재정의
- [ ] 홈 화면 학교명 동적 표시로 교체

---

## 2순위 — Repository/NEIS 조회 계약 정리

완료 기준:

- Repository가 고정 학교 코드가 아니라 저장된 학교 정보로 조회
- 시간표가 학교 종류에 맞는 엔드포인트를 사용
- 잘못된 학교/종류 조합 시 오류가 일관되게 처리됨

세부 작업:

- [ ] `SchoolRepositoryImpl`의 고정 `officeCode`, `schoolCode` 제거
- [ ] `schoolKind` 기반 시간표 엔드포인트 분기 추가
- [ ] NEIS 오류 문구를 학교 설정 문제와 일반 네트워크 오류로 구분
- [ ] 관련 단위 테스트 보강

---

## 3순위 — 캐시/복구 전략을 학교 선택 구조에 맞게 재정리

완료 기준:

- 학교를 바꿔도 캐시가 섞이지 않음
- 같은 학교/같은 기준 조회에서는 기존 복구 전략이 유지됨

세부 작업:

- [ ] 급식 캐시 키에 `officeCode + schoolCode + 일자` 반영 재검증
- [ ] 시간표 캐시 키에 `officeCode + schoolCode + 학년 + 반 + 일자` 반영 재검증
- [ ] 일정 캐시 키에 `officeCode + schoolCode + 월` 반영 재검증
- [ ] 학교 변경 시 오래된 캐시 처리 정책 정리

---

## 4순위 — 가정통신문 실제 운영 검증

완료 기준:

- production notices 경로가 Android 실기기/에뮬레이터에서 안정적으로 동작
- 미지원 학교/교육청에 대한 fallback 문구가 명확함

세부 작업:

- [ ] Android 실기기에서 production notices 수동 확인
- [ ] 에뮬레이터에서 production notices 수동 확인
- [ ] 대구교육청(`*.dge.ms.kr`) 경로 대응 가능성 조사
- [ ] 미지원 학교용 fallback 문구/버튼 흐름 점검

---

## 5순위 — 품질 보강

완료 기준:

- 학교 선택 추가 이후 회귀가 테스트로 막힘
- 주요 입력/상태/전환 흐름이 안정적임

세부 작업:

- [ ] SetupViewModel 테스트에 학교 선택 케이스 추가
- [ ] SettingsViewModel 테스트에 학교 변경 케이스 추가
- [ ] Repository 테스트에 학교 종류 분기 추가
- [ ] MainActivity/HomeViewModel의 학교 표시 동기화 테스트 추가
- [ ] 문자열/하드코딩/접근성 점검

---

## 5. 추천 작업 순서

다음 스프린트는 아래 순서가 가장 안전합니다.

1. 학교 선택 UI 추가
2. Preferences 모델/저장소 확장
3. Repository의 조회 기준 동적화
4. 시간표 엔드포인트 분기
5. 캐시 키 재검증
6. 홈 학교명 동기화
7. notices 실기기 검증
8. 테스트 보강

---

## 6. 지금 당장 가장 중요한 한 가지

지금 Android에서 가장 큰 제품 리스크는:

**학교를 바꾸는 것처럼 보이지만 실제 조회 학교는 바뀌지 않는 상태가 될 수 있다는 점**입니다.

그래서 앞으로 할 일의 핵심은 단순 UI 추가가 아니라,

**학교 선택 UI + 저장 계약 + Repository 조회 기준 + 캐시 키**를 한 묶음으로 끝내는 것입니다.

---

## 7. 관련 파일 빠른 진입점

학교 선택 기능 작업 시 먼저 볼 파일:

- `android/app/src/main/java/com/bsbarron/midschoolapp/SetupActivity.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/SettingsActivity.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/setup/SetupViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/settings/SettingsViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/UserPreferences.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepository.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepositoryImpl.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/SchoolRepositoryImpl.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/remote/NeisApiService.kt`
- `android/app/src/main/res/layout/activity_setup.xml`
- `android/app/src/main/res/layout/activity_settings.xml`

---

## 8. 팀 명령어

Android 앞으로 할 일을 바로 병렬 실행할 수 있도록 아래 팀 명령 스크립트를 둡니다.

### 학교 선택 A/B 구현

- `android/scripts/team-school-selection-ab.sh`

대상:

- 3. 핵심 미완 항목의 A. 학교 검색/선택 기능
- 3. 핵심 미완 항목의 B. 학교 코드/학교 종류 저장

### 위젯 시간표 자동 업데이트 확인

- `android/scripts/team-widget-timetable-check.sh`

대상:

- 위젯 자동 업데이트 시 간헐적으로 시간표를 못 가져오는 문제 조사/검증
