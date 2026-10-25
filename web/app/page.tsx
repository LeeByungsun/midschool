/** 홈 대시보드와 초기 설정 안내를 함께 보여주는 메인 페이지입니다. */

import Link from "next/link";

import { AppPage } from "@/components/app-page";
import { HomeDashboard } from "@/components/home-dashboard";
import { StudentSetupCallout } from "@/components/student-setup-callout";

export default function HomePage() {
  return (
    <AppPage
      title="오늘의 학교 생활을 한눈에"
      description="시간표, 급식, 학사 일정, 집중 타이머를 모바일 퍼스트 대시보드로 정리했습니다."
      activePath="/"
    >
      <div className="grid gap-4">
        <Link
          href="/android"
          className="group flex flex-wrap items-center justify-between gap-4 rounded-3xl border border-sky-200 bg-sky-50 px-5 py-4 text-slate-900 transition hover:border-sky-400 hover:bg-sky-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-600"
        >
          <span>
            <span className="block text-sm font-semibold text-sky-700">Android 앱</span>
            <span className="mt-1 block text-base font-semibold">스쿨온을 휴대전화에서도 만나보세요</span>
          </span>
          <span className="text-sm font-semibold text-sky-700 group-hover:underline">앱 소개 보기 →</span>
        </Link>
        <StudentSetupCallout />
        <HomeDashboard />
      </div>
    </AppPage>
  );
}
