---
name: kotlin-concurrency-expert
description: MisSchoolApp에서 코루틴/스레드 관련 문제를 점검할 때 참고하는 리뷰 체크리스트.
---

# MisSchoolApp Concurrency Review

## 이 스킬을 쓸 때 확인할 것
- ViewModel이 아닌 곳에서 과도한 상태 계산을 하고 있는지
- `Dispatchers.IO` 하드코딩이 새 코드에 늘어나고 있는지
- `CancellationException`이 일반 예외 처리에 묻히지 않는지
- 위젯/브로드캐스트 비동기 작업이 `goAsync()` 수명 안에서 끝나는지
- Flow 수집이 lifecycle-safe 한지

## 자주 보는 문제
### 1. Activity 로직 과다
- 입력 검증, 포맷팅, 로딩 상태 계산은 ViewModel로 이동

### 2. 직접 Flow 수집 문제
- `repeatOnLifecycle` 없이 오래 수집하면 누수 가능성 점검

### 3. 위젯 갱신 누락
- 날짜 변경, 시간 변경, 재부팅 이벤트 이후 재갱신 여부 점검

### 4. 타이머 상태 불일치
- 저장된 종료 시각과 현재 남은 시간이 맞는지
- 알람 수신 후 상태 정리가 되는지

## 리뷰 우선순위
1. 사용자에게 보이는 오동작
2. lifecycle 누수 가능성
3. 취소/예외 처리
4. 테스트 가능성
