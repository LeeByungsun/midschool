"use client";

/** 공부 타이머 진행 상태와 제어 버튼을 담당하는 클라이언트 컴포넌트입니다. */

import { useEffect, useMemo, useRef, useState } from "react";

import { DashboardCard } from "@/components/dashboard-card";
import { timerPresets } from "@/lib/site-data";
import {
  clearTimerSnapshot,
  readTimerSettings,
  readTimerSnapshot,
  readTodayTimerStats,
  recordTimerCompletion,
  saveTimerSettings,
  saveTimerSnapshot,
  type TimerDailyStats,
  type TimerSettings,
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

const timeLabelFormatter = new Intl.DateTimeFormat("ko-KR", {
  hour: "numeric",
  minute: "2-digit",
});

const statusLabelMap = {
  idle: "준비됨",
  running: "집중 중",
  paused: "잠시 멈춤",
  completed: "완료",
} as const;

function formatTimerMinutes(durationMs: number) {
  return `${Math.round(durationMs / (60 * 1000))}분`;
}

function formatClockTime(timestamp: number | null) {
  if (!timestamp) {
    return "계산 전";
  }

  return timeLabelFormatter.format(new Date(timestamp));
}

export function TimerPanel() {
  const [snapshot, setSnapshot] = useState<TimerSnapshot>(
    () => readTimerSnapshot() ?? defaultTimerSnapshot,
  );
  const [settings, setSettings] = useState<TimerSettings>(() => readTimerSettings());
  const [todayStats, setTodayStats] = useState<TimerDailyStats>(() => readTodayTimerStats());
  const [notificationPermission, setNotificationPermission] = useState<
    NotificationPermission | "unsupported"
  >(() =>
    typeof window !== "undefined" && "Notification" in window
      ? Notification.permission
      : "unsupported",
  );
  const previousStatusRef = useRef(snapshot.status);

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

  useEffect(() => {
    const previousStatus = previousStatusRef.current;

    if (previousStatus !== "completed" && snapshot.status === "completed") {
      const nextStats = recordTimerCompletion(snapshot);
      setTodayStats(nextStats);

      if (
        settings.notificationsEnabled &&
        typeof window !== "undefined" &&
        "Notification" in window &&
        Notification.permission === "granted"
      ) {
        new Notification("학교도우미 타이머", {
          body: `${snapshot.label} 세션이 끝났어요. 바로 다음 흐름을 이어 가세요.`,
          tag: "study-timer-complete",
        });
      }
    }

    previousStatusRef.current = snapshot.status;
  }, [settings.notificationsEnabled, snapshot]);

  const timerView = useMemo(() => getTimerView(snapshot), [snapshot]);

  const activePreset =
    timerPresets.find((preset) => preset.label === snapshot.label) ?? defaultPreset;

  const expectedEndTime =
    snapshot.status === "running" && snapshot.targetTime ? snapshot.targetTime : null;

  const notificationSummary =
    notificationPermission === "unsupported"
      ? "이 브라우저는 알림 API를 지원하지 않아요."
      : notificationPermission === "granted" && settings.notificationsEnabled
        ? "완료되면 브라우저 알림을 보냅니다."
        : notificationPermission === "denied"
          ? "브라우저에서 알림 권한이 차단되어 있어요."
          : "현재는 화면 안의 완료 상태만 보여 주고 있어요.";

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

  const handleNotificationsToggle = async () => {
    if (typeof window === "undefined" || !("Notification" in window)) {
      setNotificationPermission("unsupported");
      return;
    }

    if (settings.notificationsEnabled) {
      const nextSettings = { notificationsEnabled: false };
      saveTimerSettings(nextSettings);
      setSettings(nextSettings);
      setNotificationPermission(Notification.permission);
      return;
    }

    const permission =
      Notification.permission === "default"
        ? await Notification.requestPermission()
        : Notification.permission;

    setNotificationPermission(permission);

    const nextSettings = {
      notificationsEnabled: permission === "granted",
    };

    saveTimerSettings(nextSettings);
    setSettings(nextSettings);
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
              <p className="mt-2 text-sm text-slate-300" role="status" aria-live="polite">
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
                role="progressbar"
                aria-label={`${timerView.label} 타이머 진행률`}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={Math.round(timerView.progress * 100)}
                aria-valuetext={`${formatTimerClock(timerView.remainingMs)} 남음`}
              />
            </div>

            <div className="mt-6 flex flex-wrap gap-3">
              {timerView.status === "idle" ? (
                <button
                  type="button"
                  onClick={handleStart}
                  aria-label={`${timerView.label} 타이머 시작`}
                  className="rounded-full bg-sky-400 px-5 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-sky-300"
                >
                  시작
                </button>
              ) : null}

              {timerView.status === "running" ? (
                <button
                  type="button"
                  onClick={handlePause}
                  aria-label={`${timerView.label} 타이머 일시정지`}
                  className="rounded-full bg-amber-300 px-5 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-amber-200"
                >
                  일시정지
                </button>
              ) : null}

              {timerView.status === "paused" ? (
                <button
                  type="button"
                  onClick={handleResume}
                  aria-label={`${timerView.label} 타이머 재시작`}
                  className="rounded-full bg-emerald-300 px-5 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-emerald-200"
                >
                  재시작
                </button>
              ) : null}

              {timerView.status === "completed" ? (
                <button
                  type="button"
                  onClick={handleStart}
                  aria-label={`${timerView.label} 타이머 다시 시작`}
                  className="rounded-full bg-emerald-300 px-5 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-emerald-200"
                >
                  같은 프리셋으로 다시 시작
                </button>
              ) : null}

              <button
                type="button"
                onClick={handleReset}
                aria-label={`${timerView.label} 타이머 종료 후 리셋`}
                className="rounded-full bg-white/10 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/20"
              >
                종료 / 리셋
              </button>

              <button
                type="button"
                onClick={handleClear}
                aria-label="저장된 타이머 상태 초기화"
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

      <div className="grid gap-4">
        <DashboardCard title="세션 요약" subtitle="현재 타이머 흐름과 종료 시각을 바로 확인합니다.">
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                총 세션 시간
              </p>
              <p className="mt-2 text-lg font-semibold text-slate-900">
                {formatTimerMinutes(timerView.durationMs)}
              </p>
            </div>

            <div className="rounded-2xl bg-slate-50 px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                현재 상태
              </p>
              <p className="mt-2 text-lg font-semibold text-slate-900">
                {statusLabelMap[timerView.status]}
              </p>
            </div>

            <div className="rounded-2xl bg-slate-50 px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                경과 시간
              </p>
              <p className="mt-2 text-lg font-semibold text-slate-900">
                {formatTimerMinutes(timerView.elapsedMs)}
              </p>
            </div>

            <div className="rounded-2xl bg-slate-50 px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                종료 예정 시각
              </p>
              <p className="mt-2 text-lg font-semibold text-slate-900">
                {formatClockTime(expectedEndTime)}
              </p>
            </div>
          </div>
        </DashboardCard>

        <DashboardCard title="완료 알림" subtitle="브라우저 알림을 켜 두면 다른 탭에 있어도 완료를 알려 줍니다.">
          <div className="space-y-4">
            <div className="rounded-2xl bg-slate-50 px-4 py-4">
              <p className="font-semibold text-slate-900">
                {settings.notificationsEnabled ? "알림 켜짐" : "알림 꺼짐"}
              </p>
              <p className="mt-1 text-sm leading-7 text-slate-600">
                {notificationSummary}
              </p>
            </div>

            <button
              type="button"
              onClick={() => {
                void handleNotificationsToggle();
              }}
              className="rounded-full bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-slate-800"
            >
              {settings.notificationsEnabled ? "브라우저 알림 끄기" : "브라우저 알림 켜기"}
            </button>
          </div>
        </DashboardCard>

        <DashboardCard title="오늘 기록" subtitle="오늘 완료한 세션 흐름을 홈 화면과 함께 공유합니다.">
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                완료 세션
              </p>
              <p className="mt-2 text-lg font-semibold text-slate-900">
                {todayStats.completedSessions}회
              </p>
            </div>

            <div className="rounded-2xl bg-slate-50 px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                총 누적 시간
              </p>
              <p className="mt-2 text-lg font-semibold text-slate-900">
                {todayStats.totalMinutes}분
              </p>
            </div>

            <div className="rounded-2xl bg-slate-50 px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                집중 시간
              </p>
              <p className="mt-2 text-lg font-semibold text-slate-900">
                {todayStats.focusMinutes}분
              </p>
            </div>

            <div className="rounded-2xl bg-slate-50 px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                마지막 완료
              </p>
              <p className="mt-2 text-sm font-semibold text-slate-900">
                {todayStats.lastCompletedLabel
                  ? `${todayStats.lastCompletedLabel} · ${formatClockTime(
                      todayStats.lastCompletedAt,
                    )}`
                  : "아직 완료한 세션이 없어요."}
              </p>
            </div>
          </div>
        </DashboardCard>
      </div>
    </div>
  );
}
