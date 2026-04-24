"use client";

import { useEffect, useMemo, useState } from "react";

import { DashboardCard } from "@/components/dashboard-card";
import { timerPresets } from "@/lib/site-data";
import {
  clearTimerSnapshot,
  readTimerSnapshot,
  saveTimerSnapshot,
} from "@/lib/storage/timer";
import {
  createTimerSnapshot,
  formatTimerClock,
  getTimerView,
  pauseTimer,
  resetTimer,
  resumeTimer,
  selectTimerPreset,
  startTimer,
  syncTimerSnapshot,
  TIMER_TICK_MS,
  type TimerSnapshot,
} from "@/lib/timer";

const defaultPreset = timerPresets[0];
const defaultTimerSnapshot = createTimerSnapshot(
  defaultPreset.label,
  defaultPreset.minutes * 60 * 1000,
  Date.now(),
);

const statusLabelMap = {
  idle: "준비됨",
  running: "집중 중",
  paused: "잠시 멈춤",
  completed: "완료",
} as const;

export function TimerPanel() {
  const [snapshot, setSnapshot] = useState<TimerSnapshot>(
    () => readTimerSnapshot() ?? defaultTimerSnapshot,
  );

  useEffect(() => {
    saveTimerSnapshot(syncTimerSnapshot(snapshot));
  }, [snapshot]);

  useEffect(() => {
    if (snapshot.status !== "running") {
      return;
    }

    const timerId = window.setInterval(() => {
      setSnapshot((current) => syncTimerSnapshot(current));
    }, TIMER_TICK_MS);

    return () => {
      window.clearInterval(timerId);
    };
  }, [snapshot.status]);

  const timerView = useMemo(() => getTimerView(snapshot), [snapshot]);

  const activePreset =
    timerPresets.find((preset) => preset.label === snapshot.label) ?? defaultPreset;

  const selectPreset = (label: string, minutes: number) => {
    setSnapshot(selectTimerPreset(label, minutes));
  };

  const handleStart = () => {
    setSnapshot((current) => startTimer(current));
  };

  const handlePause = () => {
    setSnapshot((current) => pauseTimer(current));
  };

  const handleResume = () => {
    setSnapshot((current) => resumeTimer(current));
  };

  const handleReset = () => {
    setSnapshot((current) => resetTimer(current));
  };

  const handleClear = () => {
    clearTimerSnapshot();
    setSnapshot(
      createTimerSnapshot(
        defaultPreset.label,
        defaultPreset.minutes * 60 * 1000,
        Date.now(),
      ),
    );
  };

  return (
    <div className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
      <DashboardCard
        title="집중 타이머"
        subtitle="프리셋을 고르고 시작하면 새로고침 후에도 남은 시간이 이어집니다."
      >
        <div className="grid gap-5">
          <div className="rounded-[2rem] bg-slate-950 px-6 py-7 text-white">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.28em] text-sky-200">
                  현재 프리셋
                </p>
                <p className="mt-2 text-xl font-semibold">{timerView.label}</p>
              </div>
              <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-semibold text-slate-100">
                {statusLabelMap[timerView.status]}
              </span>
            </div>

            <div className="mt-6">
              <p
                className="text-5xl font-semibold tabular-nums sm:text-6xl"
                aria-live="polite"
              >
                {formatTimerClock(timerView.remainingMs)}
              </p>
              <p className="mt-2 text-sm text-slate-300">
                {timerView.isCompleted
                  ? "집중 세션이 끝났어요. 바로 다시 시작하거나 프리셋을 바꿔 보세요."
                  : timerView.isActive
                    ? "백그라운드에 있어도 남은 시간을 계산해서 다시 복원합니다."
                    : "시작 전에는 프리셋을 바꾸고, 멈춘 뒤에는 이어서 다시 시작할 수 있어요."}
              </p>
            </div>

            <div className="mt-5 h-3 overflow-hidden rounded-full bg-white/10">
              <div
                className="h-full rounded-full bg-sky-400 transition-[width] duration-700"
                style={{ width: `${Math.max(timerView.progress * 100, 4)}%` }}
              />
            </div>

            <div className="mt-6 flex flex-wrap gap-3">
              {timerView.status === "idle" ? (
                <button
                  type="button"
                  onClick={handleStart}
                  className="rounded-full bg-sky-400 px-5 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-sky-300"
                >
                  시작
                </button>
              ) : null}

              {timerView.status === "running" ? (
                <button
                  type="button"
                  onClick={handlePause}
                  className="rounded-full bg-amber-300 px-5 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-amber-200"
                >
                  일시정지
                </button>
              ) : null}

              {timerView.status === "paused" ? (
                <button
                  type="button"
                  onClick={handleResume}
                  className="rounded-full bg-emerald-300 px-5 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-emerald-200"
                >
                  재시작
                </button>
              ) : null}

              {timerView.status === "completed" ? (
                <button
                  type="button"
                  onClick={handleStart}
                  className="rounded-full bg-emerald-300 px-5 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-emerald-200"
                >
                  같은 프리셋으로 다시 시작
                </button>
              ) : null}

              <button
                type="button"
                onClick={handleReset}
                className="rounded-full bg-white/10 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/20"
              >
                종료 / 리셋
              </button>

              <button
                type="button"
                onClick={handleClear}
                className="rounded-full border border-white/20 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/10"
              >
                저장 상태 초기화
              </button>
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            {timerPresets.map((preset) => {
              const isActivePreset = preset.label === activePreset.label;

              return (
                <button
                  key={preset.label}
                  type="button"
                  onClick={() => selectPreset(preset.label, preset.minutes)}
                  className={`rounded-3xl border px-4 py-4 text-left transition ${
                    isActivePreset
                      ? "border-slate-950 bg-slate-950 text-white"
                      : "border-slate-200 bg-slate-50 text-slate-900 hover:border-slate-300 hover:bg-white"
                  }`}
                  aria-pressed={isActivePreset}
                >
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-sm font-semibold">{preset.label}</span>
                    <span
                      className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                        isActivePreset ? "bg-white/10 text-white" : preset.tone
                      }`}
                    >
                      {preset.minutes}분
                    </span>
                  </div>
                  <p
                    className={`mt-3 text-sm leading-6 ${
                      isActivePreset ? "text-slate-200" : "text-slate-500"
                    }`}
                  >
                    {preset.label === "집중"
                      ? "짧고 안정적인 기본 집중 세션"
                      : preset.label === "휴식"
                        ? "집중 세션 뒤 리듬을 회복하는 짧은 휴식"
                        : "긴 과제나 시험 준비를 위한 확장 집중 모드"}
                  </p>
                </button>
              );
            })}
          </div>
        </div>
      </DashboardCard>

      <DashboardCard title="상태 안내" subtitle="이번 1차 구현에서 포함한 기능입니다.">
        <div className="space-y-4 text-sm leading-7 text-slate-600">
          <div className="rounded-2xl bg-slate-50 px-4 py-4">
            <p className="font-semibold text-slate-900">현재 상태 복원</p>
            <p className="mt-1">
              실행 중이던 세션은 목표 종료 시각을 저장해서 새로고침 후에도 이어집니다.
            </p>
          </div>

          <div className="rounded-2xl bg-slate-50 px-4 py-4">
            <p className="font-semibold text-slate-900">완료 UX</p>
            <p className="mt-1">
              완료되면 상태가 즉시 완료로 전환되고 다시 시작 버튼이 표시됩니다.
            </p>
          </div>

          <div className="rounded-2xl bg-slate-50 px-4 py-4">
            <p className="font-semibold text-slate-900">남은 시간</p>
            <p className="mt-1">
              1초 단위 카운트다운과 진행률 바로 현재 집중 흐름을 바로 확인할 수 있습니다.
            </p>
          </div>

          <div className="rounded-2xl border border-dashed border-slate-200 px-4 py-4">
            <p className="font-semibold text-slate-900">다음 단계 후보</p>
            <ul className="mt-2 list-disc space-y-1 pl-5 text-slate-600">
              <li>브라우저 Notification API 연동</li>
              <li>홈 대시보드 요약 카드 연동</li>
              <li>PWA 설치 후 빠른 재진입 흐름 보강</li>
            </ul>
          </div>
        </div>
      </DashboardCard>
    </div>
  );
}
