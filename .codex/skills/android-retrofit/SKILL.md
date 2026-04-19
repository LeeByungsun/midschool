---
name: android-retrofit
description: MisSchoolApp의 NEIS API 연동 규칙. Retrofit, Hilt, Repository 예외 처리 방식을 맞출 때 사용한다.
---

# MisSchoolApp Retrofit / NEIS

## 현재 기준
- API 서버: `https://open.neis.go.kr/`
- 사용 API:
  - `mealServiceDietInfo`
  - `SchoolSchedule`
  - `misTimetable`
- 네트워크 계층은 `di/NetworkModule.kt`와 `data/remote/`를 기준으로 유지한다

## 작업 원칙
### 1. 쿼리 파라미터는 하드코딩 문자열 결합 대신 Retrofit annotation으로 처리한다
- `KEY`
- `ATPT_OFCDC_SC_CODE`
- `SD_SCHUL_CODE`
- 날짜 / 학년 / 반

### 2. Repository에서 예외를 정리한다
- HTTP 실패
- 네트워크 실패
- NEIS `RESULT.CODE` 실패
- 데이터 없음

UI는 최대한 정리된 결과만 받게 유지한다.

### 3. NEIS 응답 특성 반영
- `HTTP 200`이어도 본문 `RESULT.CODE`가 실패일 수 있다
- 줄바꿈 HTML, 알레르기 번호, 빈 리스트 같은 응답 특성을 Repository나 ViewModel에서 정리한다

### 4. 캐시와 함께 생각한다
- 급식/시간표는 성공 응답을 캐시에 저장하고 실패 시 마지막 성공값을 사용할 수 있게 유지한다

### 5. 디버그 로그
- 네트워크 디버깅은 유지하되, 민감한 키 노출에는 주의한다
