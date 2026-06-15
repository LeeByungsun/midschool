/** 로컬 캐시 우선 표시 후 네트워크 재검증 계약을 검증합니다. */

import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import ts from "typescript";

const scriptsDir = dirname(fileURLToPath(import.meta.url));
const webRoot = resolve(scriptsDir, "..");

async function loadSchoolApiModule() {
  const sourceUrl = new URL("../lib/school-api.ts", import.meta.url);
  const source = await readFile(sourceUrl, "utf8");
  const cacheMockSource = `
    export function readCache(key) {
      globalThis.__schoolApiCacheReads = [...(globalThis.__schoolApiCacheReads ?? []), key];
      return globalThis.__schoolApiCachedValue ?? null;
    }
    export function writeCache(key, items, options) {
      globalThis.__schoolApiCacheWrites = [...(globalThis.__schoolApiCacheWrites ?? []), { key, items, options }];
      return true;
    }
  `;
  const cacheMockUrl = `data:text/javascript;base64,${Buffer.from(cacheMockSource).toString("base64")}#cache-${Date.now()}-${Math.random()}`;
  const rewritten = source.replaceAll(
    '"@/lib/storage/cache"',
    JSON.stringify(cacheMockUrl),
  );
  const transpiled = ts.transpileModule(rewritten, {
    compilerOptions: {
      module: ts.ModuleKind.ESNext,
      target: ts.ScriptTarget.ES2022,
    },
    fileName: sourceUrl.href,
  });
  const moduleUrl = `data:text/javascript;base64,${Buffer.from(
    transpiled.outputText,
  ).toString("base64")}#school-api-${Date.now()}-${Math.random()}`;

  return import(moduleUrl);
}

async function withMockFetch(handler, run) {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = handler;

  try {
    return await run();
  } finally {
    if (originalFetch === undefined) {
      delete globalThis.fetch;
    } else {
      globalThis.fetch = originalFetch;
    }
  }
}

function resetCacheGlobals(cachedValue = null) {
  globalThis.__schoolApiCachedValue = cachedValue;
  globalThis.__schoolApiCacheReads = [];
  globalThis.__schoolApiCacheWrites = [];
}

test("cached timetable can be read synchronously for local-first rendering", async () => {
  const { readCachedTimetable } = await loadSchoolApiModule();
  const savedAt = Date.now() - 1_000;
  const cachedItem = {
    date: "20260615",
    grade: "2",
    classroom: "3",
    period: "1",
    subject: "수학",
  };

  resetCacheGlobals({
    items: [cachedItem],
    savedAt,
    expiresAt: Date.now() + 60_000,
    isExpired: false,
  });

  const result = readCachedTimetable({
    officeCode: "J10",
    schoolCode: "1234567",
    schoolKind: "중학교",
    grade: "2",
    classroom: "3",
    date: "20260615",
  });

  assert.deepEqual(result?.items, [cachedItem]);
  assert.equal(result?.cacheStatus, "cache");
  assert.equal(result?.cachedAt, savedAt);
  assert.equal(
    globalThis.__schoolApiCacheReads[0],
    "timetable:J10:1234567:중학교:2:3:20260615",
  );
});

test("fresh cache is returned without network unless revalidation is requested", async () => {
  const { fetchTimetable } = await loadSchoolApiModule();
  const cachedItem = {
    date: "20260615",
    grade: "2",
    classroom: "3",
    period: "1",
    subject: "수학",
  };
  const networkItem = {
    ...cachedItem,
    subject: "영어",
  };
  const params = {
    officeCode: "J10",
    schoolCode: "1234567",
    schoolKind: "중학교",
    grade: "2",
    classroom: "3",
    date: "20260615",
  };
  const cached = {
    items: [cachedItem],
    savedAt: 100,
    expiresAt: Date.now() + 60_000,
    isExpired: false,
  };
  let fetchCount = 0;

  resetCacheGlobals(cached);

  await withMockFetch(
    async () => {
      fetchCount += 1;
      return {
        ok: true,
        json: async () => ({ items: [networkItem] }),
      };
    },
    async () => {
      const cachedResult = await fetchTimetable(params);

      assert.deepEqual(cachedResult.items, [cachedItem]);
      assert.equal(cachedResult.cacheStatus, "cache");
      assert.equal(fetchCount, 0);

      const networkResult = await fetchTimetable(params, { skipFreshCache: true });

      assert.deepEqual(networkResult.items, [networkItem]);
      assert.equal(networkResult.cacheStatus, "network");
      assert.equal(fetchCount, 1);
      assert.equal(globalThis.__schoolApiCacheWrites.length, 1);
    },
  );
});

test("network result only needs UI update when list items differ", async () => {
  const { shouldUpdateListFromNetwork } = await loadSchoolApiModule();
  const cachedItems = [{ date: "20260615", title: "체험학습" }];

  assert.equal(shouldUpdateListFromNetwork(null, cachedItems), true);
  assert.equal(shouldUpdateListFromNetwork(cachedItems, cachedItems), false);
  assert.equal(
    shouldUpdateListFromNetwork(cachedItems, [
      { date: "20260615", title: "기말고사" },
    ]),
    true,
  );
});

test("timetable, meals, home, and schedule screens use local-first revalidation", async () => {
  const [timetableSource, mealSource, scheduleSource, homeSource] = await Promise.all([
    readFile(resolve(webRoot, "components/timetable-browser.tsx"), "utf8"),
    readFile(resolve(webRoot, "components/meal-browser.tsx"), "utf8"),
    readFile(resolve(webRoot, "components/schedule-browser.tsx"), "utf8"),
    readFile(resolve(webRoot, "components/home-dashboard.tsx"), "utf8"),
  ]);

  assert.match(timetableSource, /readCachedTimetable/);
  assert.match(timetableSource, /fetchTimetable\(params, \{ skipFreshCache: true \}\)/);
  assert.match(timetableSource, /shouldUpdateListFromNetwork\(cached\.items, result\.items\)/);

  assert.match(mealSource, /readCachedMeals/);
  assert.match(mealSource, /fetchMeals\(params, \{ skipFreshCache: true \}\)/);
  assert.match(mealSource, /shouldUpdateListFromNetwork\(cached\.items, result\.items\)/);

  assert.match(scheduleSource, /readCachedSchedules/);
  assert.match(scheduleSource, /fetchSchedules\(params, \{ skipFreshCache: true \}\)/);
  assert.match(scheduleSource, /shouldUpdateListFromNetwork\(cached\.items, result\.items\)/);
  assert.match(scheduleSource, /formatCacheStatusMessage/);

  assert.match(homeSource, /readCachedTimetable/);
  assert.match(homeSource, /readCachedMeals/);
  assert.match(homeSource, /readCachedSchedules/);
  assert.match(homeSource, /skipFreshCache: true/);
  assert.match(homeSource, /isTimetableLoading/);
  assert.match(homeSource, /isMealLoading/);
  assert.match(homeSource, /isScheduleLoading/);
});
