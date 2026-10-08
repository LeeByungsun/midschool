#!/usr/bin/env bash

set -euo pipefail

usage() {
    cat <<'EOF'
사용법:
  android/scripts/build-install.sh [qa|qa-release|debug]

기본 빌드 변형은 qa입니다.
여러 단말이 연결된 경우 ANDROID_SERIAL을 지정하세요.

예시:
  android/scripts/build-install.sh
  android/scripts/build-install.sh qa
  android/scripts/build-install.sh qa-release
  ANDROID_SERIAL=R3XXXXXXXXX android/scripts/build-install.sh debug
EOF
}

fail() {
    echo "오류: $*" >&2
    exit 1
}

resolve_java() {
    if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
        return
    fi

    if command -v java >/dev/null 2>&1 && java -version >/dev/null 2>&1; then
        return
    fi

    local candidate
    for candidate in \
        "${ANDROID_STUDIO_JBR:-}" \
        "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
        "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"; do
        if [[ -x "$candidate/bin/java" ]]; then
            export JAVA_HOME="$candidate"
            export PATH="$JAVA_HOME/bin:$PATH"
            return
        fi
    done

    return 1
}

resolve_adb() {
    if command -v adb >/dev/null 2>&1; then
        command -v adb
        return
    fi

    local candidate
    for candidate in \
        "${ANDROID_SDK_ROOT:-}/platform-tools/adb" \
        "${ANDROID_HOME:-}/platform-tools/adb" \
        "$HOME/Library/Android/sdk/platform-tools/adb" \
        "$HOME/Android/Sdk/platform-tools/adb"; do
        if [[ -x "$candidate" ]]; then
            printf '%s\n' "$candidate"
            return
        fi
    done

    return 1
}

variant="${1:-qa}"

case "$variant" in
    -h|--help)
        usage
        exit 0
        ;;
    qa)
        gradle_variant="Qa"
        apk_variant_dir="qa"
        application_id="com.lbs.schoolhelper.qa"
        ;;
    qa-release)
        gradle_variant="QaRelease"
        apk_variant_dir="qaRelease"
        application_id="com.lbs.schoolhelper.qa"
        ;;
    debug)
        gradle_variant="Debug"
        apk_variant_dir="debug"
        application_id="com.lbs.schoolhelper"
        ;;
    *)
        fail "지원하지 않는 빌드 변형입니다: $variant (qa|qa-release|debug)"
        ;;
esac

[[ "$#" -le 1 ]] || fail "인수는 빌드 변형 하나만 사용할 수 있습니다."

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
GRADLEW="$ANDROID_DIR/gradlew"
ADB="$(resolve_adb)" || fail "adb를 찾을 수 없습니다. Android SDK platform-tools를 확인하세요."

[[ -x "$GRADLEW" ]] || fail "Gradle 실행 파일을 찾을 수 없습니다: $GRADLEW"

connected_devices=()
while IFS=$'\t' read -r serial state _; do
    if [[ -n "$serial" && "$state" == "device" ]]; then
        connected_devices+=("$serial")
    fi
done < <("$ADB" devices | tail -n +2)

if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    selected_device=""
    for serial in "${connected_devices[@]}"; do
        if [[ "$serial" == "$ANDROID_SERIAL" ]]; then
            selected_device="$serial"
            break
        fi
    done
    [[ -n "$selected_device" ]] ||
        fail "ANDROID_SERIAL=$ANDROID_SERIAL 단말이 연결·승인 상태가 아닙니다."
else
    case "${#connected_devices[@]}" in
        0)
            fail "연결·승인된 Android 단말이 없습니다. USB 디버깅 상태를 확인하세요."
            ;;
        1)
            selected_device="${connected_devices[0]}"
            ;;
        *)
            printf '연결된 단말:\n' >&2
            printf '  %s\n' "${connected_devices[@]}" >&2
            fail "여러 단말이 연결되어 있습니다. ANDROID_SERIAL을 지정하세요."
            ;;
    esac
fi

echo "빌드 변형: $variant"
echo "대상 단말: $selected_device"

resolve_java || fail "Java Runtime을 찾을 수 없습니다. Android Studio JBR 또는 JAVA_HOME을 확인하세요."

(
    cd "$ANDROID_DIR"
    "$GRADLEW" ":app:assemble$gradle_variant" --console=plain
)

apk_dir="$ANDROID_DIR/app/build/outputs/apk/$apk_variant_dir"
apk_files=()
if [[ -d "$apk_dir" ]]; then
    while IFS= read -r apk; do
        apk_files+=("$apk")
    done < <(find "$apk_dir" -maxdepth 1 -type f -name '*.apk' -print | sort)
fi

case "${#apk_files[@]}" in
    0)
        fail "생성된 APK를 찾을 수 없습니다: $apk_dir"
        ;;
    1)
        apk_path="${apk_files[0]}"
        ;;
    *)
        printf '발견된 APK:\n' >&2
        printf '  %s\n' "${apk_files[@]}" >&2
        fail "설치할 APK를 하나로 결정할 수 없습니다."
        ;;
esac

echo "APK: $apk_path"
"$ADB" -s "$selected_device" install -r "$apk_path"
"$ADB" -s "$selected_device" shell am start -n \
    "$application_id/com.lbs.schoolhelper.SplashActivity"

echo "설치 및 실행 완료: $application_id"
