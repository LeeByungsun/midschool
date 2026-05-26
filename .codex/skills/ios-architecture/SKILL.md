---
name: ios-architecture
description: MisSchoolApp 기준의 iOS 아키텍처 가이드. SwiftUI, MVVM, async/await, 저장소/네트워크 분리를 유지할 때 사용한다.
---

# MisSchoolApp iOS Architecture

## 이 프로젝트에서 우선하는 구조
- iOS 코드는 `ios/` 아래에서 관리한다.
- UI는 **SwiftUI** 기반으로 시작한다.
- 화면 상태는 **ViewModel + Observable state** 로 관리한다.
- 데이터 접근은 **Repository/Client 계층**으로 분리한다.
- 로컬 저장은 **UserDefaults 래퍼**를 거친다.

## 권장 폴더 기준
- `App/`: App 진입점, 루트 탭/네비게이션
- `Core/Models/`: 공통 도메인 모델
- `Core/Networking/`: NEIS / notices / 공통 API 클라이언트
- `Core/Storage/`: 사용자 설정, 캐시, 타이머 저장
- `Features/Setup/`
- `Features/Home/`
- `Features/Timetable/`
- `Features/Meals/`
- `Features/Schedule/`
- `Features/Timer/`
- `Features/Settings/`

## 작업 원칙

### 1. Android/Web의 도메인 계약을 유지한다
- 학생 설정
- 시간표
- 급식
- 학사 일정
- 타이머
- 가정통신문

기능 이름이 아니라 **같은 도메인 의미**를 유지하는 것이 우선이다.

### 2. View는 표현만 담당한다
- 입력 검증
- 네트워크 호출 시작
- 저장 여부 판단
- 캐시 fallback
- 타이머 상태 전이

이런 로직은 ViewModel 또는 Repository로 이동한다.

### 3. 저장 경로를 직접 만지지 않는다
- `UserDefaults.standard` 직접 접근을 화면 코드에 두지 않는다.
- 설정/캐시/타이머 상태는 storage wrapper를 거친다.

### 4. 네트워크 계층은 명시적으로 분리한다
- NEIS API
- notices 경계
- 공통 에러 매핑

을 분리해 테스트 가능하게 유지한다.

### 5. iOS 전용 확장 기능은 나중에 분리한다
- 알림: `UserNotifications`
- 위젯: `WidgetKit`
- 백그라운드 복구

초기 앱 골격에서는 핵심 기능 parity가 우선이고,
플랫폼 확장 기능은 별도 레이어로 붙인다.

## 문서 동기화
- iOS 범위가 바뀌면 `docs/ios-project-specification.md` 를 같이 갱신한다.
- 공통 기능 계약이 바뀌면 `docs/project_specification.md` 도 함께 갱신한다.
