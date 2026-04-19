---
name: android-viewmodel
description: MisSchoolApp에서 ViewModel을 구현할 때의 기준. StateFlow, SharedFlow, DataBinding 기반 XML 화면 구조에 맞춘다.
---

# MisSchoolApp ViewModel Rules

## 기본 원칙
- 화면 상태는 `UiState` data class로 관리한다.
- 지속 상태는 `StateFlow`
- 일회성 이벤트는 `SharedFlow`
- View는 ViewModel의 상태를 구독하고 클릭 이벤트만 전달한다.

## 이 프로젝트의 표준 패턴
```kotlin
private val _uiState = MutableStateFlow(ScreenUiState())
val uiState = _uiState.asStateFlow()

private val _event = MutableSharedFlow<Unit>()
val event = _event.asSharedFlow()
```

## 사용 규칙
### 1. Activity에 비즈니스 로직을 두지 않는다
- 날짜 계산
- 네트워크 호출 시작
- 입력값 검증
- 타이머 상태 전이
이런 로직은 ViewModel로 이동한다.

### 2. UI 상태는 한곳에서 조립한다
- 텍스트 포맷, 버튼 노출 여부, 로딩 여부를 ViewModel에서 정리한다
- XML은 가능한 한 표시만 담당한다

### 3. SharedFlow 사용처
- 저장 완료 메시지
- 화면 닫기
- 다음 화면 이동
- 위젯 설정 저장 완료

### 4. XML 화면 수집 기준
- Activity에서는 `repeatOnLifecycle`로 `uiState`와 이벤트를 수집한다
- DataBinding을 쓰더라도 코루틴 수집 책임은 Activity/Fragment에 둔다
