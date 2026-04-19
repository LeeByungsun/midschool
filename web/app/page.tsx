import Link from "next/link";

import { AppPage } from "@/components/app-page";
import { DashboardCard } from "@/components/dashboard-card";
import {
  mealSummary,
  timerPresets,
  todayClasses,
  upcomingEvents,
} from "@/lib/site-data";

export default function HomePage() {
  return (
    <AppPage
      title="오늘의 학교 생활을 한눈에"
      description="시간표, 급식, 학사 일정, 집중 타이머를 모바일 퍼스트 대시보드로 먼저 정리했습니다. 실제 NEIS 연동 전까지는 구조 검증용 목업 데이터를 보여줍니다."
      activePath="/"
    >
      <div className="grid gap-4 lg:grid-cols-[1.35fr_0.95fr]">
        <div className="grid gap-4">
          <DashboardCard
            title="오늘 시간표"
            subtitle="4월 19일 · 교시별 흐름 확인"
            action={
              <Link
                href="/timetable"
                className="rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-200"
              >
                전체 보기
              </Link>
            }
          >
            <ul className="space-y-3">
              {todayClasses.map((item) => (
                <li
                  key={`${item.period}-${item.subject}`}
                  className="flex items-start justify-between gap-3 rounded-2xl bg-slate-50 px-4 py-3"
                >
                  <div>
                    <p className="text-sm font-semibold text-slate-900">
                      {item.period} · {item.subject}
                    </p>
                    <p className="mt-1 text-sm text-slate-500">{item.emphasis}</p>
                  </div>
                  <span className="rounded-full bg-white px-2.5 py-1 text-xs font-medium text-slate-500 shadow-sm">
                    준비
                  </span>
                </li>
              ))}
            </ul>
          </DashboardCard>

          <DashboardCard title={mealSummary.title} subtitle="점심 메뉴 요약">
            <div className="rounded-3xl bg-gradient-to-br from-amber-50 via-white to-orange-50 p-4">
              <ul className="grid gap-2 sm:grid-cols-2">
                {mealSummary.menu.map((menu) => (
                  <li
                    key={menu}
                    className="rounded-2xl border border-amber-100 bg-white px-3 py-2 text-sm text-slate-700"
                  >
                    {menu}
                  </li>
                ))}
              </ul>
              <p className="mt-3 text-sm text-slate-500">{mealSummary.note}</p>
            </div>
          </DashboardCard>
        </div>

        <div className="grid gap-4">
          <DashboardCard title="다가오는 일정" subtitle="가까운 일정만 먼저 요약">
            <ul className="space-y-3">
              {upcomingEvents.map((event) => (
                <li
                  key={`${event.date}-${event.title}`}
                  className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3"
                >
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-700">
                    {event.date}
                  </p>
                  <p className="mt-2 text-sm font-semibold text-slate-900">
                    {event.title}
                  </p>
                  <p className="mt-1 text-sm text-slate-500">{event.category}</p>
                </li>
              ))}
            </ul>
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

          <DashboardCard title="다음 작업 추천" subtitle="웹 1차 구현 순서">
            <ol className="space-y-2 text-sm text-slate-600">
              <li>1. 설정 저장과 학년/반 입력 연결</li>
              <li>2. Route Handler 기반 NEIS BFF 연결</li>
              <li>3. 시간표/일정 상세 페이지 실제 데이터 연동</li>
            </ol>
          </DashboardCard>
        </div>
      </div>
    </AppPage>
  );
}
