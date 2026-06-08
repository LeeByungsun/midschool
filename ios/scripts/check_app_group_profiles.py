#!/usr/bin/env python3
import plistlib
import subprocess
import sys
from pathlib import Path

PROFILE_DIR = Path.home() / "Library/Developer/Xcode/UserData/Provisioning Profiles"
DEFAULT_TARGETS = [
    ("com.lbs.shcoolhelper", "group.com.lbs.shcoolhelper"),
    ("com.lbs.shcoolhelper.widget", "group.com.lbs.shcoolhelper"),
]


def decode_profile(path: Path) -> dict | None:
    try:
        data = subprocess.check_output(
            ["security", "cms", "-D", "-i", str(path)],
            stderr=subprocess.DEVNULL,
        )
        return plistlib.loads(data)
    except Exception:
        return None


def profile_matches_bundle(profile: dict, bundle_id: str) -> bool:
    app_identifier = profile.get("Entitlements", {}).get("application-identifier", "")
    return app_identifier.endswith(f".{bundle_id}")


def main() -> int:
    targets = DEFAULT_TARGETS
    if len(sys.argv) > 1:
        targets = []
        for raw in sys.argv[1:]:
            try:
                bundle_id, group_id = raw.split(":", 1)
            except ValueError:
                print(f"Invalid target '{raw}'. Use bundle.id:group.id", file=sys.stderr)
                return 2
            targets.append((bundle_id, group_id))

    profiles = []
    for path in sorted(PROFILE_DIR.glob("*.mobileprovision")):
        profile = decode_profile(path)
        if profile is not None:
            profiles.append((path, profile))

    if not profiles:
        print(f"No provisioning profiles found under {PROFILE_DIR}", file=sys.stderr)
        return 1

    failed = False
    for bundle_id, group_id in targets:
        matches = [(path, profile) for path, profile in profiles if profile_matches_bundle(profile, bundle_id)]
        if not matches:
            print(f"FAIL {bundle_id}: no local provisioning profile found")
            failed = True
            continue

        ok = False
        print(f"\n{bundle_id}")
        for path, profile in matches:
            entitlements = profile.get("Entitlements", {})
            groups = entitlements.get("com.apple.security.application-groups") or []
            name = profile.get("Name", path.name)
            marker = "OK" if group_id in groups else "FAIL"
            print(f"  {marker} {name}")
            print(f"      file: {path}")
            print(f"      groups: {groups}")
            ok = ok or group_id in groups
        if not ok:
            failed = True

    if failed:
        print("\nAt least one profile is missing the required App Group entitlement.")
        print("Enable the App Group for both the app and widget identifiers, then refresh Xcode provisioning profiles.")
        return 1

    print("\nAll checked profiles include the required App Group entitlement.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
