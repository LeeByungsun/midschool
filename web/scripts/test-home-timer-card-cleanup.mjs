/** 홈 타이머 카드 정리 후 레거시 경로 재사용을 막는 회귀 테스트입니다. */

import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
import { dirname, extname, resolve } from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const scriptsDir = dirname(fileURLToPath(import.meta.url));
const webRoot = resolve(scriptsDir, "..");
const dashboardPath = resolve(webRoot, "components/home-dashboard.tsx");
const canonicalCardPath = resolve(webRoot, "components/home-timer-card.tsx");
const searchableExtensions = new Set([".ts", ".tsx", ".js", ".jsx", ".mjs", ".cjs"]);
const legacySpecifierPatterns = [
  "@/components/home-study-timer-card",
  "./home-study-timer-card",
  "../components/home-study-timer-card",
];

async function collectSourceFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = await Promise.all(
    entries.map(async (entry) => {
      const fullPath = resolve(directory, entry.name);

      if (entry.name === "node_modules" || entry.name === ".next") {
        return [];
      }

      if (entry.isDirectory()) {
        return collectSourceFiles(fullPath);
      }

      return searchableExtensions.has(extname(entry.name)) ? [fullPath] : [];
    }),
  );

  return files.flat();
}

test("dashboard renders the canonical home timer card", async () => {
  const [dashboardSource, canonicalCardSource] = await Promise.all([
    readFile(dashboardPath, "utf8"),
    readFile(canonicalCardPath, "utf8"),
  ]);

  assert.match(
    dashboardSource,
    /import\s+\{\s*HomeTimerCard\s*\}\s+from\s+["']@\/components\/home-timer-card["'];?/,
  );
  assert.match(dashboardSource, /<HomeTimerCard\s*\/>/);
  assert.match(canonicalCardSource, /export function HomeTimerCard\s*\(/);
});

test("no source imports the legacy duplicate timer card path", async () => {
  const sourceFiles = await collectSourceFiles(webRoot);
  const legacyImporters = [];

  for (const filePath of sourceFiles) {
    if (filePath.endsWith("components/home-study-timer-card.tsx")) {
      continue;
    }

    if (filePath.endsWith("scripts/test-home-timer-card-cleanup.mjs")) {
      continue;
    }

    const source = await readFile(filePath, "utf8");

    if (legacySpecifierPatterns.some((pattern) => source.includes(pattern))) {
      legacyImporters.push(filePath.slice(webRoot.length + 1));
    }
  }

  assert.deepEqual(legacyImporters, []);
});
