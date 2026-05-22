/** 가정통신문 목록과 상세 항목에서 사용하는 타입 정의를 모아 둡니다. */

export type NoticeSummary = {
  id: string;
  title: string;
  date: string;
  author: string;
  url: string;
  sourceUrl: string;
};


export type NoticeResponseStatus =
  | "success"
  | "empty"
  | "unsupported"
  | "unavailable"
  | "error";

export type NoticeErrorCode =
  | "HOMEPAGE_NOT_FOUND"
  | "NOTICES_TIMEOUT"
  | "UNSUPPORTED_PROVIDER"
  | "NOTICE_SOURCE_UNAVAILABLE"
  | "NEIS_ERROR"
  | "INTERNAL_ERROR";

export type NoticeListResponse = {
  status: NoticeResponseStatus;
  items: NoticeSummary[];
  message?: string;
  errorCode?: NoticeErrorCode;
};
