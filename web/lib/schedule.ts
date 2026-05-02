/** 학사 일정 표시 여부와 정렬 규칙을 정리한 도메인 유틸입니다. */

import type { SchoolEvent } from "@/lib/neis/types";

export function isVisibleSchedule(event: SchoolEvent) {
  const blockedKeywords = ["토요휴업일"];

  return blockedKeywords.every(
    (keyword) => !event.title.includes(keyword) && !event.description.includes(keyword),
  );
}
