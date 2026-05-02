/** 홈 대시보드와 초기 설정 안내를 함께 보여주는 메인 페이지입니다. */

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
        <StudentSetupCallout />
        <HomeDashboard />
      </div>
    </AppPage>
  );
}
