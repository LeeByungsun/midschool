import assert from "node:assert/strict";
import test from "node:test";

import { formatDateKey, getWeekDates } from "../lib/date.ts";

test("getWeekDates returns the Monday-to-Sunday range for a weekday", () => {
  const weekKeys = getWeekDates(new Date(2026, 4, 20)).map(formatDateKey);

  assert.deepEqual(weekKeys, [
    "20260518",
    "20260519",
    "20260520",
    "20260521",
    "20260522",
    "20260523",
    "20260524",
  ]);
});

test("getWeekDates keeps Sunday inside the same Monday-starting week", () => {
  const weekKeys = getWeekDates(new Date(2026, 4, 24)).map(formatDateKey);

  assert.deepEqual(weekKeys, [
    "20260518",
    "20260519",
    "20260520",
    "20260521",
    "20260522",
    "20260523",
    "20260524",
  ]);
});
