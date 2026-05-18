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
import { useHydrated } from "@/hooks/use-hydrated";
import { useStudyTimer } from "@/hooks/use-study-timer";
import { useStudentPreferences } from "@/hooks/use-student-preferences";
import { formatDateKey, formatKoreanDateLabel, formatMonthKey } from "@/lib/date";
import type { NoticeSummary } from "@/lib/notices/types";
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
import { saveTimerSnapshot } from "@/lib/storage/timer";
import { timerPresets } from "@/lib/site-data";
import {
  createTimerSnapshot,
  formatTimerClock,
  getTimerView,
  pauseTimer,
  resetTimer,
  resumeTimer,
  startTimer,
  type TimerView,
} from "@/lib/timer";

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

type NoticeState = {
  requestToken: string;
  items: NoticeSummary[];
  error: string | null;
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

const initialNoticeState: NoticeState = {
  requestToken: "",
  items: [],
  error: null,
};

const DGE_NOTICE_UNSUPPORTED_MESSAGE =
  "대구교육청 학교 홈페이지는 가정통신문 조회를 아직 지원하지 않습니다.";

function getNextMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 1);
}

const defaultTimerPreset = timerPresets[0];
const defaultHomeTimerSnapshot = createTimerSnapshot(
  defaultTimerPreset.label,
  defaultTimerPreset.minutes * 60 * 1000,
  Date.now(),
);

export function HomeDashboard() {
  const hydrated = useHydrated();
  const studentInfo = useStudentPreferences();
  const studyTimer = useStudyTimer();
  const [state, setState] = useState<DashboardState>(initialState);
  const [noticeState, setNoticeState] = useState<NoticeState>(initialNoticeState);
  const [reloadCount, setReloadCount] = useState(0);
  const [noticeReloadCount, setNoticeReloadCount] = useState(0);
  const [timerNow, setTimerNow] = useState(() => Date.now());

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
    ? `${studentInfo.officeCode}-${studentInfo.schoolCode}-${studentInfo.homepage ?? ""}`
    : "";
  const noticeRequestToken = `${noticeRequestKey}:${noticeReloadCount}`;

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
          requestToken: noticeRequestToken,
          items: result.items,
          error: null,
        });
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return;
        }

        setNoticeState({
          requestToken: noticeRequestToken,
          items: [],
          error:
            error instanceof Error
              ? error.message
              : "가정통신문 목록을 불러오지 못했어요.",
        });
      });

    return () => {
      isCancelled = true;
    };
  }, [hydrated, noticeReloadCount, noticeRequestToken, studentInfo]);

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
  const isNoticeLoading =
    hydrated && Boolean(studentInfo) && noticeState.requestToken !== noticeRequestToken;
  const canRetryNoticeFetch =
    noticeState.error !== DGE_NOTICE_UNSUPPORTED_MESSAGE;
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
  const timerView: TimerView | null = useMemo(
    () =>
      studyTimer.snapshot ? getTimerView(studyTimer.snapshot, timerNow) : null,
    [studyTimer.snapshot, timerNow],
  );
  const currentTimerSnapshot = studyTimer.snapshot ?? defaultHomeTimerSnapshot;

  useEffect(() => {
    if (timerView?.status !== "running") {
      return;
    }

    const timerId = window.setInterval(() => {
      setTimerNow(Date.now());
    }, 1000);

    return () => {
      window.clearInterval(timerId);
    };
  }, [timerView?.status]);

  const handleTimerStart = () => {
    saveTimerSnapshot(startTimer(currentTimerSnapshot));
  };

  const handleTimerPause = () => {
    saveTimerSnapshot(pauseTimer(currentTimerSnapshot));
  };

  const handleTimerResume = () => {
    saveTimerSnapshot(resumeTimer(currentTimerSnapshot));
  };

  const handleTimerReset = () => {
    saveTimerSnapshot(resetTimer(currentTimerSnapshot));
  };

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
              날짜별 보기
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
          {!hydrated ? (
            <LoadingState message="가정통신문 기준을 준비 중..." />
          ) : !studentInfo ? (
            <SetupRequiredState
              title="초기 설정이 먼저 필요해요."
              message="학교를 저장하면 가정통신문도 같은 기준으로 가져올 수 있어요."
            />
          ) : isNoticeLoading ? (
            <LoadingState message="가정통신문 목록을 불러오는 중..." />
          ) : noticeState.error ? (
            <ErrorState
              message={noticeState.error}
              onRetry={canRetryNoticeFetch ? retryNoticeFetch : undefined}
            />
          ) : noticeState.items.length === 0 ? (
            <EmptyState
              title="가정통신문이 없어요."
              message="현재 학교 홈페이지에서 확인된 최근 가정통신문이 없어요."
            />
          ) : (
            <ul className="space-y-3">
              {noticeState.items.slice(0, 4).map((notice) => (
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

        <DashboardCard
          title="집중 타이머"
          subtitle="타이머 탭의 현재 상태와 오늘 기록을 홈에서도 바로 이어 봅니다."
        >
          <div className="rounded-[2rem] bg-slate-950 p-5 text-white">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-sm text-slate-300">현재 상태</p>
                <p className="mt-2 text-4xl font-semibold tracking-tight">
                  {timerView ? formatTimerClock(timerView.remainingMs) : "25:00"}
                </p>
                <p className="mt-2 text-sm text-slate-300">
                  {timerView
                    ? `${timerView.label} · ${
                        timerView.status === "running"
                          ? "집중 중"
                          : timerView.status === "paused"
                            ? "잠시 멈춤"
                            : timerView.status === "completed"
                              ? "완료"
                              : "준비됨"
                      }`
                    : "아직 시작하지 않은 기본 집중 세션"}
                </p>
              </div>
              <Link
                href="/timer"
                className="rounded-full bg-white px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-slate-100"
              >
                타이머 열기
              </Link>
            </div>

            <div className="mt-5 h-3 overflow-hidden rounded-full bg-white/10">
              <div
                className="h-full rounded-full bg-sky-400 transition-[width] duration-700"
                style={{ width: `${Math.max((timerView?.progress ?? 0) * 100, 4)}%` }}
                role="progressbar"
                aria-label={`${timerView?.label ?? "기본 집중"} 타이머 진행률`}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={Math.round((timerView?.progress ?? 0) * 100)}
                aria-valuetext={
                  timerView
                    ? `${formatTimerClock(timerView.remainingMs)} 남음`
                    : "아직 시작하지 않음"
                }
              />
            </div>

            <div className="mt-5 flex flex-wrap gap-3">
              {!timerView || timerView.status === "idle" ? (
                <button
                  type="button"
                  onClick={handleTimerStart}
                  aria-label={`${currentTimerSnapshot.label} 타이머 시작`}
                  className="rounded-full bg-sky-400 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-sky-300"
                >
                  시작
                </button>
              ) : null}

              {timerView?.status === "running" ? (
                <button
                  type="button"
                  onClick={handleTimerPause}
                  aria-label={`${currentTimerSnapshot.label} 타이머 일시정지`}
                  className="rounded-full bg-amber-300 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-amber-200"
                >
                  일시정지
                </button>
              ) : null}

              {timerView?.status === "paused" ? (
                <button
                  type="button"
                  onClick={handleTimerResume}
                  aria-label={`${currentTimerSnapshot.label} 타이머 재시작`}
                  className="rounded-full bg-emerald-300 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-emerald-200"
                >
                  재시작
                </button>
              ) : null}

              {timerView?.status === "completed" ? (
                <button
                  type="button"
                  onClick={handleTimerStart}
                  aria-label={`${currentTimerSnapshot.label} 타이머 다시 시작`}
                  className="rounded-full bg-emerald-300 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-emerald-200"
                >
                  다시 시작
                </button>
              ) : null}

              <button
                type="button"
                onClick={handleTimerReset}
                aria-label={`${currentTimerSnapshot.label} 타이머 종료 후 리셋`}
                className="rounded-full bg-white/10 px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/20"
              >
                종료 / 리셋
              </button>
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <div className="rounded-2xl bg-white/10 px-4 py-3">
                <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-300">
                  오늘 완료 세션
                </p>
                <p className="mt-2 text-lg font-semibold text-white">
                  {studyTimer.todayStats.completedSessions}회
                </p>
              </div>

              <div className="rounded-2xl bg-white/10 px-4 py-3">
                <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-300">
                  오늘 집중 시간
                </p>
                <p className="mt-2 text-lg font-semibold text-white">
                  {studyTimer.todayStats.focusMinutes}분
                </p>
              </div>
            </div>

            <p className="mt-5 text-sm text-slate-400">
              {studyTimer.settings.notificationsEnabled
                ? "브라우저 알림이 켜져 있어 완료 시 다른 탭에서도 확인할 수 있어요."
                : "타이머 탭에서 브라우저 알림을 켜면 완료 시 더 쉽게 알아챌 수 있어요."}
            </p>

            {!timerView ? (
              <div className="mt-4 flex flex-wrap gap-2">
                {timerPresets.map((preset) => (
                  <span
                    key={preset.label}
                    className={`rounded-full px-3 py-1.5 text-sm font-semibold ${preset.tone}`}
                  >
                    {preset.label} {preset.minutes}분
                  </span>
                ))}
              </div>
            ) : null}
          </div>
        </DashboardCard>
      </div>
    </div>
  );
}
