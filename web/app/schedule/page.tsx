/** 학사 일정 조회 화면을 페이지 단위로 조합합니다. */

import { AppPage } from "@/components/app-page";
import { ScheduleBrowser } from "@/components/schedule-browser";

export default function SchedulePage() {
  return (
    <AppPage
      title="학사 일정"
      description="월 단위 학사 일정을 실제 BFF를 통해 불러와서 보여줍니다."
      activePath="/schedule"
    >
      <ScheduleBrowser />
    </AppPage>
  );
}
