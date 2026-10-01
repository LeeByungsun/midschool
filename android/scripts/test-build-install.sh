#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_SCRIPT="$SCRIPT_DIR/build-install.sh"

fail() {
    echo "FAIL: $*" >&2
    exit 1
}

assert_contains() {
    local file="$1"
    local expected="$2"
    grep -F -- "$expected" "$file" >/dev/null ||
        fail "Expected '$expected' in $file"
}

[[ -f "$SOURCE_SCRIPT" ]] || fail "Missing $SOURCE_SCRIPT"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

android_dir="$tmp_dir/android"
mkdir -p "$android_dir/scripts" "$tmp_dir/bin" "$tmp_dir/log" "$tmp_dir/fake-jbr/bin"
cat >"$tmp_dir/fake-jbr/bin/java" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
chmod +x "$tmp_dir/fake-jbr/bin/java"
cp "$SOURCE_SCRIPT" "$android_dir/scripts/build-install.sh"
chmod +x "$android_dir/scripts/build-install.sh"

cat >"$android_dir/gradlew" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
echo "$*" >"$TEST_LOG/gradle.log"
echo "${JAVA_HOME:-}" >"$TEST_LOG/java-home.log"
mkdir -p app/build/outputs/apk/qa
: >app/build/outputs/apk/qa/app-qa.apk
EOF
chmod +x "$android_dir/gradlew"

cat >"$tmp_dir/bin/adb" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
if [[ "${1:-}" == "devices" ]]; then
    printf 'List of devices attached\n%s\tdevice\n' "${FAKE_DEVICE_SERIAL:-test-device}"
    exit 0
fi
echo "$*" >>"$TEST_LOG/adb.log"
EOF
chmod +x "$tmp_dir/bin/adb"

export TEST_LOG="$tmp_dir/log"
export FAKE_DEVICE_SERIAL="test-device"
export ANDROID_STUDIO_JBR="$tmp_dir/fake-jbr"
export JAVA_HOME=""

PATH="$tmp_dir/bin:$PATH" "$android_dir/scripts/build-install.sh" qa \
    >"$tmp_dir/log/output.log"

assert_contains "$tmp_dir/log/gradle.log" ":app:assembleQa"
assert_contains "$tmp_dir/log/java-home.log" "$tmp_dir/fake-jbr"
assert_contains "$tmp_dir/log/adb.log" \
    "-s test-device install -r $android_dir/app/build/outputs/apk/qa/app-qa.apk"
assert_contains "$tmp_dir/log/adb.log" \
    "-s test-device shell am start -n com.lbs.schoolhelper.qa/com.lbs.schoolhelper.SplashActivity"
assert_contains "$tmp_dir/log/output.log" "설치 및 실행 완료"

if PATH="$tmp_dir/bin:$PATH" "$android_dir/scripts/build-install.sh" invalid \
    >"$tmp_dir/log/invalid.log" 2>&1; then
    fail "Invalid variant unexpectedly succeeded"
fi
assert_contains "$tmp_dir/log/invalid.log" "지원하지 않는 빌드 변형"

if PATH="$tmp_dir/bin:$PATH" "$android_dir/scripts/build-install.sh" release \
    >"$tmp_dir/log/release.log" 2>&1; then
    fail "Unsigned release variant unexpectedly succeeded"
fi
assert_contains "$tmp_dir/log/release.log" "지원하지 않는 빌드 변형"

PATH="$tmp_dir/bin:$PATH" "$android_dir/scripts/build-install.sh" --help \
    >"$tmp_dir/log/help.log"
assert_contains "$tmp_dir/log/help.log" "사용법:"
assert_contains "$tmp_dir/log/help.log" "qa|debug"

echo "PASS: build-install.sh"
