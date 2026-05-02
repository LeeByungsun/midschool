/** 학교 홈페이지 유형별 가정통신문 수집 전략 이름을 정의합니다. */

export type NoticeProvider =
  | "sen-preview"
  | "goehs-board"
  | "gne-board"
  | "gyo6-board"
  | "jje-board"
  | "gwe-board"
  | "use-board"
  | "cbe-board"
  | "jbe-board"
  | "gen-xhomenews"
  | "busan-school";

export function detectNoticeProvider(homepageUrl: string, homepageHtml: string): NoticeProvider | null {
  const hostname = new URL(homepageUrl).hostname;

  if (hostname.endsWith("busanedu.net") || homepageHtml.includes("school.busanedu.net")) {
    return "busan-school";
  }

  if (hostname.endsWith("gwe.ms.kr") || hostname.endsWith("icems.kr") || homepageHtml.includes("boardCnts/list.do?boardID=")) {
    return "gwe-board";
  }

  if (hostname.endsWith("gne.go.kr")) {
    return "gne-board";
  }

  if (hostname.endsWith("school.gyo6.net")) {
    return "gyo6-board";
  }

  if (hostname.endsWith("school.jje.go.kr")) {
    return "jje-board";
  }

  if (hostname.endsWith("school.use.go.kr")) {
    return "use-board";
  }

  if (hostname.endsWith("school.cbe.go.kr")) {
    return "cbe-board";
  }

  if (hostname.endsWith("school.jbedu.kr")) {
    return "jbe-board";
  }

  if (hostname.endsWith("gen.ms.kr") || homepageHtml.includes("xhomenews/board.php?tbnum=")) {
    return "gen-xhomenews";
  }


  if (hostname.endsWith("goehs.kr") || homepageHtml.includes("/na/ntt/selectNttList.do")) {
    return "goehs-board";
  }

  if (hostname.endsWith("sen.ms.kr") || homepageHtml.includes("fnBoardPage_")) {
    return "sen-preview";
  }

  return null;
}
