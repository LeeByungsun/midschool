/** 타이머 상태 계산과 스냅샷 변환에 쓰는 순수 유틸을 제공합니다. */

export type TimerStatus = "idle" | "running" | "paused" | "completed";

export type TimerSnapshot = {
  version: 1;
  label: string;
  durationMs: number;
  remainingMs: number;
  status: TimerStatus;
  targetTime: number | null;
  startedAt: number | null;
  updatedAt: number;
  completedAt: number | null;
};

export type TimerView = {
  label: string;
  durationMs: number;
  remainingMs: number;
  elapsedMs: number;
  progress: number;
  status: TimerStatus;
  isActive: boolean;
  isCompleted: boolean;
};

export const TIMER_TICK_MS = 1000;
export const MINUTE_MS = 60 * 1000;

const clampMs = (value: number) => Math.max(0, Math.round(value));

const clampProgress = (value: number) => Math.min(1, Math.max(0, value));

export function createTimerSnapshot(
  label: string,
  durationMs: number,
  now = Date.now(),
): TimerSnapshot {
  const normalizedDuration = clampMs(durationMs);

  return {
    version: 1,
    label,
    durationMs: normalizedDuration,
    remainingMs: normalizedDuration,
    status: "idle",
    targetTime: null,
    startedAt: null,
    updatedAt: now,
    completedAt: null,
  };
}

export function syncTimerSnapshot(
  snapshot: TimerSnapshot,
  now = Date.now(),
): TimerSnapshot {
  if (snapshot.status !== "running" || snapshot.targetTime === null) {
    return snapshot;
  }

  const remainingMs = clampMs(snapshot.targetTime - now);

  if (remainingMs > 0) {
    if (remainingMs === snapshot.remainingMs) {
      return snapshot;
    }

    return {
      ...snapshot,
      remainingMs,
      updatedAt: now,
    };
  }

  return {
    ...snapshot,
    remainingMs: 0,
    status: "completed",
    targetTime: null,
    updatedAt: now,
    completedAt: snapshot.completedAt ?? now,
  };
}

export function selectTimerPreset(
  label: string,
  minutes: number,
  now = Date.now(),
): TimerSnapshot {
  return createTimerSnapshot(label, minutes * MINUTE_MS, now);
}

export function startTimer(snapshot: TimerSnapshot, now = Date.now()): TimerSnapshot {
  const synced = syncTimerSnapshot(snapshot, now);
  const remainingMs =
    synced.status === "completed" ? synced.durationMs : synced.remainingMs;

  return {
    ...synced,
    remainingMs,
    status: "running",
    startedAt: now,
    targetTime: now + remainingMs,
    updatedAt: now,
    completedAt: null,
  };
}

export function pauseTimer(snapshot: TimerSnapshot, now = Date.now()): TimerSnapshot {
  const synced = syncTimerSnapshot(snapshot, now);

  if (synced.status !== "running") {
    return synced;
  }

  return {
    ...synced,
    remainingMs: clampMs((synced.targetTime ?? now) - now),
    status: "paused",
    targetTime: null,
    updatedAt: now,
  };
}

export function resumeTimer(snapshot: TimerSnapshot, now = Date.now()): TimerSnapshot {
  const synced = syncTimerSnapshot(snapshot, now);

  if (synced.status !== "paused") {
    return synced;
  }

  return {
    ...synced,
    status: "running",
    startedAt: now,
    targetTime: now + synced.remainingMs,
    updatedAt: now,
  };
}

export function resetTimer(snapshot: TimerSnapshot, now = Date.now()): TimerSnapshot {
  return {
    ...snapshot,
    remainingMs: snapshot.durationMs,
    status: "idle",
    targetTime: null,
    startedAt: null,
    updatedAt: now,
    completedAt: null,
  };
}

export function getTimerView(
  snapshot: TimerSnapshot,
  now = Date.now(),
): TimerView {
  const synced = syncTimerSnapshot(snapshot, now);
  const elapsedMs = clampMs(synced.durationMs - synced.remainingMs);

  return {
    label: synced.label,
    durationMs: synced.durationMs,
    remainingMs: synced.remainingMs,
    elapsedMs,
    progress:
      synced.durationMs === 0 ? 0 : clampProgress(elapsedMs / synced.durationMs),
    status: synced.status,
    isActive: synced.status === "running",
    isCompleted: synced.status === "completed",
  };
}

export function formatTimerClock(remainingMs: number) {
  const totalSeconds = Math.ceil(clampMs(remainingMs) / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}
