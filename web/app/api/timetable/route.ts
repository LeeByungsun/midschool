import { NextRequest, NextResponse } from "next/server";

import { fetchNeisJson, NeisClientError } from "@/lib/neis/client";
import { mapTimetable } from "@/lib/neis/mapper";
import type { NeisResponse, TimetableRowDto } from "@/lib/neis/types";

export async function GET(request: NextRequest) {
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
    const response = await fetchNeisJson<NeisResponse<TimetableRowDto>>(
      "hub/misTimetable",
      {
        GRADE: grade,
        CLASS_NM: classroom,
        ALL_TI_YMD: date,
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
