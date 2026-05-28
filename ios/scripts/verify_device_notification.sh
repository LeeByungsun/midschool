#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
BUNDLE_ID="${BUNDLE_ID:-com.leebyungsun.schoolhelperios}"
DEVICE_ID="${DEVICE_ID:-}"
REMAINING_SECONDS="${REMAINING_SECONDS:-20}"
PRESET="${PRESET:-shortBreak}"
ROUTE_DELAY_SECONDS="${ROUTE_DELAY_SECONDS:-2}"
export DEVELOPER_DIR

resolve_device_id() {
  if [[ -n "$DEVICE_ID" ]]; then
    printf '%s\n' "$DEVICE_ID"
    return 0
  fi
  local destinations_file
  destinations_file="$(mktemp)"
  xcodebuild \
    -project "$ROOT_DIR/ios/SchoolHelperIOS.xcodeproj" \
    -scheme SchoolHelperIOS \
    -showdestinations >"$destinations_file" 2>/dev/null || true
  python3 - "$destinations_file" <<'PY'
import re
import sys
text = open(sys.argv[1], encoding="utf-8", errors="replace").read()
for line in text.splitlines():
    if "platform:iOS," not in line or "Simulator" in line:
        continue
    match = re.search(r"id:([^,} ]+)", line)
    if match:
        print(match.group(1))
        break
PY
  rm -f "$destinations_file"
}

json_env() {
  python3 - "$@" <<'PY'
import json
import sys
pairs = {}
for raw in sys.argv[1:]:
    key, value = raw.split("=", 1)
    pairs[key] = value
print(json.dumps(pairs, ensure_ascii=False))
PY
}

DEVICE_ID="$(resolve_device_id)"
if [[ -z "$DEVICE_ID" ]]; then
  echo "No paired real iOS device was found." >&2
  exit 3
fi

ENV_JSON="$(json_env \
  SCHOOLHELPER_SEED_PROFILE=fixture \
  SCHOOLHELPER_INITIAL_ROUTE=timer \
  SCHOOLHELPER_TIMER_PRESET="$PRESET" \
  SCHOOLHELPER_TIMER_TOTAL_SECONDS="$REMAINING_SECONDS" \
  SCHOOLHELPER_TIMER_REMAINING_SECONDS="$REMAINING_SECONDS" \
  SCHOOLHELPER_TIMER_RUNNING=1 \
  SCHOOLHELPER_SCHEDULE_TIMER_NOTIFICATION=1)"

cat <<EOF2
Device: $DEVICE_ID
Bundle: $BUNDLE_ID
Timer preset: $PRESET
Notification delay: ${REMAINING_SECONDS}s

If iOS shows a notification permission prompt, tap Allow, then run this script once more.
After the app opens, background or lock the iPhone and wait for the timer notification.
EOF2

xcrun devicectl device process launch \
  --device "$DEVICE_ID" \
  --terminate-existing \
  --environment-variables "$ENV_JSON" \
  "$BUNDLE_ID" \
  --timeout 30

sleep "$ROUTE_DELAY_SECONDS"

cat <<EOF3

Notification smoke launch finished.
Manual evidence required:
- permission prompt appears on first run when not determined
- after permission is allowed, rerun schedules a timer completion notification
- notification appears after roughly ${REMAINING_SECONDS}s while app is backgrounded/locked
EOF3
