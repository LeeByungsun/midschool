import Link from "next/link";

import { AppPage } from "@/components/app-page";
import { DashboardCard } from "@/components/dashboard-card";

export default function TimetablePage() {
  return (
    <AppPage
      title="시간표"
      description="날짜 이동형 일간 시간표 화면의 자리입니다. 다음 단계에서 NEIS BFF와 연결해 실제 시간표를 불러옵니다."
      activePath="/timetable"
    >
      <DashboardCard title="구현 예정" subtitle="현재는 레이아웃 확인용 스켈레톤입니다.">
        <p className="text-sm leading-7 text-slate-600">
          상단 날짜 네비게이션, 교시별 과목 카드, 빈 데이터/오류 상태를 이 페이지에
          연결할 예정입니다.
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
