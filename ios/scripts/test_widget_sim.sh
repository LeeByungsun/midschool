#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
PROJECT_PATH="${PROJECT_PATH:-$ROOT_DIR/ios/SchoolHelperIOS.xcodeproj}"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
SCHEME="${SCHEME:-SchoolHelperWidget}"
CONFIGURATION="${CONFIGURATION:-Debug}"
DESTINATION="${DESTINATION:-platform=iOS Simulator,name=iPhone 16 Pro,OS=18.3.1}"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/misschool-ios-widget-sim-test}"
APP_GROUP="${APP_GROUP:-group.com.leebyungsun.schoolhelperios}"
APP_BUNDLE_ID="${APP_BUNDLE_ID:-com.leebyungsun.schoolhelperios}"
WIDGET_BUNDLE_ID="${WIDGET_BUNDLE_ID:-com.leebyungsun.schoolhelperios.widget}"

export DEVELOPER_DIR

VERBOSE="${VERBOSE:-0}"

assert_file_exists() {
  local path="$1"
  if [[ ! -e "$path" ]]; then
    echo "Missing expected path: $path" >&2
    exit 1
  fi
}

assert_plist_value() {
  local plist="$1"
  local key_path="$2"
  local expected="$3"
  local actual
  actual="$(/usr/libexec/PlistBuddy -c "Print $key_path" "$plist")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Unexpected plist value in $plist" >&2
    echo "  key: $key_path" >&2
    echo "  expected: $expected" >&2
    echo "  actual: $actual" >&2
    exit 1
  fi
}

assert_entitlement_has_group() {
  local entitlements="$1"
  assert_file_exists "$entitlements"
  if ! /usr/libexec/PlistBuddy -c 'Print :com.apple.security.application-groups' "$entitlements" | grep -Fq "$APP_GROUP"; then
    echo "Missing App Group $APP_GROUP in $entitlements" >&2
    exit 1
  fi
}

assert_source_contains() {
  local file="$1"
  local expected="$2"
  assert_file_exists "$file"
  if ! grep -Fq "$expected" "$file"; then
    echo "Missing expected source text in $file" >&2
    echo "  expected: $expected" >&2
    exit 1
  fi
}

BUILD_SETTINGS_FILE="$(mktemp)"
trap 'rm -f "$BUILD_SETTINGS_FILE"' EXIT

xcodebuild \
  -project "$PROJECT_PATH" \
  -scheme "$SCHEME" \
  -configuration "$CONFIGURATION" \
  -showBuildSettings >"$BUILD_SETTINGS_FILE" 2>/dev/null

grep -Fq 'CODE_SIGN_ENTITLEMENTS = SchoolHelperIOS/SchoolHelperIOS.entitlements' "$BUILD_SETTINGS_FILE"
grep -Fq 'CODE_SIGN_ENTITLEMENTS = SchoolHelperWidget/SchoolHelperWidget.entitlements' "$BUILD_SETTINGS_FILE"

echo "Destination: $DESTINATION"
echo "DerivedData: $DERIVED_DATA_PATH"
echo "App Group: $APP_GROUP"

mkdir -p "$DERIVED_DATA_PATH"
BUILD_LOG="$DERIVED_DATA_PATH/widget-sim-build.log"
XCODEBUILD_ARGS=(
  -project "$PROJECT_PATH"
  -scheme "$SCHEME"
  -destination "$DESTINATION"
  -configuration "$CONFIGURATION"
  -derivedDataPath "$DERIVED_DATA_PATH"
  build
)

if [[ "$VERBOSE" == "1" || "$VERBOSE" == "true" || "$VERBOSE" == "yes" ]]; then
  xcodebuild "${XCODEBUILD_ARGS[@]}"
else
  if ! xcodebuild "${XCODEBUILD_ARGS[@]}" >"$BUILD_LOG" 2>&1; then
    echo "Widget simulator build failed. Last 120 log lines:" >&2
    tail -120 "$BUILD_LOG" >&2
    exit 1
  fi
fi

APP_PATH="$DERIVED_DATA_PATH/Build/Products/$CONFIGURATION-iphonesimulator/SchoolHelperIOS.app"
WIDGET_PATH="$APP_PATH/PlugIns/SchoolHelperWidget.appex"
DIRECT_WIDGET_PATH="$DERIVED_DATA_PATH/Build/Products/$CONFIGURATION-iphonesimulator/SchoolHelperWidget.appex"

assert_file_exists "$APP_PATH/Info.plist"
assert_file_exists "$WIDGET_PATH/Info.plist"
assert_file_exists "$DIRECT_WIDGET_PATH/Info.plist"
assert_file_exists "$APP_PATH/SchoolHelperIOS"
assert_file_exists "$WIDGET_PATH/SchoolHelperWidget"

assert_plist_value "$APP_PATH/Info.plist" ':CFBundleIdentifier' "$APP_BUNDLE_ID"
assert_plist_value "$WIDGET_PATH/Info.plist" ':CFBundleIdentifier' "$WIDGET_BUNDLE_ID"
assert_plist_value "$WIDGET_PATH/Info.plist" ':NSExtension:NSExtensionPointIdentifier' 'com.apple.widgetkit-extension'

assert_entitlement_has_group "$ROOT_DIR/ios/SchoolHelperIOS/SchoolHelperIOS.entitlements"
assert_entitlement_has_group "$ROOT_DIR/ios/SchoolHelperWidget/SchoolHelperWidget.entitlements"
assert_source_contains "$ROOT_DIR/ios/SchoolHelperIOS.xcodeproj/project.pbxproj" 'SystemCapabilities = { com.apple.ApplicationGroups.iOS = { enabled = 1; }; };'
assert_source_contains "$ROOT_DIR/ios/SchoolHelperIOS/Core/Storage/AppStorageConfig.swift" "static let appGroupSuiteName = \"$APP_GROUP\""
assert_source_contains "$ROOT_DIR/ios/SchoolHelperWidget/SchoolHelperWidget.swift" 'schoolhelper://settings'
assert_source_contains "$ROOT_DIR/ios/SchoolHelperWidget/SchoolHelperWidget.swift" 'schoolhelper://timetable'

cat <<EOF_REPORT

Widget simulator packaging verified:
  App: $APP_PATH
  Widget: $WIDGET_PATH
  Extension point: com.apple.widgetkit-extension
  App Group entitlement/config source: $APP_GROUP
  Widget tap routes: schoolhelper://settings, schoolhelper://timetable
EOF_REPORT
