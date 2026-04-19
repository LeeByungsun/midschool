---
name: android-coroutines
description: MisSchoolApp의 코루틴 규칙. ViewModel, Repository, Widget, Timer 코드에서 lifecycle-safe 하게 비동기 처리를 할 때 사용한다.
---

# MisSchoolApp Coroutines

## 현재 프로젝트에서 중요한 포인트
- ViewModel은 `viewModelScope`
- Activity는 `lifecycleScope + repeatOnLifecycle`
- Repository는 main-safe 하게 유지
- 위젯/브로드캐스트 쪽 비동기 작업은 수명과 예외 처리를 더 조심한다

## 규칙
### 1. ViewModel
- 코루틴 시작은 `viewModelScope`
- 상태는 `StateFlow`, 이벤트는 `SharedFlow`

### 2. Activity
- `launchWhenStarted` 대신 `repeatOnLifecycle` 사용
- UI에서 Flow를 직접 오래 붙잡지 않는다

### 3. Repository
- 긴 작업이나 네트워크/파싱은 main-safe 하게 유지한다
- `CancellationException`을 일반 실패로 삼키지 않는다

### 4. 위젯
- `AppWidgetProvider`는 수명이 짧으므로 `goAsync()`와 `finally { finish() }`를 유지한다
- 위젯 갱신은 실패해도 런처를 깨지 않게 방어적으로 처리한다

### 5. 타이머
- 타이머 상태 복원과 알림 스케줄은 앱 프로세스 재진입을 고려한다

## 개선 우선순위
- 새 코드에서는 dispatcher 주입을 우선 고려한다
- 기존 코드의 `Dispatchers.IO` 하드코딩은 점진적으로 줄인다
