/** 타이머 스냅샷을 날짜 기준으로 저장하고 복원하는 모듈입니다. */

import { formatDateKey } from "@/lib/date";
import { browserStorage } from "@/lib/storage/browser-storage";
import {
  createTimerSnapshot,
  syncTimerSnapshot,
  type TimerSnapshot,
} from "@/lib/timer";

const TIMER_STORAGE_KEY = "midschool:web:study-timer";
const TIMER_SETTINGS_STORAGE_KEY = "midschool:web:study-timer-settings";
const TIMER_DAILY_STATS_STORAGE_KEY = "midschool:web:study-timer-daily-stats";

export const STUDY_TIMER_UPDATED_EVENT = "midschool:web:study-timer-updated";

export type TimerSettings = {
  notificationsEnabled: boolean;
};

export type TimerDailyStats = {
  dateKey: string;
  completedSessions: number;
  totalMinutes: number;
  focusMinutes: number;
  breakMinutes: number;
  lastCompletedAt: number | null;
  lastCompletedLabel: string | null;
};

let cachedRawSnapshot: string | null = null;
let cachedParsedSnapshot: TimerSnapshot | null = null;
let cachedRawSettings: string | null = null;
let cachedParsedSettings: TimerSettings = { notificationsEnabled: false };
let cachedRawDailyStats: string | null = null;
let cachedParsedDailyStats: TimerDailyStats | null = null;
let cachedStudyTimerState:
  | {
      snapshot: TimerSnapshot | null;
      settings: TimerSettings;
      todayStats: TimerDailyStats;
    }
  | null = null;

function dispatchTimerUpdate(detail?: unknown) {
  if (typeof window === "undefined") {
    return;
  }

  window.dispatchEvent(
    new CustomEvent(STUDY_TIMER_UPDATED_EVENT, {
      detail,
    }),
  );
}

function createEmptyDailyStats(now = Date.now()): TimerDailyStats {
  return {
    dateKey: formatDateKey(new Date(now)),
    completedSessions: 0,
    totalMinutes: 0,
    focusMinutes: 0,
    breakMinutes: 0,
    lastCompletedAt: null,
    lastCompletedLabel: null,
  };
}

function normalizeTimerSnapshot(value: unknown): TimerSnapshot | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const candidate = value as Partial<TimerSnapshot>;

  if (
    candidate.version !== 1 ||
    typeof candidate.label !== "string" ||
    typeof candidate.durationMs !== "number" ||
    typeof candidate.remainingMs !== "number" ||
    typeof candidate.updatedAt !== "number"
  ) {
    return null;
  }

  const baseSnapshot = createTimerSnapshot(
    candidate.label,
    candidate.durationMs,
    candidate.updatedAt,
  );

  return syncTimerSnapshot({
    ...baseSnapshot,
    remainingMs: Math.max(0, Math.round(candidate.remainingMs)),
    status:
      candidate.status === "running" ||
      candidate.status === "paused" ||
      candidate.status === "completed"
        ? candidate.status
        : "idle",
    targetTime:
      typeof candidate.targetTime === "number" ? Math.round(candidate.targetTime) : null,
    startedAt:
      typeof candidate.startedAt === "number" ? Math.round(candidate.startedAt) : null,
    updatedAt: Math.round(candidate.updatedAt),
    completedAt:
      typeof candidate.completedAt === "number"
        ? Math.round(candidate.completedAt)
        : null,
  });
}

function normalizeTimerSettings(value: unknown): TimerSettings {
  if (!value || typeof value !== "object") {
    return { notificationsEnabled: false };
  }

  const candidate = value as Partial<TimerSettings>;

  return {
    notificationsEnabled: Boolean(candidate.notificationsEnabled),
  };
}

function normalizeTimerDailyStats(value: unknown, now = Date.now()): TimerDailyStats {
  if (!value || typeof value !== "object") {
    return createEmptyDailyStats(now);
  }

  const candidate = value as Partial<TimerDailyStats>;
  const todayKey = formatDateKey(new Date(now));

  if (candidate.dateKey !== todayKey) {
    return createEmptyDailyStats(now);
  }

  return {
    dateKey: todayKey,
    completedSessions: Math.max(0, Math.round(candidate.completedSessions ?? 0)),
    totalMinutes: Math.max(0, Math.round(candidate.totalMinutes ?? 0)),
    focusMinutes: Math.max(0, Math.round(candidate.focusMinutes ?? 0)),
    breakMinutes: Math.max(0, Math.round(candidate.breakMinutes ?? 0)),
    lastCompletedAt:
      typeof candidate.lastCompletedAt === "number"
        ? Math.round(candidate.lastCompletedAt)
        : null,
    lastCompletedLabel:
      typeof candidate.lastCompletedLabel === "string"
        ? candidate.lastCompletedLabel
        : null,
  };
}

function isBreakPreset(label: string) {
  return label.trim() === "휴식";
}

export function readTimerSnapshot() {
  const raw = browserStorage.getItem(TIMER_STORAGE_KEY);

  if (raw === cachedRawSnapshot) {
    return cachedParsedSnapshot;
  }

  if (!raw) {
    cachedRawSnapshot = null;
    cachedParsedSnapshot = null;
    return null;
  }

  try {
    const parsed = normalizeTimerSnapshot(JSON.parse(raw));
    cachedRawSnapshot = raw;
    cachedParsedSnapshot = parsed;
    return parsed;
  } catch {
    cachedRawSnapshot = raw;
    cachedParsedSnapshot = null;
    return null;
  }
}

export function saveTimerSnapshot(snapshot: TimerSnapshot) {
  const normalized = syncTimerSnapshot(snapshot);
  const wasSaved = browserStorage.setItem(
    TIMER_STORAGE_KEY,
    JSON.stringify(normalized),
  );

  if (wasSaved) {
    dispatchTimerUpdate(normalized);
  }

  return wasSaved;
}

export function clearTimerSnapshot() {
  const wasRemoved = browserStorage.removeItem(TIMER_STORAGE_KEY);

  if (wasRemoved) {
    dispatchTimerUpdate();
  }

  return wasRemoved;
}

export function readTimerSettings() {
  const raw = browserStorage.getItem(TIMER_SETTINGS_STORAGE_KEY);

  if (raw === cachedRawSettings) {
    return cachedParsedSettings;
  }

  if (!raw) {
    cachedRawSettings = null;
    cachedParsedSettings = { notificationsEnabled: false };
    return cachedParsedSettings;
  }

  try {
    const parsed = normalizeTimerSettings(JSON.parse(raw));
    cachedRawSettings = raw;
    cachedParsedSettings = parsed;
    return parsed;
  } catch {
    cachedRawSettings = raw;
    cachedParsedSettings = { notificationsEnabled: false };
    return cachedParsedSettings;
  }
}

export function saveTimerSettings(settings: TimerSettings) {
  const normalized = normalizeTimerSettings(settings);
  const wasSaved = browserStorage.setItem(
    TIMER_SETTINGS_STORAGE_KEY,
    JSON.stringify(normalized),
  );

  if (wasSaved) {
    dispatchTimerUpdate(normalized);
  }

  return wasSaved;
}

export function readTodayTimerStats(now = Date.now()) {
  const raw = browserStorage.getItem(TIMER_DAILY_STATS_STORAGE_KEY);
  const todayKey = formatDateKey(new Date(now));

  if (
    raw === cachedRawDailyStats &&
    cachedParsedDailyStats &&
    cachedParsedDailyStats.dateKey === todayKey
  ) {
    return cachedParsedDailyStats;
  }

  if (!raw) {
    const emptyStats = createEmptyDailyStats(now);
    cachedRawDailyStats = null;
    cachedParsedDailyStats = emptyStats;
    return emptyStats;
  }

  try {
    const parsed = normalizeTimerDailyStats(JSON.parse(raw), now);
    cachedRawDailyStats = raw;
    cachedParsedDailyStats = parsed;
    return parsed;
  } catch {
    const emptyStats = createEmptyDailyStats(now);
    cachedRawDailyStats = raw;
    cachedParsedDailyStats = emptyStats;
    return emptyStats;
  }
}

export function saveTodayTimerStats(stats: TimerDailyStats) {
  const normalized = normalizeTimerDailyStats(stats);
  const wasSaved = browserStorage.setItem(
    TIMER_DAILY_STATS_STORAGE_KEY,
    JSON.stringify(normalized),
  );

  if (wasSaved) {
    dispatchTimerUpdate(normalized);
  }

  return wasSaved;
}

export function recordTimerCompletion(snapshot: TimerSnapshot, now = Date.now()) {
  const currentStats = readTodayTimerStats(now);
  const durationMinutes = Math.round(snapshot.durationMs / (60 * 1000));
  const isBreak = isBreakPreset(snapshot.label);

  const nextStats: TimerDailyStats = {
    dateKey: currentStats.dateKey,
    completedSessions: currentStats.completedSessions + 1,
    totalMinutes: currentStats.totalMinutes + durationMinutes,
    focusMinutes: currentStats.focusMinutes + (isBreak ? 0 : durationMinutes),
    breakMinutes: currentStats.breakMinutes + (isBreak ? durationMinutes : 0),
    lastCompletedAt: now,
    lastCompletedLabel: snapshot.label,
  };

  saveTodayTimerStats(nextStats);

  return nextStats;
}

export function readStudyTimerState(now = Date.now()) {
  const snapshot = readTimerSnapshot();
  const settings = readTimerSettings();
  const todayStats = readTodayTimerStats(now);

  if (
    cachedStudyTimerState &&
    cachedStudyTimerState.snapshot === snapshot &&
    cachedStudyTimerState.settings === settings &&
    cachedStudyTimerState.todayStats === todayStats
  ) {
    return cachedStudyTimerState;
  }

  cachedStudyTimerState = {
    snapshot,
    settings,
    todayStats,
  };

  return cachedStudyTimerState;
}
