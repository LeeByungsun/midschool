---
name: compose-navigation
description: MisSchoolApp에서 Compose Navigation을 도입하거나 일부 화면을 Compose로 옮길 때 참고하는 보류형 가이드.
---

# Compose Navigation for Future Migration

현재 MisSchoolApp은 **Compose Navigation을 사용하지 않습니다**. 화면 이동은 Activity 기반입니다.

## 이 스킬을 쓰는 경우
- 특정 화면을 Compose로 새로 만든다
- Activity 구조를 Compose Navigation으로 옮기려 한다
- NavHost 기반 단일 Activity 구조를 검토한다

## 현재 기준 원칙
- Compose 전환이 명시적으로 요구되지 않으면 기존 Activity 네비게이션을 유지한다
- 새 기능도 기본적으로는 XML 화면에 맞춘다
- Compose 도입 시에는 일부 화면 단위로 시작하고, 전체 라우팅 교체는 별도 작업으로 본다

## 전환 시 체크포인트
- Hilt ViewModel 주입 방식 유지
- 기존 `UiState` / Repository 구조 재사용
- 기존 위젯, 타이머, 설정 저장 흐름과 충돌 없는지 확인
