export type NeisResultDto = {
  CODE?: string;
  MESSAGE?: string;
};

export type NeisHeadDto = {
  RESULT?: NeisResultDto;
  list_total_count?: number;
};

export type NeisSection<T> = {
  head?: NeisHeadDto[];
  row?: T[];
};

export type NeisResponse<T> = {
  mealServiceDietInfo?: NeisSection<T>[];
  SchoolSchedule?: NeisSection<T>[];
  misTimetable?: NeisSection<T>[];
};

export type MealRowDto = {
  MLSV_YMD: string;
  MMEAL_SC_NM?: string;
  DDISH_NM?: string;
  CAL_INFO?: string;
};

export type ScheduleRowDto = {
  AA_YMD: string;
  EVENT_NM?: string;
  EVENT_CNTNT?: string;
};

export type TimetableRowDto = {
  ALL_TI_YMD: string;
  PERIO?: string;
  ITRT_CNTNT?: string;
  GRADE?: string;
  CLASS_NM?: string;
};

export type MealInfo = {
  date: string;
  mealType: string;
  menu: string;
  calorieInfo: string;
};

export type SchoolEvent = {
  date: string;
  title: string;
  description: string;
};

export type TimetableItem = {
  date: string;
  period: string;
  subject: string;
  grade: string;
  classroom: string;
};
