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
import { formatDateKey, formatKoreanDateLabel } from "@/lib/date";
import type { TimetableItem } from "@/lib/neis/types";
import {
  type CacheStatus,
  fetchTimetable,
  formatCacheStatusMessage,
} from "@/lib/school-api";

type TimetableState = {
  requestToken: string;
  items: TimetableItem[];
  error: string | null;
  cacheStatus: CacheStatus;
  cachedAt: number | null;
};

const initialState: TimetableState = {
  requestToken: "",
  items: [],
  error: null,
  cacheStatus: "network",
  cachedAt: null,
};

export function TimetableBrowser() {
  const hydrated = useHydrated();
  const studentInfo = useStudentPreferences();
  const [selectedDate, setSelectedDate] = useState(() => new Date());
  const [state, setState] = useState<TimetableState>(initialState);
  const [reloadCount, setReloadCount] = useState(0);

  const dateKey = useMemo(() => formatDateKey(selectedDate), [selectedDate]);
  const dateLabel = useMemo(
    () => formatKoreanDateLabel(selectedDate),
    [selectedDate],
  );
  const requestKey = studentInfo
    ? `${studentInfo.schoolKind ?? "중학교"}-${studentInfo.grade}-${studentInfo.classroom}-${dateKey}`
    : "";
  const requestToken = `${requestKey}:${reloadCount}`;

  useEffect(() => {
    let isCancelled = false;

    if (!hydrated || !studentInfo) {
      return;
    }

    fetchTimetable({
      officeCode: studentInfo.officeCode,
      schoolCode: studentInfo.schoolCode,
      schoolKind: studentInfo.schoolKind,
      grade: studentInfo.grade,
      classroom: studentInfo.classroom,
      date: dateKey,
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
              : "시간표 정보를 불러오지 못했어요.",
          cacheStatus: "network",
          cachedAt: null,
        });
      });

    return () => {
      isCancelled = true;
    };
  }, [dateKey, hydrated, reloadCount, requestToken, studentInfo]);

  const moveDate = (offset: number) => {
    setSelectedDate((prev) => {
      const next = new Date(prev);
      next.setDate(prev.getDate() + offset);
      return next;
    });
  };

  const retryFetch = () => {
    setReloadCount((prev) => prev + 1);
  };
  const isLoading = hydrated && Boolean(studentInfo) && state.requestToken !== requestToken;
  const cacheNotice = formatCacheStatusMessage(
    state.cacheStatus,
    state.cachedAt,
    "시간표",
  );

  return (
    <DashboardCard
      title="일간 시간표"
      subtitle={`${dateLabel} 기준`}
      action={
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => moveDate(-1)}
            aria-label="시간표 날짜를 하루 이전으로 이동"
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            이전 날
          </button>
          <button
            type="button"
            onClick={() => moveDate(1)}
            aria-label="시간표 날짜를 하루 다음으로 이동"
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            다음 날
          </button>
        </div>
      }
    >
      {!hydrated ? (
        <LoadingState message="브라우저 상태와 날짜를 동기화하는 중..." />
      ) : !studentInfo ? (
        <SetupRequiredState message="시간표를 보려면 학교 이름과 학년/반 설정을 먼저 저장해 주세요." />
      ) : isLoading ? (
        <LoadingState message="시간표를 불러오는 중..." />
      ) : state.error ? (
        <ErrorState message={state.error} onRetry={retryFetch} />
      ) : state.items.length === 0 ? (
        <EmptyState
          title="수업 정보가 없어요."
          message="이 날짜에는 표시할 시간표가 없어요."
        />
      ) : (
        <div className="grid gap-4">
          {cacheNotice ? <InfoState message={cacheNotice} /> : null}
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
        </div>
      )}
    </DashboardCard>
  );
}
