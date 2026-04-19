import Link from "next/link";

import { AppPage } from "@/components/app-page";
import { DashboardCard } from "@/components/dashboard-card";

export default function SettingsPage() {
  return (
    <AppPage
      title="설정"
      description="학년/반과 개인 선호값을 웹 저장소에 연결하는 설정 화면의 자리입니다."
      activePath="/settings"
    >
      <DashboardCard title="설정 화면 스켈레톤" subtitle="학년/반 저장이 1차 구현 우선순위입니다.">
        <p className="text-sm leading-7 text-slate-600">
          다음 단계에서 학년/반 입력, 마지막 선택값 유지, 간단한 사용자 선호 설정을
          연결합니다.
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
