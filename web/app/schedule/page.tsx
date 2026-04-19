import Link from "next/link";

import { AppPage } from "@/components/app-page";
import { DashboardCard } from "@/components/dashboard-card";

export default function SchedulePage() {
  return (
    <AppPage
      title="학사 일정"
      description="월간 일정과 주요 학교 이벤트를 보여주는 페이지의 기본 골격입니다."
      activePath="/schedule"
    >
      <DashboardCard title="일정 페이지 스켈레톤" subtitle="월 단위 또는 리스트형 구성 예정">
        <p className="text-sm leading-7 text-slate-600">
          다음 단계에서 일정 필터, 날짜 범위 이동, 이벤트 카테고리 구분을 추가합니다.
        </p>
        <Link
          href="/"
          className="mt-4 inline-flex rounded-full bg-slate-950 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
        >
          홈으로 돌아가기
        </Link>
      </DashboardCard>
    </AppPage>
  );
}
