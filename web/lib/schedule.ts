import type { SchoolEvent } from "@/lib/neis/types";

export function isVisibleSchedule(event: SchoolEvent) {
  const blockedKeywords = ["토요휴업일"];

  return blockedKeywords.every(
    (keyword) => !event.title.includes(keyword) && !event.description.includes(keyword),
  );
}
