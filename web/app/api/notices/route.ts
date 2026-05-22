/** 가정통신문 조회 요청을 받아 학교별 공지 목록을 반환하는 서버 라우트입니다. */

import { NextRequest, NextResponse } from "next/server";

import { fetchNeisJson, NeisClientError } from "@/lib/neis/client";
import { buildNoticeHomepageCandidates } from "@/lib/notices/candidates";
import { isRecoverableNoticeError } from "@/lib/notices/errors";
import { createNoticeErrorResponse, createNoticeSuccessResponse } from "@/lib/notices/response";
import { mapSchoolInfo } from "@/lib/neis/mapper";
import { fetchSchoolHomepageNotices } from "@/lib/notices/fetch";
import type { NeisResponse, SchoolInfoRowDto } from "@/lib/neis/types";

const NOTICES_TOTAL_TIMEOUT_MS = 15_000;

export async function GET(request: NextRequest) {
  const homepage = request.nextUrl.searchParams.get("homepage")?.trim() ?? "";
  const officeCode = request.nextUrl.searchParams.get("officeCode")?.trim() ?? "";
  const schoolCode = request.nextUrl.searchParams.get("schoolCode")?.trim() ?? "";
  const limit = Number(request.nextUrl.searchParams.get("limit") ?? "5");
  let resolvedHomepageUrl = "";

  try {
    const homepageCandidates = buildNoticeHomepageCandidates({
      requestedHomepage: homepage,
      resolvedHomepage: await resolveHomepageBySchoolCode({ officeCode, schoolCode }),
    });

    if (homepageCandidates.length === 0) {
      return NextResponse.json(
        createNoticeErrorResponse({
          message: "학교 홈페이지 주소를 찾지 못했어요.",
          errorCode: "HOMEPAGE_NOT_FOUND",
        }),
        { status: 404 },
      );
    }

    const safeLimit = Number.isFinite(limit) ? Math.max(1, Math.min(limit, 10)) : 5;
    let lastError: unknown;

    for (const homepageCandidate of homepageCandidates) {
      resolvedHomepageUrl = homepageCandidate;

      try {
        const items = await Promise.race([
          fetchSchoolHomepageNotices({
            homepageUrl: homepageCandidate,
            limit: safeLimit,
          }),
          createTimeoutPromise(homepageCandidate),
        ]);

        return NextResponse.json(createNoticeSuccessResponse(items));
      } catch (error) {
        lastError = error;
      }
    }

    throw lastError;
  } catch (error) {
    if (error instanceof Error && error.message.startsWith("NOTICES_TIMEOUT:")) {
      return NextResponse.json(
        createNoticeErrorResponse({
          message: `가정통신문 목록을 불러오는 시간이 너무 오래 걸려 중단했어요. (homepage: ${resolvedHomepageUrl})`,
          errorCode: "NOTICES_TIMEOUT",
        }),
        { status: 504 },
      );
    }

    if (error instanceof NeisClientError) {
      return NextResponse.json(
        createNoticeErrorResponse({
          message: error.message,
          errorCode: "NEIS_ERROR",
        }),
        { status: error.status },
      );
    }

    if (isRecoverableNoticeError(error)) {
      const recoverableMessage = buildRecoverableNoticeMessage({
        error,
        homepageUrl: resolvedHomepageUrl,
      });

      return NextResponse.json(
        createNoticeErrorResponse({
          message: recoverableMessage,
          status: isDgeSchoolHomepage(resolvedHomepageUrl) ? "unsupported" : "unavailable",
          errorCode: isDgeSchoolHomepage(resolvedHomepageUrl)
            ? "UNSUPPORTED_PROVIDER"
            : "NOTICE_SOURCE_UNAVAILABLE",
        }),
        { status: 200 },
      );
    }

    return NextResponse.json(
      createNoticeErrorResponse({
        message:
          error instanceof Error
            ? `${error.message}${resolvedHomepageUrl ? ` (homepage: ${resolvedHomepageUrl})` : ""}`
            : "가정통신문 목록을 불러오지 못했어요.",
        errorCode: "INTERNAL_ERROR",
      }),
      { status: 500 },
    );
  }
}

function buildRecoverableNoticeMessage(params: {
  error: unknown;
  homepageUrl: string;
}) {
  if (isDgeSchoolHomepage(params.homepageUrl)) {
    return "대구교육청 학교 홈페이지는 가정통신문 조회를 아직 지원하지 않습니다.";
  }

  if (params.error instanceof Error) {
    return `${params.error.message}${params.homepageUrl ? ` (homepage: ${params.homepageUrl})` : ""}`;
  }

  return "가정통신문 목록을 불러오지 못했어요.";
}

function isDgeSchoolHomepage(homepageUrl: string) {
  if (!homepageUrl) {
    return false;
  }

  try {
    return new URL(homepageUrl).hostname.endsWith("dge.ms.kr");
  } catch {
    return false;
  }
}

function createTimeoutPromise(homepageUrl: string) {
  return new Promise<never>((_, reject) => {
    setTimeout(() => {
      reject(new Error(`NOTICES_TIMEOUT:${homepageUrl}`));
    }, NOTICES_TOTAL_TIMEOUT_MS);
  });
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
