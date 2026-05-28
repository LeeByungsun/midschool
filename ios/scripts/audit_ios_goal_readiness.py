#!/usr/bin/env python3
"""Audit whether the iOS parity goal is ready to be declared complete.

This script is intentionally conservative. It verifies durable repo artifacts and
local provisioning state, then exits non-zero while any required external/system
verification is still missing.
"""
from __future__ import annotations

import json
import subprocess
from dataclasses import dataclass, asdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


@dataclass
class AuditItem:
    id: str
    status: str
    evidence: list[str]
    missing: list[str]


def exists(path: str) -> bool:
    return (ROOT / path).exists()


def files_exist(paths: list[str]) -> tuple[list[str], list[str]]:
    present = [path for path in paths if exists(path)]
    missing = [path for path in paths if not exists(path)]
    return present, missing


def item_from_files(item_id: str, paths: list[str]) -> AuditItem:
    present, missing = files_exist(paths)
    return AuditItem(
        id=item_id,
        status="pass" if not missing else "missing",
        evidence=present,
        missing=missing,
    )


def run_profile_check() -> AuditItem:
    command = [str(ROOT / "ios/scripts/check_app_group_profiles.py")]
    result = subprocess.run(command, cwd=ROOT, text=True, capture_output=True, check=False)
    evidence = ["ios/scripts/check_app_group_profiles.py"]
    if result.stdout.strip():
        evidence.append(result.stdout.strip())
    if result.stderr.strip():
        evidence.append(result.stderr.strip())

    return AuditItem(
        id="full_app_group_profiles",
        status="pass" if result.returncode == 0 else "blocked_external",
        evidence=evidence,
        missing=[] if result.returncode == 0 else [
            "Widget provisioning profile must include group.com.leebyungsun.schoolhelperios",
        ],
    )


def run_system_evidence_check() -> AuditItem:
    command = [str(ROOT / "ios/scripts/validate_ios_system_evidence.py")]
    result = subprocess.run(command, cwd=ROOT, text=True, capture_output=True, check=False)
    evidence = ["ios/scripts/validate_ios_system_evidence.py"]
    missing = [
        "Real iPhone home-screen WidgetKit placement/content/tap E2E",
        "Full App Group app/widget shared-data E2E on real iPhone",
        "Real iPhone system notification banner/sound/vibration UX",
    ]

    if result.stdout.strip():
        try:
            payload = json.loads(result.stdout)
            evidence.extend(payload.get("evidence") or [])
            missing = payload.get("missing") or missing
        except json.JSONDecodeError:
            evidence.append(result.stdout.strip())
    if result.stderr.strip():
        evidence.append(result.stderr.strip())

    return AuditItem(
        id="system_level_manual_evidence",
        status="pass" if result.returncode == 0 else "manual_pending",
        evidence=evidence,
        missing=[] if result.returncode == 0 else missing,
    )


def main() -> int:
    items: list[AuditItem] = [
        item_from_files("ios_agent_skill", [".codex/skills/ios-architecture/SKILL.md"]),
        item_from_files("ios_spec_and_audit_docs", [
            "docs/ios-project-specification.md",
            "docs/ios-runtime-verification.md",
            "docs/ios-parity-audit.md",
        ]),
        item_from_files("ios_workspace_and_targets", [
            "ios/Package.swift",
            "ios/SchoolHelperIOS.xcodeproj/project.pbxproj",
            "ios/SchoolHelperIOS/App/SchoolHelperIOSApp.swift",
            "ios/SchoolHelperWidget/SchoolHelperWidget.swift",
        ]),
        item_from_files("core_feature_surfaces", [
            "ios/SchoolHelperIOS/Features/Setup/SetupView.swift",
            "ios/SchoolHelperIOS/Features/Home/HomeView.swift",
            "ios/SchoolHelperIOS/Features/Timetable/TimetableView.swift",
            "ios/SchoolHelperIOS/Features/Meals/MealsView.swift",
            "ios/SchoolHelperIOS/Features/Schedule/ScheduleView.swift",
            "ios/SchoolHelperIOS/Features/Timer/TimerView.swift",
            "ios/SchoolHelperIOS/Features/Settings/SettingsView.swift",
            "ios/SchoolHelperIOS/Core/Networking/NEISClient.swift",
            "ios/SchoolHelperIOS/Core/Repositories/SchoolRepository.swift",
        ]),
        item_from_files("regression_and_runtime_verifiers", [
            "ios/Tests/SchoolHelperIOSCoreTests/AppStateTests.swift",
            "ios/Tests/SchoolHelperIOSCoreTests/NEISClientTests.swift",
            "ios/Tests/SchoolHelperIOSCoreTests/DefaultSchoolRepositoryTests.swift",
            "ios/Tests/SchoolHelperIOSCoreTests/HomeWidgetSnapshotLoaderTests.swift",
            "ios/Tests/SchoolHelperIOSCoreTests/NotificationPermissionCoordinatorTests.swift",
            "ios/SchoolHelperIOSUITests/SchoolHelperIOSUITests.swift",
            "ios/scripts/test_device_ui.sh",
            "ios/scripts/verify_live_school_data.py",
            "ios/scripts/test_widget_sim.sh",
            "ios/scripts/verify_widget_app_group_readiness.sh",
            "ios/scripts/validate_ios_system_evidence.py",
            "ios/system-evidence.template.json",
        ]),
        run_profile_check(),
        run_system_evidence_check(),
    ]

    complete = all(item.status == "pass" for item in items)
    summary = {
        "complete": complete,
        "status": "complete" if complete else "incomplete",
        "items": [asdict(item) for item in items],
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0 if complete else 20


if __name__ == "__main__":
    raise SystemExit(main())
