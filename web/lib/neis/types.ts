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
  elsTimetable?: NeisSection<T>[];
  misTimetable?: NeisSection<T>[];
  hisTimetable?: NeisSection<T>[];
  spsTimetable?: NeisSection<T>[];
  schoolInfo?: NeisSection<T>[];
};

export type MealRowDto = {
  MLSV_YMD: string;
  MMEAL_SC_NM?: string;
  DDISH_NM?: string;
  CAL_INFO?: string;
  NTR_INFO?: string;
  ORPLC_INFO?: string;
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

export type SchoolInfoRowDto = {
  ATPT_OFCDC_SC_CODE: string;
  ATPT_OFCDC_SC_NM?: string;
  SD_SCHUL_CODE: string;
  SCHUL_NM?: string;
  SCHUL_KND_SC_NM?: string;
  LCTN_SC_NM?: string;
  JU_ORG_NM?: string;
  FOND_SC_NM?: string;
  ORG_RDNMA?: string;
  ORG_TELNO?: string;
  HMPG_ADRES?: string;
};

export type MealInfo = {
  date: string;
  mealType: string;
  menu: string;
  calorieInfo: string;
  nutritionInfo: string;
  originInfo: string;
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

export type SchoolInfo = {
  officeCode: string;
  officeName: string;
  schoolCode: string;
  schoolName: string;
  schoolKind: string;
  location: string;
  jurisdiction: string;
  foundation: string;
  roadAddress: string;
  telephone: string;
  homepage: string;
};
