/** 타이머 저장소 복원/공유 계약을 검증하는 경량 스크립트 테스트입니다. */

import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import ts from "typescript";

import { formatDateKey } from "../lib/date.ts";
import { createTimerSnapshot, startTimer } from "../lib/timer.ts";

const TIMER_STORAGE_KEY = "midschool:web:study-timer";
const TIMER_DAILY_STATS_STORAGE_KEY = "midschool:web:study-timer-daily-stats";

class MemoryStorage {
  #values = new Map();

  getItem(key) {
    return this.#values.has(key) ? this.#values.get(key) : null;
  }

  setItem(key, value) {
    this.#values.set(key, String(value));
  }

  removeItem(key) {
    this.#values.delete(key);
  }
}

class TestCustomEvent extends Event {
  constructor(type, init = {}) {
    super(type);
    this.detail = init.detail;
  }
}

async function loadTimerStorageModule() {
  const sourceUrl = new URL("../lib/storage/timer.ts", import.meta.url);
  const source = await readFile(sourceUrl, "utf8");
  const rewritten = source
    .replaceAll(
      '"@/lib/date"',
      JSON.stringify(new URL("../lib/date.ts", import.meta.url).href),
    )
    .replaceAll(
      '"@/lib/storage/browser-storage"',
      JSON.stringify(new URL("../lib/storage/browser-storage.ts", import.meta.url).href),
    )
    .replaceAll(
      '"@/lib/timer"',
      JSON.stringify(new URL("../lib/timer.ts", import.meta.url).href),
    );

  const transpiled = ts.transpileModule(rewritten, {
    compilerOptions: {
      module: ts.ModuleKind.ESNext,
      target: ts.ScriptTarget.ES2022,
    },
    fileName: sourceUrl.href,
  });
  const moduleUrl = `data:text/javascript;base64,${Buffer.from(transpiled.outputText).toString("base64")}#${Date.now()}-${Math.random()}`;

  return import(moduleUrl);
}

async function withMockBrowser(run) {
  const originalWindow = globalThis.window;
  const originalCustomEvent = globalThis.CustomEvent;
  const eventTarget = new EventTarget();
  const window = {
    localStorage: new MemoryStorage(),
    addEventListener: (...args) => eventTarget.addEventListener(...args),
    removeEventListener: (...args) => eventTarget.removeEventListener(...args),
    dispatchEvent: (event) => eventTarget.dispatchEvent(event),
  };

  globalThis.window = window;
  globalThis.CustomEvent = TestCustomEvent;

  try {
    return await run(window);
  } finally {
    if (originalWindow === undefined) {
      delete globalThis.window;
    } else {
      globalThis.window = originalWindow;
    }

    if (originalCustomEvent === undefined) {
      delete globalThis.CustomEvent;
    } else {
      globalThis.CustomEvent = originalCustomEvent;
    }
  }
}

test("shared timer state exposes saved snapshot, settings, stats, and update events", async () => {
  const {
    STUDY_TIMER_UPDATED_EVENT,
    readStudyTimerState,
    recordTimerCompletion,
    saveTimerSettings,
    saveTimerSnapshot,
  } = await loadTimerStorageModule();

  await withMockBrowser(async (window) => {
    const events = [];
    const now = Date.now();
    const snapshot = startTimer(createTimerSnapshot("집중", 25 * 60 * 1000, now), now);

    window.addEventListener(STUDY_TIMER_UPDATED_EVENT, (event) => {
      events.push(event.detail);
    });

    assert.equal(saveTimerSnapshot(snapshot), true);
    assert.equal(saveTimerSettings({ notificationsEnabled: true }), true);

    const stats = recordTimerCompletion(snapshot, now);
    const state = readStudyTimerState(now);

    assert.equal(state.snapshot?.label, "집중");
    assert.equal(state.snapshot?.status, "running");
    assert.equal(state.snapshot?.durationMs, 25 * 60 * 1000);
    assert.deepEqual(state.settings, { notificationsEnabled: true });
    assert.deepEqual(state.todayStats, stats);
    assert.equal(state.todayStats.completedSessions, 1);
    assert.equal(state.todayStats.focusMinutes, 25);
    assert.equal(state.todayStats.breakMinutes, 0);

    assert.equal(events.length, 3);
    assert.equal(events[0]?.status, "running");
    assert.equal(events[1]?.notificationsEnabled, true);
    assert.equal(events[2]?.completedSessions, 1);
  });
});

test("expired running snapshots are restored as completed state", async () => {
  const { readTimerSnapshot } = await loadTimerStorageModule();

  await withMockBrowser(async (window) => {
    const snapshot = startTimer(createTimerSnapshot("딥포커스", 10 * 60 * 1000, 0), 0);

    window.localStorage.setItem(TIMER_STORAGE_KEY, JSON.stringify(snapshot));

    const restored = readTimerSnapshot();

    assert.equal(restored?.status, "completed");
    assert.equal(restored?.remainingMs, 0);
    assert.ok(restored?.completedAt);
  });
});

test("daily stats reset after date rollover and keep break time separate from focus time", async () => {
  const { readTodayTimerStats, recordTimerCompletion } = await loadTimerStorageModule();

  await withMockBrowser(async (window) => {
    const now = Date.now();
    const yesterdayKey = formatDateKey(new Date(now - 24 * 60 * 60 * 1000));

    window.localStorage.setItem(
      TIMER_DAILY_STATS_STORAGE_KEY,
      JSON.stringify({
        dateKey: yesterdayKey,
        completedSessions: 9,
        totalMinutes: 180,
        focusMinutes: 150,
        breakMinutes: 30,
        lastCompletedAt: now - 1_000,
        lastCompletedLabel: "집중",
      }),
    );

    const resetStats = readTodayTimerStats(now);
    const breakStats = recordTimerCompletion(
      createTimerSnapshot("휴식", 10 * 60 * 1000, now),
      now,
    );

    assert.equal(resetStats.dateKey, formatDateKey(new Date(now)));
    assert.equal(resetStats.completedSessions, 0);
    assert.equal(resetStats.totalMinutes, 0);
    assert.equal(resetStats.focusMinutes, 0);
    assert.equal(resetStats.breakMinutes, 0);

    assert.equal(breakStats.completedSessions, 1);
    assert.equal(breakStats.totalMinutes, 10);
    assert.equal(breakStats.focusMinutes, 0);
    assert.equal(breakStats.breakMinutes, 10);
  });
});

test("clearing the saved snapshot removes the shared timer state and emits an empty update", async () => {
  const {
    STUDY_TIMER_UPDATED_EVENT,
    clearTimerSnapshot,
    readStudyTimerState,
    saveTimerSnapshot,
  } = await loadTimerStorageModule();

  await withMockBrowser(async (window) => {
    const events = [];
    const now = Date.now();

    window.addEventListener(STUDY_TIMER_UPDATED_EVENT, (event) => {
      events.push(event.detail);
    });

    saveTimerSnapshot(createTimerSnapshot("집중", 25 * 60 * 1000, now));
    assert.equal(clearTimerSnapshot(), true);

    const state = readStudyTimerState(now);

    assert.equal(state.snapshot, null);
    assert.equal(events.length, 2);
    assert.equal(events[1], undefined);
  });
});
