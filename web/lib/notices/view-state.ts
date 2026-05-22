/** 가정통신문 카드가 보여줄 안전한 화면 상태를 한곳에서 계산합니다. */

import type { StudentPreferences } from "@/lib/storage/preferences";
import type { NoticeSummary } from "@/lib/notices/types";

export type NoticeLoadState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "error"; message: string; canRetry: boolean }
  | { status: "success"; items: NoticeSummary[] };

export type NoticeCardState =
  | {
      status: "not-configured";
      title: string;
      message: string;
    }
  | {
      status: "loading";
      message: string;
    }
  | {
      status: "empty";
      title: string;
      message: string;
    }
  | {
      status: "error";
      message: string;
      canRetry: boolean;
    }
  | {
      status: "success";
      items: NoticeSummary[];
    };

const NOTICE_LOADING_MESSAGE = "가정통신문 목록을 불러오는 중...";
const NOTICE_NOT_CONFIGURED_TITLE = "초기 설정이 먼저 필요해요.";
const NOTICE_NOT_CONFIGURED_MESSAGE =
  "학교를 저장하면 가정통신문도 같은 기준으로 가져올 수 있어요.";
const NOTICE_EMPTY_TITLE = "가정통신문이 없어요.";
const NOTICE_EMPTY_MESSAGE =
  "현재 학교 홈페이지에서 확인된 최근 가정통신문이 없어요.";

function hasNoticeConfiguration(studentInfo: StudentPreferences | null) {
  return Boolean(
    studentInfo?.officeCode.trim() &&
      studentInfo.schoolCode.trim() &&
      studentInfo.grade.trim() &&
      studentInfo.classroom.trim(),
  );
}

export function resolveNoticeCardState(params: {
  hydrated: boolean;
  studentInfo: StudentPreferences | null;
  loadState: NoticeLoadState;
}): NoticeCardState {
  if (!params.hydrated) {
    return {
      status: "loading",
      message: "가정통신문 기준을 준비 중...",
    };
  }

  if (!hasNoticeConfiguration(params.studentInfo)) {
    return {
      status: "not-configured",
      title: NOTICE_NOT_CONFIGURED_TITLE,
      message: NOTICE_NOT_CONFIGURED_MESSAGE,
    };
  }

  switch (params.loadState.status) {
    case "idle":
    case "loading":
      return {
        status: "loading",
        message: NOTICE_LOADING_MESSAGE,
      };
    case "error":
      return {
        status: "error",
        message: params.loadState.message,
        canRetry: params.loadState.canRetry,
      };
    case "success":
      return params.loadState.items.length === 0
        ? {
            status: "empty",
            title: NOTICE_EMPTY_TITLE,
            message: NOTICE_EMPTY_MESSAGE,
          }
        : {
            status: "success",
            items: params.loadState.items,
          };
  }
}
