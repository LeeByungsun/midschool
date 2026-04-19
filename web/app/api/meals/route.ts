import { NextRequest, NextResponse } from "next/server";

import { fetchNeisJson, NeisClientError } from "@/lib/neis/client";
import { mapMeals } from "@/lib/neis/mapper";
import type { MealRowDto, NeisResponse } from "@/lib/neis/types";

export async function GET(request: NextRequest) {
  const officeCode =
    request.nextUrl.searchParams.get("officeCode")?.trim() ?? undefined;
  const schoolCode =
    request.nextUrl.searchParams.get("schoolCode")?.trim() ?? undefined;
  const date = request.nextUrl.searchParams.get("date")?.trim() ?? undefined;

  try {
    const response = await fetchNeisJson<NeisResponse<MealRowDto>>(
      "hub/mealServiceDietInfo",
      {
        MLSV_YMD: date,
      },
      {
        officeCode,
        schoolCode,
      },
    );

    return NextResponse.json({
      items: mapMeals(response),
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
    { message: "급식 정보를 불러오지 못했어요." },
    { status: 500 },
  );
}
