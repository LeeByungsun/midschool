import { NextRequest, NextResponse } from "next/server";

import { fetchNeisJson, NeisClientError } from "@/lib/neis/client";
import { mapTimetable } from "@/lib/neis/mapper";
import type { NeisResponse, TimetableRowDto } from "@/lib/neis/types";

function resolveTimetableEndpoint(schoolKind?: string) {
  switch (schoolKind) {
    case "초등학교":
      return "hub/elsTimetable";
    case "고등학교":
      return "hub/hisTimetable";
    case "특수학교":
      return "hub/spsTimetable";
    default:
      return "hub/misTimetable";
  }
}

export async function GET(request: NextRequest) {
  const officeCode =
    request.nextUrl.searchParams.get("officeCode")?.trim() ?? undefined;
  const schoolCode =
    request.nextUrl.searchParams.get("schoolCode")?.trim() ?? undefined;
  const schoolKind =
    request.nextUrl.searchParams.get("schoolKind")?.trim() ?? undefined;
  const grade = request.nextUrl.searchParams.get("grade")?.trim() ?? "";
  const classroom =
    request.nextUrl.searchParams.get("classroom")?.trim() ?? "";
  const date = request.nextUrl.searchParams.get("date")?.trim() ?? undefined;

  if (!grade || !classroom) {
    return NextResponse.json(
      { message: "grade와 classroom 파라미터가 필요해요." },
      { status: 400 },
    );
  }

  try {
    const endpoint = resolveTimetableEndpoint(schoolKind);
    const response = await fetchNeisJson<NeisResponse<TimetableRowDto>>(
      endpoint,
      {
        GRADE: grade,
        CLASS_NM: classroom,
        ALL_TI_YMD: date,
      },
      {
        officeCode,
        schoolCode,
      },
    );

    return NextResponse.json({
      items: mapTimetable(response),
    });
  } catch (error) {
    return handleApiError(error);
  }
}

function handleApiError(error: unknown) {
  if (error instanceof NeisClientError) {
    return NextResponse.json({ message: error.message }, { status: error.status });
  }

  return NextResponse.json(
    { message: "시간표 정보를 불러오지 못했어요." },
    { status: 500 },
  );
}
