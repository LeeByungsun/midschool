/** 웹 화면에서 재사용하는 날짜 포맷과 날짜 계산 유틸을 제공합니다. */

const DATE_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  month: "long",
  day: "numeric",
  weekday: "short",
});

const MONTH_TITLE_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "long",
});

function padNumber(value: number) {
  return String(value).padStart(2, "0");
}

export function formatDateKey(date: Date) {
  return `${date.getFullYear()}${padNumber(date.getMonth() + 1)}${padNumber(
    date.getDate(),
  )}`;
}

export function formatMonthKey(date: Date) {
  return `${date.getFullYear()}${padNumber(date.getMonth() + 1)}`;
}

export function formatKoreanDateLabel(date: Date | string) {
  const parsed = typeof date === "string" ? parseBasicDate(date) : date;

  if (!parsed) {
    return typeof date === "string" ? date : "";
  }

  return DATE_FORMATTER.format(parsed);
}

export function formatKoreanMonthLabel(date: Date) {
  return MONTH_TITLE_FORMATTER.format(date);
}

export function parseBasicDate(value: string) {
  if (!/^\d{8}$/.test(value)) {
    return null;
  }

  const year = Number(value.slice(0, 4));
  const month = Number(value.slice(4, 6));
  const day = Number(value.slice(6, 8));

  return new Date(year, month - 1, day);
}
