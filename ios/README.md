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
- `../docs/ios-parity-audit.md`
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

실제 iPhone parity smoke:

```bash
# 이미 설치된 앱의 현재 저장값으로 딥링크/런치만 확인
ios/scripts/verify_device_parity.sh

# 초기 설정/fixture 저장까지 포함해 결정적으로 확인
RUN_FRESH_SETUP=1 SEED_PROFILE=1 ios/scripts/verify_device_parity.sh

# 설치부터 다시 수행
INSTALL=1 TEAM_ID=YOUR_TEAM_ID SEED_PROFILE=1 ios/scripts/verify_device_parity.sh
```

실제 iPhone 타이머 알림 smoke:

```bash
# 앱이 최신으로 설치된 상태에서 실행
ios/scripts/verify_device_notification.sh

# 권한 팝업이 처음 뜨면 허용 후 한 번 더 실행
REMAINING_SECONDS=20 ios/scripts/verify_device_notification.sh
```

live NEIS/BFF 데이터 smoke:

```bash
# 미사중학교 기준 실제 NEIS 급식/시간표/일정과 notices BFF 응답 확인
DATE=20260528 MONTH=202605 ios/scripts/verify_live_school_data.py
```

- 기본값은 `SCHOOL_NAME=미사중학교`, `OFFICE_CODE=J10`, `SCHOOL_CODE=7692129`, `GRADE=1`, `CLASSROOM=2` 입니다.
- `NEIS_API_KEY` 는 앱에 저장하지 않고 이 스크립트에서도 환경변수로만 선택 주입합니다. 키가 없으면 iOS 앱과 동일하게 `KEY` 없이 공개 조회를 시도합니다.
- notices는 `WEB_BASE_URL` 의 `/api/notices` BFF를 호출합니다. 기본값은 `https://midschool.vercel.app/` 입니다.

실제 iPhone UI 테스트:

```bash
# 기본: 초기 설정 학교 검색/저장 UI 테스트만 실행
TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh

# 전체 UI 테스트를 실기기에서 실행
ONLY_TESTING= TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh
```

- 기본 `ENTITLEMENTS_MODE=device-preview` 는 App Group entitlement를 제외하고 앱 본체 확인에 집중합니다.
- 위젯/App Group까지 검증하려면 `ios/scripts/check_app_group_profiles.py` 로 앱/위젯 profile이 모두 `OK` 인지 확인한 뒤 `ENTITLEMENTS_MODE=app-groups` 로 실행합니다.
- 실기기 UI 테스트는 `CODE_SIGNING_ALLOWED=YES` signing override가 필요하므로 `test_device_ui.sh` 를 사용합니다.
- `test_device_ui.sh` 는 기기 잠금 상태를 감지해 종료하고, `Timed out while enabling automation mode` 는 기본 1회 자동 재시도합니다. 필요하면 `AUTOMATION_RETRY_LIMIT=0` 으로 끌 수 있습니다.
- NEIS API 키는 앱에 저장하지 않고 런타임 환경변수에서만 읽습니다. 키가 없으면 `KEY` 없이 공개 조회를 시도합니다.
