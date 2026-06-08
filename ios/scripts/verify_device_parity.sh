#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
PROJECT_PATH="${PROJECT_PATH:-$ROOT_DIR/ios/SchoolHelper.xcodeproj}"
SCHEME="${SCHEME:-SchoolHelperIOS}"
BUNDLE_ID="${BUNDLE_ID:-com.lbs.shcoolhelper}"
DEVICE_ID="${DEVICE_ID:-}"
INSTALL="${INSTALL:-0}"
TEAM_ID="${TEAM_ID:-${DEVELOPMENT_TEAM:-}}"
ENTITLEMENTS_MODE="${ENTITLEMENTS_MODE:-device-preview}"
ROUTE_DELAY_SECONDS="${ROUTE_DELAY_SECONDS:-2}"
RUN_FRESH_SETUP="${RUN_FRESH_SETUP:-0}"
SEED_PROFILE="${SEED_PROFILE:-0}"

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

launch_with_env() {
  local label="$1"
  local env_json="$2"
  printf "\n==> %s\n" "$label"
  xcrun devicectl device process launch \
    --device "$DEVICE_ID" \
    --terminate-existing \
    --environment-variables "$env_json" \
    "$BUNDLE_ID" \
    --timeout 30
  sleep "$ROUTE_DELAY_SECONDS"
}

open_deeplink() {
  local route="$1"
  printf "\n==> deeplink schoolhelper://%s\n" "$route"
  xcrun devicectl device process launch \
    --device "$DEVICE_ID" \
    --terminate-existing \
    --payload-url "schoolhelper://$route" \
    "$BUNDLE_ID" \
    --timeout 30
  sleep "$ROUTE_DELAY_SECONDS"
}

DEVICE_ID="$(resolve_device_id)"
if [[ -z "$DEVICE_ID" ]]; then
  echo "No paired real iOS device was found." >&2
  exit 3
fi

if [[ "$INSTALL" == "1" || "$INSTALL" == "true" || "$INSTALL" == "yes" ]]; then
  if [[ -z "$TEAM_ID" ]]; then
    echo "TEAM_ID is required when INSTALL=1." >&2
    exit 2
  fi
  TEAM_ID="$TEAM_ID" \
  DEVICE_ID="$DEVICE_ID" \
  ENTITLEMENTS_MODE="$ENTITLEMENTS_MODE" \
  LAUNCH=0 \
  "$ROOT_DIR/ios/scripts/install_device.sh"
fi

echo "Device: $DEVICE_ID"
echo "Bundle: $BUNDLE_ID"
echo "Install: $INSTALL"
echo "Entitlements mode: $ENTITLEMENTS_MODE"
echo "Run fresh setup: $RUN_FRESH_SETUP"
echo "Seed profile: $SEED_PROFILE"

if [[ "$RUN_FRESH_SETUP" == "1" || "$RUN_FRESH_SETUP" == "true" || "$RUN_FRESH_SETUP" == "yes" ]]; then
  launch_with_env "fresh setup launch" "$(json_env \
    SCHOOLHELPER_RESET_PROFILE=1 \
    SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST=1)"
fi

if [[ "$SEED_PROFILE" == "1" || "$SEED_PROFILE" == "true" || "$SEED_PROFILE" == "yes" ]]; then
  launch_with_env "seeded home launch" "$(json_env \
    SCHOOLHELPER_SEED_PROFILE=fixture \
    SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST=1 \
    SCHOOLHELPER_INITIAL_ROUTE=home)"
fi

for route in timetable meals schedule settings timer; do
  open_deeplink "$route"
done

timer_env=(
  SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST=1
  SCHOOLHELPER_INITIAL_ROUTE=timer
  SCHOOLHELPER_TIMER_PRESET=shortBreak
  SCHOOLHELPER_TIMER_TOTAL_SECONDS=60
  SCHOOLHELPER_TIMER_REMAINING_SECONDS=45
  SCHOOLHELPER_TIMER_RUNNING=1
)
if [[ "$SEED_PROFILE" == "1" || "$SEED_PROFILE" == "true" || "$SEED_PROFILE" == "yes" ]]; then
  timer_env+=(SCHOOLHELPER_SEED_PROFILE=fixture)
fi
launch_with_env "running timer launch" "$(json_env "${timer_env[@]}")"

cat <<EOF2

Device parity smoke finished.
Manual visual checks still required:
- fresh setup screen is shown when RUN_FRESH_SETUP=1
- seeded home shows Misa profile when SEED_PROFILE=1
- deeplink launches route to timetable/meals/schedule/settings/timer when the app has a complete profile
- running timer shows countdown state
- actual WidgetKit home-screen placement/tap and notification completion UX are not proven by this smoke
EOF2
