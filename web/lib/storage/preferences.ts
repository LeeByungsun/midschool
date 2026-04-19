import { browserStorage } from "@/lib/storage/browser-storage";

export type StudentPreferences = {
  grade: string;
  classroom: string;
};

const STUDENT_PREFERENCES_KEY = "midschool:web:student-preferences";
export const STUDENT_PREFERENCES_UPDATED_EVENT =
  "midschool:web:student-preferences-updated";

const normalizeValue = (value: string) => value.trim();

export const isStudentPreferencesComplete = (
  value: StudentPreferences | null,
): value is StudentPreferences =>
  Boolean(value?.grade?.trim() && value?.classroom?.trim());

export function readStudentPreferences(): StudentPreferences | null {
  const raw = browserStorage.getItem(STUDENT_PREFERENCES_KEY);

  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as Partial<StudentPreferences>;
    const grade = normalizeValue(parsed.grade ?? "");
    const classroom = normalizeValue(parsed.classroom ?? "");

    if (!grade || !classroom) {
      return null;
    }

    return { grade, classroom };
  } catch {
    return null;
  }
}

export function saveStudentPreferences(value: StudentPreferences) {
  const normalized = {
    grade: normalizeValue(value.grade),
    classroom: normalizeValue(value.classroom),
  };

  const wasSaved = browserStorage.setItem(
    STUDENT_PREFERENCES_KEY,
    JSON.stringify(normalized),
  );

  if (wasSaved && typeof window !== "undefined") {
    window.dispatchEvent(
      new CustomEvent(STUDENT_PREFERENCES_UPDATED_EVENT, {
        detail: normalized,
      }),
    );
  }

  return wasSaved;
}

export function clearStudentPreferences() {
  const wasRemoved = browserStorage.removeItem(STUDENT_PREFERENCES_KEY);

  if (wasRemoved && typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(STUDENT_PREFERENCES_UPDATED_EVENT));
  }

  return wasRemoved;
}

export function formatStudentPreferences(
  value: StudentPreferences | null,
  fallback = "학년/반 설정 필요",
) {
  if (!isStudentPreferencesComplete(value)) {
    return fallback;
  }

  return `${value.grade}학년 ${value.classroom}반`;
}
