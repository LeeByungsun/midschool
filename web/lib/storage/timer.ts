import { browserStorage } from "@/lib/storage/browser-storage";
import {
  createTimerSnapshot,
  syncTimerSnapshot,
  type TimerSnapshot,
} from "@/lib/timer";

const TIMER_STORAGE_KEY = "midschool:web:study-timer";
export const STUDY_TIMER_UPDATED_EVENT = "midschool:web:study-timer-updated";

let cachedRawSnapshot: string | null = null;
let cachedParsedSnapshot: TimerSnapshot | null = null;

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

  if (wasSaved && typeof window !== "undefined") {
    window.dispatchEvent(
      new CustomEvent(STUDY_TIMER_UPDATED_EVENT, {
        detail: normalized,
      }),
    );
  }

  return wasSaved;
}

export function clearTimerSnapshot() {
  const wasRemoved = browserStorage.removeItem(TIMER_STORAGE_KEY);

  if (wasRemoved && typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(STUDY_TIMER_UPDATED_EVENT));
  }

  return wasRemoved;
}
