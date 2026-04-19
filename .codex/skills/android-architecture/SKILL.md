---
name: android-architecture
description: MisSchoolApp 기준의 안드로이드 아키텍처 가이드. XML + DataBinding, Hilt, MVVM, Repository 구조를 유지할 때 사용한다.
---

# MisSchoolApp Architecture

## 이 프로젝트에서 우선하는 구조
- 현재는 **단일 `:app` 모듈**이다.
- UI는 **XML + DataBinding** 기반이다.
- 화면 상태는 **ViewModel + StateFlow/SharedFlow**로 관리한다.
- 데이터 접근은 **Repository 인터페이스**를 통해 통일한다.
- 의존성 주입은 **Hilt**를 사용한다.

## 현재 패키지 기준
- `ui/`: ViewModel, UiState
- `data/`: model, remote, repository
- `di/`: Hilt 모듈
- `widget/`: AppWidget 관련 코드
- `timer/`: 타이머 알람/스케줄링 관련 코드

## 작업 원칙
### 1. 새 기능은 기존 구조를 유지한다
- 새 화면이 필요하면 Activity + ViewModel + UiState 조합으로 추가한다.
- Activity에는 상태 계산 로직을 넣지 않는다.
- 설정/캐시/학교 정보는 직접 저장하지 말고 Repository를 거친다.

### 2. 과한 구조 분리는 지금 하지 않는다
- 현재 프로젝트에는 별도 `domain` 모듈이나 UseCase 레이어가 없다.
- 비즈니스 로직이 정말 커지기 전까지는 Repository + ViewModel 구조를 유지한다.

### 3. Hilt 기준
- `Application`은 `@HiltAndroidApp`
- Activity/Receiver는 필요한 경우 `@AndroidEntryPoint`
- ViewModel은 `@HiltViewModel`
- 인터페이스 바인딩은 `@Binds`를 우선한다.

### 4. 문서 동기화
- 기능 흐름이나 화면 구조가 바뀌면 `docs/project_specification.md`도 함께 갱신한다.
