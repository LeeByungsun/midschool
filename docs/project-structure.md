# 프로젝트 구조

이 저장소는 **학교도우미** 서비스를 위한 멀티플랫폼 작업공간입니다. Android, iOS, Web 클라이언트가 같은 기능 도메인(학생 설정, 시간표, 급식, 학사 일정, 타이머)을 제공하며, 플랫폼별 구현과 검증 도구는 각 폴더에 분리되어 있습니다.

> 이 문서는 현재 Git 추적 소스 기준 구조를 설명합니다. 빌드 산출물(`.gradle/`, `.build/`, `.next/`, `node_modules/`)은 구조 설명에서 제외합니다.

## 최상위 구조

```text
misSchoolApp/
├── README.md                  # 저장소 개요와 플랫폼별 시작 안내
├── android/                   # Kotlin/Gradle Android 앱
├── ios/                       # SwiftUI/Xcode iOS 앱 및 WidgetKit 위젯
├── web/                       # Next.js App Router 웹 클라이언트
├── docs/                      # 제품·운영·검증 문서
├── scripts/                   # 저장소 공통 보조 스크립트
└── .codex/                    # 로컬 에이전트/스킬 설정
```

## Android: `android/`

단일 `:app` 모듈의 Kotlin 앱입니다. XML + DataBinding UI, Hilt DI, MVVM, Retrofit 기반 NEIS 연동을 사용합니다.

```text
android/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/lbs/schoolhelper/
│       │   │   ├── data/       # 모델, NEIS/공지 원격 API, repository
│       │   │   ├── di/         # Hilt 모듈
│       │   │   ├── timer/      # 알람, 부팅 복원, 수신기
│       │   │   ├── ui/         # 화면별 ViewModel, UiState, adapter
│       │   │   ├── util/       # 공용 유틸
│       │   │   └── widget/     # App Widget provider와 설정 화면
│       │   └── res/            # XML 레이아웃, drawable, 문자열, 위젯 메타데이터
│       ├── test/               # Robolectric/JUnit 단위 테스트
│       └── androidTest/        # 계측 테스트
├── gradle/libs.versions.toml  # 의존성 버전 카탈로그
├── scripts/                   # Android 검증 보조 스크립트
└── AGENTS.md                  # Android 작업 규칙
```

주요 Activity는 루트 패키지에, 화면 상태와 로직은 `ui/<feature>/`에 둡니다. 학교·사용자 설정은 `data/repository/`, 외부 데이터 계약은 `data/remote/`에 둡니다.

## iOS: `ios/`

SwiftUI 앱, WidgetKit extension, Swift Package 기반 Core 테스트를 함께 관리합니다.

```text
ios/
├── SchoolHelper.xcodeproj/    # 앱·UI 테스트·위젯 target 및 공유 scheme
├── SchoolHelper/
│   ├── App/                   # 앱 진입점, 탭 구성, 앱 상태, Crashlytics 초기화
│   ├── Core/
│   │   ├── Models/            # 공통 도메인 모델
│   │   ├── Networking/        # NEIS와 학교 검색 클라이언트
│   │   ├── Notifications/     # 권한 및 타이머 알림
│   │   ├── Repositories/      # 학교 데이터 및 위젯 스냅샷 조합
│   │   ├── Storage/           # UserDefaults/App Group 기반 저장소
│   │   └── Views/             # 앱·위젯 공유 표시 뷰
│   ├── Features/              # Home, Setup, Timetable, Meals, Schedule, Timer, Settings
│   └── Resources/             # 에셋과 Firebase 설정 파일
├── SchoolHelperWidget/        # WidgetKit extension
├── SchoolHelperTests/         # Xcode 단위 테스트
├── SchoolHelperUITests/       # Xcode UI 테스트
├── Tests/SchoolHelperIOSCoreTests/ # SwiftPM Core 테스트
├── Package.swift              # UI 의존성을 제외한 Core 테스트 package
└── scripts/                   # simulator·실기기·App Group 검증 스크립트
```

공유 도메인/저장소/네트워크는 `Core/`, 화면과 ViewModel은 `Features/<feature>/`에 둡니다. Xcode 프로젝트 파일명은 `SchoolHelper.xcodeproj`이고, 앱 실행 scheme은 `SchoolHelperIOS`입니다.

## Web: `web/`

Next.js App Router 기반 웹 앱입니다. 브라우저는 외부 NEIS API를 직접 호출하지 않고 `app/api/` route handler를 통해 서버 측 BFF 경계에 접근합니다.

```text
web/
├── app/
│   ├── api/                  # schools, timetable, meals, schedule, notices BFF route handlers
│   ├── page.tsx              # 홈 대시보드
│   ├── setup/ settings/      # 학생 초기 설정과 변경
│   ├── timetable/ meals/ schedule/ timer/ # 기능별 페이지
│   ├── layout.tsx
│   └── globals.css
├── components/               # 페이지 조합, 카드, 공통 데이터 상태 UI
├── hooks/                    # hydration, 학생 설정, 타이머 상태 구독
├── lib/
│   ├── neis/                 # NEIS 타입, 매퍼, 서버 클라이언트
│   ├── notices/              # 가정통신문 provider 판별·수집·오류 처리
│   ├── storage/              # 브라우저 저장소, 캐시, 사용자 설정, 타이머 스냅샷
│   └── *.ts                  # 대시보드, 날짜, 화면용 도메인 유틸
├── scripts/                  # node:test 기반 회귀 테스트
├── public/
├── package.json
└── AGENTS.md                 # Web 작업 규칙
```

서버 데이터와 브라우저 로컬 상태를 분리합니다. `lib/storage/`는 설정·캐시·타이머 영속화를 담당하고, `hooks/`는 클라이언트 UI 구독 경계를 제공합니다.

## 문서와 검증의 기준

- 전체 기능/정책: `project_specification.md`
- Android 실행/구조: `../android/README.md`, `android-studio-setup.md`
- iOS 실행/검증: `../ios/README.md`, `ios-project-specification.md`, `ios-runtime-verification.md`
- Web 실행/구조: `../web/README.md`

대표 검증 명령은 각 플랫폼 README를 우선합니다. 코드 구조가 바뀌면 이 문서와 해당 플랫폼 README를 함께 갱신합니다.
