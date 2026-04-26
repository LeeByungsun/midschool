import type {
  MealInfo,
  SchoolEvent,
  SchoolInfo,
  TimetableItem,
} from "@/lib/neis/types";
import { readCache, writeCache } from "@/lib/storage/cache";

type ApiListResponse<T> = {
  items: T[];
  message?: string;
};

export type CacheStatus = "network" | "cache" | "stale-fallback";

export type CachedListResult<T> = {
  items: T[];
  cacheStatus: CacheStatus;
  cachedAt: number | null;
};

function formatElapsedMinutes(cachedAt: number) {
  const diffMs = Math.max(Date.now() - cachedAt, 0);
  const diffMinutes = Math.floor(diffMs / (60 * 1000));

  if (diffMinutes < 1) {
    return "방금 전";
  }

  if (diffMinutes < 60) {
    return `${diffMinutes}분 전`;
  }

  const diffHours = Math.floor(diffMinutes / 60);

  if (diffHours < 24) {
    return `${diffHours}시간 전`;
  }

  return `${Math.floor(diffHours / 24)}일 전`;
}

export function formatCacheStatusMessage(
  cacheStatus: CacheStatus,
  cachedAt: number | null,
  subject: string,
) {
  if (!cachedAt) {
    return null;
  }

  const savedLabel = formatElapsedMinutes(cachedAt);

  if (cacheStatus === "cache") {
    return `${subject} 최신 요청 대신 ${savedLabel} 저장된 캐시를 표시하고 있어요.`;
  }

  if (cacheStatus === "stale-fallback") {
    return `최신 ${subject}을 불러오지 못해 ${savedLabel} 저장된 마지막 성공 데이터를 대신 표시하고 있어요.`;
  }

  return null;
}

type CacheOptions = {
  key: string;
  ttlMs: number;
  fallbackTtlMs?: number;
};

async function fetchList<T>(path: string, cacheOptions?: CacheOptions) {
  const cached = cacheOptions ? readCache<T[]>(cacheOptions.key) : null;

  if (cached && !cached.isExpired) {
    return {
      items: cached.items,
      cacheStatus: "cache",
      cachedAt: cached.savedAt,
    } satisfies CachedListResult<T>;
  }

  const response = await fetch(path, {
    method: "GET",
    cache: "no-store",
  });

  const json = (await response.json()) as ApiListResponse<T>;

  if (!response.ok) {
    if (cached) {
      return {
        items: cached.items,
        cacheStatus: "stale-fallback",
        cachedAt: cached.savedAt,
      } satisfies CachedListResult<T>;
    }

    throw new Error(json.message ?? "데이터를 불러오지 못했어요.");
  }

  if (cacheOptions) {
    writeCache(cacheOptions.key, json.items, {
      ttlMs: cacheOptions.ttlMs,
      fallbackTtlMs: cacheOptions.fallbackTtlMs,
    });
  }

  return {
    items: json.items,
    cacheStatus: "network",
    cachedAt: null,
  } satisfies CachedListResult<T>;
}

function buildMealCacheKey(params: {
  officeCode: string;
  schoolCode: string;
  date: string;
}) {
  return `meal:${params.officeCode}:${params.schoolCode}:${params.date}`;
}

function buildTimetableCacheKey(params: {
  officeCode: string;
  schoolCode: string;
  schoolKind?: string;
  grade: string;
  classroom: string;
  date: string;
}) {
  return `timetable:${params.officeCode}:${params.schoolCode}:${params.schoolKind ?? "중학교"}:${params.grade}:${params.classroom}:${params.date}`;
}

function buildScheduleCacheKey(params: {
  officeCode: string;
  schoolCode: string;
  date: string;
}) {
  return `schedule:${params.officeCode}:${params.schoolCode}:${params.date}`;
}

export function fetchTimetable(params: {
  officeCode: string;
  schoolCode: string;
  schoolKind?: string;
  grade: string;
  classroom: string;
  date: string;
}) {
  const search = new URLSearchParams({
    officeCode: params.officeCode,
    schoolCode: params.schoolCode,
    schoolKind: params.schoolKind ?? "",
    grade: params.grade,
    classroom: params.classroom,
    date: params.date,
  });

  return fetchList<TimetableItem>(`/api/timetable?${search.toString()}`, {
    key: buildTimetableCacheKey(params),
    ttlMs: 24 * 60 * 60 * 1000,
    fallbackTtlMs: 48 * 60 * 60 * 1000,
  });
}

export function fetchMeals(params: {
  officeCode: string;
  schoolCode: string;
  date: string;
}) {
  const search = new URLSearchParams({
    officeCode: params.officeCode,
    schoolCode: params.schoolCode,
    date: params.date,
  });

  return fetchList<MealInfo>(`/api/meals?${search.toString()}`, {
    key: buildMealCacheKey(params),
    ttlMs: 12 * 60 * 60 * 1000,
    fallbackTtlMs: 36 * 60 * 60 * 1000,
  });
}

export function fetchSchedules(params: {
  officeCode: string;
  schoolCode: string;
  date: string;
}) {
  const search = new URLSearchParams({
    officeCode: params.officeCode,
    schoolCode: params.schoolCode,
    date: params.date,
  });

  return fetchList<SchoolEvent>(`/api/schedule?${search.toString()}`, {
    key: buildScheduleCacheKey(params),
    ttlMs: 12 * 60 * 60 * 1000,
    fallbackTtlMs: 36 * 60 * 60 * 1000,
  });
}

export function fetchSchools(params: { query: string }) {
  const search = new URLSearchParams({
    query: params.query,
  });

  return fetchList<SchoolInfo>(`/api/schools?${search.toString()}`).then(
    (result) => result.items,
  );
}

export async function fetchSchoolByCode(params: {
  officeCode: string;
  schoolCode: string;
}) {
  const search = new URLSearchParams({
    officeCode: params.officeCode,
    schoolCode: params.schoolCode,
  });

  const result = await fetchList<SchoolInfo>(`/api/schools?${search.toString()}`);

  return result.items[0] ?? null;
}
