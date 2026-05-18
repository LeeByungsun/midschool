import assert from "node:assert/strict";
import test from "node:test";

import { resolveDashboardData } from "../lib/dashboard-load.ts";

const timetableItem = {
  date: "20260518",
  grade: "2",
  classroom: "3",
  period: "1",
  subject: "수학",
};

const mealItem = {
  date: "20260518",
  mealType: "중식",
  menu: "비빔밥",
  calorieInfo: "500kcal",
};

const maySchedule = {
  date: "20260520",
  title: "중간고사",
  description: "",
};

const juneSchedule = {
  date: "20260602",
  title: "체험학습",
  description: "",
};

function fulfilled(value) {
  return {
    status: "fulfilled",
    value,
  };
}

function rejected(reason) {
  return {
    status: "rejected",
    reason,
  };
}

test("meal fetch failure does not clear timetable and schedules", () => {
  const result = resolveDashboardData({
    timetableResult: fulfilled({
      items: [timetableItem],
      cacheStatus: "network",
      cachedAt: null,
    }),
    mealResult: rejected(new Error("급식 API 오류")),
    currentMonthScheduleResult: fulfilled({
      items: [maySchedule],
      cacheStatus: "network",
      cachedAt: null,
    }),
    nextMonthScheduleResult: fulfilled({
      items: [juneSchedule],
      cacheStatus: "network",
      cachedAt: null,
    }),
  });

  assert.deepEqual(result.timetable.items, [timetableItem]);
  assert.deepEqual(result.meals.items, []);
  assert.equal(result.meals.error, "급식 API 오류");
  assert.deepEqual(result.schedules.items, [maySchedule, juneSchedule]);
});

test("partial schedule failure keeps the successful month and exposes a warning", () => {
  const result = resolveDashboardData({
    timetableResult: fulfilled({
      items: [timetableItem],
      cacheStatus: "cache",
      cachedAt: 100,
    }),
    mealResult: fulfilled({
      items: [mealItem],
      cacheStatus: "network",
      cachedAt: null,
    }),
    currentMonthScheduleResult: fulfilled({
      items: [maySchedule],
      cacheStatus: "stale-fallback",
      cachedAt: 300,
    }),
    nextMonthScheduleResult: rejected(new Error("다음 달 일정 오류")),
  });

  assert.deepEqual(result.schedules.items, [maySchedule]);
  assert.equal(
    result.schedules.error,
    "일정 일부를 불러오지 못해 확인된 일정만 먼저 표시하고 있어요.",
  );
  assert.equal(result.schedules.cacheStatus, "stale-fallback");
  assert.equal(result.schedules.cachedAt, 300);
});

test("full schedule failure reports an error instead of pretending the schedule is empty", () => {
  const result = resolveDashboardData({
    timetableResult: fulfilled({
      items: [],
      cacheStatus: "network",
      cachedAt: null,
    }),
    mealResult: fulfilled({
      items: [],
      cacheStatus: "network",
      cachedAt: null,
    }),
    currentMonthScheduleResult: rejected(new Error("이번 달 일정 오류")),
    nextMonthScheduleResult: rejected(new Error("다음 달 일정 오류")),
  });

  assert.deepEqual(result.schedules.items, []);
  assert.equal(result.schedules.error, "이번 달 일정 오류");
  assert.equal(result.schedules.cachedAt, null);
});
