#!/usr/bin/env python3
"""Validate manually collected iOS system-level verification evidence."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_EVIDENCE_PATH = ROOT / "ios/system-evidence.local.json"
REQUIRED_TRUE_FIELDS = [
    "home_widget_placed",
    "home_widget_content_verified",
    "home_widget_tap_opens_app",
    "app_group_device_build_verified",
    "app_group_shared_data_verified",
    "notification_banner_verified",
    "notification_sound_or_vibration_verified",
]
REQUIRED_TEXT_FIELDS = ["verified_at", "device", "tester", "notes"]


def evidence_path() -> Path:
    if len(sys.argv) > 2:
        print("Usage: validate_ios_system_evidence.py [evidence.json]", file=sys.stderr)
        raise SystemExit(2)
    if len(sys.argv) == 2:
        return Path(sys.argv[1]).expanduser().resolve()
    return DEFAULT_EVIDENCE_PATH


def load_json(path: Path) -> tuple[dict[str, Any] | None, list[str]]:
    if not path.exists():
        return None, [f"Missing evidence file: {path}"]
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        return None, [f"Invalid JSON: {error}"]
    if not isinstance(payload, dict):
        return None, ["Evidence root must be a JSON object"]
    return payload, []


def validate(payload: dict[str, Any] | None, pre_errors: list[str]) -> dict[str, Any]:
    missing = list(pre_errors)
    evidence: list[str] = []

    if payload is not None:
        for field in REQUIRED_TRUE_FIELDS:
            if payload.get(field) is True:
                evidence.append(field)
            else:
                missing.append(f"{field} must be true")

        for field in REQUIRED_TEXT_FIELDS:
            value = payload.get(field)
            if isinstance(value, str) and value.strip():
                evidence.append(f"{field}: {value.strip()}")
            else:
                missing.append(f"{field} must be a non-empty string")

    return {
        "complete": not missing,
        "status": "pass" if not missing else "manual_pending",
        "evidence": evidence,
        "missing": missing,
    }


def main() -> int:
    path = evidence_path()
    payload, pre_errors = load_json(path)
    result = validate(payload, pre_errors)
    result["path"] = str(path)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["complete"] else 30


if __name__ == "__main__":
    raise SystemExit(main())
