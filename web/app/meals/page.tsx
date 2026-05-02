/** 급식 조회 화면을 페이지 단위로 조합합니다. */

import { AppPage } from "@/components/app-page";
import { MealBrowser } from "@/components/meal-browser";

export default function MealsPage() {
  return (
    <AppPage
      title="급식"
      description="선택한 날짜 기준으로 급식 메뉴, 알레르기 표시, 칼로리, 영양/원산지 정보를 자세히 확인합니다."
      activePath="/meals"
    >
      <MealBrowser />
    </AppPage>
  );
}
