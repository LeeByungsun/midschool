/** 주간 급식 상세 흐름이 다시 일간 흐름으로 되돌아가지 않도록 지키는 회귀 테스트입니다. */

import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const scriptsDir = dirname(fileURLToPath(import.meta.url));
const webRoot = resolve(scriptsDir, "..");
const mealBrowserPath = resolve(webRoot, "components/meal-browser.tsx");
const mealsPagePath = resolve(webRoot, "app/meals/page.tsx");
const mealsRoutePath = resolve(webRoot, "app/api/meals/route.ts");
const homeDashboardPath = resolve(webRoot, "components/home-dashboard.tsx");

async function readSources() {
  const [mealBrowserSource, mealsPageSource, mealsRouteSource, homeDashboardSource] =
    await Promise.all([
      readFile(mealBrowserPath, "utf8"),
      readFile(mealsPagePath, "utf8"),
      readFile(mealsRoutePath, "utf8"),
      readFile(homeDashboardPath, "utf8"),
    ]);

  return {
    mealBrowserSource,
    mealsPageSource,
    mealsRouteSource,
    homeDashboardSource,
  };
}

test("meal browser keeps the weekly loading and navigation contract", async () => {
  const { mealBrowserSource } = await readSources();

  assert.match(mealBrowserSource, /getWeekDates\(selectedDate\)/);
  assert.match(mealBrowserSource, /date:\s*weekStartKey/);
  assert.match(mealBrowserSource, /endDate:\s*weekEndKey/);
  assert.match(mealBrowserSource, /moveWeek\(-7\)/);
  assert.match(mealBrowserSource, /jumpToCurrentWeek/);
  assert.match(mealBrowserSource, /moveWeek\(7\)/);
  assert.match(mealBrowserSource, /weekMealGroups/);
  assert.match(mealBrowserSource, /선택한 주의 급식을 불러오는 중/);
});

test("meal nutrition and origin details stay hidden until their buttons are used", async () => {
  const { mealBrowserSource } = await readSources();

  assert.match(
    mealBrowserSource,
    /const \[isNutritionExpanded, setIsNutritionExpanded\] = useState\(false\)/,
  );
  assert.match(
    mealBrowserSource,
    /const \[isOriginExpanded, setIsOriginExpanded\] = useState\(false\)/,
  );
  assert.match(mealBrowserSource, /aria-expanded=\{isNutritionExpanded\}/);
  assert.match(mealBrowserSource, /aria-expanded=\{isOriginExpanded\}/);
  assert.match(mealBrowserSource, /aria-controls=\{nutritionPanelId\}/);
  assert.match(mealBrowserSource, /aria-controls=\{originPanelId\}/);
  assert.match(
    mealBrowserSource,
    /onClick=\{\(\) => setIsNutritionExpanded\(\(prev\) => !prev\)\}/,
  );
  assert.match(
    mealBrowserSource,
    /onClick=\{\(\) => setIsOriginExpanded\(\(prev\) => !prev\)\}/,
  );
  assert.match(mealBrowserSource, /hidden=\{!isNutritionExpanded\}/);
  assert.match(mealBrowserSource, /hidden=\{!isOriginExpanded\}/);
  assert.match(mealBrowserSource, /isNutritionExpanded \? "숨기기" : "보기"/);
  assert.match(mealBrowserSource, /isOriginExpanded \? "숨기기" : "보기"/);
});

test("weekly meals page and API stay aligned with the weekly detail flow", async () => {
  const { mealsPageSource, mealsRouteSource, homeDashboardSource } = await readSources();

  assert.match(mealsPageSource, /title="주간 급식"/);
  assert.match(mealsPageSource, /이번 주 급식 메뉴/);
  assert.match(homeDashboardSource, />\s*일주일 보기\s*</);
  assert.match(mealsRouteSource, /get\("endDate"\)/);
  assert.match(mealsRouteSource, /MLSV_FROM_YMD/);
  assert.match(mealsRouteSource, /MLSV_TO_YMD/);
});
