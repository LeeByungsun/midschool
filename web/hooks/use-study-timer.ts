"use client";

/** 타이머 저장소를 구독해 타이머 스냅샷을 화면과 동기화하는 훅입니다. */

import { useSyncExternalStore } from "react";

import {
  readStudyTimerState,
  STUDY_TIMER_UPDATED_EVENT,
} from "@/lib/storage/timer";

const SERVER_TIMER_STATE = {
  snapshot: null,
  settings: { notificationsEnabled: false },
  todayStats: {
    dateKey: "",
    completedSessions: 0,
    totalMinutes: 0,
    focusMinutes: 0,
    breakMinutes: 0,
    lastCompletedAt: null,
    lastCompletedLabel: null,
  },
} as const;

function subscribe(callback: () => void) {
  if (typeof window === "undefined") {
    return () => {};
  }

  window.addEventListener("storage", callback);
  window.addEventListener(STUDY_TIMER_UPDATED_EVENT, callback);

  return () => {
    window.removeEventListener("storage", callback);
    window.removeEventListener(STUDY_TIMER_UPDATED_EVENT, callback);
  };
}

function getSnapshot() {
  return readStudyTimerState();
}

function getServerSnapshot() {
  return SERVER_TIMER_STATE;
}

export function useStudyTimer() {
  return useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
}
