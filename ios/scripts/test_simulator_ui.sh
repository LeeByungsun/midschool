#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IOS_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/schoolon-ios-simulator-ui-derived}"
FIXTURE_HOST="${FIXTURE_HOST:-127.0.0.1}"
FIXTURE_PORT="${FIXTURE_PORT:-18080}"
FIXTURE_BASE_URL="http://${FIXTURE_HOST}:${FIXTURE_PORT}/"

export DEVELOPER_DIR

find_booted_simulator() {
  xcrun simctl list devices booted -j | python3 -c '
import json, sys
for runtime, devices in json.load(sys.stdin).get("devices", {}).items():
    if "iOS" not in runtime:
        continue
    for device in devices:
        if device.get("state") == "Booted" and device.get("isAvailable", True):
            print(device["udid"])
            raise SystemExit(0)
raise SystemExit(1)
'
}

DEVICE_ID="${DEVICE_ID:-$(find_booted_simulator || true)}"
if [[ -z "$DEVICE_ID" ]]; then
  echo "실행 중인 iOS 시뮬레이터가 없습니다. Xcode에서 시뮬레이터를 먼저 실행하거나 DEVICE_ID를 지정하세요." >&2
  exit 1
fi

FIXTURE_LOG="$(mktemp -t schoolon-ui-fixture.XXXXXX.log)"
cleanup() {
  if [[ -n "${FIXTURE_PID:-}" ]]; then
    kill "$FIXTURE_PID" 2>/dev/null || true
    wait "$FIXTURE_PID" 2>/dev/null || true
  fi
  rm -f "$FIXTURE_LOG"
}
trap cleanup EXIT INT TERM

python3 "$SCRIPT_DIR/ui_fixture_server.py" \
  --host "$FIXTURE_HOST" \
  --port "$FIXTURE_PORT" >"$FIXTURE_LOG" 2>&1 &
FIXTURE_PID=$!

for _ in {1..50}; do
  if curl --silent --fail "${FIXTURE_BASE_URL}health" >/dev/null; then
    break
  fi
  if ! kill -0 "$FIXTURE_PID" 2>/dev/null; then
    cat "$FIXTURE_LOG" >&2
    exit 1
  fi
  sleep 0.1
done
curl --silent --fail "${FIXTURE_BASE_URL}health" >/dev/null

command=(
  xcodebuild
  -project "$IOS_DIR/SchoolHelper.xcodeproj"
  -scheme SchoolHelperIOSUI
  -destination "platform=iOS Simulator,id=$DEVICE_ID"
  -derivedDataPath "$DERIVED_DATA_PATH"
  ONLY_ACTIVE_ARCH=YES
  ARCHS=arm64
  test
)

if [[ -n "${ONLY_TESTING:-}" ]]; then
  command+=("-only-testing:${ONLY_TESTING}")
fi

echo "시뮬레이터 UI 테스트: $DEVICE_ID"
SCHOOLHELPER_UI_FIXTURE_BASE_URL="$FIXTURE_BASE_URL" "${command[@]}"
