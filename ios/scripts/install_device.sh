#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
PROJECT_PATH="${PROJECT_PATH:-$ROOT_DIR/ios/SchoolHelperIOS.xcodeproj}"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/misschool-ios-device}"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
SCHEME="${SCHEME:-SchoolHelperIOS}"
BUNDLE_ID="${BUNDLE_ID:-com.leebyungsun.schoolhelperios}"
CONFIGURATION="${CONFIGURATION:-Debug}"
DEVICE_ID="${DEVICE_ID:-}"
TEAM_ID="${TEAM_ID:-${DEVELOPMENT_TEAM:-}}"
LAUNCH="${LAUNCH:-1}"
ENTITLEMENTS_MODE="${ENTITLEMENTS_MODE:-device-preview}"

export DEVELOPER_DIR

if [[ -z "$TEAM_ID" ]]; then
  cat >&2 <<'EOF'
TEAM_ID is required for real-device signing.

Example:
  TEAM_ID=YOUR_TEAM_ID ios/scripts/install_device.sh

For local preview without App Groups, this script defaults to:
  ENTITLEMENTS_MODE=device-preview

For full widget/App Group validation, enable the App Group capability in the
Apple Developer portal/provisioning profiles, then run:
  ENTITLEMENTS_MODE=app-groups TEAM_ID=YOUR_TEAM_ID ios/scripts/install_device.sh
EOF
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

APP_PATH="$DERIVED_DATA_PATH/Build/Products/$CONFIGURATION-iphoneos/SchoolHelperIOS.app"

echo "Device: $DEVICE_ID"
echo "Team: $TEAM_ID"
echo "Entitlements mode: $ENTITLEMENTS_MODE"
echo "DerivedData: $DERIVED_DATA_PATH"

xcodebuild \
  -project "$PROJECT_PATH" \
  -scheme "$SCHEME" \
  -destination "platform=iOS,id=$DEVICE_ID" \
  -configuration "$CONFIGURATION" \
  -derivedDataPath "$DERIVED_DATA_PATH" \
  -allowProvisioningUpdates \
  "${BUILD_SETTINGS[@]}" \
  build

xcrun devicectl device install app \
  --device "$DEVICE_ID" \
  "$APP_PATH" \
  --timeout 60

if [[ "$LAUNCH" == "1" || "$LAUNCH" == "true" || "$LAUNCH" == "yes" ]]; then
  xcrun devicectl device process launch \
    --device "$DEVICE_ID" \
    --terminate-existing \
    "$BUNDLE_ID" \
    --timeout 30
fi

cat <<EOF

Installed app: $BUNDLE_ID
Device: $DEVICE_ID
EOF
