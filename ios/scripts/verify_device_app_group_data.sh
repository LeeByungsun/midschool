#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
PROJECT_PATH="${PROJECT_PATH:-$ROOT_DIR/ios/SchoolHelper.xcodeproj}"
SCHEME="${SCHEME:-SchoolHelperIOS}"
BUNDLE_ID="${BUNDLE_ID:-com.lbs.shcoolhelper}"
GROUP_ID="${GROUP_ID:-group.com.lbs.shcoolhelper}"
DEVICE_ID="${DEVICE_ID:-}"
OUTPUT_DIR="${OUTPUT_DIR:-/tmp/misschool-ios-device-app-group-data}"
UNLOCK_WAIT_SECONDS="${UNLOCK_WAIT_SECONDS:-0}"
ROUTE_DELAY_SECONDS="${ROUTE_DELAY_SECONDS:-3}"

export DEVELOPER_DIR

resolve_device_id() {
  if [[ -n "$DEVICE_ID" ]]; then
    printf '%s\n' "$DEVICE_ID"
    return 0
  fi

  local destinations_file
  destinations_file="$(mktemp)"
  xcodebuild \
    -project "$PROJECT_PATH" \
    -scheme "$SCHEME" \
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

launch_app() {
  local env_json="$1"
  local output
  local status
  : >"$LAUNCH_LOG"
  set +e
  output="$(xcrun devicectl device process launch \
    --device "$DEVICE_ID" \
    --terminate-existing \
    --environment-variables "$env_json" \
    "$BUNDLE_ID" \
    --timeout 30 2>&1)"
  status=$?
  printf '%s\n' "$output" | tee "$LAUNCH_LOG"
  return "$status"
}

DEVICE_ID="$(resolve_device_id)"
if [[ -z "$DEVICE_ID" ]]; then
  echo "No paired real iOS device was found." >&2
  exit 3
fi

ENV_JSON="$(json_env \
  SCHOOLHELPER_SEED_PROFILE=fixture \
  SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST=1 \
  SCHOOLHELPER_INITIAL_ROUTE=home)"

cat <<EOF2
Device: $DEVICE_ID
Bundle: $BUNDLE_ID
App Group: $GROUP_ID
Output: $OUTPUT_DIR

This smoke launches the app with a seeded profile, then copies the App Group
UserDefaults plist from the real device and checks that the shared profile data
was written to the App Group container.
EOF2

LAUNCH_LOG="$(mktemp)"
set +e
launch_app "$ENV_JSON"
LAUNCH_STATUS=$?
set -e
if [[ "$LAUNCH_STATUS" -ne 0 ]] && grep -Eqi "Locked|could not be, unlocked|device.*locked" "$LAUNCH_LOG"; then
  if [[ "$UNLOCK_WAIT_SECONDS" =~ ^[0-9]+$ && "$UNLOCK_WAIT_SECONDS" -gt 0 ]]; then
    echo "The iPhone is locked. Waiting up to ${UNLOCK_WAIT_SECONDS}s for unlock..." >&2
    STARTED_AT="$(date +%s)"
    while true; do
      sleep 2
      set +e
      launch_app "$ENV_JSON"
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
    echo "The iPhone is locked. Unlock the device and rerun, or set UNLOCK_WAIT_SECONDS." >&2
    rm -f "$LAUNCH_LOG"
    exit 5
  fi
  rm -f "$LAUNCH_LOG"
  exit "$LAUNCH_STATUS"
fi
rm -f "$LAUNCH_LOG"

sleep "$ROUTE_DELAY_SECONDS"
mkdir -p "$OUTPUT_DIR"
PLIST_PATH="$OUTPUT_DIR/${GROUP_ID}.plist"
rm -f "$PLIST_PATH"

xcrun devicectl device copy from \
  --device "$DEVICE_ID" \
  --domain-type appGroupDataContainer \
  --domain-identifier "$GROUP_ID" \
  --source "Library/Preferences/${GROUP_ID}.plist" \
  --destination "$PLIST_PATH" \
  --timeout 30 >/dev/null

python3 - "$PLIST_PATH" <<'PY'
import json
import plistlib
import sys
from pathlib import Path

plist_path = Path(sys.argv[1])
with plist_path.open("rb") as handle:
    payload = plistlib.load(handle)

raw_profile = payload.get("student_profile")
if not isinstance(raw_profile, (bytes, bytearray)):
    raise SystemExit("student_profile was not found in App Group UserDefaults")

profile = json.loads(bytes(raw_profile).decode("utf-8"))
expected = {
    "grade": "1",
    "classroom": "2",
    "schoolName": "미사중학교",
    "officeCode": "J10",
    "schoolCode": "7692129",
    "schoolKind": "중학교",
}
missing = {key: value for key, value in expected.items() if profile.get(key) != value}
if missing:
    raise SystemExit(f"seeded profile mismatch: expected subset {missing}, actual {profile}")

print(json.dumps({
    "status": "ok",
    "plist": str(plist_path),
    "verifiedKeys": sorted(expected),
    "profile": profile,
}, ensure_ascii=False, indent=2))
PY

cat <<EOF3

Device App Group data smoke finished.
Automated evidence:
- seeded app launch wrote student_profile into the real device App Group container
- copied plist: $PLIST_PATH

Manual evidence still required:
- actual home-screen WidgetKit placement/content/tap UX
- notification banner/sound/vibration UX
EOF3
