/** NEIS 응답 DTO를 웹 UI에서 쓰는 도메인 타입으로 변환합니다. */

import type {
  MealInfo,
  MealRowDto,
  NeisResponse,
  NeisResultDto,
  NeisSection,
  ScheduleRowDto,
  SchoolInfo,
  SchoolInfoRowDto,
  SchoolEvent,
  TimetableItem,
  TimetableRowDto,
} from "@/lib/neis/types";

import { NeisClientError } from "@/lib/neis/client";

function extractRows<T>(sections: NeisSection<T>[] | undefined, dataLabel: string) {
  const result = sections
    ?.flatMap((section) => section.head ?? [])
    .find((head) => head.RESULT)?.RESULT;

  validateNeisResult(result, dataLabel);

  return sections?.[1]?.row ?? [];
}

function validateNeisResult(result: NeisResultDto | undefined, dataLabel: string) {
  const code = result?.CODE?.trim() ?? "";

  if (!code || code === "INFO-000" || code === "INFO-200") {
    return;
  }

  const message =
    {
      "INFO-100": `${dataLabel} 조회에 필요한 값이 누락되었어요.`,
      "INFO-300": `${dataLabel} 데이터가 준비되지 않았어요.`,
      "ERROR-300": "나이스 인증키를 다시 확인해 주세요.",
      "ERROR-336": "요청 횟수가 많아 잠시 후 다시 시도해 주세요.",
    }[code] ??
    result?.MESSAGE?.trim() ??
    `${dataLabel} 정보를 불러오지 못했어요.`;

  throw new NeisClientError(message, 502);
}

export function mapMeals(response: NeisResponse<MealRowDto>): MealInfo[] {
  return extractRows(response.mealServiceDietInfo, "급식").map((row) => ({
    date: row.MLSV_YMD,
    mealType: row.MMEAL_SC_NM ?? "",
    menu: row.DDISH_NM ?? "",
    calorieInfo: row.CAL_INFO ?? "",
    nutritionInfo: row.NTR_INFO ?? "",
    originInfo: row.ORPLC_INFO ?? "",
  }));
}

export function mapSchedules(
  response: NeisResponse<ScheduleRowDto>,
): SchoolEvent[] {
  return extractRows(response.SchoolSchedule, "학사 일정").map((row) => ({
    date: row.AA_YMD,
    title: row.EVENT_NM ?? "",
    description: row.EVENT_CNTNT ?? "",
  }));
}

export function mapTimetable(
  response: NeisResponse<TimetableRowDto>,
): TimetableItem[] {
  const timetableSections =
    response.elsTimetable ??
    response.misTimetable ??
    response.hisTimetable ??
    response.spsTimetable;

  return extractRows(timetableSections, "시간표").map((row) => ({
    date: row.ALL_TI_YMD,
    period: row.PERIO ?? "",
    subject: row.ITRT_CNTNT ?? "",
    grade: row.GRADE ?? "",
    classroom: row.CLASS_NM ?? "",
  }));
}

export function mapSchoolInfo(
  response: NeisResponse<SchoolInfoRowDto>,
): SchoolInfo[] {
  return extractRows(response.schoolInfo, "학교 검색").map((row) => ({
    officeCode: row.ATPT_OFCDC_SC_CODE,
    officeName: row.ATPT_OFCDC_SC_NM ?? "",
    schoolCode: row.SD_SCHUL_CODE,
    schoolName: row.SCHUL_NM ?? "",
    schoolKind: row.SCHUL_KND_SC_NM ?? "",
    location: row.LCTN_SC_NM ?? "",
    jurisdiction: row.JU_ORG_NM ?? "",
    foundation: row.FOND_SC_NM ?? "",
    roadAddress: row.ORG_RDNMA ?? "",
    telephone: row.ORG_TELNO ?? "",
    homepage: row.HMPG_ADRES ?? "",
  }));
}
