#!/usr/bin/env bash
set -euo pipefail

require_config=false
if [[ "${1:-}" == "--require-config" ]]; then
  require_config=true
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

failures=0
warns=0

pass() { printf '✅ %s\n' "$1"; }
warn() { printf '⚠️  %s\n' "$1"; warns=$((warns + 1)); }
fail() { printf '❌ %s\n' "$1"; failures=$((failures + 1)); }
contains() {
  local file="$1"
  local needle="$2"
  [[ -f "$file" ]] && grep -Fq "$needle" "$file"
}

printf '# Firebase Crashlytics 설정 점검\n\n'

# Android static wiring
contains android/gradle/libs.versions.toml 'firebaseBom = "34.14.1"' \
  && pass 'Android Firebase BoM 버전 설정 확인' \
  || fail 'Android Firebase BoM 버전 설정 누락'
contains android/gradle/libs.versions.toml 'google-services = { id = "com.google.gms.google-services"' \
  && pass 'Android Google Services Gradle Plugin 등록 확인' \
  || fail 'Android Google Services Gradle Plugin 등록 누락'
contains android/gradle/libs.versions.toml 'firebase-crashlytics = { id = "com.google.firebase.crashlytics"' \
  && pass 'Android Crashlytics Gradle Plugin 등록 확인' \
  || fail 'Android Crashlytics Gradle Plugin 등록 누락'
contains android/app/build.gradle.kts 'implementation(libs.firebase.crashlytics)' \
  && pass 'Android Crashlytics SDK 의존성 확인' \
  || fail 'Android Crashlytics SDK 의존성 누락'
contains android/app/build.gradle.kts 'implementation(libs.firebase.analytics)' \
  && pass 'Android Analytics SDK 의존성 확인' \
  || fail 'Android Analytics SDK 의존성 누락'
contains android/app/build.gradle.kts 'if (googleServicesFile.exists())' \
  && pass 'Android Firebase 설정 파일 조건부 플러그인 적용 확인' \
  || fail 'Android Firebase 설정 파일 조건부 플러그인 적용 누락'

# iOS static wiring
contains ios/SchoolHelper/App/FirebaseCrashReporting.swift 'FirebaseApp.configure()' \
  && pass 'iOS Firebase 초기화 코드 확인' \
  || fail 'iOS Firebase 초기화 코드 누락'
contains ios/SchoolHelper/App/FirebaseCrashReporting.swift 'Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist")' \
  && pass 'iOS GoogleService-Info.plist 존재 가드 확인' \
  || fail 'iOS GoogleService-Info.plist 존재 가드 누락'
contains ios/SchoolHelper.xcodeproj/project.pbxproj 'FirebaseCrashlytics' \
  && pass 'iOS FirebaseCrashlytics 패키지 연결 확인' \
  || fail 'iOS FirebaseCrashlytics 패키지 연결 누락'
contains ios/SchoolHelper.xcodeproj/project.pbxproj 'GoogleService-Info.plist in Resources' \
  && pass 'iOS Firebase 설정 파일 Resource 등록 확인' \
  || fail 'iOS Firebase 설정 파일 Resource 등록 누락'
contains ios/SchoolHelper.xcodeproj/project.pbxproj 'Upload Crashlytics dSYMs' \
  && pass 'iOS Crashlytics dSYM 업로드 스크립트 확인' \
  || fail 'iOS Crashlytics dSYM 업로드 스크립트 누락'
contains ios/Package.swift 'App/FirebaseCrashReporting.swift' \
  && pass 'iOS SwiftPM 코어 빌드 제외 설정 확인' \
  || fail 'iOS SwiftPM 코어 빌드 제외 설정 누락'

# Console config files
if [[ -f android/app/google-services.json ]]; then
  pass 'Android google-services.json 존재'
else
  if $require_config; then
    fail 'Android google-services.json 없음: android/app/google-services.json'
  else
    warn 'Android google-services.json 없음: Firebase 콘솔에서 받아 android/app/google-services.json에 추가 필요'
  fi
fi

if [[ -f ios/SchoolHelper/Resources/GoogleService-Info.plist ]]; then
  pass 'iOS GoogleService-Info.plist 존재'
else
  if $require_config; then
    fail 'iOS GoogleService-Info.plist 없음: ios/SchoolHelper/Resources/GoogleService-Info.plist'
  else
    warn 'iOS GoogleService-Info.plist 없음: Firebase 콘솔에서 받아 ios/SchoolHelper/Resources/GoogleService-Info.plist에 추가 필요'
  fi
fi

printf '\n# 결과\n'
if (( failures > 0 )); then
  printf '실패: %d, 경고: %d\n' "$failures" "$warns"
  exit 1
fi
printf '통과: 정적 설정 정상, 경고: %d\n' "$warns"
