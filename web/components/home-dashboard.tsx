"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { DashboardCard } from "@/components/dashboard-card";
import { useHydrated } from "@/hooks/use-hydrated";
import { useStudentPreferences } from "@/hooks/use-student-preferences";
import { formatDateKey, formatKoreanDateLabel, formatMonthKey } from "@/lib/date";
import type { MealInfo, SchoolEvent, TimetableItem } from "@/lib/neis/types";
import { isVisibleSchedule } from "@/lib/schedule";
import { fetchMeals, fetchSchedules, fetchTimetable } from "@/lib/school-api";
import { timerPresets } from "@/lib/site-data";

type DashboardState = {
  requestKey: string;
  timetable: TimetableItem[];
  meals: MealInfo[];
  schedules: SchoolEvent[];
  error: string | null;
};

const initialState: DashboardState = {
  requestKey: "",
  timetable: [],
  meals: [],
  schedules: [],
  error: null,
};

export function HomeDashboard() {
  const hydrated = useHydrated();
  const studentInfo = useStudentPreferences();
  const [state, setState] = useState<DashboardState>(initialState);

  const today = useMemo(() => new Date(), []);
  const todayKey = useMemo(() => formatDateKey(today), [today]);
  const monthKey = useMemo(() => formatMonthKey(today), [today]);
  const todayLabel = useMemo(() => formatKoreanDateLabel(today), [today]);
  const requestKey = studentInfo
    ? `${studentInfo.grade}-${studentInfo.classroom}-${todayKey}-${monthKey}`
    : "";

  useEffect(() => {
    let isCancelled = false;

    if (!hydrated || !studentInfo) {
      return;
    }

    Promise.all([
      fetchTimetable({
        officeCode: studentInfo.officeCode,
        schoolCode: studentInfo.schoolCode,
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
    ])
      .then(([timetable, meals, schedules]) => {
        if (isCancelled) {
          return;
        }

        setState({
          requestKey,
          timetable,
          meals,
          schedules,
          error: null,
        });
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return;
        }

        setState({
          requestKey,
          timetable: [],
          meals: [],
          schedules: [],
          error:
            error instanceof Error
              ? error.message
              : "대시보드 데이터를 불러오지 못했어요.",
        });
      });

    return () => {
      isCancelled = true;
    };
  }, [hydrated, monthKey, requestKey, studentInfo, todayKey]);

  const isLoading = hydrated && Boolean(studentInfo) && state.requestKey !== requestKey;

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
            <p className="text-sm leading-7 text-slate-500">
              브라우저 설정과 오늘 날짜를 맞추는 중...
            </p>
          ) : !studentInfo ? (
            <p className="text-sm leading-7 text-slate-500">
              먼저 초기 설정에서 학교 이름과 학년/반을 저장해 주세요.
            </p>
          ) : isLoading ? (
            <p className="text-sm leading-7 text-slate-500">시간표를 불러오는 중...</p>
          ) : state.error ? (
            <p className="text-sm leading-7 text-rose-600">{state.error}</p>
          ) : state.timetable.length === 0 ? (
            <p className="text-sm leading-7 text-slate-500">오늘 등록된 수업이 없어요.</p>
          ) : (
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
          )}
        </DashboardCard>

        <DashboardCard title="오늘 급식" subtitle={`${todayLabel} 점심 메뉴 요약`}>
          {!hydrated ? (
            <p className="text-sm leading-7 text-slate-500">급식을 불러올 준비 중...</p>
          ) : !studentInfo ? (
            <p className="text-sm leading-7 text-slate-500">
              학년/반 설정과 무관하게 급식은 확인할 수 있지만, 우선 공통 데이터 연결 흐름에
              맞춰 함께 준비 중입니다.
            </p>
          ) : isLoading ? (
            <p className="text-sm leading-7 text-slate-500">급식을 불러오는 중...</p>
          ) : state.error ? (
            <p className="text-sm leading-7 text-rose-600">{state.error}</p>
          ) : !todayMeal ? (
            <p className="text-sm leading-7 text-slate-500">오늘 급식 정보가 없어요.</p>
          ) : (
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
          )}
        </DashboardCard>
      </div>

      <div className="grid gap-4">
        <DashboardCard title="다가오는 일정" subtitle="가까운 일정만 먼저 요약">
          {!hydrated ? (
            <p className="text-sm leading-7 text-slate-500">일정을 불러올 준비 중...</p>
          ) : !studentInfo ? (
            <p className="text-sm leading-7 text-slate-500">
              초기 설정을 저장하면 일정/시간표와 함께 학교 생활 정보를 계속 확인할 수 있어요.
            </p>
          ) : isLoading ? (
            <p className="text-sm leading-7 text-slate-500">일정을 불러오는 중...</p>
          ) : state.error ? (
            <p className="text-sm leading-7 text-rose-600">{state.error}</p>
          ) : upcomingSchedules.length === 0 ? (
            <p className="text-sm leading-7 text-slate-500">
              이번 기간에 보여줄 일정이 없어요.
            </p>
          ) : (
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
          )}
        </DashboardCard>

        <DashboardCard
          title="집중 타이머"
          subtitle="웹에서는 설치 없이 바로 사용하는 흐름을 우선합니다."
        >
          <div className="rounded-[2rem] bg-slate-950 p-5 text-white">
            <p className="text-sm text-slate-300">추천 프리셋</p>
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
            <div className="mt-6 flex items-end justify-between gap-4">
              <div>
                <p className="text-4xl font-semibold tracking-tight">25:00</p>
                <p className="mt-2 text-sm text-slate-400">
                  추후 localStorage 기반 복원과 알림을 연결합니다.
                </p>
              </div>
              <Link
                href="/timer"
                className="rounded-full bg-white px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-slate-100"
              >
                타이머 열기
              </Link>
            </div>
          </div>
        </DashboardCard>
      </div>
    </div>
  );
}
