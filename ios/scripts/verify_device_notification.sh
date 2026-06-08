#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
BUNDLE_ID="${BUNDLE_ID:-com.lbs.shcoolhelper}"
DEVICE_ID="${DEVICE_ID:-}"
REMAINING_SECONDS="${REMAINING_SECONDS:-20}"
PRESET="${PRESET:-shortBreak}"
ROUTE_DELAY_SECONDS="${ROUTE_DELAY_SECONDS:-2}"
STATUS_WAIT_SECONDS="${STATUS_WAIT_SECONDS:-3}"
STATUS_OUTPUT_DIR="${STATUS_OUTPUT_DIR:-/tmp/misschool-ios-device-notification-smoke-status}"
REQUIRE_SCHEDULED="${REQUIRE_SCHEDULED:-1}"
SMOKE_RUN_ID="${SMOKE_RUN_ID:-$(date +%s)-$$}"
UNLOCK_WAIT_SECONDS="${UNLOCK_WAIT_SECONDS:-0}"
export DEVELOPER_DIR

resolve_device_id() {
  if [[ -n "$DEVICE_ID" ]]; then
    printf '%s\n' "$DEVICE_ID"
    return 0
  fi
  local destinations_file
  destinations_file="$(mktemp)"
  xcodebuild \
    -project "$ROOT_DIR/ios/SchoolHelper.xcodeproj" \
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
  SCHOOLHELPER_SCHEDULE_TIMER_NOTIFICATION=1 \
  SCHOOLHELPER_NOTIFICATION_SMOKE_STATUS=1 \
  SCHOOLHELPER_NOTIFICATION_SMOKE_RUN_ID="$SMOKE_RUN_ID")"

cat <<EOF2
Device: $DEVICE_ID
Bundle: $BUNDLE_ID
Timer preset: $PRESET
Notification delay: ${REMAINING_SECONDS}s

If iOS shows a notification permission prompt, tap Allow, then run this script once more.
After the app opens, background or lock the iPhone and wait for the timer notification.
EOF2

LAUNCH_LOG="$(mktemp)"
launch_app() {
  : >"$LAUNCH_LOG"
  set +e
  xcrun devicectl device process launch \
    --device "$DEVICE_ID" \
    --terminate-existing \
    --environment-variables "$ENV_JSON" \
    "$BUNDLE_ID" \
    --timeout 30 2>&1 | tee "$LAUNCH_LOG"
  local status=${PIPESTATUS[0]}
  return "$status"
}

set +e
launch_app
LAUNCH_STATUS=$?
set -e
if [[ "$LAUNCH_STATUS" -ne 0 ]] && grep -Eqi "Locked|could not be, unlocked|device.*locked" "$LAUNCH_LOG"; then
  if [[ "$UNLOCK_WAIT_SECONDS" =~ ^[0-9]+$ && "$UNLOCK_WAIT_SECONDS" -gt 0 ]]; then
    echo "The iPhone is locked. Waiting up to ${UNLOCK_WAIT_SECONDS}s for unlock..." >&2
    STARTED_AT="$(date +%s)"
    while true; do
      sleep 2
      set +e
      launch_app
      LAUNCH_STATUS=$?
      set -e
      if [[ "$LAUNCH_STATUS" -eq 0 ]]; then
        break
      fi
      NOW="$(date +%s)"
      if (( NOW - STARTED_AT >= UNLOCK_WAIT_SECONDS )); then
        break
      fi
      if ! grep -Eqi "Locked|could not be, unlocked|device.*locked" "$LAUNCH_LOG"; then
        break
      fi
    done
  fi
fi

if [[ "$LAUNCH_STATUS" -ne 0 ]]; then
  if grep -Eqi "Locked|could not be, unlocked|device.*locked" "$LAUNCH_LOG"; then
    echo "The iPhone is locked. Unlock the device and rerun this script, or set UNLOCK_WAIT_SECONDS." >&2
    rm -f "$LAUNCH_LOG"
    exit 5
  fi
  rm -f "$LAUNCH_LOG"
  exit "$LAUNCH_STATUS"
fi
rm -f "$LAUNCH_LOG"

sleep "$ROUTE_DELAY_SECONDS"
sleep "$STATUS_WAIT_SECONDS"

mkdir -p "$STATUS_OUTPUT_DIR"
STATUS_FILE="$STATUS_OUTPUT_DIR/schoolhelper-notification-smoke.json"
rm -f "$STATUS_FILE"

if xcrun devicectl device copy from \
  --device "$DEVICE_ID" \
  --domain-type appDataContainer \
  --domain-identifier "$BUNDLE_ID" \
  --source "Documents/schoolhelper-notification-smoke.json" \
  --destination "$STATUS_FILE" \
  --timeout 30 >/dev/null; then
  echo "Notification smoke status:"
  cat "$STATUS_FILE"
  echo

python3 - "$STATUS_FILE" "$REQUIRE_SCHEDULED" "$SMOKE_RUN_ID" <<'PY'
import json
import sys

path = sys.argv[1]
require_scheduled = sys.argv[2].lower() in {"1", "true", "yes"}
expected_run_id = sys.argv[3]
with open(path, encoding="utf-8") as handle:
    payload = json.load(handle)

if payload.get("runID") != expected_run_id:
    message = (
        "Notification smoke status is stale or from another run. "
        f"expectedRunID={expected_run_id} status={payload}"
    )
    if require_scheduled:
        print(message, file=sys.stderr)
        raise SystemExit(6)
    print(message)
    raise SystemExit(0)

scheduled = payload.get("scheduled") == "true"
pending = payload.get("pending") == "true"
if scheduled and pending:
    raise SystemExit(0)

message = (
    "Timer notification was not verified as a pending local notification. "
    f"status={payload}"
)
if require_scheduled:
    print(message, file=sys.stderr)
    raise SystemExit(6)
print(message)
PY
else
  echo "Failed to copy notification smoke status from app data container." >&2
  if [[ "$REQUIRE_SCHEDULED" == "1" || "$REQUIRE_SCHEDULED" == "true" || "$REQUIRE_SCHEDULED" == "yes" ]]; then
    exit 6
  fi
fi

cat <<EOF3

Notification smoke launch finished.
Automated evidence:
- app wrote local notification schedule status to $STATUS_FILE

Manual evidence still required:
- permission prompt appears on first run when not determined
- notification appears after roughly ${REMAINING_SECONDS}s while app is backgrounded/locked
EOF3
