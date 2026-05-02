/** 내비게이션과 기본 화면 문구에 쓰는 정적 사이트 데이터를 모아 둡니다. */

export const navigationItems = [
  { href: "/", label: "홈", icon: "🏠" },
  { href: "/timetable", label: "시간표", icon: "📚" },
  { href: "/schedule", label: "일정", icon: "🗓️" },
  { href: "/timer", label: "타이머", icon: "⏱️" },
  { href: "/settings", label: "설정", icon: "⚙️" },
] as const;

export const todayClasses = [
  { period: "1교시", subject: "국어", emphasis: "발표 준비 있음" },
  { period: "2교시", subject: "수학", emphasis: "문제풀이" },
  { period: "3교시", subject: "과학", emphasis: "실험 보고서 제출" },
  { period: "4교시", subject: "영어", emphasis: "듣기 평가 대비" },
];

export const mealSummary = {
  title: "오늘 급식",
  menu: ["잡곡밥", "소고기미역국", "간장불고기", "시금치나물", "배추김치"],
  note: "알레르기 정보와 상세 영양표는 곧 연결 예정",
};

export const upcomingEvents = [
  { date: "4/22", title: "과학의 날 행사", category: "학교 행사" },
  { date: "4/24", title: "영어 단어 시험", category: "학습" },
  { date: "4/29", title: "체육대회 예선", category: "체육" },
];

export const timerPresets = [
  { label: "집중", minutes: 25, tone: "bg-sky-100 text-sky-700" },
  { label: "휴식", minutes: 5, tone: "bg-emerald-100 text-emerald-700" },
  { label: "딥포커스", minutes: 50, tone: "bg-violet-100 text-violet-700" },
];
