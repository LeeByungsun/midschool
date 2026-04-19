---
name: android-data-layer
description: MisSchoolApp의 데이터 계층 가이드. NEIS API, PreferencesRepository 기반 캐시/설정 저장, SchoolRepository 구조에 맞춰 작업할 때 사용한다.
---

# MisSchoolApp Data Layer

## 현재 데이터 계층
- 원격 데이터: **NEIS API**
- 원격 호출: **Retrofit**
- 공통 접근점: **`SchoolRepository`**
- 로컬 설정/캐시: **`PreferencesRepository`**
- 실제 저장소: `SharedPreferences` 기반

## 저장소 역할
### `SchoolRepository`
- 급식, 시간표, 학사일정 조회
- NEIS 응답 파싱
- 네트워크 예외를 사용자 친화적인 결과로 변환
- 캐시 사용 여부 판단

### `PreferencesRepository`
- 학년/반 저장
- 타이머 설정 저장
- 위젯 설정 저장
- 급식/시간표 캐시 저장
- 타이머 상태 저장

## 작업 원칙
### 1. Activity에서 저장소 구현체를 직접 다루지 않는다
- `SharedPreferences` 직접 접근 금지
- 새 설정값이 필요하면 `PreferencesRepository` 인터페이스부터 확장한다

### 2. NEIS 관련 예외 처리는 Repository에서 끝낸다
- UI는 성공/실패 상태만 받도록 유지한다
- NEIS `RESULT.CODE` 해석은 UI가 아니라 Repository 쪽에서 담당한다

### 3. 캐시 정책은 명확하게 둔다
- 날짜 단위 응답은 키를 명확히 만든다
- TTL이 있는 캐시는 만료 기준을 문서와 코드에 같이 반영한다

### 4. 현재는 Room보다 단순 캐시가 우선
- 이 프로젝트는 아직 Room 기반 오프라인 우선 구조가 아니다
- 큰 기능 확장 전까지는 `PreferencesRepository` 기반 캐시 전략을 유지한다
