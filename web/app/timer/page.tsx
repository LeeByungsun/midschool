import { AppPage } from "@/components/app-page";
import { TimerPanel } from "@/components/timer-panel";

export default function TimerPage() {
  return (
    <AppPage
      title="집중 타이머"
      description="집중과 휴식 프리셋을 바로 시작하고, 새로고침 후에도 남은 시간을 이어서 확인할 수 있습니다."
      activePath="/timer"
    >
      <TimerPanel />
    </AppPage>
  );
}
