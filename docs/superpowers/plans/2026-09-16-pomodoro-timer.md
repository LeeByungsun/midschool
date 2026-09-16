# 포모도로 타이머 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 집중·짧은 휴식·긴 휴식과 1~4회 라운드를 지원하는 사용자 설정형 포모도로 타이머를 구현한다.

**Architecture:** 기존 `TimerViewModel`의 단일 타이머를 `PomodoroPhase`와 `PomodoroSettings` 기반 상태 머신으로 확장한다. 설정과 진행 상태는 기존 `PreferencesRepository`를 통해 저장하고, UI는 기존 타이머 카드와 설정 화면을 확장한다. 완료 신호는 현재의 로컬 소리/진동 경로를 재사용한다.

**Tech Stack:** Kotlin, Android ViewModel/StateFlow, CountDownTimer, SharedPreferences repository, XML layouts, JUnit/Robolectric.

**Spec:** `docs/superpowers/specs/2026-09-16-pomodoro-timer-design.md`

## Global Constraints

- 라운드 수는 1~4회로 제한한다.
- 각 시간은 1~120분으로 제한하고 기본값은 25/5/15분이다.
- 구간 완료 후 자동 시작하지 않고 수동 시작 버튼을 표시한다.
- QA 빌드에서는 모든 시간이 1분으로 축약한다.
- 타이머 완료 시 푸시 알림을 만들지 않고 `TimerCompletionAlert`만 호출한다.

### Task 1: 포모도로 도메인 모델과 저장 계약

**Files:**
- Create: `android/app/src/main/java/com/lbs/schoolhelper/ui/timer/PomodoroModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/data/repository/PreferencesRepository.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/data/repository/PreferencesRepositoryImpl.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/data/repository/UserPreferencesStore.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/UserPreferences.kt`
- Test: `android/app/src/test/java/com/lbs/schoolhelper/ui/timer/PomodoroModelTest.kt`

**Interfaces:**
- `PomodoroPhase { FOCUS, SHORT_BREAK, LONG_BREAK }`
- `PomodoroSettings(focusMinutes: Int = 25, shortBreakMinutes: Int = 5, longBreakMinutes: Int = 15, rounds: Int = 4)` with `normalized()`.
- `PomodoroProgress(phase, completedRounds, isRunning, targetAtMillis)`.
- Repository methods `getPomodoroSettings/savePomodoroSettings`, `getPomodoroProgress/savePomodoroProgress/clearPomodoroProgress`.

- [ ] Write tests for normalization to 1..120 minutes, rounds to 1..4, defaults, and phase/round persistence.
- [ ] Run the focused tests and confirm they fail before implementation.
- [ ] Implement model normalization and SharedPreferences serialization with named keys and default fallback.
- [ ] Run focused tests until passing.
- [ ] Commit `feat(timer): add pomodoro settings and progress model`.

### Task 2: Timer state machine

**Files:**
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/timer/TimerUiState.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/timer/TimerViewModel.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/timer/TimerAlarmReceiver.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/timer/TimerAlarmScheduler.kt`
- Test: `android/app/src/test/java/com/lbs/schoolhelper/ui/timer/TimerViewModelTest.kt`

**Interfaces:**
- `TimerUiState` exposes settings, phase, current round, total rounds, `awaitingNextPhase`, and `sessionCompleted`.
- ViewModel methods `updatePomodoroSettings`, `startOrAdvance`, `pauseTimer`, `resetTimer`, and `selectPhaseForPreview`.

- [ ] Add failing tests for first focus start, focus completion awaiting break, break completion advancing round, long break after final round, pause/resume, and persisted restore.
- [ ] Implement transitions without automatic next-phase start; call `TimerCompletionAlert.play` exactly once per completed phase.
- [ ] Persist target and phase before scheduling alarms; restore expired progress into awaiting-next-phase state.
- [ ] Preserve existing display mode and QA 1-minute durations.
- [ ] Run timer tests and fix transition regressions.
- [ ] Commit `feat(timer): implement pomodoro phase transitions`.

### Task 3: Settings and timer UI

**Files:**
- Modify: `android/app/src/main/res/layout/activity_main.xml`
- Modify: `android/app/src/main/res/layout/activity_settings.xml`
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/MainActivity.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/SettingsActivity.kt`
- Modify: `android/app/src/main/java/com/lbs/schoolhelper/ui/settings/SettingsViewModel.kt`
- Test: `android/app/src/test/java/com/lbs/schoolhelper/MainActivityNavigationTest.kt`

- [ ] Add settings controls for focus/short/long minutes and a 1~4 round selector with validation copy.
- [ ] Add timer phase/round labels and contextual manual next-step labels.
- [ ] Bind save/reset behavior and ensure existing school/display settings remain unchanged.
- [ ] Add UI/state tests for labels and round selector boundaries.
- [ ] Run resource compilation and focused tests.
- [ ] Commit `feat(timer): add pomodoro controls and phase UI`.

### Task 4: Verification and device rollout

**Files:**
- Modify: `docs/superpowers/specs/2026-09-16-pomodoro-timer-design.md` only if verified behavior requires clarification.

- [ ] Run `./gradlew :app:testDebugUnitTest --console=plain` with Android Studio JBR.
- [ ] Run `./gradlew :app:assembleQa :app:lintQa --console=plain`.
- [ ] Install `android/app/build/outputs/apk/qa/app-qa.apk` on connected device.
- [ ] Verify QA flow: focus 1 minute → manual short break → manual next focus, and 1~4 round selector.
- [ ] Check `git diff --check`, commit verification fixes, and push `main`.
