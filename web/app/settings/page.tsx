import { AppPage } from "@/components/app-page";
import { SettingsForm } from "@/components/settings-form";

export default function SettingsPage() {
  return (
    <AppPage
      title="설정"
      description="학교 이름과 학년/반을 수정하는 설정 화면입니다."
      activePath="/settings"
    >
      <SettingsForm />
    </AppPage>
  );
}
