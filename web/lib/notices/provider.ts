export type NoticeProvider =
  | "sen-preview"
  | "goehs-board"
  | "gwe-board"
  | "busan-school";

export function detectNoticeProvider(homepageUrl: string, homepageHtml: string): NoticeProvider | null {
  const hostname = new URL(homepageUrl).hostname;

  if (hostname.endsWith("busanedu.net") || homepageHtml.includes("school.busanedu.net")) {
    return "busan-school";
  }

  if (hostname.endsWith("gwe.ms.kr") || homepageHtml.includes("boardCnts/list.do?boardID=")) {
    return "gwe-board";
  }

  if (hostname.endsWith("goehs.kr") || homepageHtml.includes("/na/ntt/selectNttList.do")) {
    return "goehs-board";
  }

  if (hostname.endsWith("sen.ms.kr") || homepageHtml.includes("fnBoardPage_")) {
    return "sen-preview";
  }

  return null;
}
