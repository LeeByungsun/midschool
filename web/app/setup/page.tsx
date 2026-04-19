import { AppPage } from "@/components/app-page";
import { SettingsForm } from "@/components/settings-form";

export default function SetupPage() {
  return (
    <AppPage
      title="초기 설정"
      description="웹에서 사용할 학교 이름과 학년/반을 먼저 저장해 주세요. 저장된 값은 이후 대시보드와 상세 조회의 기준이 됩니다."
      activePath="/settings"
    >
      <SettingsForm />
    </AppPage>
  );
}
