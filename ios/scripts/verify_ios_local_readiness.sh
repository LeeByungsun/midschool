#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)"
DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/misschool-ios-local-readiness}"
RUN_SWIFT_TESTS="${RUN_SWIFT_TESTS:-1}"
RUN_WIDGET_READINESS="${RUN_WIDGET_READINESS:-1}"
RUN_GOAL_AUDIT="${RUN_GOAL_AUDIT:-1}"
RUN_LIVE_BACKEND="${RUN_LIVE_BACKEND:-0}"
ALLOW_INCOMPLETE_GOAL_AUDIT="${ALLOW_INCOMPLETE_GOAL_AUDIT:-1}"

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

section "Python syntax checks"
python3 -m py_compile \
  "$ROOT_DIR/ios/scripts/audit_ios_goal_readiness.py" \
  "$ROOT_DIR/ios/scripts/check_app_group_profiles.py" \
  "$ROOT_DIR/ios/scripts/validate_ios_system_evidence.py" \
  "$ROOT_DIR/ios/scripts/verify_live_school_data.py"

section "Shell syntax checks"
bash -n \
  "$ROOT_DIR/ios/scripts/test_widget_sim.sh" \
  "$ROOT_DIR/ios/scripts/verify_widget_app_group_readiness.sh" \
  "$ROOT_DIR/ios/scripts/refresh_app_group_profiles.sh" \
  "$ROOT_DIR/ios/scripts/verify_ios_local_readiness.sh" \
  "$ROOT_DIR/ios/scripts/install_device.sh" \
  "$ROOT_DIR/ios/scripts/test_device_ui.sh" \
  "$ROOT_DIR/ios/scripts/verify_device_notification.sh"

if is_truthy "$RUN_SWIFT_TESTS"; then
  section "SwiftPM core regression tests"
  swift test --package-path "$ROOT_DIR/ios"
fi

if is_truthy "$RUN_WIDGET_READINESS"; then
  section "Widget package/config readiness"
  ALLOW_PROFILE_BLOCKED=1 \
    DERIVED_DATA_PATH="$DERIVED_DATA_PATH/widget-app-group-readiness" \
    "$ROOT_DIR/ios/scripts/verify_widget_app_group_readiness.sh"
fi

if is_truthy "$RUN_LIVE_BACKEND"; then
  section "Live NEIS/BFF backend smoke"
  "$ROOT_DIR/ios/scripts/verify_live_school_data.py"
fi

if is_truthy "$RUN_GOAL_AUDIT"; then
  section "Goal completion audit"
  set +e
  "$ROOT_DIR/ios/scripts/audit_ios_goal_readiness.py"
  audit_status=$?
  set -e

  if [[ "$audit_status" == "0" ]]; then
    echo "Goal audit complete=true."
  elif [[ "$audit_status" == "20" ]] && is_truthy "$ALLOW_INCOMPLETE_GOAL_AUDIT"; then
    echo "Goal audit is incomplete as expected until system-level manual evidence is recorded."
  else
    echo "Unexpected goal audit status: $audit_status" >&2
    exit "$audit_status"
  fi
fi

cat <<EOF_REPORT

Local iOS readiness checks completed.
DerivedData: $DERIVED_DATA_PATH
EOF_REPORT
