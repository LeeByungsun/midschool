import "server-only";

import type { NoticeSummary } from "@/lib/notices/types";

type NoticeProvider = "sen-preview" | "goehs-board";

type NoticeCacheEntry = {
  savedAt: number;
  items: NoticeSummary[];
};

const NOTICE_CACHE_TTL_MS = 30 * 60 * 1000;
const noticeCache = new Map<string, NoticeCacheEntry>();

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

function normalizeHomepageUrl(value: string) {
  const trimmed = value.trim();

  if (!trimmed) {
    return "";
  }

  const withProtocol = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  const url = new URL(withProtocol);
  url.hash = "";

  return url.toString();
}

async function fetchHtml(url: string) {
  const response = await fetch(url, {
    method: "GET",
    headers: {
      Accept: "text/html,application/xhtml+xml",
      "User-Agent": "Mozilla/5.0 (compatible; SchoolHelperBot/1.0)",
    },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`학교 홈페이지 응답이 실패했어요. (${response.status})`);
  }

  return response.text();
}

function toAbsoluteUrl(baseUrl: string, path: string) {
  return new URL(path, baseUrl).toString();
}

function parseGoehsNoticeBoardUrl(homepageUrl: string, html: string) {
  const matches = Array.from(
    html.matchAll(
      /<a[^>]+href=['"]([^'"]*selectNttList\.do\?[^'"]*bbsId=[^'"]+)['"][^>]*>([\s\S]*?)<\/a>/gi,
    ),
  );

  for (const match of matches) {
    const href = match[1];
    const titleText = stripTags(match[2]);

    if (titleText === "가정통신문") {
      return toAbsoluteUrl(homepageUrl, href);
    }
  }

  return "";
}

function parseGoehsNoticeList(boardUrl: string, html: string, limit: number) {
  const boardUrlObject = new URL(boardUrl);
  const mi = boardUrlObject.searchParams.get("mi") ?? "";
  const bbsId = boardUrlObject.searchParams.get("bbsId") ?? "";
  const detailPath = boardUrlObject.pathname.replace("selectNttList.do", "selectNttInfo.do");
  const rows = Array.from(html.matchAll(/<tr>([\s\S]*?)<\/tr>/gi));
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

function parseSenBoardMeta(homepageUrl: string, html: string) {
  const pattern =
    /<div class=['"]index_board_box['"][\s\S]*?<h3>\s*(가정통신문(?:\(학교\))?)\s*<\/h3>[\s\S]*?<ul class=['"]main_small_list['"]>([\s\S]*?)<\/ul>/gi;

  const match = pattern.exec(html);

  if (!match) {
    return null;
  }

  const listHtml = match[2];
  const firstCallMatch = listHtml.match(
    /fnBoardPage_(\d+)\('\s*([^']+)\s*',\s*'([^']+)',\s*'([^']+)'\)/i,
  );

  if (!firstCallMatch) {
    return null;
  }

  const widgetId = firstCallMatch[1].trim();
  const bbsId = firstCallMatch[2].trim();
  const menuNo = firstCallMatch[4].trim();
  const boardUrl = menuNo ? toAbsoluteUrl(homepageUrl, `/${menuNo}/subMenu.do`) : homepageUrl;

  return { widgetId, bbsId, boardUrl, listHtml };
}

function parseSenNoticePreview(homepageUrl: string, html: string, limit: number) {
  const meta = parseSenBoardMeta(homepageUrl, html);

  if (!meta) {
    return [];
  }

  const itemPattern = new RegExp(
    `fnBoardPage_${meta.widgetId}\\('([^']*)',\\s*'([^']+)',\\s*'([^']+)'\\)[\\s\\S]*?<div class=['"]ellipsis['"]>([\\s\\S]*?)<\\/div>[\\s\\S]*?<p class=['"]date['"][^>]*>([\\s\\S]*?)<\\/p>`,
    "gi",
  );

  const items: NoticeSummary[] = [];

  for (const match of meta.listHtml.matchAll(itemPattern)) {
    const noticeId = match[2].trim();
    const title = stripTags(match[4]);
    const date = stripTags(match[5]);

    if (!noticeId || !title) {
      continue;
    }

    items.push({
      id: noticeId,
      title,
      date,
      author: "",
      url: meta.boardUrl,
      sourceUrl: meta.boardUrl,
    });

    if (items.length >= limit) {
      break;
    }
  }

  return items;
}

function detectProvider(homepageUrl: string, homepageHtml: string): NoticeProvider | null {
  const hostname = new URL(homepageUrl).hostname;

  if (hostname.endsWith("goehs.kr") || homepageHtml.includes("/na/ntt/selectNttList.do")) {
    return "goehs-board";
  }

  if (hostname.endsWith("sen.ms.kr") || homepageHtml.includes("fnBoardPage_")) {
    return "sen-preview";
  }

  return null;
}

export async function fetchSchoolHomepageNotices(params: {
  homepageUrl: string;
  limit?: number;
}) {
  const homepageUrl = normalizeHomepageUrl(params.homepageUrl);
  const limit = params.limit ?? 5;

  if (!homepageUrl) {
    throw new Error("학교 홈페이지 주소가 아직 저장되지 않았어요.");
  }

  const cached = noticeCache.get(homepageUrl);

  if (cached && Date.now() - cached.savedAt < NOTICE_CACHE_TTL_MS) {
    return cached.items.slice(0, limit);
  }

  const homepageHtml = await fetchHtml(homepageUrl);
  const provider = detectProvider(homepageUrl, homepageHtml);

  if (!provider) {
    throw new Error("학교 홈페이지에서 가정통신문 구조를 찾지 못했어요.");
  }

  let items: NoticeSummary[] = [];

  if (provider === "goehs-board") {
    const boardUrl = parseGoehsNoticeBoardUrl(homepageUrl, homepageHtml);

    if (!boardUrl) {
      throw new Error("가정통신문 게시판 링크를 찾지 못했어요.");
    }

    const boardHtml = await fetchHtml(boardUrl);
    items = parseGoehsNoticeList(boardUrl, boardHtml, limit);
  } else if (provider === "sen-preview") {
    items = parseSenNoticePreview(homepageUrl, homepageHtml, limit);
  }

  noticeCache.set(homepageUrl, {
    savedAt: Date.now(),
    items,
  });

  return items;
}
