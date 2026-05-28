#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
PROJECT_PATH="${PROJECT_PATH:-$ROOT_DIR/ios/SchoolHelperIOS.xcodeproj}"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/misschool-ios-device-ui-test}"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
SCHEME="${SCHEME:-SchoolHelperIOSUI}"
CONFIGURATION="${CONFIGURATION:-Debug}"
DEVICE_ID="${DEVICE_ID:-}"
TEAM_ID="${TEAM_ID:-${DEVELOPMENT_TEAM:-}}"
ENTITLEMENTS_MODE="${ENTITLEMENTS_MODE:-device-preview}"
LIVE_UI_TEST="${LIVE_UI_TEST:-0}"
if [[ "$LIVE_UI_TEST" == "1" || "$LIVE_UI_TEST" == "true" || "$LIVE_UI_TEST" == "yes" ]]; then
  ONLY_TESTING="${ONLY_TESTING-SchoolHelperIOSUITests/SchoolHelperIOSUITests/testLiveSchoolDataDisplaysBackendContent}"
else
  ONLY_TESTING="${ONLY_TESTING-SchoolHelperIOSUITests/SchoolHelperIOSUITests/testInitialSetupSearchSelectsSchoolAndSavesProfile}"
fi

export DEVELOPER_DIR

if [[ -z "$TEAM_ID" ]]; then
  cat >&2 <<'EOM'
TEAM_ID is required for real-device UI test signing.

Example:
  TEAM_ID=YOUR_TEAM_ID ios/scripts/test_device_ui.sh

The default ENTITLEMENTS_MODE=device-preview clears App Group entitlements so the
app/test runner can validate the app body before full widget provisioning exists.
EOM
  exit 2
fi

if [[ -z "$DEVICE_ID" ]]; then
  DESTINATIONS_FILE="$(mktemp)"
  xcodebuild \
    -project "$PROJECT_PATH" \
    -scheme "$SCHEME" \
    -showdestinations >"$DESTINATIONS_FILE" 2>/dev/null || true
  DEVICE_ID="$(python3 - "$DESTINATIONS_FILE" <<'PY'
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
  )"
  rm -f "$DESTINATIONS_FILE"
fi

if [[ -z "$DEVICE_ID" ]]; then
  echo "No paired real iOS device destination was found." >&2
  echo "Connect/unlock the iPhone, enable Developer Mode, and pair it in Xcode/devicectl." >&2
  exit 3
fi

BUILD_SETTINGS=(
  CODE_SIGNING_ALLOWED=YES
  CODE_SIGNING_REQUIRED=YES
  CODE_SIGN_STYLE=Automatic
  DEVELOPMENT_TEAM="$TEAM_ID"
)

case "$ENTITLEMENTS_MODE" in
  device-preview)
    BUILD_SETTINGS+=(CODE_SIGN_ENTITLEMENTS=)
    ;;
  app-groups)
    "$ROOT_DIR/ios/scripts/check_app_group_profiles.py"
    ;;
  *)
    echo "Unknown ENTITLEMENTS_MODE: $ENTITLEMENTS_MODE" >&2
    echo "Use 'device-preview' or 'app-groups'." >&2
    exit 4
    ;;
esac

XCODEBUILD_ARGS=(
  -project "$PROJECT_PATH"
  -scheme "$SCHEME"
  -destination "platform=iOS,id=$DEVICE_ID"
  -configuration "$CONFIGURATION"
  -derivedDataPath "$DERIVED_DATA_PATH"
  -allowProvisioningUpdates
)

if [[ "$LIVE_UI_TEST" == "1" || "$LIVE_UI_TEST" == "true" || "$LIVE_UI_TEST" == "yes" ]]; then
  XCODEBUILD_ARGS+=('OTHER_SWIFT_FLAGS=$(inherited) -DLIVE_UI_TEST_ENABLED')
fi

if [[ -n "$ONLY_TESTING" ]]; then
  XCODEBUILD_ARGS+=("-only-testing:$ONLY_TESTING")
fi

XCODEBUILD_ARGS+=("${BUILD_SETTINGS[@]}" test)

echo "Device: $DEVICE_ID"
echo "Team: $TEAM_ID"
echo "Entitlements mode: $ENTITLEMENTS_MODE"
echo "DerivedData: $DERIVED_DATA_PATH"
echo "Only testing: ${ONLY_TESTING:-<all>}"
echo "Live UI test: $LIVE_UI_TEST"

XCODEBUILD_LOG="${XCODEBUILD_LOG:-$DERIVED_DATA_PATH/test_device_ui.xcodebuild.log}"
AUTOMATION_RETRY_LIMIT="${AUTOMATION_RETRY_LIMIT:-1}"
mkdir -p "$(dirname "$XCODEBUILD_LOG")"
: >"$XCODEBUILD_LOG"

run_xcodebuild_attempt() {
  local attempt="$1"
  local attempt_log="$DERIVED_DATA_PATH/test_device_ui.attempt-$attempt.xcodebuild.log"
  : >"$attempt_log"

  echo "xcodebuild attempt: $attempt" | tee -a "$XCODEBUILD_LOG"
  xcodebuild "${XCODEBUILD_ARGS[@]}" > >(tee -a "$XCODEBUILD_LOG" "$attempt_log") 2>&1 &
  XCODEBUILD_PID=$!

  while kill -0 "$XCODEBUILD_PID" 2>/dev/null; do
    if grep -Eq "Unlock .* to Continue|device is locked" "$attempt_log"; then
      echo >&2
      echo "The iPhone is locked. Unlock the device and rerun this script." >&2
      echo "Xcodebuild log: $XCODEBUILD_LOG" >&2
      kill "$XCODEBUILD_PID" 2>/dev/null || true
      wait "$XCODEBUILD_PID" 2>/dev/null || true
      return 5
    fi
    sleep 2
  done

  set +e
  wait "$XCODEBUILD_PID"
  STATUS=$?
  set -e

  if [[ "$STATUS" -ne 0 ]] && grep -q "Timed out while enabling automation mode" "$attempt_log"; then
    return 6
  fi

  return "$STATUS"
}

ATTEMPT=1
while true; do
  set +e
  run_xcodebuild_attempt "$ATTEMPT"
  STATUS=$?
  set -e

  if [[ "$STATUS" -eq 0 ]]; then
    exit 0
  fi

  if [[ "$STATUS" -eq 6 && "$ATTEMPT" -le "$AUTOMATION_RETRY_LIMIT" ]]; then
    echo "UI automation mode timed out. Retrying xcodebuild once more..." >&2
    ATTEMPT=$((ATTEMPT + 1))
    continue
  fi

  exit "$STATUS"
done
