import type {
  MealInfo,
  SchoolEvent,
  SchoolInfo,
  TimetableItem,
} from "@/lib/neis/types";

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

  return fetchList<TimetableItem>(`/api/timetable?${search.toString()}`);
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

  return fetchList<MealInfo>(`/api/meals?${search.toString()}`);
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

  return fetchList<SchoolEvent>(`/api/schedule?${search.toString()}`);
}

export function fetchSchools(params: { query: string }) {
  const search = new URLSearchParams({
    query: params.query,
  });

  return fetchList<SchoolInfo>(`/api/schools?${search.toString()}`);
}
