#!/usr/bin/env python3
"""Deterministic local HTTP fixtures for SchoolHelper UI tests."""

from __future__ import annotations

import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse


def neis_payload(section: str, rows: list[dict[str, str]]) -> dict[str, object]:
    return {
        section: [
            {"head": [{"RESULT": {"CODE": "INFO-000", "MESSAGE": "정상 처리되었습니다."}}]},
            {"row": rows},
        ]
    }


class FixtureHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:  # noqa: N802 - BaseHTTPRequestHandler API
        request = urlparse(self.path)
        query = parse_qs(request.query)

        if request.path == "/health":
            self.respond({"status": "ok"})
            return

        if request.path == "/hub/schoolInfo":
            self.respond(
                neis_payload(
                    "schoolInfo",
                    [
                        {
                            "ATPT_OFCDC_SC_CODE": "J10",
                            "ATPT_OFCDC_SC_NM": "경기도교육청",
                            "SD_SCHUL_CODE": "7692129",
                            "SCHUL_NM": "미사중학교",
                            "SCHUL_KND_SC_NM": "중학교",
                            "ORG_RDNMA": "경기도 하남시",
                        }
                    ],
                )
            )
            return

        if request.path == "/hub/mealServiceDietInfo":
            date = query.get("MLSV_YMD", [""])[0]
            self.respond(
                neis_payload(
                    "mealServiceDietInfo",
                    [
                        {
                            "MLSV_YMD": date,
                            "MMEAL_SC_NM": "중식",
                            "DDISH_NM": f"비빔밥<br/>샘플 급식 {date}",
                            "CAL_INFO": "712 kcal",
                        }
                    ],
                )
            )
            return

        if request.path == "/hub/SchoolSchedule":
            month = query.get("AA_YMD", [""])[0]
            self.respond(
                neis_payload(
                    "SchoolSchedule",
                    [
                        {"AA_YMD": f"{month}23", "EVENT_NM": "체육대회", "EVENT_CNTNT": "운동장"},
                        {"AA_YMD": f"{month}24", "EVENT_NM": "중간고사", "EVENT_CNTNT": "교실"},
                    ],
                )
            )
            return

        if request.path == "/api/schedule":
            month = query.get("date", [""])[0]
            self.respond({"items": [
                {"date": f"{month}23", "title": "체육대회", "description": "운동장"},
                {"date": f"{month}24", "title": "중간고사", "description": "교실"},
            ]})
            return

        if request.path == "/api/timetable":
            date = query.get("date", [""])[0]
            subjects = ["국어", "수학", "과학", "영어", "사회", "수업6", "수업7"]
            self.respond(
                {
                    "items": [
                        {
                            "date": date,
                            "period": str(index),
                            "subject": f"{subject} {date}" if index <= 2 else subject,
                            "grade": query.get("grade", ["1"])[0],
                            "classroom": query.get("classroom", ["2"])[0],
                        }
                        for index, subject in enumerate(subjects, start=1)
                    ]
                }
            )
            return

        if request.path == "/api/notices":
            self.respond(
                {
                    "status": "success",
                    "items": [
                        {
                            "id": "fixture-notice",
                            "title": "현장학습 안내",
                            "date": "2026-05-20",
                            "author": "미사중학교",
                            "url": "https://example.com/notice",
                            "sourceUrl": "https://example.com/notices",
                        }
                    ],
                    "message": None,
                }
            )
            return

        self.send_error(404)

    def respond(self, payload: dict[str, object]) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format: str, *args: object) -> None:
        return


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=18080)
    args = parser.parse_args()
    ThreadingHTTPServer((args.host, args.port), FixtureHandler).serve_forever()


if __name__ == "__main__":
    main()
