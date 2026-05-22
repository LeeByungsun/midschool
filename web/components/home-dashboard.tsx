"use client";

/** 홈 화면에서 시간표, 급식, 일정 요약 카드를 조합해 보여줍니다. */

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import {
  EmptyState,
  ErrorState,
  InfoState,
  LoadingState,
  SetupRequiredState,
} from "@/components/data-state";
import { DashboardCard } from "@/components/dashboard-card";
import { HomeTimerCard } from "@/components/home-timer-card";
import { useHydrated } from "@/hooks/use-hydrated";
import { useStudentPreferences } from "@/hooks/use-student-preferences";
import { formatDateKey, formatKoreanDateLabel, formatMonthKey } from "@/lib/date";
import { resolveNoticeCardState, type NoticeLoadState } from "@/lib/notices/view-state";
import type { MealInfo, SchoolEvent, TimetableItem } from "@/lib/neis/types";
import { isVisibleSchedule } from "@/lib/schedule";
import {
  type CacheStatus,
  fetchMeals,
  fetchNotices,
  fetchSchedules,
  fetchTimetable,
  formatCacheStatusMessage,
} from "@/lib/school-api";
import { resolveDashboardData } from "@/lib/dashboard-load";

type DashboardState = {
  requestToken: string;
  timetable: TimetableItem[];
  meals: MealInfo[];
  schedules: SchoolEvent[];
  timetableError: string | null;
  mealError: string | null;
  scheduleError: string | null;
  timetableCacheStatus: CacheStatus;
  timetableCachedAt: number | null;
  mealCacheStatus: CacheStatus;
  mealCachedAt: number | null;
  scheduleCacheStatus: CacheStatus;
  scheduleCachedAt: number | null;
};

type NoticeRequestState = {
  requestToken: string;
  loadState: NoticeLoadState;
};

const initialState: DashboardState = {
  requestToken: "",
  timetable: [],
  meals: [],
  schedules: [],
  timetableError: null,
  mealError: null,
  scheduleError: null,
  timetableCacheStatus: "network",
  timetableCachedAt: null,
  mealCacheStatus: "network",
  mealCachedAt: null,
  scheduleCacheStatus: "network",
  scheduleCachedAt: null,
};

const initialNoticeLoadState: NoticeLoadState = {
  status: "idle",
};

const loadingNoticeState: NoticeLoadState = {
  status: "loading",
};

const initialNoticeState: NoticeRequestState = {
  requestToken: "",
  loadState: initialNoticeLoadState,
};

const DGE_NOTICE_UNSUPPORTED_MESSAGE =
  "대구교육청 학교 홈페이지는 가정통신문 조회를 아직 지원하지 않습니다.";

function getNextMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 1);
}

export function HomeDashboard() {
  const hydrated = useHydrated();
  const studentInfo = useStudentPreferences();
  const [state, setState] = useState<DashboardState>(initialState);
  const [noticeState, setNoticeState] = useState<NoticeRequestState>(initialNoticeState);
  const [reloadCount, setReloadCount] = useState(0);
  const [noticeReloadCount, setNoticeReloadCount] = useState(0);

  const today = useMemo(() => new Date(), []);
  const todayKey = useMemo(() => formatDateKey(today), [today]);
  const monthKey = useMemo(() => formatMonthKey(today), [today]);
  const nextMonthKey = useMemo(
    () => formatMonthKey(getNextMonth(today)),
    [today],
  );
  const todayLabel = useMemo(() => formatKoreanDateLabel(today), [today]);
  const requestKey = studentInfo
    ? `${studentInfo.schoolKind ?? "중학교"}-${studentInfo.grade}-${studentInfo.classroom}-${todayKey}-${monthKey}-${nextMonthKey}`
    : "";
  const requestToken = `${requestKey}:${reloadCount}`;
  const noticeRequestKey = studentInfo
    ? `${studentInfo.officeCode}-${studentInfo.schoolCode}-${studentInfo.homepage ?? ""}:${noticeReloadCount}`
    : "";

  useEffect(() => {
    let isCancelled = false;

    if (!hydrated || !studentInfo) {
      return;
    }

    Promise.allSettled([
      fetchTimetable({
        officeCode: studentInfo.officeCode,
        schoolCode: studentInfo.schoolCode,
        schoolKind: studentInfo.schoolKind,
        grade: studentInfo.grade,
        classroom: studentInfo.classroom,
        date: todayKey,
      }),
      fetchMeals({
        officeCode: studentInfo.officeCode,
        schoolCode: studentInfo.schoolCode,
        date: todayKey,
      }),
      fetchSchedules({
        officeCode: studentInfo.officeCode,
        schoolCode: studentInfo.schoolCode,
        date: monthKey,
      }),
      fetchSchedules({
        officeCode: studentInfo.officeCode,
        schoolCode: studentInfo.schoolCode,
        date: nextMonthKey,
      }),
    ])
      .then(([timetableResult, mealResult, currentMonthScheduleResult, nextMonthScheduleResult]) => {
        if (isCancelled) {
          return;
        }

        const resolvedData = resolveDashboardData({
          timetableResult,
          mealResult,
          currentMonthScheduleResult,
          nextMonthScheduleResult,
        });

        setState({
          requestToken,
          timetable: resolvedData.timetable.items,
          meals: resolvedData.meals.items,
          schedules: resolvedData.schedules.items,
          timetableError: resolvedData.timetable.error,
          mealError: resolvedData.meals.error,
          scheduleError: resolvedData.schedules.error,
          timetableCacheStatus: resolvedData.timetable.cacheStatus,
          timetableCachedAt: resolvedData.timetable.cachedAt,
          mealCacheStatus: resolvedData.meals.cacheStatus,
          mealCachedAt: resolvedData.meals.cachedAt,
          scheduleCacheStatus: resolvedData.schedules.cacheStatus,
          scheduleCachedAt: resolvedData.schedules.cachedAt,
        });
      });

    return () => {
      isCancelled = true;
    };
  }, [hydrated, monthKey, nextMonthKey, reloadCount, requestToken, studentInfo, todayKey]);

  useEffect(() => {
    let isCancelled = false;

    if (!hydrated || !studentInfo) {
      return;
    }
    const activeRequestToken = noticeRequestKey;

    fetchNotices({
      officeCode: studentInfo.officeCode,
      schoolCode: studentInfo.schoolCode,
      homepage: studentInfo.homepage,
      limit: 5,
    })
      .then((result) => {
        if (isCancelled) {
          return;
        }

        setNoticeState({
          requestToken: activeRequestToken,
          loadState: {
            status: "success",
            items: result.items,
          },
        });
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return;
        }

        const message =
          error instanceof Error
            ? error.message
            : "가정통신문 목록을 불러오지 못했어요.";

        setNoticeState({
          requestToken: activeRequestToken,
          loadState: {
            status: "error",
            message,
            canRetry: message !== DGE_NOTICE_UNSUPPORTED_MESSAGE,
          },
        });
      });

    return () => {
      isCancelled = true;
    };
  }, [hydrated, noticeReloadCount, noticeRequestKey, studentInfo]);

  const upcomingSchedules = useMemo(
    () =>
      state.schedules
        .filter(isVisibleSchedule)
        .filter((event) => event.date >= todayKey)
        .sort((a, b) => a.date.localeCompare(b.date))
        .slice(0, 3),
    [state.schedules, todayKey],
  );

  const todayMeal = state.meals[0];
  const retryFetch = () => {
    setReloadCount((prev) => prev + 1);
  };
  const retryNoticeFetch = () => {
    setNoticeReloadCount((prev) => prev + 1);
  };
  const isLoading = hydrated && Boolean(studentInfo) && state.requestToken !== requestToken;
  const noticeLoadState =
    !hydrated || !studentInfo
      ? initialNoticeLoadState
      : noticeState.requestToken === noticeRequestKey
        ? noticeState.loadState
        : loadingNoticeState;
  const noticeCardState = resolveNoticeCardState({
    hydrated,
    studentInfo,
    loadState: noticeLoadState,
  });
  const timetableCacheNotice = formatCacheStatusMessage(
    state.timetableCacheStatus,
    state.timetableCachedAt,
    "시간표",
  );
  const mealCacheNotice = formatCacheStatusMessage(
    state.mealCacheStatus,
    state.mealCachedAt,
    "급식",
  );

  return (
    <div className="grid gap-4 lg:grid-cols-[1.35fr_0.95fr]">
      <div className="grid gap-4">
        <DashboardCard
          title="오늘 시간표"
          subtitle={`${todayLabel} · 교시별 흐름 확인`}
          action={
            <Link
              href="/timetable"
              className="rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-200"
            >
              전체 보기
            </Link>
          }
        >
          {!hydrated ? (
            <LoadingState message="브라우저 설정과 오늘 날짜를 맞추는 중..." />
          ) : !studentInfo ? (
            <SetupRequiredState message="먼저 초기 설정에서 학교 이름과 학년/반을 저장해 주세요." />
          ) : isLoading ? (
            <LoadingState message="시간표를 불러오는 중..." />
          ) : state.timetableError ? (
            <ErrorState message={state.timetableError} onRetry={retryFetch} />
          ) : state.timetable.length === 0 ? (
            <EmptyState
              title="오늘 수업이 없어요."
              message="오늘 등록된 시간표가 아직 없어요."
            />
          ) : (
            <div className="grid gap-4">
              {timetableCacheNotice ? <InfoState message={timetableCacheNotice} /> : null}
              <ul className="space-y-3">
                {state.timetable.map((item) => (
                  <li
                    key={`${item.date}-${item.period}-${item.subject}`}
                    className="flex items-start justify-between gap-3 rounded-2xl bg-slate-50 px-4 py-3"
                  >
                    <div>
                      <p className="text-sm font-semibold text-slate-900">
                        {item.period || "?"}교시 · {item.subject || "과목 정보 없음"}
                      </p>
                      <p className="mt-1 text-sm text-slate-500">
                        {item.grade}학년 {item.classroom}반
                      </p>
                    </div>
                    <span className="rounded-full bg-white px-2.5 py-1 text-xs font-medium text-slate-500 shadow-sm">
                      수업
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </DashboardCard>

        <DashboardCard
          title="오늘 급식"
          subtitle={`${todayLabel} 점심 메뉴 요약`}
          action={
            <Link
              href="/meals"
              className="rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-200"
            >
              일주일 보기
            </Link>
          }
        >
          {!hydrated ? (
            <LoadingState message="급식을 불러올 준비 중..." />
          ) : !studentInfo ? (
            <SetupRequiredState
              title="초기 설정을 먼저 해 주세요."
              message="급식도 학교 기준으로 불러오므로, 학교 이름과 학년/반을 먼저 저장해 주세요."
            />
          ) : isLoading ? (
            <LoadingState message="급식을 불러오는 중..." />
          ) : state.mealError ? (
            <ErrorState message={state.mealError} onRetry={retryFetch} />
          ) : !todayMeal ? (
            <EmptyState
              title="오늘 급식 정보가 없어요."
              message="현재 날짜 기준으로 표시할 급식이 아직 없어요."
            />
          ) : (
            <div className="grid gap-4">
              {mealCacheNotice ? <InfoState message={mealCacheNotice} /> : null}
              <div className="rounded-3xl bg-gradient-to-br from-amber-50 via-white to-orange-50 p-4">
                <p className="text-sm font-semibold text-slate-900">
                  {todayMeal.mealType || "식사"}
                </p>
                <ul className="mt-3 grid gap-2 sm:grid-cols-2">
                  {todayMeal.menu
                    .split(/<br\s*\/?>/i)
                    .map((menu) => menu.replace(/\([^)]*\)/g, "").trim())
                    .filter(Boolean)
                    .map((menu) => (
                      <li
                        key={menu}
                        className="rounded-2xl border border-amber-100 bg-white px-3 py-2 text-sm text-slate-700"
                      >
                        {menu}
                      </li>
                    ))}
                </ul>
                <p className="mt-3 text-sm text-slate-500">
                  {todayMeal.calorieInfo || "칼로리 정보는 아직 제공되지 않았어요."}
                </p>
              </div>
            </div>
          )}
        </DashboardCard>
      </div>

      <div className="grid gap-4">
        <DashboardCard title="다가오는 일정" subtitle="가까운 일정만 먼저 요약">
          {!hydrated ? (
            <LoadingState message="일정을 불러올 준비 중..." />
          ) : !studentInfo ? (
            <SetupRequiredState
              title="초기 설정을 저장해 주세요."
              message="설정을 저장하면 일정과 시간표를 같은 기준으로 계속 확인할 수 있어요."
            />
          ) : isLoading ? (
            <LoadingState message="일정을 불러오는 중..." />
          ) : upcomingSchedules.length > 0 ? (
            <div className="grid gap-4">
              {state.scheduleError ? <InfoState message={state.scheduleError} /> : null}
              <ul className="space-y-3">
                {upcomingSchedules.map((event) => (
                  <li
                    key={`${event.date}-${event.title}`}
                    className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3"
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
          ) : state.scheduleError ? (
            <ErrorState message={state.scheduleError} onRetry={retryFetch} />
          ) : (
            <EmptyState
              title="다가오는 일정이 없어요."
              message="이번 기간에는 보여줄 학사 일정이 없어요."
            />
          )}
        </DashboardCard>

        <DashboardCard
          title="가정통신문"
          subtitle="선택한 학교 홈페이지에서 최근 목록만 먼저 가져옵니다."
        >
          {noticeCardState.status === "not-configured" ? (
            <SetupRequiredState
              title={noticeCardState.title}
              message={noticeCardState.message}
            />
          ) : noticeCardState.status === "loading" ? (
            <LoadingState message={noticeCardState.message} />
          ) : noticeCardState.status === "error" ? (
            <ErrorState
              message={noticeCardState.message}
              onRetry={noticeCardState.canRetry ? retryNoticeFetch : undefined}
            />
          ) : noticeCardState.status === "empty" ? (
            <EmptyState
              title={noticeCardState.title}
              message={noticeCardState.message}
            />
          ) : (
            <ul className="space-y-3">
              {noticeCardState.items.slice(0, 4).map((notice) => (
                <li
                  key={`${notice.id}-${notice.date}`}
                  className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3"
                >
                  <a
                    href={notice.url}
                    target="_blank"
                    rel="noreferrer"
                    className="block"
                  >
                    <p className="text-sm font-semibold text-slate-900">
                      {notice.title}
                    </p>
                    <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                      {notice.date ? <span>{notice.date}</span> : null}
                      {notice.author ? (
                        <>
                          <span aria-hidden="true">•</span>
                          <span>{notice.author}</span>
                        </>
                      ) : null}
                    </div>
                  </a>
                </li>
              ))}
            </ul>
          )}
        </DashboardCard>

        <HomeTimerCard />
      </div>
    </div>
  );
}
