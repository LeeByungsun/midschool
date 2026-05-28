#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
PROJECT_PATH="${PROJECT_PATH:-$ROOT_DIR/ios/SchoolHelperIOS.xcodeproj}"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
SCHEME="${SCHEME:-SchoolHelperIOSUI}"
CONFIGURATION="${CONFIGURATION:-Debug}"
DESTINATION="${DESTINATION:-platform=iOS Simulator,name=iPhone 16 Pro,OS=18.3.1}"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/misschool-ios-live-navigation-ui-test}"
ONLY_TESTING="${ONLY_TESTING:-SchoolHelperIOSUITests/SchoolHelperIOSUITests/testLiveDateNavigationUpdatesTitles}"
SCHOOLHELPER_LIVE_TEST_DATE="${SCHOOLHELPER_LIVE_TEST_DATE:-20260528}"

export DEVELOPER_DIR
export SCHOOLHELPER_LIVE_TEST_DATE

XCODEBUILD_ARGS=(
  -project "$PROJECT_PATH"
  -scheme "$SCHEME"
  -destination "$DESTINATION"
  -configuration "$CONFIGURATION"
  -derivedDataPath "$DERIVED_DATA_PATH"
  "-only-testing:$ONLY_TESTING"
  'OTHER_SWIFT_FLAGS=$(inherited) -DLIVE_UI_TEST_ENABLED'
  test
)

echo "Destination: $DESTINATION"
echo "DerivedData: $DERIVED_DATA_PATH"
echo "Live date: $SCHOOLHELPER_LIVE_TEST_DATE"
echo "Only testing: $ONLY_TESTING"

xcodebuild "${XCODEBUILD_ARGS[@]}"
