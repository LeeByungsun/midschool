import { browserStorage } from "@/lib/storage/browser-storage";

type CacheEnvelope<T> = {
  items: T;
  savedAt: number;
  expiresAt: number;
  fallbackExpiresAt: number;
};

type ReadCacheResult<T> = {
  items: T;
  savedAt: number;
  expiresAt: number;
  isExpired: boolean;
};

type CacheWriteOptions = {
  ttlMs: number;
  fallbackTtlMs?: number;
};

const CACHE_KEY_PREFIX = "midschool:web:cache:";

function buildStorageKey(key: string) {
  return `${CACHE_KEY_PREFIX}${key}`;
}

export function readCache<T>(key: string): ReadCacheResult<T> | null {
  const raw = browserStorage.getItem(buildStorageKey(key));

  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as CacheEnvelope<T>;
    const now = Date.now();

    if (!parsed || typeof parsed.savedAt !== "number" || typeof parsed.expiresAt !== "number") {
      return null;
    }

    if (typeof parsed.fallbackExpiresAt !== "number" || now > parsed.fallbackExpiresAt) {
      browserStorage.removeItem(buildStorageKey(key));
      return null;
    }

    return {
      items: parsed.items,
      savedAt: parsed.savedAt,
      expiresAt: parsed.expiresAt,
      isExpired: now > parsed.expiresAt,
    };
  } catch {
    return null;
  }
}

export function writeCache<T>(
  key: string,
  items: T,
  { ttlMs, fallbackTtlMs = ttlMs * 3 }: CacheWriteOptions,
) {
  const now = Date.now();

  return browserStorage.setItem(
    buildStorageKey(key),
    JSON.stringify({
      items,
      savedAt: now,
      expiresAt: now + ttlMs,
      fallbackExpiresAt: now + fallbackTtlMs,
    } satisfies CacheEnvelope<T>),
  );
}

export function clearCache(key: string) {
  return browserStorage.removeItem(buildStorageKey(key));
}
