# 플랫폼별 현재 미비점 및 추가 할 일

작성일: 2026-06-08 · Android 항목 코드 대조: 2026-09-29 (`ae20ff6`). Web/iOS 상태와 아래 과거 검증 명령 결과는 이번에 재검증하지 않았다.

이 문서는 현재 저장소 기준으로 Web, Android, iOS의 남은 미비점과 후속 작업을 정리한 목록입니다.
검증은 로컬에서 가능한 범위의 lint, typecheck, test, readiness script 결과를 기준으로 했습니다.

## 전체 요약

- Web
  - 핵심 화면과 BFF, 테스트는 동작합니다.
  - 접근성, 반응형 완성도, PWA, 브라우저 알림 fallback, 공통 도메인 정리가 남았습니다.
- Android
  - 앱 기능과 테스트/린트 실행은 성공했습니다.
  - 2026-09-15 QA 실기기에서 신규 설정·학교 변경 후 학교명/급식/시간표 복원을 확인했지만 일정·위젯은 미검증입니다.
  - 출시 빌드 가정통신문 조회·외부 링크, SSO 학교 대응, 위젯·재부팅·타이머 알림, 개인정보/Play/Firebase 운영 검토가 남았습니다.
- iOS
  - readiness audit 기준 구현/검증 목표는 완료 상태입니다.
  - 다만 일부 문서가 과거의 “수동 검증 필요” 상태를 아직 담고 있어 문서 정합성 갱신이 필요합니다.

## Web

### 현재 구현 상태

- Next.js App Router 기반 화면이 구성되어 있습니다.
  - 홈, 초기 설정, 설정, 시간표, 학사일정, 급식, 타이머 화면
- NEIS 조회와 학교 검색을 위한 server-side API route가 있습니다.
- 가정통신문 BFF route가 있고 Android도 이 contract를 소비하도록 맞춰져 있습니다.
- 로컬 검증 기준 lint, typecheck, unit/integration test가 통과했습니다.

### 미비점

- 접근성 점검이 완료되지 않았습니다.
  - 설정 폼 label/description 연결
  - 키보드 탐색과 focus 이동
  - 타이머 조작부의 touch target과 스크린리더 문구
- 반응형 품질 검증이 부족합니다.
  - tablet/desktop에서 정보 밀도와 긴 문구 표시 확인 필요
  - 일정/급식 항목이 많을 때 레이아웃 깨짐 여부 확인 필요
- PWA 지원이 아직 명확하지 않습니다.
  - manifest, service worker, install UX, offline shell 전략이 필요합니다.
- 브라우저 알림 fallback UX가 더 필요합니다.
  - 권한 거부, 미지원 브라우저, 백그라운드 제한 상황에서 화면 내 대체 알림을 정리해야 합니다.
- 테스트 실행 중 Node ESM 경고가 반복됩니다.
  - 현재 실패는 아니지만 `package.json`의 module type 또는 테스트 설정 정리가 필요합니다.
- 가정통신문 provider coverage가 제한적입니다.
  - 일부 학교 홈페이지 구조와 SSO 기반 공지 경로는 unsupported/fallback UX를 더 다듬어야 합니다.

### 추가 할 일

- 접근성 audit checklist를 만들고 주요 화면별 keyboard/screen-reader smoke test를 기록합니다.
- 모바일, tablet, desktop viewport별 screenshot 기반 smoke test를 추가합니다.
- PWA 범위를 결정합니다.
  - 최소 범위: manifest + app icon + offline fallback page
  - 확장 범위: service worker cache 전략 + install prompt UX
- notification permission denied/unsupported 상태의 UI copy와 fallback 동작을 고정합니다.
- 가정통신문 unsupported school 상태의 문구와 재시도/외부 열기 경로를 통일합니다.
- Node ESM 경고를 제거할지, 현재 테스트 설정을 유지할지 결정하고 문서화합니다.

## Android

### 현재 구현 상태

- Kotlin, XML/DataBinding, Hilt, MVVM 구조로 앱이 구성되어 있습니다.
- 학교 선택/저장, 시간표, 급식, 학사일정, 타이머, 알림/진동, 홈 위젯 기능이 구현되어 있습니다.
- 홈 가정통신문 preview는 web BFF를 통해 데이터를 받는 구조입니다.
- `WEB_BASE_URL`은 `local.properties`에서 빌드에 주입되며 기본값은 빈 문자열입니다. 실제 출시 값과 동작을 확인해야 합니다.
- Firebase Analytics/Crashlytics는 독립 선택 동의이며 기본 수집이 꺼져 있습니다. 일반 debug 변형은 전송하지 않습니다.
- 아래 단위 테스트·lint 성공 기록은 2026-06-08 확인 결과이며, 현재 빌드의 통과 판정은 아닙니다.

### 미비점

- production 가정통신문 동작은 실제 기기/에뮬레이터에서 추가 확인이 필요합니다.
  - 출시 빌드에 주입한 `WEB_BASE_URL` 경로
  - unsupported school fallback 문구
  - 외부 링크 열기 동작
- 대구교육청 계열 등 SSO/특수 홈페이지 경로는 아직 조사 대상입니다.
- lint warning이 남아 있습니다.
  - `UseKtx` warning
  - 홈 위젯 장식용 ImageView의 accessibility warning
- 일부 UX는 자동 테스트보다 수동 smoke test가 더 적합합니다.
  - 학교 변경 후 급식·시간표·일정·위젯 잔류 데이터 확인(학교명/급식/시간표 복원은 2026-09-15 QA 기기에서 확인)
  - 알림 권한/진동 정책
  - 위젯 갱신과 탭 동작
- 공개 전 앱 내 개인정보처리방침 링크를 구현하고, Play Data safety, Firebase 보유 기간·알림 담당자를 검토해야 합니다. [Android 출시 준비 기록](android-release-readiness.md) 참조.

### 추가 할 일

- 출시 빌드의 가정통신문 조회·외부 링크·미지원 학교 안내를 수동 확인하고 결과를 문서에 남깁니다.
- `*.dge.ms.kr` 등 SSO 기반 학교 공지의 지원 가능 여부를 조사합니다.
- unsupported school fallback UX를 Web/iOS와 같은 문구 체계로 맞춥니다.
- lint warning을 별도 cleanup으로 줄입니다.
  - 장식용 widget ImageView는 접근성 트리에서 제외하거나 설명을 추가합니다.
  - `UseKtx` warning은 기존 패턴을 유지할지 KTX 방식으로 정리할지 결정합니다.
- [Android 출시 준비 체크리스트](android-release-readiness.md)의 미검증 실기기 경로를 완료합니다.

## iOS

### 현재 구현 상태

- SwiftUI 앱, Widget Extension, tests, readiness scripts가 구성되어 있습니다.
- readiness audit script 기준 iOS 목표는 complete 상태입니다.
- 로컬 evidence 기준 widget 배치/표시/탭, 알림 banner/sound/vibration 확인 기록이 있습니다.

### 미비점

- 일부 문서가 현재 evidence와 맞지 않습니다.
  - 과거 문서에는 “수동 검증 필요” 표현이 남아 있습니다.
  - 최신 audit script는 local evidence를 읽고 complete를 반환합니다.
- iOS complete 판정은 local-only evidence에 의존합니다.
  - `ios/system-evidence.local.json`은 추적 파일이 아니므로 다른 환경에서는 재검증이 필요합니다.
- App Store/TestFlight 배포 준비는 별도 검증 대상입니다.
  - 현재 readiness는 구현/로컬 검증 중심입니다.
  - signing, archive, TestFlight, privacy manifest, release metadata는 별도 release checklist가 필요합니다.
- live NEIS 데이터는 날짜에 따라 빈 응답이 있을 수 있습니다.
  - 테스트 스크립트의 fallback 날짜 전략과 실제 사용자 copy가 어긋나지 않는지 확인이 필요합니다.

### 추가 할 일

- `docs/ios-parity-audit.md`, `docs/ios-runtime-verification.md`의 오래된 상태 문구를 현재 evidence 기준으로 갱신합니다.
- local evidence 파일의 역할을 문서화합니다.
  - 저장소 추적 대상이 아닌 검증 증거
  - 새 환경에서 다시 생성해야 하는 증거
- release checklist를 별도 문서로 분리합니다.
  - signing/archive/TestFlight
  - widget entitlement/app group
  - notification permission copy
  - privacy/export compliance
- release 전 `ios/scripts/verify_ios_local_readiness.sh`와 readiness audit을 다시 실행합니다.

## 공통 후속 작업

- Web, Android, iOS의 공통 도메인 모델을 정렬합니다.
  - 학교, 급식, 일정, 시간표, 타이머, 가정통신문 상태값
- unsupported school/fallback UX를 플랫폼별로 같은 의미 체계로 맞춥니다.
- 접근성 기준을 공통 체크리스트로 만듭니다.
- 릴리스 준비 상태와 구현 완료 상태를 문서에서 분리합니다.
- 플랫폼별 검증 증거를 한 곳에서 찾을 수 있도록 문서 링크를 정리합니다.

## 2026-06-08 확인에 사용한 검증(역사 기록)

- Web
  - `cd web && npm run lint && npm run typecheck && npm test`
  - 결과: 통과, 테스트 48개 통과
- Android
  - `cd android && ./gradlew testDebugUnitTest lintDebug`
  - 결과: BUILD SUCCESSFUL, lint warning 62개
- iOS
  - `ios/scripts/audit_ios_goal_readiness.py`
  - 결과: complete
  - 주의: local-only evidence 파일에 의존
