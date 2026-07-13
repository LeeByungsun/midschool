/** 가정통신문 수집 흐름에서 재사용하는 오류 판별과 메시지 규칙을 정의합니다. */

import type { NoticeErrorCode } from "@/lib/notices/types";

const RECOVERABLE_NOTICE_ERROR_PREFIXES = [
  "가정통신문 게시판 링크를 찾지 못했어요.",
  "학교 홈페이지에서 가정통신문 구조를 찾지 못했어요.",
  "학교 홈페이지 응답 대기 시간이 초과되었어요:",
];

export function isRecoverableNoticeError(error: unknown) {
  if (!(error instanceof Error)) {
    return false;
  }

  return RECOVERABLE_NOTICE_ERROR_PREFIXES.some((prefix) =>
    error.message.startsWith(prefix),
  );
}

export class NoticeRequestError extends Error {
  readonly errorCode?: NoticeErrorCode;

  constructor(message: string, errorCode?: NoticeErrorCode) {
    super(message);
    this.name = "NoticeRequestError";
    this.errorCode = errorCode;
  }
}

export function createNoticeRequestError(response: {
  message?: string;
  errorCode?: NoticeErrorCode;
}) {
  return new NoticeRequestError(
    response.message ?? "가정통신문 목록을 불러오지 못했어요.",
    response.errorCode,
  );
}

export function canRetryNoticeRequest(error: unknown) {
  return !(
    error instanceof NoticeRequestError &&
    error.errorCode === "UNSUPPORTED_PROVIDER"
  );
}

export function isDgeSchoolHomepage(homepageUrl: string) {
  if (!homepageUrl) {
    return false;
  }

  try {
    const hostname = new URL(homepageUrl).hostname;
    return hostname === "dge.ms.kr" || hostname.endsWith(".dge.ms.kr");
  } catch {
    return false;
  }
}
