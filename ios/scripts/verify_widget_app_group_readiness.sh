#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/misschool-ios-widget-app-group-readiness}"
WIDGET_SIM_DERIVED_DATA_PATH="${WIDGET_SIM_DERIVED_DATA_PATH:-$DERIVED_DATA_PATH/widget-sim}"
RUN_DEVICE_BUILD="${RUN_DEVICE_BUILD:-0}"
ALLOW_PROFILE_BLOCKED="${ALLOW_PROFILE_BLOCKED:-0}"
TEAM_ID="${TEAM_ID:-${DEVELOPMENT_TEAM:-}}"

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

section "Widget simulator package/config smoke"
DERIVED_DATA_PATH="$WIDGET_SIM_DERIVED_DATA_PATH" \
  "$ROOT_DIR/ios/scripts/test_widget_sim.sh"

section "Local App Group provisioning profiles"
if "$ROOT_DIR/ios/scripts/check_app_group_profiles.py"; then
  PROFILES_READY=1
else
  PROFILES_READY=0
fi

if [[ "$PROFILES_READY" != "1" ]]; then
  cat <<'EOF_REPORT'

Widget/App Group readiness: BLOCKED_BY_PROVISIONING_PROFILE

The iOS app target profile includes the App Group, but at least one checked
profile is missing it. For the current bundle ids this usually means the widget
extension profile must be regenerated after enabling:

  group.com.leebyungsun.schoolhelperios

Required next steps:
  1. Enable the App Group for both app and widget identifiers in Apple Developer/Xcode.
  2. Refresh local provisioning profiles.
  3. Re-run this script.
  4. Then run with RUN_DEVICE_BUILD=1 TEAM_ID=... for full real-device signing.

EOF_REPORT

  if is_truthy "$ALLOW_PROFILE_BLOCKED"; then
    echo "ALLOW_PROFILE_BLOCKED=$ALLOW_PROFILE_BLOCKED, returning success after documenting the blocker."
    exit 0
  fi

  exit 10
fi

if is_truthy "$RUN_DEVICE_BUILD"; then
  section "Full App Group real-device build/install smoke"
  if [[ -z "$TEAM_ID" ]]; then
    echo "TEAM_ID is required when RUN_DEVICE_BUILD=1." >&2
    exit 2
  fi

  ENTITLEMENTS_MODE=app-groups \
    APP_GROUP_PROFILE_CHECK=strict \
    TEAM_ID="$TEAM_ID" \
    LAUNCH=0 \
    DERIVED_DATA_PATH="$DERIVED_DATA_PATH/device-app-groups" \
    "$ROOT_DIR/ios/scripts/install_device.sh"
else
  cat <<'EOF_REPORT'

Widget/App Group readiness: PROFILES_READY

Simulator package/config and local App Group profiles are ready.
Set RUN_DEVICE_BUILD=1 TEAM_ID=... to run the full real-device App Group
signing/install smoke.
EOF_REPORT
fi
