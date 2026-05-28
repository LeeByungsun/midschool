#!/usr/bin/env python3
"""Verify live NEIS and notices BFF data used by the iOS app.

This script intentionally uses only Python's standard library so it can run on a
fresh macOS/Xcode machine without adding project dependencies.
"""

from __future__ import annotations

import datetime as dt
import html
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Any

NEIS_BASE_URL = os.environ.get("NEIS_BASE_URL", "https://open.neis.go.kr/")
WEB_BASE_URL = os.environ.get("WEB_BASE_URL", "https://midschool.vercel.app/")
NEIS_API_KEY = os.environ.get("NEIS_API_KEY", "").strip()

SCHOOL_NAME = os.environ.get("SCHOOL_NAME", "미사중학교")
OFFICE_CODE = os.environ.get("OFFICE_CODE", "J10")
SCHOOL_CODE = os.environ.get("SCHOOL_CODE", "7692129")
SCHOOL_KIND = os.environ.get("SCHOOL_KIND", "중학교")
GRADE = os.environ.get("GRADE", "1")
CLASSROOM = os.environ.get("CLASSROOM", "2")
VERIFY_DATE = os.environ.get("DATE", dt.datetime.now().strftime("%Y%m%d"))
VERIFY_MONTH = os.environ.get("MONTH", VERIFY_DATE[:6])
NOTICE_LIMIT = int(os.environ.get("NOTICE_LIMIT", "3"))
TIMEOUT_SECONDS = float(os.environ.get("TIMEOUT_SECONDS", "20"))
FETCH_RETRIES = int(os.environ.get("FETCH_RETRIES", "2"))


def main() -> int:
    checks: list[dict[str, Any]] = []

    school_rows = neis_rows(
        "hub/schoolInfo",
        {"SCHUL_NM": SCHOOL_NAME},
        section="schoolInfo",
        label="학교 검색",
        include_school_context=False,
    )
    matching_school = next(
        (
            row
            for row in school_rows
            if row.get("SD_SCHUL_CODE") == SCHOOL_CODE
            and row.get("ATPT_OFCDC_SC_CODE") == OFFICE_CODE
        ),
        None,
    )
    require(matching_school is not None, f"{SCHOOL_NAME} 학교 검색 결과에서 {OFFICE_CODE}/{SCHOOL_CODE} 를 찾지 못했습니다.")
    checks.append(
        {
            "check": "schoolInfo",
            "count": len(school_rows),
            "sample": matching_school.get("SCHUL_NM"),
        }
    )

    meal_rows = neis_rows(
        "hub/mealServiceDietInfo",
        {"MLSV_YMD": VERIFY_DATE},
        section="mealServiceDietInfo",
        label="급식",
    )
    require(meal_rows, f"{VERIFY_DATE} 급식 live row가 없습니다.")
    checks.append(
        {
            "check": "mealServiceDietInfo",
            "date": VERIFY_DATE,
            "count": len(meal_rows),
            "sample": clean_text(meal_rows[0].get("DDISH_NM", ""))[:80],
        }
    )

    timetable_section = timetable_endpoint(SCHOOL_KIND)
    timetable_rows = neis_rows(
        f"hub/{timetable_section}",
        {
            "GRADE": GRADE,
            "CLASS_NM": CLASSROOM,
            "ALL_TI_YMD": VERIFY_DATE,
        },
        section=timetable_section,
        label="시간표",
    )
    require(timetable_rows, f"{VERIFY_DATE} {GRADE}학년 {CLASSROOM}반 시간표 live row가 없습니다.")
    checks.append(
        {
            "check": timetable_section,
            "date": VERIFY_DATE,
            "count": len(timetable_rows),
            "sample": timetable_rows[0].get("ITRT_CNTNT", ""),
        }
    )

    schedule_rows = neis_rows(
        "hub/SchoolSchedule",
        {"AA_YMD": VERIFY_MONTH},
        section="SchoolSchedule",
        label="학사 일정",
    )
    require(schedule_rows, f"{VERIFY_MONTH} 학사 일정 live row가 없습니다.")
    checks.append(
        {
            "check": "SchoolSchedule",
            "month": VERIFY_MONTH,
            "count": len(schedule_rows),
            "sample": schedule_rows[0].get("EVENT_NM", ""),
        }
    )

    notices_payload = fetch_json(
        url_join(WEB_BASE_URL, "api/notices"),
        {
            "officeCode": OFFICE_CODE,
            "schoolCode": SCHOOL_CODE,
            "limit": str(NOTICE_LIMIT),
        },
    )
    notice_status = str(notices_payload.get("status", ""))
    require(notice_status in {"success", "empty"}, f"notices BFF status가 실패입니다: {notice_status}")
    notices = notices_payload.get("items") or []
    require(notices, "notices BFF live item이 없습니다.")
    checks.append(
        {
            "check": "noticesBFF",
            "status": notice_status,
            "count": len(notices),
            "sample": notices[0].get("title", ""),
        }
    )

    print(json.dumps({"status": "ok", "checks": checks}, ensure_ascii=False, indent=2))
    return 0


def timetable_endpoint(school_kind: str) -> str:
    stripped = school_kind.strip()
    if stripped == "초등학교":
        return "elsTimetable"
    if stripped == "고등학교":
        return "hisTimetable"
    return "misTimetable"


def neis_rows(
    endpoint: str,
    params: dict[str, str],
    *,
    section: str,
    label: str,
    include_school_context: bool = True,
) -> list[dict[str, Any]]:
    query = {
        "Type": "json",
        "pIndex": "1",
        "pSize": "100",
    }
    if NEIS_API_KEY:
        query["KEY"] = NEIS_API_KEY
    if include_school_context:
        query["ATPT_OFCDC_SC_CODE"] = OFFICE_CODE
        query["SD_SCHUL_CODE"] = SCHOOL_CODE
    query.update({key: value for key, value in params.items() if value})

    payload = fetch_json(url_join(NEIS_BASE_URL, endpoint), query)
    validate_neis_result(payload.get("RESULT"), label)
    sections = payload.get(section) or []
    for head in (head_item for item in sections for head_item in item.get("head", [])):
        validate_neis_result(head.get("RESULT"), label)
    return sections[1].get("row", []) if len(sections) > 1 else []


def validate_neis_result(result: Any, label: str) -> None:
    if not isinstance(result, dict):
        return
    code = str(result.get("CODE", "")).strip()
    message = str(result.get("MESSAGE", "")).strip()
    if code and code not in {"INFO-000", "INFO-200"}:
        raise RuntimeError(f"{label} NEIS 오류: {code} {message}")


def fetch_json(base_url: str, params: dict[str, str]) -> dict[str, Any]:
    url = f"{base_url}?{urllib.parse.urlencode(params)}"
    last_error: Exception | None = None

    for attempt in range(1, FETCH_RETRIES + 2):
        request = urllib.request.Request(url, headers={"Accept": "*/*"})
        try:
            with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
                charset = response.headers.get_content_charset() or "utf-8"
                return json.loads(response.read().decode(charset, "replace"))
        except urllib.error.HTTPError as error:
            body = error.read().decode("utf-8", "replace")[:500]
            last_error = RuntimeError(f"HTTP {error.code} for {url}: {body}")
            if error.code < 500 or attempt > FETCH_RETRIES:
                break
            time.sleep(0.5 * attempt)
        except (TimeoutError, urllib.error.URLError) as error:
            last_error = RuntimeError(f"Request failed for {url}: {error}")
            if attempt > FETCH_RETRIES:
                break
            time.sleep(0.5 * attempt)

    assert last_error is not None
    raise last_error


def url_join(base_url: str, path: str) -> str:
    return urllib.parse.urljoin(base_url.rstrip("/") + "/", path)


def clean_text(raw: str) -> str:
    text = re.sub(r"<br\s*/?>", " / ", raw, flags=re.IGNORECASE)
    text = re.sub(r"<[^>]+>", " ", text)
    return re.sub(r"\s+", " ", html.unescape(text)).strip()


def require(condition: Any, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:  # noqa: BLE001 - CLI should print concise failure.
        print(json.dumps({"status": "error", "message": str(error)}, ensure_ascii=False, indent=2), file=sys.stderr)
        raise SystemExit(1)
