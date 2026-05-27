#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
PROJECT_PATH="$ROOT_DIR/ios/SchoolHelperIOS.xcodeproj"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/misschool-ios-run}"
OUTPUT_DIR="${OUTPUT_DIR:-/tmp/misschool-ios-captures}"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
SCHEME="${SCHEME:-SchoolHelperIOS}"
BUNDLE_ID="${BUNDLE_ID:-com.leebyungsun.schoolhelperios}"
SIMULATOR_NAME="${SIMULATOR_NAME:-iPhone 16 Pro}"
SIMULATOR_OS="${SIMULATOR_OS:-18.3.1}"

export DEVELOPER_DIR

mkdir -p "$OUTPUT_DIR"

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
echo "Output dir: $OUTPUT_DIR"

build_app() {
  xcodebuild \
    -project "$PROJECT_PATH" \
    -scheme "$SCHEME" \
    -destination "platform=iOS Simulator,name=$SIMULATOR_NAME,OS=$SIMULATOR_OS" \
    -configuration Debug \
    -derivedDataPath "$DERIVED_DATA_PATH" \
    CODE_SIGNING_ALLOWED=NO \
    build
}

install_fresh() {
  xcrun simctl terminate "$DEVICE_ID" "$BUNDLE_ID" >/dev/null 2>&1 || true
  xcrun simctl uninstall "$DEVICE_ID" "$BUNDLE_ID" >/dev/null 2>&1 || true
  xcrun simctl install "$DEVICE_ID" "$APP_PATH"
}

launch_and_capture() {
  local route="$1"
  local wait_secs="$2"
  local output_file="$3"
  shift 3

  xcrun simctl terminate "$DEVICE_ID" "$BUNDLE_ID" >/dev/null 2>&1 || true

  env \
    SIMCTL_CHILD_SCHOOLHELPER_SEED_PROFILE=fixture \
    SIMCTL_CHILD_SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST=1 \
    SIMCTL_CHILD_SCHOOLHELPER_INITIAL_ROUTE="$route" \
    "$@" \
    xcrun simctl launch "$DEVICE_ID" "$BUNDLE_ID" >/dev/null

  sleep "$wait_secs"
  xcrun simctl io "$DEVICE_ID" screenshot "$output_file" >/dev/null
  echo "Captured: $output_file"
}

capture_timer_running_sequence() {
  local output_file_1="$1"
  local output_file_2="$2"

  xcrun simctl terminate "$DEVICE_ID" "$BUNDLE_ID" >/dev/null 2>&1 || true

  env \
    SIMCTL_CHILD_SCHOOLHELPER_SEED_PROFILE=fixture \
    SIMCTL_CHILD_SCHOOLHELPER_SKIP_NOTIFICATION_REQUEST=1 \
    SIMCTL_CHILD_SCHOOLHELPER_INITIAL_ROUTE=timer \
    SIMCTL_CHILD_SCHOOLHELPER_TIMER_PRESET=shortBreak \
    SIMCTL_CHILD_SCHOOLHELPER_TIMER_REMAINING_SECONDS=123 \
    SIMCTL_CHILD_SCHOOLHELPER_TIMER_RUNNING=1 \
    xcrun simctl launch "$DEVICE_ID" "$BUNDLE_ID" >/dev/null

  sleep 2
  xcrun simctl io "$DEVICE_ID" screenshot "$output_file_1" >/dev/null
  sleep 3
  xcrun simctl io "$DEVICE_ID" screenshot "$output_file_2" >/dev/null
  echo "Captured: $output_file_1"
  echo "Captured: $output_file_2"
}

build_app
install_fresh

launch_and_capture home 5 "$OUTPUT_DIR/home.png"
launch_and_capture timetable 5 "$OUTPUT_DIR/timetable.png"
launch_and_capture meals 8 "$OUTPUT_DIR/meals.png"
launch_and_capture schedule 8 "$OUTPUT_DIR/schedule.png"
launch_and_capture settings 6 "$OUTPUT_DIR/settings.png"
launch_and_capture timer 5 "$OUTPUT_DIR/timer.png"
capture_timer_running_sequence "$OUTPUT_DIR/timer-running-1.png" "$OUTPUT_DIR/timer-running-2.png"

echo
echo "Done. Captures written to: $OUTPUT_DIR"
