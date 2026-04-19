import { AppPage } from "@/components/app-page";
import { SettingsForm } from "@/components/settings-form";

export default function SettingsPage() {
  return (
    <AppPage
      title="설정"
      description="학년/반과 개인 선호값을 웹 저장소에 연결하는 설정 화면의 자리입니다."
      activePath="/settings"
    >
      <SettingsForm />
    </AppPage>
  );
}
