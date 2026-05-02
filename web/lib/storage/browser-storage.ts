/** 브라우저 저장소 사용 가능 여부를 감싼 안전한 접근 래퍼입니다. */

const canUseBrowserStorage = () =>
  typeof window !== "undefined" && typeof window.localStorage !== "undefined";

export const browserStorage = {
  getItem(key: string) {
    if (!canUseBrowserStorage()) {
      return null;
    }

    try {
      return window.localStorage.getItem(key);
    } catch {
      return null;
    }
  },
  setItem(key: string, value: string) {
    if (!canUseBrowserStorage()) {
      return false;
    }

    try {
      window.localStorage.setItem(key, value);
      return true;
    } catch {
      return false;
    }
  },
  removeItem(key: string) {
    if (!canUseBrowserStorage()) {
      return false;
    }

    try {
      window.localStorage.removeItem(key);
      return true;
    } catch {
      return false;
    }
  },
  isAvailable: canUseBrowserStorage,
};
