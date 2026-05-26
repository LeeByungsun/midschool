#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CONTEXT_DIR="${REPO_ROOT}/.omx/context"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
CONTEXT_FILE="${CONTEXT_DIR}/android-school-selection-ab-${TIMESTAMP}.md"

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
# Android school selection A/B team context

## Task statement

Implement `docs/android-app-todo.md` section 3's unfinished items:

- A. 학교 검색/선택 기능
- B. 학교 코드/학교 종류 저장

## Desired outcome

- Setup/Settings can search and select schools.
- Selected `officeCode`, `schoolCode`, `schoolKind` are persisted.
- Existing setup completion rules reflect school selection.
- Any migration/reset flow for older grade/class-only users is defined and implemented safely.

## Known facts

- Current Android app only stores grade/class.
- Current Android NEIS usage still depends on fixed school identity in parts of the stack.
- Relevant notes already exist in `docs/android-school-selection-review.md`.

## Constraints

- Scope Android only.
- Keep XML + DataBinding + Hilt + MVVM structure.
- Prefer existing repository/util patterns.
- No new dependencies.

## Likely touchpoints

- `android/app/src/main/java/com/bsbarron/midschoolapp/SetupActivity.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/SettingsActivity.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/setup/SetupViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/ui/settings/SettingsViewModel.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/UserPreferences.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepository.kt`
- `android/app/src/main/java/com/bsbarron/midschoolapp/data/repository/PreferencesRepositoryImpl.kt`
- `android/app/src/main/res/layout/activity_setup.xml`
- `android/app/src/main/res/layout/activity_settings.xml`

## Verification expectation

- Run Android unit tests relevant to setup/settings/preferences.
- Report any remaining gaps explicitly.
EOF

echo "Created context: ${CONTEXT_FILE}"
echo "Launching OMX team for Android school selection A/B work..."

cd "${REPO_ROOT}"

omx team 4:executor "Implement Android school selection unfinished items A and B from docs/android-app-todo.md. Split work into lanes for Setup/Settings UI, preference model/repository persistence, migration/setup-complete rules, and regression verification. Scope Android files only. Use docs/android-school-selection-review.md as the source of truth. Verify with focused Android tests and summarize changed files, tests run, and remaining risks."
