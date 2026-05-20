/** 급식 상세 화면에서 표시할 카드 목록과 요청 날짜 일치 여부를 계산합니다. */

import type { MealInfo } from "@/lib/neis/types";

export type MealDetailState = {
  visibleMeals: MealInfo[];
  hasRequestedDate: boolean;
};

function compareMeals(a: MealInfo, b: MealInfo) {
  const dateOrder = a.date.localeCompare(b.date);

  if (dateOrder !== 0) {
    return dateOrder;
  }

  return a.mealType.localeCompare(b.mealType);
}

export function resolveMealDetailState(
  items: MealInfo[],
  requestedDate: string,
): MealDetailState {
  return {
    visibleMeals: [...items].sort(compareMeals),
    hasRequestedDate: items.some((meal) => meal.date === requestedDate),
  };
}
