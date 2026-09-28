/** 경기도·대구 등 공개 NTT 게시판의 공통 목록 파서입니다. */
import type { NoticeSummary } from "./types";

function toAbsoluteUrl(baseUrl: string, path: string) { return new URL(path, baseUrl).toString(); }

function normalizeWhitespace(value: string) {
  return value.replace(/\s+/g, " ").trim();
}

function decodeHtml(value: string) {
  return value
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'");
}

function stripTags(value: string) {
  return normalizeWhitespace(decodeHtml(value.replace(/<[^>]+>/g, " ")));
}


export function parseGoehsNoticeBoardUrl(homepageUrl: string, html: string) {
  const matches = Array.from(
    html.matchAll(
      /<a[^>]+href=['"]([^'"]*selectNttList\.do\?[^'"]*bbsId=[^'"]+)['"][^>]*>([\s\S]*?)<\/a>/gi,
    ),
  );

  for (const match of matches) {
    const href = match[1];
    const titleText = stripTags(match[2]);

    if (titleText.includes("가정통신문") || titleText.endsWith("교육통신")) {
      return toAbsoluteUrl(homepageUrl, decodeHtml(href));
    }
  }

  return "";
}

export function parseGoehsNoticeList(boardUrl: string, html: string, limit: number) {
  const boardUrlObject = new URL(boardUrl);
  const mi = boardUrlObject.searchParams.get("mi") ?? "";
  const bbsId = boardUrlObject.searchParams.get("bbsId") ?? "";
  const detailPath = boardUrlObject.pathname.replace("selectNttList.do", "selectNttInfo.do");
  const rows = Array.from(html.matchAll(/<tr[\s\S]*?>([\s\S]*?)<\/tr>/gi));
  const items: NoticeSummary[] = [];

  for (const row of rows) {
    const rowHtml = row[1];
    const titleMatch = rowHtml.match(
      /<a[^>]+data-id=['"]([^'"]+)['"][^>]*class=['"]nttInfoBtn['"][^>]*>([\s\S]*?)<\/a>/i,
    );

    if (!titleMatch) {
      continue;
    }

    const noticeId = titleMatch[1].trim();
    const title = stripTags(titleMatch[2]);
    const authorMatch = rowHtml.match(
      /작성자\s*<\/em>\s*(?:<!--[\s\S]*?-->\s*)?([^<]+)/i,
    );
    const dateMatch = rowHtml.match(/등록일\s*<\/em>\s*([0-9]{4}\.[0-9]{2}\.[0-9]{2})/i);

    if (!title) {
      continue;
    }

    const detailUrl = new URL(detailPath, boardUrlObject.origin);
    detailUrl.searchParams.set("mi", mi);
    detailUrl.searchParams.set("bbsId", bbsId);
    detailUrl.searchParams.set("nttSn", noticeId);

    items.push({
      id: noticeId,
      title,
      date: dateMatch?.[1]?.trim() ?? "",
      author: normalizeWhitespace(authorMatch?.[1] ?? ""),
      url: detailUrl.toString(),
      sourceUrl: boardUrl,
    });

    if (items.length >= limit) {
      break;
    }
  }

  return items;
}
