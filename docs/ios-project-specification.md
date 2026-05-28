# 학교도우미 iOS 앱 스펙 초안

기준일: 2026-05-26

이 문서는 `docs/project_specification.md` 의 공통 요구사항을 바탕으로,
`ios/` 폴더에서 구현할 iPhone용 앱 범위를 정리한 iOS 전용 스펙 초안입니다.

---

## 1. 목표

iOS 앱은 Android 앱과 동일한 핵심 학생 기능을 iPhone에서도 사용할 수 있도록 만드는 것이 목표입니다.

초기 목표:

- 학교 검색 및 학생 설정 저장
  - 기존 저장값에 학교명만 있고 학교 코드가 없으면 재검색/재선택 요구
  - 학교/학년/반 완료 판정은 Android와 동일하게 공백 문자열을 미완료로 처리
- 홈 대시보드
- 시간표 조회
- 급식 조회
- 학사 일정 조회
- 스터디 타이머
- 가정통신문 미리보기

후속 목표:

- 알림 권한/완료 알림
- 위젯/홈 화면 확장

---

## 2. 1차 MVP 기능

### 2.1 Setup
- 학교 검색
- 학교 선택
- 빠른 재검색 시 이전 검색 응답이 최신 결과를 덮어쓰지 않음
- 학년/반 입력
- 저장 완료 후 홈 진입

### 2.2 Home
- 오늘 날짜
- 저장된 학교/학년/반
- 오늘 시간표 요약
- 오늘 급식 요약 + 식사 구분/칼로리 메타
- 다가오는 일정 요약
- 타이머 요약 카드
- 타이머 프리셋 선택 / 시작 / 일시정지 / 리셋 quick controls
- 설정 화면 진입 버튼

### 2.3 Timetable
- 날짜 이동형 일간 시간표
- 교시별 과목 리스트

### 2.4 Meals
- 주간 급식 조회
- 날짜별 메뉴/칼로리 표시

### 2.5 Schedule
- 월간 또는 기간형 일정 조회

### 2.6 Timer
- 집중/휴식 프리셋
- 숫자/링 표시 모드
- 시작/일시정지/재시작/리셋
- 상태 복구 가능한 저장 구조
- 실행 중 countdown 갱신
- 타이머 완료 알림 예약/취소

### 2.7 Settings
- 학교 재검색/재선택
- 빠른 재검색 시 이전 검색 응답이 최신 결과를 덮어쓰지 않음
- 학년/반 수정
- 타이머 표시 모드
- 알림음/진동 사용 여부
  - 알림 OFF 저장 시 이미 예약된 타이머 완료 알림 취소
- 알림 권한 상태 확인 및 요청

### 2.8 Notices
- Android와 동일하게 web `/api/notices` BFF 사용
- 학교/교육청 코드와 limit query 전달
- 최근 가정통신문 최대 3개 홈 카드 표시
- 날짜가 없으면 Android와 동일하게 제목만 표시
- 첫 번째 가정통신문 URL 열기

### 2.9 Widget foundation
- 앱/위젯 shared suite 저장소
- 오늘/내일 시간표 snapshot 로더
- 타이머 요약 snapshot
- `SchoolHelperWidget` WidgetKit source/target scaffold
- app group entitlement scaffold
- 위젯용 `내일 시간표 표시` 설정 저장
- `WidgetConfigurationIntent` 기반 위젯 개별 설정

---

## 3. 기술 방향

### UI
- SwiftUI

### 상태
- MVVM
- `@StateObject` / `@ObservableObject` 기반 화면 상태

### 비동기
- async/await

### 저장
- UserDefaults 래퍼
- 위젯 확장을 위한 shared suite 경계
- NEIS 데이터 캐시
  - 급식: 12시간
  - 시간표: 24시간
  - 학사 일정: 12시간
  - 최신 요청 실패 시 fresh cache를 우선 fallback으로 사용

### 네트워크
- URLSession 기반 client
- NEIS / notices 경계 분리
- notices는 Android와 동일하게 web `/api/notices` BFF를 우선 사용

---

## 4. 공통 계약

iOS 앱은 다음 공통 규칙을 Android/Web와 맞춥니다.

- 학교 identity 저장:
  - `schoolName`
  - `officeCode`
  - `schoolCode`
  - `schoolKind`
- 학생 설정 완료 조건:
  - 학년/반 + 학교 identity
- 캐시 키:
  - 급식: 학교 + 날짜
  - 시간표: 학교 + 학년/반 + 날짜
  - 일정: 학교 + 월

---

## 5. 현재 구현 우선순위

1. iOS 앱 폴더 구조 확정
2. SwiftUI 앱 진입점과 화면 골격 생성
3. Setup/Home/Settings 흐름 우선 연결
4. NEIS client와 mock 데이터/실데이터 경계 설계
5. 화면별 ViewModel/Model 정리
6. SwiftPM 기반 코어 회귀 테스트 경로 유지

---

## 6. 아직 열려 있는 결정/검증

해결됨:

- SwiftUI 앱은 `ios/SchoolHelperIOS.xcodeproj` + SwiftPM 코어 테스트 병행 구조로 둠
- 실제 `SchoolHelperWidget` WidgetKit target/scaffold를 포함함
- SwiftPM 코어 테스트는 Repository/ViewModel/Widget snapshot/Notification coordination까지 확장함
- 가정통신문 BFF 호출과 홈 카드 포맷을 Android와 맞춤

남은 검증:

- 홈 화면 위젯 실제 배치/탭 동작
- 시스템 딥링크 확인 다이얼로그 이후 최종 전환
- 실기기 알림/권한 UX
- full App Group 실기기 빌드: widget provisioning profile에 App Group entitlement 반영 필요

현재는 **기능 parity를 우선하는 iPhone 앱 구현 + 코어 회귀 테스트 + simulator/device-preview 검증**을 기준으로 진행합니다.
