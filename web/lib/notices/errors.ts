/** 가정통신문 수집 흐름에서 재사용하는 오류 판별과 메시지 규칙을 정의합니다. */

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
