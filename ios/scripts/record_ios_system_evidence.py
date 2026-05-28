#!/usr/bin/env python3
"""Record local, manually observed iOS system-level evidence.

This helper intentionally requires explicit flags for each visual/system UX
claim. It never infers that a home-screen widget or physical notification banner
was observed from automated smoke tests alone.
"""
from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone, timedelta
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_PATH = ROOT / "ios/system-evidence.local.json"
TEMPLATE_PATH = ROOT / "ios/system-evidence.template.json"
MANUAL_FLAGS = {
    "home_widget_placed": "Home-screen widget was placed on the real iPhone.",
    "home_widget_content_verified": "Home-screen widget rendered expected school/timetable content.",
    "home_widget_tap_opens_app": "Tapping the home-screen widget opened the app/expected route.",
    "notification_banner_verified": "A real iOS notification banner was observed.",
    "notification_sound_or_vibration_verified": "Notification sound or vibration was observed.",
}
AUTOMATED_FLAGS = {
    "app_group_device_build_verified": "Full App Group real-device build/install passed.",
    "app_group_shared_data_verified": "Real-device App Group shared data was copied and verified.",
}


def load_json(path: Path) -> dict[str, Any]:
    source = path if path.exists() and path.stat().st_size > 0 else TEMPLATE_PATH
    if source.exists():
        payload = json.loads(source.read_text(encoding="utf-8"))
        if isinstance(payload, dict):
            return payload
    return {}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--path", type=Path, default=DEFAULT_PATH, help="Evidence JSON path")
    parser.add_argument("--device", help="Device description to store")
    parser.add_argument("--tester", help="Tester name/description to store")
    parser.add_argument("--notes", help="Replace notes with this text")
    parser.add_argument("--append-note", action="append", default=[], help="Append a note line")
    parser.add_argument("--preserve-verified-at", action="store_true", help="Do not update verified_at")

    parser.add_argument("--all-manual-verified", action="store_true", help="Set all manual visual UX fields to true")
    for field, help_text in MANUAL_FLAGS.items():
        parser.add_argument(f"--{field.replace('_', '-')}", action="store_true", help=help_text)
    for field, help_text in AUTOMATED_FLAGS.items():
        parser.add_argument(f"--{field.replace('_', '-')}", action="store_true", help=help_text)

    return parser.parse_args()


def main() -> int:
    args = parse_args()
    path = args.path.expanduser().resolve()
    payload = load_json(path)

    if not args.preserve_verified_at:
        payload["verified_at"] = datetime.now(timezone(timedelta(hours=9))).isoformat(timespec="seconds")
    if args.device:
        payload["device"] = args.device
    elif not str(payload.get("device", "")).strip() or payload.get("device") == "iPhone model/name and iOS version":
        payload["device"] = "iPhone 15 Pro buggyani / 00008130-0012603E3CC3001C / iOS 26.5 (23F77)"
    if args.tester:
        payload["tester"] = args.tester
    elif not str(payload.get("tester", "")).strip() or payload.get("tester") == "person or agent recording the manual verification":
        payload["tester"] = "Manual tester plus Codex/Xcode devicectl automation"

    for field in AUTOMATED_FLAGS:
        if getattr(args, field):
            payload[field] = True
    for field in MANUAL_FLAGS:
        if args.all_manual_verified or getattr(args, field):
            payload[field] = True

    if args.notes is not None:
        payload["notes"] = args.notes
    notes = str(payload.get("notes", "")).strip()
    for note in args.append_note:
        note = note.strip()
        if not note:
            continue
        notes = f"{notes}\n{note}" if notes else note
    if notes:
        payload["notes"] = notes
    elif not str(payload.get("notes", "")).strip():
        payload["notes"] = "Manual evidence not fully recorded yet."

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {path}")
    print(json.dumps(payload, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
