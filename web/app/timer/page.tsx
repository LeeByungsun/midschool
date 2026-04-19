import Link from "next/link";

import { AppPage } from "@/components/app-page";
import { DashboardCard } from "@/components/dashboard-card";

export default function TimerPage() {
  return (
    <AppPage
      title="집중 타이머"
      description="브라우저 환경에 맞는 집중/휴식 타이머를 준비하는 자리입니다."
      activePath="/timer"
    >
      <DashboardCard title="타이머 상세" subtitle="1차는 화면 내부 동작과 상태 복원을 우선합니다.">
        <p className="text-sm leading-7 text-slate-600">
          브라우저 알림과 PWA 확장은 2차 이후 범위로 두고, 먼저 안정적인 상태 저장과
          모바일 가독성을 확보합니다.
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
