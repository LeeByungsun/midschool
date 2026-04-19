import { NextRequest, NextResponse } from "next/server";

import { fetchNeisJson, NeisClientError } from "@/lib/neis/client";
import { mapSchedules } from "@/lib/neis/mapper";
import type { NeisResponse, ScheduleRowDto } from "@/lib/neis/types";

export async function GET(request: NextRequest) {
  const date = request.nextUrl.searchParams.get("date")?.trim() ?? undefined;

  try {
    const response = await fetchNeisJson<NeisResponse<ScheduleRowDto>>(
      "hub/SchoolSchedule",
      {
        AA_YMD: date,
      },
    );

    return NextResponse.json({
      items: mapSchedules(response),
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
    { message: "학사 일정 정보를 불러오지 못했어요." },
    { status: 500 },
  );
}
