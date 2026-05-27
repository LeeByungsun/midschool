#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
PROJECT_PATH="$ROOT_DIR/ios/SchoolHelperIOS.xcodeproj"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/misschool-ios-run}"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
SCHEME="${SCHEME:-SchoolHelperIOS}"
BUNDLE_ID="${BUNDLE_ID:-com.leebyungsun.schoolhelperios}"
SIMULATOR_NAME="${SIMULATOR_NAME:-iPhone 16 Pro}"
SIMULATOR_OS="${SIMULATOR_OS:-18.3.1}"
OUTPUT_FILE="${OUTPUT_FILE:-/tmp/deeplink-confirm.png}"
DEEPLINK_URL="${DEEPLINK_URL:-schoolhelper://timetable}"

export DEVELOPER_DIR

APP_PATH="$DERIVED_DATA_PATH/Build/Products/Debug-iphonesimulator/SchoolHelperIOS.app"
DEVICE_ID="$(
  python3 - "$SIMULATOR_NAME" <<'PY'
import re
import subprocess
import sys

name = sys.argv[1]
output = subprocess.check_output(
    ["xcrun", "simctl", "list", "devices", "available"],
    text=True,
)

pattern = re.compile(rf"{re.escape(name)} \(([A-F0-9-]+)\)")
for line in output.splitlines():
    match = pattern.search(line)
    if match:
        print(match.group(1))
        break
PY
)"

if [[ -z "$DEVICE_ID" ]]; then
  echo "Failed to find simulator named '$SIMULATOR_NAME'." >&2
  exit 1
fi

echo "Using simulator: $SIMULATOR_NAME ($DEVICE_ID)"
echo "Output file: $OUTPUT_FILE"

xcodebuild \
  -project "$PROJECT_PATH" \
  -scheme "$SCHEME" \
  -destination "platform=iOS Simulator,name=$SIMULATOR_NAME,OS=$SIMULATOR_OS" \
  -configuration Debug \
  -derivedDataPath "$DERIVED_DATA_PATH" \
  CODE_SIGNING_ALLOWED=NO \
  build >/dev/null

xcrun simctl terminate "$DEVICE_ID" "$BUNDLE_ID" >/dev/null 2>&1 || true
xcrun simctl uninstall "$DEVICE_ID" "$BUNDLE_ID" >/dev/null 2>&1 || true
xcrun simctl install "$DEVICE_ID" "$APP_PATH" >/dev/null

env \
  SIMCTL_CHILD_SCHOOLHELPER_SEED_PROFILE=fixture \
  SIMCTL_CHILD_SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST=1 \
  xcrun simctl launch "$DEVICE_ID" "$BUNDLE_ID" >/dev/null

sleep 2
xcrun simctl openurl "$DEVICE_ID" "$DEEPLINK_URL"
sleep 1
xcrun simctl io "$DEVICE_ID" screenshot "$OUTPUT_FILE" >/dev/null

echo "Captured: $OUTPUT_FILE"
