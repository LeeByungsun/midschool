import type {
  NoticeErrorCode,
  NoticeListResponse,
  NoticeResponseStatus,
  NoticeSummary,
} from "@/lib/notices/types";

export function createNoticeSuccessResponse(items: NoticeSummary[]): NoticeListResponse {
  return {
    status: items.length > 0 ? "success" : "empty",
    items,
  };
}

export function createNoticeErrorResponse(params: {
  message: string;
  items?: NoticeSummary[];
  status?: NoticeResponseStatus;
  errorCode?: NoticeErrorCode;
}): NoticeListResponse {
  return {
    status: params.status ?? "error",
    errorCode: params.errorCode,
    message: params.message,
    items: params.items ?? [],
  };
}
