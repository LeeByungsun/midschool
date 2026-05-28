#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
PROJECT_PATH="${PROJECT_PATH:-$ROOT_DIR/ios/SchoolHelperIOS.xcodeproj}"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
SCHEME="${SCHEME:-SchoolHelperIOSUI}"
CONFIGURATION="${CONFIGURATION:-Debug}"
DESTINATION="${DESTINATION:-platform=iOS Simulator,name=iPhone 16 Pro,OS=18.3.1}"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/misschool-ios-live-ui-test}"
ONLY_TESTING="${ONLY_TESTING:-SchoolHelperIOSUITests/SchoolHelperIOSUITests/testLiveSchoolDataDisplaysBackendContent}"
SCHOOLHELPER_LIVE_TEST_DATE="${SCHOOLHELPER_LIVE_TEST_DATE:-20260528}"
SENTINEL_PATH="${SENTINEL_PATH:-/tmp/misschool-ios-enable-live-ui-test}"

export DEVELOPER_DIR
export SCHOOLHELPER_LIVE_TEST_DATE

touch "$SENTINEL_PATH"
trap 'rm -f "$SENTINEL_PATH"' EXIT

XCODEBUILD_ARGS=(
  -project "$PROJECT_PATH"
  -scheme "$SCHEME"
  -destination "$DESTINATION"
  -configuration "$CONFIGURATION"
  -derivedDataPath "$DERIVED_DATA_PATH"
  "-only-testing:$ONLY_TESTING"
  test
)

echo "Destination: $DESTINATION"
echo "DerivedData: $DERIVED_DATA_PATH"
echo "Live date: $SCHOOLHELPER_LIVE_TEST_DATE"
echo "Only testing: $ONLY_TESTING"
echo "Sentinel: $SENTINEL_PATH"

xcodebuild "${XCODEBUILD_ARGS[@]}"
