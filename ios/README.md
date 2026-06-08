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

# 기기가 잠겨 있으면 일정 시간 잠금 해제를 기다림
UNLOCK_WAIT_SECONDS=20 REMAINING_SECONDS=20 ios/scripts/verify_device_notification.sh
```

- 알림 smoke는 앱 sandbox의 `Documents/schoolhelper-notification-smoke.json` 을 복사해 `scheduled=true`, `pending=true`, `runID` 일치를 확인합니다.
- 실제 알림 배너/소리/진동 체감은 iPhone을 잠그거나 앱을 백그라운드로 보낸 뒤 사람이 확인해야 합니다.

live NEIS/BFF 데이터 smoke:

```bash
# 미사중학교 기준 실제 NEIS 급식/시간표/일정, notices BFF 응답, 첫 notice 상세 URL 로드 확인
DATE=20260528 MONTH=202605 ios/scripts/verify_live_school_data.py

# simulator에서 live 데이터가 실제 앱 화면에 렌더링되는지 확인
ios/scripts/test_live_ui.sh

# simulator에서 live 날짜 이동 제목이 갱신되는지 확인
ios/scripts/test_live_navigation_ui.sh

# simulator에서 live 운영 가정통신문이 Safari로 열리는지 확인
ios/scripts/test_live_external_link_ui.sh
```

- 기본값은 `SCHOOL_NAME=미사중학교`, `OFFICE_CODE=J10`, `SCHOOL_CODE=7692129`, `GRADE=1`, `CLASSROOM=2` 입니다.
- `NEIS_API_KEY` 는 앱에 저장하지 않고 이 스크립트에서도 환경변수로만 선택 주입합니다. 키가 없으면 iOS 앱과 동일하게 `KEY` 없이 공개 조회를 시도합니다.
- notices는 `WEB_BASE_URL` 의 `/api/notices` BFF를 호출합니다. 기본값은 `https://midschool.vercel.app/` 입니다.
- `verify_live_school_data.py` 는 기본적으로 첫 notice 상세 URL도 HTTP 2xx/3xx 및 제목 포함 여부로 확인합니다. 외부 학교 홈페이지 상태를 제외하려면 `VERIFY_NOTICE_URL=0` 을 지정합니다.
- `test_live_ui.sh` 는 `SCHOOLHELPER_REFERENCE_DATE=20260528` launch override로 홈/시간표/급식/일정 화면의 live 표시를 검증합니다.
- `test_live_navigation_ui.sh` 는 같은 기준일로 시간표 다음 날, 급식 다음 주, 일정 다음 달 제목 갱신을 검증합니다.
- `test_live_external_link_ui.sh` 는 live BFF notice의 `가정통신문 열기`가 Safari에서 `misaj-m.goegh.kr`/공지 텍스트로 열리는지 검증합니다.

위젯 simulator 패키징 smoke:

```bash
ios/scripts/test_widget_sim.sh
```

- `SchoolHelperWidget` scheme이 simulator에서 빌드되는지 확인합니다.
- 빌드된 `SchoolHelperIOS.app/PlugIns/SchoolHelperWidget.appex` 존재, WidgetKit extension point, app/widget bundle id와 위젯 탭 deep link route를 확인합니다.
- 앱/위젯 entitlements source와 `AppStorageConfig.appGroupSuiteName` 이 `group.com.lbs.shcoolhelper` 로 맞춰져 있는지도 확인합니다.
- 기본 출력은 짧게 유지하며, 전체 `xcodebuild` 로그가 필요하면 `VERBOSE=1 ios/scripts/test_widget_sim.sh` 로 실행합니다.

로컬 iOS 통합 검증:

```bash
ios/scripts/verify_ios_local_readiness.sh

# 외부 live NEIS/BFF까지 포함
RUN_LIVE_BACKEND=1 ios/scripts/verify_ios_local_readiness.sh
```

- Python/Shell 문법, SwiftPM 코어 테스트, 위젯 패키징/App Group readiness, 목표 완료 audit을 한 번에 실행합니다.
- 현재 외부/system blocker는 `ALLOW_INCOMPLETE_GOAL_AUDIT=1` 기본값으로 허용하고, 예상 밖 실패만 중단합니다.

목표 완료 준비도 감사:

```bash
ios/scripts/audit_ios_goal_readiness.py
```

- 원래 iOS 목표의 핵심 산출물, 기능 surface, 검증 스크립트, App Group profile, 남은 시스템 수동 증거를 JSON으로 출력합니다.
- 모든 항목이 완료되면 exit `0`, 아직 남은 증거가 있으면 exit `20` 입니다.
- 시스템 수동 증거는 `ios/system-evidence.template.json` 을 `ios/system-evidence.local.json` 으로 복사해 작성합니다. local 파일은 git에 올리지 않습니다.
- 수동 증거만 따로 확인하려면 `ios/scripts/validate_ios_system_evidence.py` 를 실행합니다.

App Group profile 새로고침 보조:

```bash
# 기본은 dry-run: 현재 app/widget bundle id의 local profile만 나열
ios/scripts/refresh_app_group_profiles.sh

# matching local profiles를 backup dir로 옮김
APPLY=1 ios/scripts/refresh_app_group_profiles.sh

# Apple Developer/Xcode capability 반영 후 Xcode profile 재생성까지 요청
APPLY=1 RUN_XCODE_REFRESH=1 TEAM_ID=YOUR_TEAM_ID ios/scripts/refresh_app_group_profiles.sh
```

- backup은 `ios/profile-backups.local/` 아래에 만들며 git에 올리지 않습니다.
- Apple Developer 쪽 App Group capability가 먼저 켜져 있어야 새 profile도 App Group을 포함합니다.

위젯/App Group 준비도 검증:

```bash
ios/scripts/verify_widget_app_group_readiness.sh

# 현재처럼 외부 provisioning blocker를 문서화하면서 0으로 종료해야 할 때
ALLOW_PROFILE_BLOCKED=1 ios/scripts/verify_widget_app_group_readiness.sh

# profile이 준비된 뒤 실제 iPhone full App Group signing/install까지 확인
RUN_DEVICE_BUILD=1 TEAM_ID=YOUR_TEAM_ID ios/scripts/verify_widget_app_group_readiness.sh
```

- simulator 위젯 패키징 smoke를 먼저 실행한 뒤 앱/위젯 provisioning profile의 App Group entitlement를 확인합니다.
- profile이 준비되지 않았으면 `BLOCKED_BY_PROVISIONING_PROFILE` 을 출력하고 exit `10` 으로 종료합니다.
- profile이 준비되면 `RUN_DEVICE_BUILD=1` 로 `ENTITLEMENTS_MODE=app-groups` 실기기 signing/install smoke까지 이어서 실행할 수 있습니다.

실제 iPhone UI 테스트:

```bash
# 기본: 초기 설정 학교 검색/저장 UI 테스트만 실행
TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh

# 전체 UI 테스트를 실기기에서 실행
ONLY_TESTING= TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh

# live NEIS/BFF 데이터가 실제 iPhone 앱 화면에 표시되는지 확인
LIVE_UI_TEST=1 TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh

# live 날짜 이동 제목 갱신을 실제 iPhone에서 확인
LIVE_NAVIGATION_TEST=1 TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh

# seeded 가정통신문 링크가 Safari로 전환되는지 확인
EXTERNAL_LINK_TEST=1 TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh

# live 운영 가정통신문 링크가 실제 iPhone Safari로 열리는지 확인
LIVE_EXTERNAL_LINK_TEST=1 TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh
```

- 기본 `ENTITLEMENTS_MODE=device-preview` 는 App Group entitlement를 제외하고 앱 본체 확인에 집중합니다.
- 위젯/App Group까지 검증하려면 `ios/scripts/check_app_group_profiles.py` 로 앱/위젯 profile이 모두 `OK` 인지 확인한 뒤 `ENTITLEMENTS_MODE=app-groups` 로 실행합니다.
- 실기기 UI 테스트는 `CODE_SIGNING_ALLOWED=YES` signing override가 필요하므로 `test_device_ui.sh` 를 사용합니다.
- `test_device_ui.sh` 는 기기 잠금 상태를 감지해 종료합니다. 잠금 해제를 기다리며 실행하려면 `UNLOCK_WAIT_SECONDS=120` 처럼 대기 시간을 지정합니다.
- `Timed out while enabling automation mode` 는 기본 1회 자동 재시도합니다. 필요하면 `AUTOMATION_RETRY_LIMIT=0` 으로 끌 수 있습니다.
- `APP_GROUP_PROFILE_CHECK=warn` 은 `ENTITLEMENTS_MODE=app-groups` 에서 profile precheck 실패 후에도 Xcode profile 갱신/빌드를 시도합니다.
- `LIVE_UI_TEST=1` 은 live UI 테스트 전용 Swift flag를 켜고 `testLiveSchoolDataDisplaysBackendContent` 만 실행합니다.
- `LIVE_NAVIGATION_TEST=1` 은 live UI 테스트 전용 Swift flag를 켜고 `testLiveDateNavigationUpdatesTitles` 만 실행합니다.
- `EXTERNAL_LINK_TEST=1` 은 외부 앱 전환 테스트 전용 Swift flag를 켜고 `testNoticeButtonOpensExternalSafariURL` 만 실행합니다.
- NEIS API 키는 앱에 저장하지 않고 런타임 환경변수에서만 읽습니다. 키가 없으면 `KEY` 없이 공개 조회를 시도합니다.
- 학교 검색 fallback도 학교명 공백을 제거해 `미사 중학교` 같은 입력을 처리합니다.
