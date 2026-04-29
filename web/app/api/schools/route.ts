import { NextRequest, NextResponse } from "next/server";

import { fetchNeisJson, NeisClientError } from "@/lib/neis/client";
import { mapSchoolInfo } from "@/lib/neis/mapper";
import type { NeisResponse, SchoolInfoRowDto } from "@/lib/neis/types";

const SEARCHABLE_SCHOOL_KINDS = new Set(["초등학교", "중학교"]);

export async function GET(request: NextRequest) {
  const query = request.nextUrl.searchParams.get("query")?.trim() ?? "";
  const officeCode = request.nextUrl.searchParams.get("officeCode")?.trim() ?? "";
  const schoolCode = request.nextUrl.searchParams.get("schoolCode")?.trim() ?? "";

  if (officeCode && schoolCode) {
    return getSchoolByCode({ officeCode, schoolCode });
  }

  if (query.length < 2) {
    return NextResponse.json(
      { message: "학교 이름은 두 글자 이상 입력해 주세요." },
      { status: 400 },
    );
  }

  try {
    const response = await fetchNeisJson<NeisResponse<SchoolInfoRowDto>>(
      "hub/schoolInfo",
      {
        SCHUL_NM: query,
      },
      {
        includeSchoolContext: false,
      },
    );

    return NextResponse.json({
      items: mapSchoolInfo(response).filter((school) =>
        SEARCHABLE_SCHOOL_KINDS.has(school.schoolKind),
      ),
    });
  } catch (error) {
    return handleApiError(error);
  }
}

async function getSchoolByCode(params: {
  officeCode: string;
  schoolCode: string;
}) {
  try {
    const response = await fetchNeisJson<NeisResponse<SchoolInfoRowDto>>(
      "hub/schoolInfo",
      {},
      {
        officeCode: params.officeCode,
        schoolCode: params.schoolCode,
      },
    );

    return NextResponse.json({
      items: mapSchoolInfo(response).filter((school) =>
        SEARCHABLE_SCHOOL_KINDS.has(school.schoolKind),
      ),
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
    { message: "학교 검색 정보를 불러오지 못했어요." },
    { status: 500 },
  );
}
