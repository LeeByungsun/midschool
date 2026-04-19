import type { MealInfo, SchoolEvent, TimetableItem } from "@/lib/neis/types";

type ApiListResponse<T> = {
  items: T[];
  message?: string;
};

async function fetchList<T>(path: string) {
  const response = await fetch(path, {
    method: "GET",
    cache: "no-store",
  });

  const json = (await response.json()) as ApiListResponse<T>;

  if (!response.ok) {
    throw new Error(json.message ?? "데이터를 불러오지 못했어요.");
  }

  return json.items;
}

export function fetchTimetable(params: {
  grade: string;
  classroom: string;
  date: string;
}) {
  const search = new URLSearchParams({
    grade: params.grade,
    classroom: params.classroom,
    date: params.date,
  });

  return fetchList<TimetableItem>(`/api/timetable?${search.toString()}`);
}

export function fetchMeals(params: { date: string }) {
  const search = new URLSearchParams({
    date: params.date,
  });

  return fetchList<MealInfo>(`/api/meals?${search.toString()}`);
}

export function fetchSchedules(params: { date: string }) {
  const search = new URLSearchParams({
    date: params.date,
  });

  return fetchList<SchoolEvent>(`/api/schedule?${search.toString()}`);
}
