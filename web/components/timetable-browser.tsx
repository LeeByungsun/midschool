"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { DashboardCard } from "@/components/dashboard-card";
import { useHydrated } from "@/hooks/use-hydrated";
import { useStudentPreferences } from "@/hooks/use-student-preferences";
import { formatDateKey, formatKoreanDateLabel } from "@/lib/date";
import type { TimetableItem } from "@/lib/neis/types";
import { fetchTimetable } from "@/lib/school-api";

type TimetableState = {
  requestKey: string;
  items: TimetableItem[];
  error: string | null;
};

const initialState: TimetableState = {
  requestKey: "",
  items: [],
  error: null,
};

export function TimetableBrowser() {
  const hydrated = useHydrated();
  const studentInfo = useStudentPreferences();
  const [selectedDate, setSelectedDate] = useState(() => new Date());
  const [state, setState] = useState<TimetableState>(initialState);

  const dateKey = useMemo(() => formatDateKey(selectedDate), [selectedDate]);
  const dateLabel = useMemo(
    () => formatKoreanDateLabel(selectedDate),
    [selectedDate],
  );
  const requestKey = studentInfo
    ? `${studentInfo.grade}-${studentInfo.classroom}-${dateKey}`
    : "";

  useEffect(() => {
    let isCancelled = false;

    if (!hydrated || !studentInfo) {
      return;
    }

    fetchTimetable({
      grade: studentInfo.grade,
      classroom: studentInfo.classroom,
      date: dateKey,
    })
      .then((items) => {
        if (isCancelled) {
          return;
        }

        setState({
          requestKey,
          items,
          error: null,
        });
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return;
        }

        setState({
          requestKey,
          items: [],
          error:
            error instanceof Error
              ? error.message
              : "시간표 정보를 불러오지 못했어요.",
        });
      });

    return () => {
      isCancelled = true;
    };
  }, [dateKey, hydrated, requestKey, studentInfo]);

  const isLoading = hydrated && Boolean(studentInfo) && state.requestKey !== requestKey;

  const moveDate = (offset: number) => {
    setSelectedDate((prev) => {
      const next = new Date(prev);
      next.setDate(prev.getDate() + offset);
      return next;
    });
  };

  return (
    <DashboardCard
      title="일간 시간표"
      subtitle={`${dateLabel} 기준`}
      action={
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => moveDate(-1)}
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            이전 날
          </button>
          <button
            type="button"
            onClick={() => moveDate(1)}
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            다음 날
          </button>
        </div>
      }
    >
      {!hydrated ? (
        <p className="text-sm leading-7 text-slate-500">
          브라우저 상태와 날짜를 동기화하는 중...
        </p>
      ) : !studentInfo ? (
        <div className="rounded-3xl border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-900">
          <p className="font-semibold">시간표를 보려면 학교 이름과 학년/반 설정이 필요해요.</p>
          <Link
            href="/setup"
            className="mt-3 inline-flex rounded-full bg-amber-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-amber-800"
          >
            초기 설정 하러 가기
          </Link>
        </div>
      ) : isLoading ? (
        <p className="text-sm leading-7 text-slate-500">시간표를 불러오는 중...</p>
      ) : state.error ? (
        <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {state.error}
        </p>
      ) : state.items.length === 0 ? (
        <p className="text-sm leading-7 text-slate-500">이 날짜에는 수업 정보가 없어요.</p>
      ) : (
        <ul className="space-y-3">
          {state.items.map((item) => (
            <li
              key={`${item.date}-${item.period}-${item.subject}`}
              className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3"
            >
              <p className="text-sm font-semibold text-slate-900">
                {item.period || "?"}교시 · {item.subject || "과목 정보 없음"}
              </p>
              <p className="mt-1 text-sm text-slate-500">
                {item.grade}학년 {item.classroom}반
              </p>
            </li>
          ))}
        </ul>
      )}
    </DashboardCard>
  );
}
