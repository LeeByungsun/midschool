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
