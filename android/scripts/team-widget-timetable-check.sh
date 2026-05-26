#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CONTEXT_DIR="${REPO_ROOT}/.omx/context"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
CONTEXT_FILE="${CONTEXT_DIR}/android-widget-timetable-check-${TIMESTAMP}.md"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "error: required command not found: $1" >&2
    exit 1
  fi
}

require_cmd omx
require_cmd tmux

if [[ -z "${TMUX:-}" ]]; then
  echo "error: OMX team commands must run inside a tmux session." >&2
  exit 1
fi

mkdir -p "${CONTEXT_DIR}"

cat > "${CONTEXT_FILE}" <<'EOF'
# Android widget timetable auto-update check context

## Task statement

Investigate the intermittent issue where the Android widget auto-updates but sometimes fails to load timetable data.

## Desired outcome

- Reproduce or narrow the failure path.
- Identify root cause candidates with evidence.
- Apply a minimal safe fix if the cause is clear.
- Otherwise leave a concrete diagnosis + next verification plan.

## Known facts

- The issue appears during automatic widget refresh, not only manual app navigation.
- Widget/timetable behavior spans provider, scheduler, preferences/cache, and repository/date logic.

## Likely touchpoints

- `android/app/src/main/java/com/bsbarron/midschoolapp/widget/MisSchoolWidgetProvider.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/widget/WidgetConfigActivity.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/widget/WidgetDateFormatter.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/timer/TimerAlarmReceiver.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/SchoolRepositoryImpl.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepositoryImpl.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/widget/WidgetConfigViewModel.kt`

## Investigation checklist

- Check auto-update timing path and trigger conditions.
- Check date rollover / timezone / tomorrow-mode behavior.
- Check repository fallback and cache key behavior.
- Check whether missing grade/class/school info can affect widget-only refreshes.
- Check network failure handling for background refresh.

## Verification expectation

- Run focused Android unit tests where possible.
- If repro requires manual steps, document exact repro and observed evidence.
EOF

echo "Created context: ${CONTEXT_FILE}"
echo "Launching OMX team to investigate widget timetable auto-update failures..."

cd "${REPO_ROOT}"

omx team 3:debugger "Investigate the intermittent Android widget auto-update issue where timetable data is sometimes missing. Split work into lanes for widget trigger path, repository/cache/date logic, and verification evidence. Focus on root cause, minimal safe fix if clear, or a concrete diagnosis if not. Scope Android files only. Summarize repro evidence, changed files if any, tests run, and remaining uncertainty."
