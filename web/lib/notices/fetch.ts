import "server-only";

import { execFile } from "node:child_process";
import { promisify } from "node:util";

import { detectNoticeProvider } from "@/lib/notices/provider";
import type { NoticeSummary } from "@/lib/notices/types";
import {
  buildLegacyHomepageAliases,
  hasExplicitProtocol,
  resolveRedirectTargetUrl,
  toAbsoluteUrl,
} from "@/lib/notices/url";

type NoticeCacheEntry = {
  savedAt: number;
  items: NoticeSummary[];
};

const NOTICE_CACHE_TTL_MS = 30 * 60 * 1000;
const NOTICE_FETCH_TIMEOUT_MS = 5_000;
const noticeCache = new Map<string, NoticeCacheEntry>();
const execFileAsync = promisify(execFile);

type HtmlDocument = {
  html: string;
  url: string;
};

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
  if (url.protocol === "http:" && (url.hostname.endsWith("sen.ms.kr") || url.hostname.endsWith("gwe.ms.kr"))) {
    url.protocol = "https:";
  }
  url.hash = "";

  return url.toString();
}

function buildHomepageCandidates(value: string) {
  const trimmed = value.trim();

  if (!trimmed) {
    return [];
  }

  const baseCandidates = hasExplicitProtocol(trimmed)
    ? [normalizeHomepageUrl(trimmed)]
    : [
        normalizeHomepageUrl(`https://${trimmed}`),
        normalizeHomepageUrl(`http://${trimmed}`),
      ];
  const legacyAliases = baseCandidates.flatMap(buildLegacyHomepageAliases).map(
    normalizeHomepageUrl,
  );

  return Array.from(new Set([...baseCandidates, ...legacyAliases]));
}

function extractClientRedirect(html: string) {
  const normalizedHtml = html.replace(/\s+/g, " ");
  const match = normalizedHtml.match(
    /<script>\s*(?:document\.)?location\.href\s*=\s*["']([^"']+)["']\s*;?\s*<\/script>/i,
  );

  if (match?.[1]) {
    return match[1];
  }

  if (normalizedHtml.includes("홈페이지 접속중입니다.")) {
    return "";
  }

  return "";
}

function extractMetaRefreshRedirect(html: string) {
  const normalizedHtml = html.replace(/\s+/g, " ");
  const match = normalizedHtml.match(
    /<meta[^>]+http-equiv=["']refresh["'][^>]+content=["'][^"']*url=([^"';>]+)["']/i,
  );

  return match?.[1]?.trim() ?? "";
}

async function fetchHtml(url: string, depth = 0): Promise<HtmlDocument> {
  let html = "";
  let responseUrl = url;

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), NOTICE_FETCH_TIMEOUT_MS);
    const response = await fetch(url, {
      method: "GET",
      headers: {
        Accept: "text/html,application/xhtml+xml",
        "User-Agent": "Mozilla/5.0 (compatible; SchoolHelperBot/1.0)",
      },
      cache: "no-store",
      signal: controller.signal,
    });
    clearTimeout(timeoutId);
    responseUrl = response.url || url;

    if (!response.ok) {
      throw new Error(`학교 홈페이지 응답이 실패했어요. (${response.status})`);
    }

    html = await response.text();
  } catch (error) {
    try {
      const result = await execFileAsync("curl", [
        "-L",
        "--silent",
        "--show-error",
        "--max-time",
        String(Math.ceil(NOTICE_FETCH_TIMEOUT_MS / 1000)),
        "--user-agent",
        "Mozilla/5.0 (compatible; SchoolHelperBot/1.0)",
        url,
      ]);
      html = result.stdout;
    } catch {
      throw error instanceof Error
        ? new Error(`학교 홈페이지 응답 대기 시간이 초과되었어요: ${url}`)
        : error;
    }
  }
  const redirectPath = extractClientRedirect(html) || extractMetaRefreshRedirect(html);

  if (redirectPath && depth < 3) {
    return fetchHtml(
      resolveRedirectTargetUrl({
        requestedUrl: url,
        responseUrl,
        redirectPath,
      }),
      depth + 1,
    );
  }

  return {
    html,
    url: responseUrl,
  };
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

function parseBusanNoticeBoardUrl(homepageUrl: string, html: string) {
  const homepageOrigin = new URL(homepageUrl).origin;
  const matches = Array.from(
    html.matchAll(
      /<a[^>]+href=['"]([^'"]*selectNttList\.do\?[^'"]*bbsId=[^'"]+)['"][^>]*>([\s\S]*?)<\/a>/gi,
    ),
  );

  const candidates: Array<{ href: string; titleText: string; score: number }> = [];

  for (const match of matches) {
    const href = match[1];
    const titleText = stripTags(match[2]);

    if (!(titleText.includes("가정통신문") || titleText.includes("가정통신"))) {
      continue;
    }

    const absoluteUrl = toAbsoluteUrl(homepageUrl, href);
    const isRelative = !/^https?:\/\//i.test(href);
    const isSameOrigin = new URL(absoluteUrl).origin === homepageOrigin;
    const score = (isRelative ? 4 : 0) + (isSameOrigin ? 2 : 0) + (titleText.includes("더보기") ? 1 : 0);

    candidates.push({
      href: absoluteUrl,
      titleText,
      score,
    });
  }

  candidates.sort((a, b) => b.score - a.score);

  return candidates[0]?.href ?? "";
}

function parseGweNoticeBoardUrl(homepageUrl: string, html: string) {
  const matches = Array.from(
    html.matchAll(
      /<a[^>]+href=['"]([^'"]*boardCnts\/list\.do\?[^'"]*boardID=[^'"]+)['"][^>]*>([\s\S]*?)<\/a>/gi,
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

function parseBusanNoticeList(boardUrl: string, html: string, limit: number) {
  const rows = Array.from(html.matchAll(/<tr[\s\S]*?>([\s\S]*?)<\/tr>/gi));
  const items: NoticeSummary[] = [];

  for (const row of rows) {
    const rowHtml = row[1];
    const titleMatch = rowHtml.match(
      /<a[^>]+href=['"]([^'"]*selectNttInfo\.do\?[^'"]*nttSn=[^'"]+)['"][^>]*>([\s\S]*?)<\/a>/i,
    );

    if (!titleMatch) {
      continue;
    }

    const href = titleMatch[1];
    const title = stripTags(titleMatch[2]);
    const cells = Array.from(rowHtml.matchAll(/<td[^>]*>([\s\S]*?)<\/td>/gi)).map(
      (cell) => stripTags(cell[1]),
    );
    const detailUrl = toAbsoluteUrl(boardUrl, href);
    const detailUrlObject = new URL(detailUrl);
    const noticeId = detailUrlObject.searchParams.get("nttSn")?.trim() ?? "";
    const author = cells[2] ?? "";
    const date =
      cells.find((cell) => /^[0-9]{4}\.[0-9]{2}\.[0-9]{2}$/.test(cell)) ?? "";

    if (!title || !noticeId) {
      continue;
    }

    items.push({
      id: noticeId,
      title,
      date,
      author,
      url: detailUrl,
      sourceUrl: boardUrl,
    });

    if (items.length >= limit) {
      break;
    }
  }

  return items;
}

function parseGweNoticeList(boardUrl: string, html: string, limit: number) {
  const boardUrlObject = new URL(boardUrl);
  const boardId = boardUrlObject.searchParams.get("boardID") ?? "";
  const menuId = boardUrlObject.searchParams.get("m") ?? "";
  const siteId = boardUrlObject.searchParams.get("s") ?? "";
  const rows = Array.from(html.matchAll(/<tr[\s\S]*?>([\s\S]*?)<\/tr>/gi));
  const items: NoticeSummary[] = [];

  for (const row of rows) {
    const rowHtml = row[1];
    const onclickAttrMatch = rowHtml.match(/onclick=(["'])([\s\S]*?)\1/i);
    const titleMatch = rowHtml.match(/<a[^>]*>[\s\S]*?<\/a>/i);

    if (!onclickAttrMatch || !titleMatch) {
      continue;
    }

    const goViewArgsMatch = onclickAttrMatch[2].match(/goView\((.*)\)/i);

    if (!goViewArgsMatch) {
      continue;
    }

    const args = Array.from(goViewArgsMatch[1].matchAll(/'([^']*)'/g)).map((match) =>
      match[1].trim(),
    );
    const [, viewBoardId = boardId, noticeId = "", lev = "0", , statusYnRaw = "W", pageRaw = "1"] = args;
    const statusYn = statusYnRaw || "W";
    const page = pageRaw || "1";
    const title = stripTags(titleMatch[0]);
    const cells = Array.from(rowHtml.matchAll(/<td[^>]*>([\s\S]*?)<\/td>/gi)).map((cell) =>
      stripTags(cell[1]),
    );
    const author = cells[2] ?? "";
    const date = cells[4] ?? "";

    if (!noticeId || !title) {
      continue;
    }

    const detailUrl = new URL("/boardCnts/view.do", boardUrlObject.origin);
    detailUrl.searchParams.set("boardID", viewBoardId || boardId);
    detailUrl.searchParams.set("boardSeq", noticeId);
    detailUrl.searchParams.set("lev", lev || "0");
    detailUrl.searchParams.set("searchType", "S");
    detailUrl.searchParams.set("searchType2", "");
    detailUrl.searchParams.set("statusYN", statusYn);
    detailUrl.searchParams.set("page", page);
    detailUrl.searchParams.set("s", siteId);
    detailUrl.searchParams.set("m", menuId);
    detailUrl.searchParams.set("rsvtListYN", "N");
    detailUrl.searchParams.set("opType", "N");
    detailUrl.searchParams.set("sdate", "");
    detailUrl.searchParams.set("edate", "");

    items.push({
      id: noticeId,
      title,
      date,
      author,
      url: detailUrl.toString(),
      sourceUrl: boardUrl,
    });

    if (items.length >= limit) {
      break;
    }
  }

  return items;
}

function parseGenNoticeBoardUrl(homepageUrl: string, html: string) {
  const matches = Array.from(
    html.matchAll(/<a[^>]+href=["']([^"']*xhomenews\/board\.php\?tbnum=[^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi),
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

function parseGenNoticeList(boardUrl: string, html: string, limit: number) {
  const items: NoticeSummary[] = [];

  const rowPatterns = [
    /<li>[\s\S]*?<p class="left new_subject"><a href="([^"]+)">([\s\S]*?)<\/a><\/p>[\s\S]*?<p class="new_date">([^<]*)<\/p>[\s\S]*?<p class="new_writer">[\s\S]*?>([^<]*)<\/a><\/p>[\s\S]*?<\/li>/gi,
    /<ul class="news_index">[\s\S]*?<li class="m6"><a href="([^"]+)">([\s\S]*?)<\/a><\/li>[\s\S]*?<li class="m8">([^<]*)<\/li>[\s\S]*?<li class="m9">[\s\S]*?>([^<]*)<\/a><\/li>[\s\S]*?<\/ul>/gi,
  ];

  for (const pattern of rowPatterns) {
    for (const match of html.matchAll(pattern)) {
      const href = match[1];
      const title = stripTags(match[2]);
      const date = stripTags(match[3]);
      const author = stripTags(match[4]);
      const detailUrl = toAbsoluteUrl(boardUrl, decodeHtml(href));
      const noticeId = new URL(detailUrl).searchParams.get("number")?.trim() ?? "";

      if (!noticeId || !title) {
        continue;
      }

      items.push({
        id: noticeId,
        title,
        date,
        author,
        url: detailUrl,
        sourceUrl: boardUrl,
      });

      if (items.length >= limit) {
        return items;
      }
    }

    if (items.length > 0) {
      return items;
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

function isDirectNoticeBoardUrl(url: string) {
  const { pathname, searchParams } = new URL(url);

  return (
    pathname.includes("selectNttList.do") &&
    (searchParams.has("bbsId") || searchParams.has("boardID"))
  );
}

async function fetchNoticeItemsForHomepage(homepageUrl: string, limit: number) {
  const cached = noticeCache.get(homepageUrl);

  if (cached && Date.now() - cached.savedAt < NOTICE_CACHE_TTL_MS) {
    return cached.items.slice(0, limit);
  }

  const homepageDocument = await fetchHtml(homepageUrl);
  const homepageHtml = homepageDocument.html;
  const provider = detectNoticeProvider(homepageDocument.url, homepageHtml);

  if (!provider) {
    throw new Error("학교 홈페이지에서 가정통신문 구조를 찾지 못했어요.");
  }

  let items: NoticeSummary[] = [];

  if (
    provider === "goehs-board" ||
    provider === "gne-board" ||
    provider === "gyo6-board" ||
    provider === "jje-board" ||
    provider === "busan-school"
  ) {
    const boardUrl = isDirectNoticeBoardUrl(homepageUrl)
      ? homepageUrl
      : provider === "busan-school"
        ? parseBusanNoticeBoardUrl(homepageDocument.url, homepageHtml)
        : parseGoehsNoticeBoardUrl(homepageDocument.url, homepageHtml);

    if (!boardUrl) {
      throw new Error("가정통신문 게시판 링크를 찾지 못했어요.");
    }

    const boardDocument =
      boardUrl === homepageUrl ? homepageDocument : await fetchHtml(boardUrl);
    items =
      provider === "busan-school"
          ? parseBusanNoticeList(boardDocument.url, boardDocument.html, limit)
        : provider === "gne-board"
          ? parseBusanNoticeList(boardDocument.url, boardDocument.html, limit)
          : provider === "gyo6-board"
            ? parseGoehsNoticeList(boardDocument.url, boardDocument.html, limit)
            : provider === "jje-board"
              ? parseGoehsNoticeList(boardDocument.url, boardDocument.html, limit)
              : parseGoehsNoticeList(boardDocument.url, boardDocument.html, limit);
  } else if (provider === "gwe-board") {
    const boardUrl = isDirectNoticeBoardUrl(homepageUrl)
      ? homepageUrl
      : parseGweNoticeBoardUrl(homepageDocument.url, homepageHtml);

    if (!boardUrl) {
      throw new Error("가정통신문 게시판 링크를 찾지 못했어요.");
    }

    const boardDocument =
      boardUrl === homepageUrl ? homepageDocument : await fetchHtml(boardUrl);
    items = parseGweNoticeList(boardDocument.url, boardDocument.html, limit);
  } else if (provider === "sen-preview") {
    items = parseSenNoticePreview(homepageDocument.url, homepageHtml, limit);
  } else if (provider === "gen-xhomenews") {
    const boardUrl = homepageUrl.includes('/xhomenews/board.php')
      ? homepageUrl
      : parseGenNoticeBoardUrl(homepageDocument.url, homepageHtml);

    if (!boardUrl) {
      throw new Error("가정통신문 게시판 링크를 찾지 못했어요.");
    }

    const boardDocument =
      boardUrl === homepageUrl ? homepageDocument : await fetchHtml(boardUrl);
    items = parseGenNoticeList(boardDocument.url, boardDocument.html, limit);
  }

  if (items.length > 0) {
    noticeCache.set(homepageUrl, {
      savedAt: Date.now(),
      items,
    });
  } else {
    noticeCache.delete(homepageUrl);
  }

  return items;
}

export async function fetchSchoolHomepageNotices(params: {
  homepageUrl: string;
  limit?: number;
}) {
  const limit = params.limit ?? 5;
  const homepageCandidates = buildHomepageCandidates(params.homepageUrl);

  if (homepageCandidates.length === 0) {
    throw new Error("학교 홈페이지 주소가 아직 저장되지 않았어요.");
  }

  let lastError: unknown;

  for (const homepageUrl of homepageCandidates) {
    try {
      return await fetchNoticeItemsForHomepage(homepageUrl, limit);
    } catch (error) {
      lastError = error;
    }
  }

  throw lastError;
}
