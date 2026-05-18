import type { MealInfo, SchoolEvent, TimetableItem } from "./neis/types.ts";
import type { CachedListResult, CacheStatus } from "./school-api.ts";

type SettledListResult<T> = PromiseSettledResult<CachedListResult<T>>;

export type DashboardSectionState<T> = {
  items: T[];
  error: string | null;
  cacheStatus: CacheStatus;
  cachedAt: number | null;
};

export type ResolvedDashboardData = {
  timetable: DashboardSectionState<TimetableItem>;
  meals: DashboardSectionState<MealInfo>;
  schedules: DashboardSectionState<SchoolEvent>;
};

const DEFAULT_TIMETABLE_ERROR = "시간표를 불러오지 못했어요.";
const DEFAULT_MEAL_ERROR = "급식 정보를 불러오지 못했어요.";
const DEFAULT_SCHEDULE_ERROR = "학사 일정을 불러오지 못했어요.";
const PARTIAL_SCHEDULE_ERROR =
  "일정 일부를 불러오지 못해 확인된 일정만 먼저 표시하고 있어요.";

function mergeSchedules(...scheduleLists: SchoolEvent[][]) {
  const merged = new Map<string, SchoolEvent>();

  for (const items of scheduleLists) {
    for (const event of items) {
      merged.set(`${event.date}:${event.title}:${event.description}`, event);
    }
  }

  return Array.from(merged.values()).sort((a, b) => a.date.localeCompare(b.date));
}

function mergeCacheStatus(statuses: CacheStatus[]) {
  if (statuses.some((status) => status === "stale-fallback")) {
    return "stale-fallback" satisfies CacheStatus;
  }

  if (statuses.length > 0 && statuses.every((status) => status === "cache")) {
    return "cache" satisfies CacheStatus;
  }

  return "network" satisfies CacheStatus;
}

function getErrorMessage(error: unknown, fallbackMessage: string) {
  return error instanceof Error ? error.message : fallbackMessage;
}

function getRejectedReason<T>(results: SettledListResult<T>[]) {
  return results.find((result) => result.status === "rejected")?.reason;
}

function resolveSection<T>(
  result: SettledListResult<T>,
  fallbackMessage: string,
): DashboardSectionState<T> {
  if (result.status === "fulfilled") {
    return {
      items: result.value.items,
      error: null,
      cacheStatus: result.value.cacheStatus,
      cachedAt: result.value.cachedAt,
    };
  }

  return {
    items: [],
    error: getErrorMessage(result.reason, fallbackMessage),
    cacheStatus: "network",
    cachedAt: null,
  };
}

export function resolveDashboardData({
  timetableResult,
  mealResult,
  currentMonthScheduleResult,
  nextMonthScheduleResult,
}: {
  timetableResult: SettledListResult<TimetableItem>;
  mealResult: SettledListResult<MealInfo>;
  currentMonthScheduleResult: SettledListResult<SchoolEvent>;
  nextMonthScheduleResult: SettledListResult<SchoolEvent>;
}): ResolvedDashboardData {
  const timetable = resolveSection(timetableResult, DEFAULT_TIMETABLE_ERROR);
  const meals = resolveSection(mealResult, DEFAULT_MEAL_ERROR);
  const successfulScheduleResults = [
    currentMonthScheduleResult,
    nextMonthScheduleResult,
  ].filter(
    (
      result,
    ): result is PromiseFulfilledResult<CachedListResult<SchoolEvent>> =>
      result.status === "fulfilled",
  );
  const scheduleFailureReason = getRejectedReason([
    currentMonthScheduleResult,
    nextMonthScheduleResult,
  ]);

  const schedules: DashboardSectionState<SchoolEvent> =
    successfulScheduleResults.length === 0
      ? {
          items: [],
          error: getErrorMessage(scheduleFailureReason, DEFAULT_SCHEDULE_ERROR),
          cacheStatus: "network",
          cachedAt: null,
        }
      : {
          items: mergeSchedules(
            ...successfulScheduleResults.map((result) => result.value.items),
          ),
          error:
            successfulScheduleResults.length < 2
              ? PARTIAL_SCHEDULE_ERROR
              : null,
          cacheStatus: mergeCacheStatus(
            successfulScheduleResults.map((result) => result.value.cacheStatus),
          ),
          cachedAt:
            successfulScheduleResults
              .map((result) => result.value.cachedAt)
              .filter((value): value is number => value !== null)
              .sort((a, b) => b - a)[0] ?? null,
        };

  return {
    timetable,
    meals,
    schedules,
  };
}
