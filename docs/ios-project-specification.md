# 학교도우미 iOS 앱 스펙 초안

기준일: 2026-05-26

이 문서는 `docs/project_specification.md` 의 공통 요구사항을 바탕으로,
`ios/` 폴더에서 구현할 iPhone용 앱 범위를 정리한 iOS 전용 스펙 초안입니다.

---

## 1. 목표

iOS 앱은 Android 앱과 동일한 핵심 학생 기능을 iPhone에서도 사용할 수 있도록 만드는 것이 목표입니다.

초기 목표:

- 학교 검색 및 학생 설정 저장
- 홈 대시보드
- 시간표 조회
- 급식 조회
- 학사 일정 조회
- 스터디 타이머

후속 목표:

- 가정통신문 미리보기
- 알림 권한/완료 알림
- 위젯/홈 화면 확장

---

## 2. 1차 MVP 기능

### 2.1 Setup
- 학교 검색
- 학교 선택
- 학년/반 입력
- 저장 완료 후 홈 진입

### 2.2 Home
- 오늘 날짜
- 저장된 학교/학년/반
- 오늘 시간표 요약
- 오늘 급식 요약
- 다가오는 일정 요약
- 타이머 요약 카드

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
- 시작/일시정지/재시작/리셋
- 상태 복구 가능한 저장 구조
- 실행 중 countdown 갱신
- 타이머 완료 알림 예약/취소

### 2.7 Settings
- 학교 재검색/재선택
- 학년/반 수정

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

## 6. 아직 열려 있는 결정

- SwiftUI 앱을 어떤 Xcode 프로젝트 구조로 둘지
- SwiftUI 화면용 Xcode target 검증 외에 SwiftPM 코어 테스트를 얼마나 넓힐지
- 위젯을 1차에 넣을지 2차로 미룰지

현재는 **기능 parity를 우선하는 iPhone 앱 골격 생성 + 코어 회귀 테스트 경로 유지 + 타이머 알림 경계 정리**가 먼저입니다.
