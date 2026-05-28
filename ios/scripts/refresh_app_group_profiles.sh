#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
PROJECT_PATH="${PROJECT_PATH:-$ROOT_DIR/ios/SchoolHelperIOS.xcodeproj}"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
SCHEME="${SCHEME:-SchoolHelperIOS}"
CONFIGURATION="${CONFIGURATION:-Debug}"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/misschool-ios-app-group-profile-refresh}"
TEAM_ID="${TEAM_ID:-${DEVELOPMENT_TEAM:-}}"
PROFILE_DIR="${PROFILE_DIR:-$HOME/Library/Developer/Xcode/UserData/Provisioning Profiles}"
PROFILE_BACKUP_DIR="${PROFILE_BACKUP_DIR:-$ROOT_DIR/ios/profile-backups.local/$(date +%Y%m%d-%H%M%S)}"
APPLY="${APPLY:-0}"
RUN_XCODE_REFRESH="${RUN_XCODE_REFRESH:-0}"
APP_BUNDLE_ID="${APP_BUNDLE_ID:-com.leebyungsun.schoolhelperios}"
WIDGET_BUNDLE_ID="${WIDGET_BUNDLE_ID:-com.leebyungsun.schoolhelperios.widget}"
APP_GROUP="${APP_GROUP:-group.com.leebyungsun.schoolhelperios}"

export DEVELOPER_DIR

is_truthy() {
  case "${1:-}" in
    1|true|yes|YES|TRUE) return 0 ;;
    *) return 1 ;;
  esac
}

section() {
  printf '\n==> %s\n' "$1"
}

section "Current App Group profile status"
"$ROOT_DIR/ios/scripts/check_app_group_profiles.py" || true

MATCHING_PROFILES_FILE="$(mktemp "${TMPDIR:-/tmp}/misschool-app-group-profiles.XXXXXX")"
trap 'rm -f "$MATCHING_PROFILES_FILE"' EXIT

python3 - "$PROFILE_DIR" "$APP_BUNDLE_ID" "$WIDGET_BUNDLE_ID" > "$MATCHING_PROFILES_FILE" <<'PY'
import plistlib
import subprocess
import sys
from pathlib import Path

profile_dir = Path(sys.argv[1]).expanduser()
bundle_ids = sys.argv[2:]

for path in sorted(profile_dir.glob("*.mobileprovision")):
    try:
        data = subprocess.check_output(["security", "cms", "-D", "-i", str(path)], stderr=subprocess.DEVNULL)
        profile = plistlib.loads(data)
    except Exception:
        continue
    app_identifier = profile.get("Entitlements", {}).get("application-identifier", "")
    if any(app_identifier.endswith(f".{bundle_id}") for bundle_id in bundle_ids):
        print(path)
PY

section "Matching local profiles"
if [[ ! -s "$MATCHING_PROFILES_FILE" ]]; then
  echo "No matching local provisioning profiles found under $PROFILE_DIR"
else
  cat "$MATCHING_PROFILES_FILE"
fi

if ! is_truthy "$APPLY"; then
  cat <<EOF_REPORT

Dry run only. No profiles were moved.
To back up/remove matching local profiles, run:

  APPLY=1 ios/scripts/refresh_app_group_profiles.sh

After Apple Developer/Xcode has App Group enabled for both bundle ids,
optionally ask Xcode to regenerate/download profiles with:

  APPLY=1 RUN_XCODE_REFRESH=1 TEAM_ID=YOUR_TEAM_ID ios/scripts/refresh_app_group_profiles.sh
EOF_REPORT
  exit 0
fi

section "Backing up matching profiles"
mkdir -p "$PROFILE_BACKUP_DIR"
while IFS= read -r profile; do
  if [[ -f "$profile" ]]; then
    echo "Moving $profile -> $PROFILE_BACKUP_DIR/"
    mv "$profile" "$PROFILE_BACKUP_DIR/"
  fi
done < "$MATCHING_PROFILES_FILE"

echo "Backup dir: $PROFILE_BACKUP_DIR"

if is_truthy "$RUN_XCODE_REFRESH"; then
  if [[ -z "$TEAM_ID" ]]; then
    echo "TEAM_ID is required when RUN_XCODE_REFRESH=1." >&2
    exit 2
  fi

  section "Requesting Xcode provisioning refresh"
  xcodebuild \
    -project "$PROJECT_PATH" \
    -scheme "$SCHEME" \
    -destination "generic/platform=iOS" \
    -configuration "$CONFIGURATION" \
    -derivedDataPath "$DERIVED_DATA_PATH" \
    -allowProvisioningUpdates \
    CODE_SIGNING_ALLOWED=YES \
    CODE_SIGNING_REQUIRED=YES \
    CODE_SIGN_STYLE=Automatic \
    DEVELOPMENT_TEAM="$TEAM_ID" \
    build || true
fi

section "Post-refresh App Group profile status"
"$ROOT_DIR/ios/scripts/check_app_group_profiles.py"
