import { AppPage } from "@/components/app-page";
import { HomeDashboard } from "@/components/home-dashboard";
import { StudentSetupCallout } from "@/components/student-setup-callout";

export default function HomePage() {
  return (
    <AppPage
      title="오늘의 학교 생활을 한눈에"
      description="시간표, 급식, 학사 일정, 집중 타이머를 모바일 퍼스트 대시보드로 정리했습니다. 저장된 학년/반이 있으면 실제 BFF를 통해 데이터를 불러옵니다."
      activePath="/"
    >
      <div className="grid gap-4">
        <StudentSetupCallout />
        <HomeDashboard />
      </div>
    </AppPage>
  );
}
