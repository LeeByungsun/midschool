import { AppPage } from "@/components/app-page";
import { TimetableBrowser } from "@/components/timetable-browser";

export default function TimetablePage() {
  return (
    <AppPage
      title="시간표"
      description="저장된 학년/반과 선택한 날짜를 기준으로 실제 시간표를 불러옵니다."
      activePath="/timetable"
    >
      <TimetableBrowser />
    </AppPage>
  );
}
