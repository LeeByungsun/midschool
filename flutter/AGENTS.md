# AGENTS Guidelines for Flutter

이 문서는 `flutter/` 하위에 적용되는 Flutter 전용 작업 규칙입니다.
이 폴더는 현재 Flutter 클라이언트 도입을 위한 시작점이며, 앱 스캐폴드는 별도 승인된 작업에서 생성합니다.

## 1. 제품 기준과 범위

- **앱 이름:** 스쿨온
- **목표:** Android와 iOS에 흩어진 공통 학생 기능을 하나의 Flutter UI/도메인 구현으로 제공한다.
- **기능 기준 문서:** `../docs/project_specification.md`
- **기존 플랫폼 UI/인터랙션 비교:** `../docs/android-ios-ui-interaction-comparison.md`
- **기존 구현 참조:** `../android/`, `../ios/`

화면을 픽셀 단위로 복제하는 것보다, 학교 설정·시간표·급식·일정·타이머·가정통신문·위젯에서 사용자가 얻는 결과와 오류 피드백을 일관되게 만드는 것을 우선한다.

## 2. 도입 원칙

- Flutter 앱은 이 폴더 안에서 독립적으로 관리한다. 기존 Android/iOS 앱을 삭제하거나 구조를 바꾸지 않는다.
- 앱 스캐폴드, 상태 관리 라이브러리, 라우팅 라이브러리, 저장소 패키지는 설계 승인 없이 임의로 추가하지 않는다.
- UI, 도메인 모델, Repository 계약, API 응답 매핑, 일반 캐시는 Dart 계층에 둔다.
- WidgetKit, Android App Widget, 부팅 후 알람 복구, 플랫폼별 알림 권한처럼 OS가 요구하는 기능만 최소한의 네이티브 브리지로 분리한다.
- Widget/알림의 플랫폼별 한계를 Dart 코드로 감추거나 동작을 가정하지 않는다. 실제 iOS/Android에서 검증한다.

## 3. 권장 구조 (스캐폴드 생성 후)

```text
flutter/
  lib/
    app/                 # 앱 진입, 테마, 라우팅, 의존성 조립
    core/
      models/            # 공통 도메인 모델
      network/           # NEIS/가정통신문 API 경계와 DTO 매핑
      storage/           # 설정, 캐시, 타이머 저장 계약
      platform/          # 알림·위젯 등 MethodChannel 경계
    features/
      setup/
      home/
      timetable/
      meals/
      schedule/
      timer/
      settings/
  test/
  integration_test/
```

- Widget은 표현과 사용자 입력만 담당한다.
- API 호출, 입력 검증, 캐시 fallback, 타이머 상태 전이는 Controller/ViewModel/Notifier 또는 Repository 계층에 둔다.
- 화면에서 HTTP, 저장소, `MethodChannel`을 직접 호출하지 않는다.
- 한 기능의 public 상태와 이벤트 흐름은 한 가지 상태 관리 방식으로 일관되게 유지한다.

## 4. UI/UX 정합성 규칙

- 모바일 우선으로 구현하되, 작은 iPhone과 일반 Android 폭에서 긴 학교명·급식 메뉴·일정 목록이 잘리지 않는지 확인한다.
- Android의 강한 카드형 정보 위계와 iOS의 탭/Form 탐색 차이는 `android-ios-ui-interaction-comparison.md`를 기준으로 의도적으로 결정한다. 우연한 화면 차이를 만들지 않는다.
- 설정 후 데이터 재조회, 타이머 표시 방식 변경, 날짜 이동, 로딩/빈/오류/재시도 상태를 각 화면에 명시한다.
- 숫자 입력(학년/반)은 숫자 키보드, 길이 제한, 저장 전 검증을 함께 제공한다.
- 접근성은 버튼 라벨, 충분한 탭 영역, 포커스 순서, 화면 읽기용 상태 문구를 기본 품질 조건으로 본다.

## 5. 데이터·저장·보안

- NEIS API 키와 비밀값을 Dart 클라이언트나 저장소에 하드코딩하지 않는다.
- 가정통신문은 기존 Web BFF 계약을 사용하며, 학교 홈페이지를 Flutter 클라이언트에서 직접 스크래핑하지 않는다.
- 캐시는 학교 코드·학년·반·조회 날짜/월을 포함한 키와 TTL을 사용한다.
- Android 기존 사용자를 업데이트 대상으로 삼는 경우, SharedPreferences 데이터의 **일회성 마이그레이션 설계와 회귀 테스트**를 먼저 작성한다.
- 새 저장 형식은 버전과 이전 전략을 명시한다.

## 6. 타이머·알림·위젯

- 타이머는 target time 기반으로 복원 가능해야 하며, 앱 재진입·백그라운드·종료 뒤의 상태를 구분한다.
- 알림 권한, 알림음, 진동의 실제 가능 여부를 플랫폼마다 명확히 표시한다.
- Android App Widget과 iOS WidgetKit은 위젯 인스턴스별 설정, 공유 데이터, 갱신 정책, 탭 목적지를 별도로 검증한다.
- 앱 설정의 전역 위젯 옵션과 인스턴스별 위젯 옵션을 중복으로 만들지 않는다. 하나의 소유 정책을 설계에서 확정한다.

## 7. 테스트와 검증

스캐폴드 생성 후 모든 변경은 가능한 범위에서 다음을 통과해야 한다.

```bash
dart format --set-exit-if-changed .
flutter analyze
flutter test
```

- 모델·Repository·캐시·타이머 전이는 단위 테스트로 검증한다.
- 설정 저장, 날짜 이동, 로딩/빈/오류 상태는 widget test로 검증한다.
- 알림, 딥링크, 위젯, 기존 데이터 마이그레이션은 Android/iOS integration 또는 실기기 smoke test로 검증한다.
- Flutter 도입 시 기존 `android/`와 `ios/`의 검증을 대체하기 전까지 기존 플랫폼 검증도 유지한다.

## 8. 문서 반영

- Flutter 구조·패키지·플랫폼 브리지 정책을 확정하면 `../docs/project-structure.md`와 `../docs/project_specification.md`를 갱신한다.
- Android/iOS와 사용자가 경험하는 차이가 생기거나 해소되면 `../docs/android-ios-ui-interaction-comparison.md`를 갱신한다.
- 위젯·알림·마이그레이션처럼 출시 위험이 있는 항목은 구현 완료와 실기기 검증 완료를 구분해 기록한다.
