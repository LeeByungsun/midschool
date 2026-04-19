import "server-only";

import fs from "node:fs";
import path from "node:path";

const DEFAULT_NEIS_BASE_URL = "https://open.neis.go.kr/";
const DEFAULT_OFFICE_CODE = "J10";
const DEFAULT_SCHOOL_CODE = "7679399";

let cachedAndroidLocalProperties: Record<string, string> | null = null;

export class NeisClientError extends Error {
  status: number;

  constructor(message: string, status = 500) {
    super(message);
    this.name = "NeisClientError";
    this.status = status;
  }
}

function readAndroidLocalProperties() {
  if (cachedAndroidLocalProperties) {
    return cachedAndroidLocalProperties;
  }

  const localPropertiesPath = path.resolve(
    process.cwd(),
    "../android/local.properties",
  );

  if (!fs.existsSync(localPropertiesPath)) {
    cachedAndroidLocalProperties = {};
    return cachedAndroidLocalProperties;
  }

  const fileContents = fs.readFileSync(localPropertiesPath, "utf8");
  const parsed = Object.fromEntries(
    fileContents
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith("#") && line.includes("="))
      .map((line) => {
        const [key, ...valueParts] = line.split("=");
        return [key.trim(), valueParts.join("=").trim()];
      }),
  );

  cachedAndroidLocalProperties = parsed;
  return cachedAndroidLocalProperties;
}

export function getNeisConfig() {
  const androidLocalProperties = readAndroidLocalProperties();

  return {
    baseUrl: process.env.NEIS_BASE_URL ?? DEFAULT_NEIS_BASE_URL,
    apiKey:
      process.env.NEIS_API_KEY ??
      androidLocalProperties.NEIS_API_KEY ??
      "",
    officeCode: process.env.NEIS_OFFICE_CODE ?? DEFAULT_OFFICE_CODE,
    schoolCode: process.env.NEIS_SCHOOL_CODE ?? DEFAULT_SCHOOL_CODE,
  };
}

export async function fetchNeisJson<T>(
  endpoint: string,
  params: Record<string, string | undefined>,
) {
  const config = getNeisConfig();

  if (!config.apiKey) {
    throw new NeisClientError(
      "NEIS_API_KEY가 설정되지 않았어요. web/.env.local 또는 android/local.properties를 먼저 준비해 주세요.",
      500,
    );
  }

  const searchParams = new URLSearchParams({
    KEY: config.apiKey,
    Type: "json",
    pIndex: "1",
    pSize: "100",
    ATPT_OFCDC_SC_CODE: config.officeCode,
    SD_SCHUL_CODE: config.schoolCode,
  });

  for (const [key, value] of Object.entries(params)) {
    if (value) {
      searchParams.set(key, value);
    }
  }

  const url = new URL(endpoint, config.baseUrl);
  url.search = searchParams.toString();

  const response = await fetch(url.toString(), {
    method: "GET",
    headers: {
      Accept: "application/json",
    },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new NeisClientError(
      `NEIS 응답이 실패했어요. (${response.status})`,
      response.status,
    );
  }

  return (await response.json()) as T;
}
