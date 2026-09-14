# Android Firebase 관측 운영 문서

기준일: 2026-09-14 · 프로젝트: `schoolhelper-3c115` · Android 앱: `com.lbs.schoolhelper`, `com.lbs.schoolhelper.qa`

## 데이터 흐름

`Application`에서 저장된 분석/오류 진단 선택을 읽고 `TelemetryReporter`를 초기화한다. `TelemetryReporter`는 허용 목록·학교 코드 정규화·오류 코드 정제·중복 제한을 담당하며, `FirebaseTelemetrySink`만 Firebase SDK를 호출한다. SDK가 없거나 SDK 호출이 실패해도 학교 기능은 계속 동작한다.

- Analytics와 Crashlytics는 **독립 선택**이다.
- 초기 Manifest 수집은 꺼져 있다. 앱 설정에서 선택한 뒤에만 해당 채널을 켠다.
- 광고 ID·광고 저장소·개인 맞춤형 광고 신호는 사용하지 않는다.
- 학교 이름, 학년, 반, 검색어, URL, API 키, 응답 원문, 이름·계정 정보는 telemetry payload에 넣지 않는다.
- 학교 문맥은 `office_code`, `school_code`, `school_kind`, `setup_complete`만 사용한다. 유효하지 않은 코드는 빈 값으로 바꾼다.
- `CancellationException`은 실패 이벤트/비치명 예외가 아니다. 정상 빈 결과는 `empty`다.

## 이벤트 사전

| 이벤트 | 파라미터 | 의미 |
|---|---|---|
| `app_open_context` | `entry_point`, `device_manufacturer`, `device_model`, `os_sdk`, `app_version` | 런처/위젯/알림을 통한 포그라운드 진입과 비식별 단말·앱 환경 |
| `school_selected`, `school_changed` | 학교 문맥 | 선택 저장 완료 후 기록 |
| `feature_view` | `feature` | 화면의 실제 resume |
| `data_load_result` | `feature`, `outcome`, `source`, `duration_ms`, `error_code`, `api` + 학교 문맥 | 네트워크·캐시·검증 결과 |
| `timer_action` | `action`, `duration_ms` | 시작·일시정지·초기화·프리셋·완료 |
| `widget_action` | `action`, `widget_type` | 추가·삭제·설정·새로고침·열기. `enable`은 첫 위젯이 추가된 시점, `open`은 실제 위젯 탭으로 새 포그라운드 세션이 시작된 시점만 기록 |

Analytics Custom Definitions에 보고서용 `feature`, `outcome`, `source`, `error_code`, `school_kind`, `api`를 등록한다. 학교 코드는 필요한 운영 분석 범위와 보유 기간을 개인정보 검토 후 결정한다.

## QA 확인 절차

1. `android/app/google-services.json`에 `com.lbs.schoolhelper.qa` 클라이언트가 포함되어 있는지 확인하고 `assembleQa`로 QA 변형을 만든다. QA 앱 ID는 운영 앱과 분리되며, 일반 debug 빌드는 Firebase 초기화 공급자를 제외해 telemetry를 전송하지 않는다. 가능하면 Firebase 프로젝트/GA4 속성도 운영과 분리한다.
2. `adb shell setprop debug.firebase.analytics.app com.lbs.schoolhelper.qa`로 Analytics 디버그 모드를 켠다. Firebase Analytics DebugView에서 `app_open_context`, `school_selected`, `feature_view`, `data_load_result`를 각각 한 번씩 확인한다. SDK 이벤트는 대시보드보다 DebugView/logcat이 먼저 보인다. 검증 후 `adb shell setprop debug.firebase.analytics.app .none.`으로 해제한다.
3. Crashlytics에서 진단 동의를 켠 뒤 QA에서 허용된 오류의 비치명 이슈와 `school_code`, `feature`, `error_code` 문맥을 확인한다. 테스트 크래시는 운영 앱에서 실행하지 않는다.
   - 재현 가능한 QA 비치명 진단: `adb shell am start -n com.lbs.schoolhelper.qa/com.lbs.schoolhelper.telemetry.QaTelemetryProbeActivity`
   - 이 Activity는 QA 소스셋에만 포함되고, 설정에서 허용한 채널만 전송한다. `adb shell setprop log.tag.FirebaseCrashlytics DEBUG`와 `adb logcat -s FirebaseCrashlytics`에서 업로드 완료 또는 HTTP 204를 확인하고, 필요하면 앱을 종료·재실행한다.
4. Analytics만 끄고 오류 진단만 켠 상태, 그 반대 상태를 각각 확인한다. 설정을 끄면 새 명시적 전송을 중단한다. Crashlytics 자동 수집 변경은 Firebase 정책상 다음 앱 실행부터 적용될 수 있으므로, 화면 안내도 그 의미로 표시한다. 이미 수신된 Firebase 기록이 자동 삭제된다고 안내하지 않는다.
5. 학교 A로 조회를 시작한 뒤 B로 변경하는 경합 테스트에서 조회 결과/오류의 문맥이 요청 시작 시점의 학교와 일치하는지 확인한다.
6. QA 단말이 없으면 코드/단위 테스트 완료로 대체하지 말고 이 단계의 상태를 `미검증`으로 기록한다.

## 실기기·콘솔 검증 결과

2026-09-14에 `com.lbs.schoolhelper.qa`를 Samsung SM-A346N(Android 16/API 36)에 설치해 다음을 확인했다.

- 최초 실행에서 두 수집 채널이 모두 꺼져 있었고, Analytics OFF/오류 진단 ON 및 Analytics ON/오류 진단 OFF 조합이 각각 독립적으로 적용됐다. 마지막에는 두 채널을 다시 켰다.
- Analytics DebugView에서 `app_open_context`, `feature_view`, `data_load_result`, `timer_action`이 수신됐다. 앱 버전 `1.0-qa`, Samsung 기종/API 수준과 허용된 학교 코드 문맥만 포함됐으며 학교 이름·학년·반·검색어는 포함되지 않았다.
- 실제 온라인 조회의 네트워크 성공 및 오프라인 재실행의 캐시 성공/네트워크 실패가 구분되어 수신됐다. 예상된 오프라인 `IOException`은 Crashlytics 비치명 오류로 보내지 않았다.
- QA 전용 비치명 진단을 실행한 뒤 Crashlytics 전송 HTTP 200과 콘솔 이슈를 확인했다. 이슈의 커스텀 키는 `api`, 앱/기기 환경, 기능, 결과/출처/오류 코드, 허용된 학교 문맥으로 제한됐다.
- 타이머 시작·일시정지·초기화 이벤트와 앱 프로세스 종료 후 진행 상태 복원을 확인했다.
- QA 앱을 `widget` 진입점으로 실행해 `app_open_context(entry_point=widget)`와 `widget_action(action=open)`이 각각 한 번씩 수신되는 것을 확인했다. 위젯 렌더링·갱신은 열기 이벤트를 만들지 않는다.

QA 앱은 운영 앱과 패키지/Firebase App ID가 다르지만 현재 같은 Firebase 프로젝트를 사용한다. 보고서에서는 `environment=qa`로 반드시 제외하며, 공개 출시 전 별도 Firebase 프로젝트/GA4 속성 분리가 필요한지 운영 정책으로 결정한다.

## 장애 대응

Crashlytics 알림은 P0 크래시·ANR 중심으로 담당자에게 연결한다. `data_load_result` 실패율은 기능·교육청·학교 종류·앱 버전으로 분해하고, 특정 학교만 실패하면 NEIS/학교 홈페이지 지원 범위와 데이터 변경을 먼저 확인한다. 네트워크 장애는 일반 오류로 분류하며 사용자 입력·API 키를 수집하지 않는다.

## 남은 출시 게이트

- 설정 화면 개인정보처리방침 링크와 Play Console Data safety 내용의 법무/운영 검토
- Analytics Custom Definitions, Crashlytics 알림 담당자와 보유 기간 설정
- 학교 변경 경합, 접근성·회전, 삼성 위젯, 재부팅, 알림 거부/완료 경로의 추가 실기기 검증
- 비공개 테스트와 Play Console 출시 메타데이터
