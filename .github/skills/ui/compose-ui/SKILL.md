---
name: compose-ui
description: MisSchoolApp에서 Compose UI를 실제로 도입할 때 참고하는 보류형 가이드.
---

# Compose UI for Future Migration

현재 MisSchoolApp의 주 UI는 **XML + DataBinding**입니다.

## 기본 원칙
- Compose는 현재 기본 UI 기술이 아니다
- 명시적인 전환 요청이 없는 한 새 화면도 XML 기준으로 구현한다

## Compose를 도입하는 경우
- 독립적인 새 화면
- 재사용성이 높은 작은 컴포넌트
- 실험적 UI 개선이 필요한 경우

## 도입 시 유지할 것
- 상태는 ViewModel에서 관리
- `UiState` 중심 데이터 흐름 유지
- Repository/Hilt 구조 재사용
- 문자열과 접근성 리소스는 기존 규칙 유지

## 피해야 할 것
- 이유 없이 화면 절반만 Compose로 섞어 복잡도만 늘리는 것
- 기존 XML 화면을 급하게 중간 상태로 섞는 것
- 프로젝트 전체 네비게이션까지 한 번에 바꾸는 것
