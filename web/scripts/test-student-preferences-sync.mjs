/** 학생 설정 저장소와 홈/설정 동기화 계약을 검증하는 회귀 테스트입니다. */

import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import ts from "typescript";

const scriptsDir = dirname(fileURLToPath(import.meta.url));
const webRoot = resolve(scriptsDir, "..");

class MemoryStorage {
  #values = new Map();

  getItem(key) {
    return this.#values.has(key) ? this.#values.get(key) : null;
  }

  setItem(key, value) {
    this.#values.set(key, String(value));
    return true;
  }

  removeItem(key) {
    this.#values.delete(key);
    return true;
  }
}

class TestCustomEvent extends Event {
  constructor(type, init = {}) {
    super(type);
    this.detail = init.detail;
  }
}

async function loadPreferencesModule() {
  const sourceUrl = new URL("../lib/storage/preferences.ts", import.meta.url);
  const source = await readFile(sourceUrl, "utf8");
  const rewritten = source.replaceAll(
    '"@/lib/storage/browser-storage"',
    JSON.stringify(new URL("../lib/storage/browser-storage.ts", import.meta.url).href),
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
  ).toString("base64")}#${Date.now()}-${Math.random()}`;

  return import(moduleUrl);
}

async function withMockBrowser(run) {
  const originalWindow = globalThis.window;
  const originalCustomEvent = globalThis.CustomEvent;

  const eventTarget = new EventTarget();
  const window = {
    localStorage: new MemoryStorage(),
    addEventListener: (...args) => eventTarget.addEventListener(...args),
    removeEventListener: (...args) => eventTarget.removeEventListener(...args),
    dispatchEvent: (event) => eventTarget.dispatchEvent(event),
  };

  globalThis.window = window;
  globalThis.CustomEvent = TestCustomEvent;

  try {
    return await run(window);
  } finally {
    if (originalWindow === undefined) {
      delete globalThis.window;
    } else {
      globalThis.window = originalWindow;
    }

    if (originalCustomEvent === undefined) {
      delete globalThis.CustomEvent;
    } else {
      globalThis.CustomEvent = originalCustomEvent;
    }
  }
}

function normalized(value) {
  return {
    schoolName: value.schoolName.trim(),
    officeCode: value.officeCode.trim(),
    schoolCode: value.schoolCode.trim(),
    schoolKind: value.schoolKind.trim(),
    homepage: value.homepage.trim(),
    grade: value.grade.trim(),
    classroom: value.classroom.trim(),
  };
}

test("saving student preferences normalizes input and emits one sync event", async () => {
  const {
    STUDENT_PREFERENCES_UPDATED_EVENT,
    clearStudentPreferences,
    formatStudentPreferences,
    isStudentPreferencesComplete,
    readStudentPreferences,
    saveStudentPreferences,
  } = await loadPreferencesModule();

  await withMockBrowser(async (window) => {
    const events = [];
    window.addEventListener(STUDENT_PREFERENCES_UPDATED_EVENT, (event) => {
      events.push(event.detail ?? null);
    });

    const wasSaved = saveStudentPreferences({
      schoolName: " 미사중학교 ",
      officeCode: "  J10 ",
      schoolCode: " 7679399 ",
      schoolKind: " 중학교 ",
      homepage: " https://misa-m.kr/ ",
      grade: " 2 ",
      classroom: " 03 ",
    });

    const preferences = readStudentPreferences();

    assert.equal(wasSaved, true);
    assert.deepEqual(preferences, normalized({
      schoolName: " 미사중학교 ",
      officeCode: "  J10 ",
      schoolCode: " 7679399 ",
      schoolKind: " 중학교 ",
      homepage: " https://misa-m.kr/ ",
      grade: " 2 ",
      classroom: " 03 ",
    }));
    assert.equal(isStudentPreferencesComplete(preferences), true);
    assert.equal(formatStudentPreferences(preferences), "미사중학교 · 2학년 03반");
    assert.equal(events.length, 1);
    assert.equal(events[0]?.schoolName, "미사중학교");
    assert.equal(events[0]?.officeCode, "J10");
    assert.equal(events[0]?.grade, "2");

    assert.equal(clearStudentPreferences(), true);
  });
});

test("clearing preferences removes saved value and emits no preference payload", async () => {
  const {
    STUDENT_PREFERENCES_UPDATED_EVENT,
    clearStudentPreferences,
    readStudentPreferences,
    saveStudentPreferences,
  } = await loadPreferencesModule();

  await withMockBrowser(async (window) => {
    const events = [];
    window.addEventListener(STUDENT_PREFERENCES_UPDATED_EVENT, (event) => {
      events.push("detail" in event ? event.detail : undefined);
    });

    saveStudentPreferences({
      schoolName: "서울중학교",
      officeCode: "J10",
      schoolCode: "1234567",
      schoolKind: "중학교",
      homepage: "https://seoul.ms.kr/",
      grade: "1",
      classroom: "1",
    });

    const wasRemoved = clearStudentPreferences();
    const preferences = readStudentPreferences();

    assert.equal(wasRemoved, true);
    assert.equal(preferences, null);
    assert.equal(events.length, 2);
    assert.equal(events[0]?.schoolName, "서울중학교");
    assert.equal(events[1], undefined);
  });
});

test("home and settings continue to share the same preference source contract", async () => {
  const dashboardSource = await readFile(
    resolve(webRoot, "components/home-dashboard.tsx"),
    "utf8",
  );
  const settingsSource = await readFile(
    resolve(webRoot, "components/settings-form.tsx"),
    "utf8",
  );

  assert.match(dashboardSource, /useStudentPreferences/);
  assert.match(settingsSource, /useStudentPreferences/);
  assert.match(dashboardSource, /const studentInfo = useStudentPreferences\(\);/);
  assert.match(settingsSource, /const savedPreferences = useStudentPreferences\(\);/);
});
