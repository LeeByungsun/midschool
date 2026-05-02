/** 학교 홈페이지 후보 URL을 조합해 가정통신문 탐색 대상을 정리합니다. */

export function buildNoticeHomepageCandidates(params: {
  requestedHomepage?: string;
  resolvedHomepage?: string;
}) {
  return Array.from(
    new Set(
      [params.requestedHomepage, params.resolvedHomepage]
        .map((value) => value?.trim() ?? "")
        .filter(Boolean),
    ),
  );
}
