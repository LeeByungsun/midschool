# Android release quality & observability implementation plan

> **For agentic workers:** Use superpowers:executing-plans inline. Steps use checkboxes for tracking.

**Goal:** Complete roadmap stages 1 (release scope/quality baseline) and 2 (Firebase operational observability), including verification evidence, without claiming unperformed device/console tests.

**Architecture:** Keep Kotlin/XML, Hilt/MVVM and existing repositories. A typed telemetry facade routes allowlisted events to Analytics and sanitized nonfatal exceptions to Crashlytics. Application lifecycle records foreground visits; repositories capture request-scoped school identity, outcome, cache source and elapsed time. No raw request/response or classroom data leaves this facade.

**Tech Stack:** Kotlin, Android API 26–36, Firebase BoM already in repo, JUnit/Robolectric, Gradle.

**Spec:** User-approved stages 1–2 from the release roadmap in this thread (2026-09-14).

## Global constraints
- Preserve untracked `flutter/` and `docs/android-ios-ui-interaction-comparison.md`.
- No publishing, production crash injection, account changes or real user data creation.
- Collection is independently opt-in for analytics/diagnostics; initial SDK collection is off. Advertising ID/personalization disabled. Stage 3 legal approval is still needed before public release.
- SDK calls must not block school features or turn SDK failures into application crashes.
- CancellationException propagates; no cancellation reports. Normal no-data is not an exception.
- School context uses school/office codes and kind, not name, grade/class, free text or persistent hardware IDs.
- Debug telemetry stays off unless explicitly enabled for QA; QA console/project validation remains a release gate.

## Task 1 — Baseline and launch checklist
- [ ] Run existing `testDebugUnitTest lintDebug` with Android Studio JDK and SDK.
- [ ] Create `docs/android-release-readiness.md` with scenario IDs, priority, evidence and unverified manual checks covering setup/change/cache, meals/timetable/schedule empty states, notices fallback, offline/retry, accessibility/system bars, widget/timer/reboot/permissions.
- [ ] Separate release-blocking defects from later enhancements; preserve uncertainty.

## Task 2 — Typed telemetry boundary and collection control
Files: `android/app/src/main/java/com/lbs/schoolhelper/telemetry/{AppTelemetry,TelemetryReporter,FirebaseTelemetrySink}.kt`, `di/TelemetryModule.kt`; matching telemetry tests.
- [ ] RED: fake sink records boundary payloads from real reporter. Assert no calls before consent, independent switches, no name/classroom/query fields, correct school update/clear, no raw exception message/cause, bounded duplicate nonfatal reports, no cancellation reporting.
- [ ] GREEN: implement typed facade, allowlisted enums and report payload, sink exception containment and sanitized exception stack.
- [ ] Add preference methods to `PreferencesRepository`/Impl and explicit fake implementations; RED persisted consent roundtrip/default off tests.
- [ ] Set Manifest defaults off, disable ad identifiers/personalization, exclude consent+SDK preferences from backup.
- [ ] Add settings switches and resource disclosure text. Save consent immediately and independently of school form validity; restore on app start.

## Task 3 — Real callsite wiring
Files: SchoolHelper.kt, telemetry lifecycle observer, Setup/Settings/Timer ViewModels, TimerAlarmReceiver, widget provider/config ViewModel, SchoolRepositoryImpl.
- [ ] RED: foreground transitions suppress screen-change/rotation duplicates, new foreground gets one context event.
- [ ] RED: school save updates context and selected/changed event, invalid saves emit nothing.
- [ ] RED: network/cache/empty/failure records source, duration, school snapshot; cancellation escapes get and observe paths and produces no failure report.
- [ ] GREEN: wire real callsites, use sanitized source errors before UI wrapping, report failures even when cache masks network errors.
- [ ] Record feature views, timer actions/completion without duplicate alarm/UI completion, widget add/config/refresh/open routes.

## Task 4 — Verification and operating runbook
Files: docs/android-firebase-observability.md, docs/project_specification.md, android/AGENTS.md.
- [ ] Run focused tests after each red/green cycle, then all tests, lint and assembleDebug/assembleRelease.
- [ ] Inspect merged release manifest for consent/ad settings and permission removal.
- [ ] Run available device/emulator smoke tests; record devices and exact outcomes, not assumed success.
- [ ] Verify DebugView and nonfatal/test-crash delivery, changed school context and collection-off behavior with QA environment, or record exact access blocker and ask user only then.
- [ ] Document event dictionary, custom dimensions, alert owner setup, QA commands, request/source semantics, safety controls, remaining stage 3–6 gates.
- [ ] Update project specification and Android architecture rules.
- [ ] Fresh code review and final requirement-by-requirement audit; do not mark goal complete if console verification remains unavailable.
