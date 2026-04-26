export function toAbsoluteUrl(baseUrl: string, path: string) {
  return new URL(path, baseUrl).toString();
}

export function hasExplicitProtocol(value: string) {
  return /^https?:\/\//i.test(value.trim());
}

export function resolveRedirectTargetUrl(params: {
  requestedUrl: string;
  responseUrl?: string;
  redirectPath: string;
}) {
  return toAbsoluteUrl(
    params.responseUrl?.trim() || params.requestedUrl,
    params.redirectPath,
  );
}

export function buildLegacyHomepageAliases(value: string) {
  const trimmed = value.trim();

  if (!trimmed) {
    return [];
  }

  const parsed = new URL(hasExplicitProtocol(trimmed) ? trimmed : `https://${trimmed}`);
  const aliases: string[] = [];

  if (parsed.hostname.endsWith("jje.ms.kr")) {
    const schoolId = parsed.hostname.split(".")[0]?.trim();

    if (schoolId) {
      const aliasUrl = new URL("https://school.jje.go.kr");
      const pathname =
        parsed.pathname === "/"
          ? `/${schoolId}/`
          : parsed.pathname.startsWith(`/${schoolId}/`)
            ? parsed.pathname
            : `/${schoolId}${parsed.pathname.startsWith("/") ? "" : "/"}${parsed.pathname}`;

      aliasUrl.pathname = pathname;
      aliasUrl.search = parsed.search;
      aliases.push(aliasUrl.toString());
    }
  }

  return aliases;
}
