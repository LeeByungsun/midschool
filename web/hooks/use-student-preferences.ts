"use client";

import { useSyncExternalStore } from "react";

import {
  readStudentPreferences,
  STUDENT_PREFERENCES_UPDATED_EVENT,
} from "@/lib/storage/preferences";

function subscribe(callback: () => void) {
  if (typeof window === "undefined") {
    return () => {};
  }

  window.addEventListener("storage", callback);
  window.addEventListener(STUDENT_PREFERENCES_UPDATED_EVENT, callback);

  return () => {
    window.removeEventListener("storage", callback);
    window.removeEventListener(STUDENT_PREFERENCES_UPDATED_EVENT, callback);
  };
}

function getSnapshot() {
  return readStudentPreferences();
}

function getServerSnapshot() {
  return null;
}

export function useStudentPreferences() {
  return useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
}
