---
name: android-accessibility
description: MisSchoolApp의 XML 화면과 위젯에서 접근성을 점검할 때 사용하는 체크리스트.
---

# MisSchoolApp Accessibility

## XML 화면에서 우선 확인할 것
- `ImageView`, 아이콘 버튼에 적절한 `contentDescription`이 있는지
- 터치 타겟이 최소 48dp에 가까운지
- 제목, 설명, 버튼의 대비가 충분한지
- 설정/뒤로가기 아이콘이 동작 의미로 읽히는지

## 위젯에서 우선 확인할 것
- 새로고침, 설정 버튼에 `contentDescription`이 있는지
- 작은 위젯에서도 텍스트 잘림이 과하지 않은지
- 오늘/내일 구분이 색에만 의존하지 않는지

## 현재 프로젝트에서 자주 보는 포인트
- XML에 하드코딩 문자열을 넣지 말고 `strings.xml`을 사용
- 헤더 버튼은 아이콘만 바꿀 때도 설명 문자열을 같이 확인
- 작은 폰과 삼성 런처 위젯에서도 읽히는지 같이 본다
