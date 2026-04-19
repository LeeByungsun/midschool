"use client";

import { useEffect, useMemo, useState } from "react";

import {
  EmptyState,
  ErrorState,
  InfoState,
  LoadingState,
  SetupRequiredState,
} from "@/components/data-state";
import { DashboardCard } from "@/components/dashboard-card";
import { useHydrated } from "@/hooks/use-hydrated";
import { useStudentPreferences } from "@/hooks/use-student-preferences";
import { formatKoreanDateLabel, formatKoreanMonthLabel, formatMonthKey } from "@/lib/date";
import type { SchoolEvent } from "@/lib/neis/types";
import { isVisibleSchedule } from "@/lib/schedule";
import {
  type CacheStatus,
  fetchSchedules,
  formatCacheStatusMessage,
} from "@/lib/school-api";

type ScheduleState = {
  requestToken: string;
  items: SchoolEvent[];
  error: string | null;
  cacheStatus: CacheStatus;
  cachedAt: number | null;
};

const initialState: ScheduleState = {
  requestToken: "",
  items: [],
  error: null,
  cacheStatus: "network",
  cachedAt: null,
};

export function ScheduleBrowser() {
  const hydrated = useHydrated();
  const studentInfo = useStudentPreferences();
  const [selectedMonth, setSelectedMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [state, setState] = useState<ScheduleState>(initialState);
  const [reloadCount, setReloadCount] = useState(0);

  const monthKey = useMemo(() => formatMonthKey(selectedMonth), [selectedMonth]);
  const monthLabel = useMemo(
    () => formatKoreanMonthLabel(selectedMonth),
    [selectedMonth],
  );
  const requestToken = `${monthKey}:${reloadCount}`;

  useEffect(() => {
    let isCancelled = false;

    if (!hydrated || !studentInfo) {
      return () => {
        isCancelled = true;
      };
    }

    fetchSchedules({
      officeCode: studentInfo.officeCode,
      schoolCode: studentInfo.schoolCode,
      date: monthKey,
    })
      .then((result) => {
        if (isCancelled) {
          return;
        }

        setState({
          requestToken,
          items: result.items,
          error: null,
          cacheStatus: result.cacheStatus,
          cachedAt: result.cachedAt,
        });
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return;
        }

        setState({
          requestToken,
          items: [],
          error:
            error instanceof Error
              ? error.message
              : "학사 일정을 불러오지 못했어요.",
          cacheStatus: "network",
          cachedAt: null,
        });
      });

    return () => {
      isCancelled = true;
    };
  }, [hydrated, monthKey, reloadCount, requestToken, studentInfo]);

  const visibleItems = useMemo(
    () =>
      state.items
        .filter(isVisibleSchedule)
        .sort((a, b) => a.date.localeCompare(b.date)),
    [state.items],
  );

  const moveMonth = (offset: number) => {
    setSelectedMonth((prev) => new Date(prev.getFullYear(), prev.getMonth() + offset, 1));
  };

  const retryFetch = () => {
    setReloadCount((prev) => prev + 1);
  };
  const isLoading =
    hydrated && Boolean(studentInfo) && state.requestToken !== requestToken;
  const cacheNotice = formatCacheStatusMessage(
    state.cacheStatus,
    state.cachedAt,
    "학사 일정",
  );

  return (
    <DashboardCard
      title="월간 학사 일정"
      subtitle={`${monthLabel} 기준`}
      action={
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => moveMonth(-1)}
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            이전 달
          </button>
          <button
            type="button"
            onClick={() => moveMonth(1)}
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            다음 달
          </button>
        </div>
      }
    >
      {!hydrated ? (
        <LoadingState message="월 정보를 맞추는 중..." />
      ) : !studentInfo ? (
        <SetupRequiredState message="학사 일정을 보려면 학교 이름과 학년/반을 먼저 저장해 주세요." />
      ) : isLoading ? (
        <LoadingState message="학사 일정을 불러오는 중..." />
      ) : state.error ? (
        <ErrorState message={state.error} onRetry={retryFetch} />
      ) : visibleItems.length === 0 ? (
        <EmptyState
          title="보여줄 일정이 없어요."
          message="이번 달에는 표시할 학사 일정이 없어요."
        />
      ) : (
        <div className="grid gap-4">
          {cacheNotice ? <InfoState message={cacheNotice} /> : null}
          <ul className="space-y-3">
            {visibleItems.map((event) => (
              <li
                key={`${event.date}-${event.title}`}
                className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3"
              >
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-700">
                  {formatKoreanDateLabel(event.date)}
                </p>
                <p className="mt-2 text-sm font-semibold text-slate-900">
                  {event.title || "행사명 없음"}
                </p>
                {event.description ? (
                  <p className="mt-1 text-sm text-slate-500">{event.description}</p>
                ) : null}
              </li>
            ))}
          </ul>
        </div>
      )}
    </DashboardCard>
  );
}
