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
