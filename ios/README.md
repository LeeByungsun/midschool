# 학교도우미 iOS

이 폴더는 학교도우미의 iPhone용 앱 작업 공간입니다.

현재 단계:

- iOS 전용 스펙 문서 추가
- SwiftUI 기반 폴더 골격 생성
- 화면/도메인 구조 초안 정리
- shared app/widget storage 및 widget snapshot foundation 추가
- `SchoolHelperWidget` WidgetKit source/target scaffold 추가
- app group entitlement scaffold 추가
- `SchoolHelperWidget.xcscheme` 공유 스킴 추가

관련 문서:

- `../docs/project_specification.md`
- `../docs/ios-project-specification.md`
- `../docs/ios-runtime-verification.md`
- `../.codex/skills/ios-architecture/SKILL.md`

목표 기능:

- Setup
- Home
- Timetable
- Meals
- Schedule
- Timer
- Settings

실행 참고:

- 앱 스킴: `SchoolHelperIOS`
- UI 테스트 스킴: `SchoolHelperIOSUI`
- 위젯 스킴: `SchoolHelperWidget`


실제 iPhone 설치:

```bash
TEAM_ID=YOUR_TEAM_ID ios/scripts/install_device.sh
```

- 기본 `ENTITLEMENTS_MODE=device-preview` 는 App Group entitlement를 제외하고 앱 본체 확인에 집중합니다.
- 위젯/App Group까지 검증하려면 `ios/scripts/check_app_group_profiles.py` 로 앱/위젯 profile이 모두 `OK` 인지 확인한 뒤 `ENTITLEMENTS_MODE=app-groups` 로 실행합니다.
- NEIS API 키는 앱에 저장하지 않고 런타임 환경변수에서만 읽습니다. 키가 없으면 `KEY` 없이 공개 조회를 시도합니다.
