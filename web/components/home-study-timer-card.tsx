"use client";

/** 홈 대시보드에서 타이머 요약 카드와 즉시 제어 버튼을 담당합니다. */

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { DashboardCard } from "@/components/dashboard-card";
import { useStudyTimer } from "@/hooks/use-study-timer";
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
  TIMER_TICK_MS,
  type TimerView,
} from "@/lib/timer";

const timerStatusLabelMap = {
  idle: "준비됨",
  running: "집중 중",
  paused: "잠시 멈춤",
  completed: "완료",
} as const;

const defaultTimerPreset = timerPresets[0];
const defaultHomeTimerSnapshot = createTimerSnapshot(
  defaultTimerPreset.label,
  defaultTimerPreset.minutes * 60 * 1000,
  Date.now(),
);

export function HomeStudyTimerCard() {
  const studyTimer = useStudyTimer();
  const [timerNow, setTimerNow] = useState(() => Date.now());

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
    }, TIMER_TICK_MS);

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
                ? `${timerView.label} · ${timerStatusLabelMap[timerView.status]}`
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
  );
}
