import { NextRequest, NextResponse } from "next/server";

import { fetchNeisJson, NeisClientError } from "@/lib/neis/client";
import { mapSchoolInfo } from "@/lib/neis/mapper";
import { fetchSchoolHomepageNotices } from "@/lib/notices/fetch";
import type { NeisResponse, SchoolInfoRowDto } from "@/lib/neis/types";

export async function GET(request: NextRequest) {
  const homepage = request.nextUrl.searchParams.get("homepage")?.trim() ?? "";
  const officeCode = request.nextUrl.searchParams.get("officeCode")?.trim() ?? "";
  const schoolCode = request.nextUrl.searchParams.get("schoolCode")?.trim() ?? "";
  const limit = Number(request.nextUrl.searchParams.get("limit") ?? "5");
  let resolvedHomepageUrl = "";

  try {
    resolvedHomepageUrl =
      homepage || (await resolveHomepageBySchoolCode({ officeCode, schoolCode }));

    if (!resolvedHomepageUrl) {
      return NextResponse.json(
        { message: "학교 홈페이지 주소를 찾지 못했어요.", items: [] },
        { status: 404 },
      );
    }

    const items = await fetchSchoolHomepageNotices({
      homepageUrl: resolvedHomepageUrl,
      limit: Number.isFinite(limit) ? Math.max(1, Math.min(limit, 10)) : 5,
    });

    return NextResponse.json({ items });
  } catch (error) {
    if (error instanceof NeisClientError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }

    return NextResponse.json(
      {
        message:
          error instanceof Error
            ? `${error.message}${resolvedHomepageUrl ? ` (homepage: ${resolvedHomepageUrl})` : ""}`
            : "가정통신문 목록을 불러오지 못했어요.",
      },
      { status: 500 },
    );
  }
}

async function resolveHomepageBySchoolCode(params: {
  officeCode: string;
  schoolCode: string;
}) {
  if (!params.officeCode || !params.schoolCode) {
    return "";
  }

  const response = await fetchNeisJson<NeisResponse<SchoolInfoRowDto>>(
    "hub/schoolInfo",
    {},
    {
      officeCode: params.officeCode,
      schoolCode: params.schoolCode,
    },
  );

  return mapSchoolInfo(response)[0]?.homepage?.trim() ?? "";
}
