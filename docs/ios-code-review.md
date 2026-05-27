# iOS App 코드 분석 및 후속 조치 기록

> 상태 기준일: 2026-05-27

## 상태 요약

초기 리뷰에서 지적된 5개 항목은 **현재 코드 기준 모두 반영 완료**입니다.

- 1. TimerViewModel retain 가능성 → 해결됨
- 2. 주간 급식 순차 호출 → 해결됨
- 3. 위젯 스냅샷 순차 호출 → 해결됨
- 4. WidgetKit container background 누락 → 해결됨
- 5. 알림 권한 요청 메인 스레드 문제 → 해결됨

남아 있는 큰 항목은 코드 품질 이슈가 아니라:

- 홈 화면 위젯 실제 배치/탭 동작 검증
- 시스템 딥링크 확인 다이얼로그 이후 최종 전환
- 실기기 알림/권한 UX 검증

같은 **시스템 UI / 실기기 검증 과제** 입니다.

이 문서는 `ios` 디렉토리 내의 Swift 코드베이스를 분석하여 발견된 주요 문제점(메모리 누수, 성능 병목, iOS 버전 호환성 오류, 스레드 안전성 위반)을 정리하고, 각 문제점에 대한 원인 및 개선 방안을 제공합니다.

---

## 목차
1. [TimerViewModel 내 순환 참조로 인한 메모리 누수](#1-timerviewmodel-내-순환-참조로-인한-메모리-누수)
2. [주간 식단 조회 시 API 순차 호출로 인한 성능 저하](#2-주간-식단-조회-시-api-순차-호출로-인한-성능-저하)
3. [위젯 스냅샷 로더 내 순차적 비동기 처리로 인한 위젯 로딩 지연](#3-위젯-스냅샷-로더-내-순차적-비동기-처리로-인한-위젯-로딩-지연)
4. [iOS 17 대응을 위한 위젯 컨테이너 배경 API 누락](#4-ios-17-대응을-위한-위젯-컨테이너-배경-api-누락)
5. [알림 권한 요청 시 백그라운드 스레드 호출로 인한 스레드 안전성 위반](#5-알림-권한-요청-시-백그라운드-스레드-호출로-인한-스레드-안전성-위반)

---

### 1. TimerViewModel 내 순환 참조로 인한 메모리 누수

**현재 상태: 해결됨**

반영 방향:
- `TimerViewModel.deinit` 에서 `countdownTask?.cancel()`
- countdown loop 내부에서 매 주기 `weak self` 재확인

*   **대상 파일**: `ios/SchoolHelperIOS/Features/Timer/TimerViewModel.swift` ([TimerViewModel.swift:L133-L148](file:///Users/byungsunlee/Project/misSchoolApp/ios/SchoolHelperIOS/Features/Timer/TimerViewModel.swift#L133-L148))
*   **코드 영역**:
    ```swift
    private func startCountdownLoop() {
        countdownTask?.cancel()
        countdownTask = Task { [weak self] in
            guard let self else { return } // ⚠️ Task 내부에서 self를 즉시 강한 참조로 바인딩
            while !Task.isCancelled {
                await self.sleep(1_000_000_000)
                if Task.isCancelled { break }
                await MainActor.run {
                    self.syncWithCurrentTime()
                }
                if await MainActor.run(body: { !self.state.isRunning }) {
                    break
                }
            }
        }
    }
    ```

#### 🔍 원인 분석 (Root Cause)
`Task`를 생성하면서 `[weak self]`를 통해 약한 참조로 캡처했지만, 비동기 작업 블록이 시작되자마자 `guard let self else { return }`를 통해 `self`를 강한 참조로 언래핑(re-binding)하였습니다. 이 때문에 태스크가 실행 중인 동안(`while !Task.isCancelled` 무한 루프가 도는 동안) `TimerViewModel` 인스턴스에 대한 강한 참조가 계속 유지됩니다.

#### ⚠️ 영향도 및 문제점 (Impact)
SwiftUI 뷰가 화면에서 사라지면서 `TimerViewModel` 인스턴스를 소멸시키려 해도, 무한 루프 태스크가 `self`를 잡고 있어 레퍼런스 카운트(ARC)가 0이 되지 않아 `deinit`이 호출되지 않습니다. 결과적으로 메모리 누수(Memory Leak)가 발생하며, 백그라운드에서 불필요하게 타이머 동기화 태스크가 지속되어 CPU 자원을 낭비합니다.

#### 💡 해결 방안 (Recommendation)
태스크 블록 도입부에서 즉시 강한 참조로 묶는 대신, **루프 매 주기마다 `weak self` 상태를 검사**하여 `self`가 메모리 해제되면 루프를 안전하게 이탈하도록 수정해야 합니다.

```swift
private func startCountdownLoop() {
    countdownTask?.cancel()
    countdownTask = Task { [weak self] in
        while !Task.isCancelled {
            // 루프 주기마다 weak self를 확인하고 해제 시 루프 즉시 이탈
            guard let self = self else { break } 
            
            await self.sleep(1_000_000_000)
            if Task.isCancelled { break }
            
            await MainActor.run {
                self.syncWithCurrentTime()
            }
            
            let isRunning = await MainActor.run { self.state.isRunning }
            if !isRunning {
                break
            }
        }
    }
}
```

---

### 2. 주간 식단 조회 시 API 순차 호출로 인한 성능 저하

**현재 상태: 해결됨**

반영 방향:
- `DefaultSchoolRepository.fetchWeekMeals` 를 `withThrowingTaskGroup` 기반 병렬 호출로 전환
- 테스트는 병렬 실행 순서를 허용하도록 정리

*   **대상 파일**: `ios/SchoolHelperIOS/Core/Repositories/SchoolRepository.swift` ([SchoolRepository.swift:L149-L164](file:///Users/byungsunlee/Project/misSchoolApp/ios/SchoolHelperIOS/Core/Repositories/SchoolRepository.swift#L149-L164))
*   **코드 영역**:
    ```swift
    func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] {
        let dates = weekDates(startingAt: weekStart)
        var meals: [MealInfo] = []

        for date in dates {
            let dayMeals = try await fetchTodayMeals(for: profile, date: date) // ⚠️ 5일의 데이터를 순차적으로 동기 대기(try await)
            meals.append(contentsOf: dayMeals)
        }

        return meals.sorted { ... }
    }
    ```

#### 🔍 원인 분석 (Root Cause)
`dates` 배열(월~금요일 5일분)을 순회하며 매 루프마다 `try await fetchTodayMeals(...)`를 실행하고 있습니다. 즉, 이전 날짜의 식단 요청 API 처리가 완료되어야만 다음 날짜의 식단을 호출하는 구조로 작동합니다.

#### ⚠️ 영향도 및 문제점 (Impact)
단일 API 요청에 200ms가 소요된다고 가정할 때, 5회 순차 호출을 진행하면 최소 1초 이상의 대기 시간이 발생합니다. 네트워크 레이턴시가 조금만 지연되어도 주간 식단 탭 전환 속도가 현저히 저하됩니다.

#### 💡 해결 방안 (Recommendation)
Swift Concurrency의 `withThrowingTaskGroup`을 활용하여 **5일 치 식단 정보를 병렬로 호출**하고, 각 태스크의 결과를 합치는 방식으로 대폭 개선해야 합니다.

```swift
func fetchWeekMeals(for profile: StudentProfile, weekStart: Date) async throws -> [MealInfo] {
    let dates = weekDates(startingAt: weekStart)
    
    return try await withThrowingTaskGroup(of: [MealInfo].self) { group in
        for date in dates {
            group.addTask {
                try await self.fetchTodayMeals(for: profile, date: date)
            }
        }
        
        var meals: [MealInfo] = []
        for try await dayMeals in group {
            meals.append(contentsOf: dayMeals)
        }
        
        return meals.sorted {
            if $0.date == $1.date {
                return $0.mealType < $1.mealType
            }
            return $0.date < $1.date
        }
    }
}
```

---

### 3. 위젯 스냅샷 로더 내 순차적 비동기 처리로 인한 위젯 로딩 지연

**현재 상태: 해결됨**

반영 방향:
- `HomeWidgetSnapshotLoader` 에서 `async let` 로 오늘/내일 시간표 동시 조회

*   **대상 파일**: `ios/SchoolHelperIOS/Core/Repositories/HomeWidgetSnapshotLoader.swift` ([HomeWidgetSnapshotLoader.swift:L47-L51](file:///Users/byungsunlee/Project/misSchoolApp/ios/SchoolHelperIOS/Core/Repositories/HomeWidgetSnapshotLoader.swift#L47-L51))
*   **코드 영역**:
    ```swift
    let todayLines = (try? await repository.fetchTimetable(for: profile, date: now)).orEmpty
    let tomorrowDate = calendar.date(byAdding: .day, value: 1, to: now) ?? now
    let tomorrowLines = shouldShowTomorrow
        ? (try? await repository.fetchTimetable(for: profile, date: tomorrowDate)).orEmpty // ⚠️ 내일 시간표와 오늘 시간표를 연이어 순차 비동기 호출
        : []
    ```

#### 🔍 원인 분석 (Root Cause)
위젯의 새로고침이나 타임라인 생성을 담당하는 `HomeWidgetSnapshotLoader` 내에서 오늘 시간표(`todayLines`) 호출이 완전히 끝난 후, `shouldShowTomorrow`가 활성화되어 있으면 내일 시간표(`tomorrowLines`)를 추가로 동기 대기(`await`)하고 있습니다.

#### ⚠️ 영향도 및 문제점 (Impact)
iOS 위젯 확장 프로그램(Widget Extension)은 엄격한 메모리 제한과 매우 짧은 실행 시간(Execution Limit)을 강제합니다. 위젯 로더 내부에서 직렬로 2회 이상의 네트워크 요청을 완료해야만 스냅샷을 렌더링할 수 있어, 위젯 로딩이 지연되고 위젯 시스템에 의해 타임라인 업데이트가 강제로 스킵되거나 위젯이 먹통이 될 우려가 있습니다.

#### 💡 해결 방안 (Recommendation)
`async let`을 사용하여 오늘과 내일의 시간표 API를 **병렬(concurrent)로 동시에 패치**하면 응답 성능을 극대화할 수 있습니다.

```swift
async let todayFetch = repository.fetchTimetable(for: profile, date: now)
let tomorrowDate = calendar.date(byAdding: .day, value: 1, to: now) ?? now
async let tomorrowFetch: [TimetableItem]? = shouldShowTomorrow
    ? try? await repository.fetchTimetable(for: profile, date: tomorrowDate)
    : nil

let todayLines = (try? await todayFetch).orEmpty
let tomorrowLines = shouldShowTomorrow ? (await tomorrowFetch).orEmpty : []
```

---

### 4. iOS 17 대응을 위한 위젯 컨테이너 배경 API 누락

**현재 상태: 해결됨**

반영 방향:
- `SchoolHelperWidgetEntryView` 에 `.containerBackground(for: .widget)` 추가

*   **대상 파일**: `ios/SchoolHelperWidget/SchoolHelperWidget.swift` ([SchoolHelperWidget.swift:L101-L117](file:///Users/byungsunlee/Project/misSchoolApp/ios/SchoolHelperWidget/SchoolHelperWidget.swift#L101-L117))
*   **코드 영역**:
    ```swift
    struct SchoolHelperWidgetEntryView: View {
        var entry: SchoolHelperWidgetEntry
    
        var body: some View {
            VStack(alignment: .leading, spacing: 8) {
                // ... 위젯 뷰 본문
            }
            .padding()
            .widgetURL(...)
            // ⚠️ iOS 17 이상에서 필수인 .containerBackground(for: .widget) 설정이 유실됨
        }
    }
    ```

#### 🔍 원인 분석 (Root Cause)
Apple은 iOS 17+ 및 Xcode 15+ 환경에서 모든 위젯의 루트 뷰에 배경화면 레이어인 `.containerBackground` 설정(예: `.containerBackground(.background, for: .widget)`)을 명시할 것을 규정하고 있습니다. 해당 위젯 뷰에는 해당 모디파이어가 선언되어 있지 않습니다.

#### ⚠️ 영향도 및 문제점 (Impact)
iOS 17 이상의 기기나 시뮬레이터에서 위젯이 로드될 때, 위젯 자체가 까맣게 렌더링되거나 시스템에 의해 컴파일 타임 에러 혹은 런타임 레이아웃 오류를 발생시킵니다.

#### 💡 해결 방안 (Recommendation)
iOS 17 지원을 위해 `#available` 조건문을 사용해 배경 컨테이너를 지정하거나, 하위 버전 호환성을 고려한 헬퍼 모디파이어를 제공해야 합니다.

*   **SwiftUI View 수정(예시)**:
    ```swift
    struct SchoolHelperWidgetEntryView: View {
        var entry: SchoolHelperWidgetEntry
    
        var body: some View {
            VStack(alignment: .leading, spacing: 8) {
                // 위젯 레이아웃 ...
            }
            .padding()
            .widgetURL(...)
            .applyContainerBackground() // iOS 17 대응 헬퍼 적용
        }
    }
    
    // 호환성 헬퍼 Extension 정의
    extension View {
        func applyContainerBackground() -> some View {
            if #available(iOS 17.0, *) {
                return self.containerBackground(.background, for: .widget)
            } else {
                return self // iOS 16 이하의 경우 기존 배경 스타일 유지
            }
        }
    }
    ```

---

### 5. 알림 권한 요청 시 백그라운드 스레드 호출로 인한 스레드 안전성 위반

**현재 상태: 해결됨**

반영 방향:
- `TimerNotificationScheduler.requestAuthorizationIfNeeded()` 에서
  `DispatchQueue.main.async` 로 메인 스레드 복귀 후 권한 요청

*   **대상 파일**: `ios/SchoolHelperIOS/Core/Notifications/TimerNotificationScheduler.swift` ([TimerNotificationScheduler.swift:L17-L22](file:///Users/byungsunlee/Project/misSchoolApp/ios/SchoolHelperIOS/Core/Notifications/TimerNotificationScheduler.swift#L17-L22))
*   **코드 영역**:
    ```swift
    func requestAuthorizationIfNeeded() {
        center.getNotificationSettings { [center] settings in
            guard settings.authorizationStatus == .notDetermined else { return }
            // ⚠️ 백그라운드 콜백 스레드 내에서 시스템 얼럿 대화상자를 여는 requestAuthorization 호출
            center.requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
        }
    }
    ```

#### 🔍 원인 분석 (Root Cause)
`UNUserNotificationCenter.shared().getNotificationSettings` 메서드의 완료 핸들러(Completion Handler)는 Main Thread가 아닌 Apple Notification 프레임워크 전용 백그라운드 큐(Serial Queue)에서 실행됩니다. 이 백그라운드 스레드 스코프 안에서 사용자 모달/얼럿 대화상자를 띄우는 동작인 `center.requestAuthorization`을 메인 스레드 조치 없이 연이어 실행하였습니다.

#### ⚠️ 영향도 및 문제점 (Impact)
UI 또는 대화상자와 상호작용하는 모든 API 호출은 Main Thread에서 실행되어야 합니다. 그렇지 않으면 Xcode 내 Main Thread Checker가 실시간 경고를 뱉고 기기 디버깅 중 불안정함을 가져오며, 사용자 기기에서 알림 권한 동의 팝업 창이 늦게 뜨거나 심한 경우 앱이 멈추거나 튕길(Crash) 수 있습니다.

#### 💡 해결 방안 (Recommendation)
시스템 권한 팝업을 요청하는 시점은 `DispatchQueue.main.async`를 사용하여 **메인 스레드로 콘텍스트를 복귀**시킨 뒤 실행해야 합니다.

```swift
func requestAuthorizationIfNeeded() {
    center.getNotificationSettings { [center] settings in
        guard settings.authorizationStatus == .notDetermined else { return }
        
        // 메인 스레드 전환 후 권한 요청창 실행
        DispatchQueue.main.async {
            center.requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
        }
    }
}
```

---
원본 리뷰 작성일: 2026년 5월 26일  
후속 상태 갱신일: 2026년 5월 27일  
작성자: Antigravity AI Coding Assistant
