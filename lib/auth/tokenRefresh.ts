import "server-only";

export interface RefreshResult {
  accessToken: string;
  rawSetCookies: string[];
}

/**
 * Дедуп по значению refreshToken, а не по глобальному булеву флагу —
 * так параллельные запросы РАЗНЫХ пользователей никогда не пересекутся
 * (в отличие от старого refreshPromise на уровне модуля).
 *
 * ВАЖНО: middleware (Edge runtime) и route.ts (Node runtime) — это разные
 * изоляты/процессы с разным module scope. Эта Map не шарится между ними,
 * дедуп работает только "внутри" каждого из этих двух контекстов отдельно.
 * Для accessToken с TTL >30 мин вероятность одновременных коллизий между
 * этими контекстами низкая, поэтому этого достаточно на первое время.
 */
const inFlight = new Map<string, Promise<RefreshResult | null>>();

function extractRefreshToken(cookieHeader: string): string | null {
  const match = /(?:^|;\s*)refreshToken=([^;]+)/.exec(cookieHeader);
  return match?.[1] ?? null;
}

export async function performTokenRefresh(
  cookieHeader: string,
): Promise<RefreshResult | null> {
  const refreshTokenValue = extractRefreshToken(cookieHeader);
  if (!refreshTokenValue) return null;

  if (!inFlight.has(refreshTokenValue)) {
    inFlight.set(
      refreshTokenValue,
      doRefresh(cookieHeader).finally(() => inFlight.delete(refreshTokenValue)),
    );
  }

  return inFlight.get(refreshTokenValue)!;
}

async function doRefresh(cookieHeader: string): Promise<RefreshResult | null> {
  const res = await fetch(`${process.env.API_BASE_URL}/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      cookie: cookieHeader,
    },
    cache: "no-store",
  });

  if (!res.ok) return null;

  const data = (await res.json()) as { accessToken: string };
  const rawSetCookies = (res.headers as any).getSetCookie?.() ?? [];

  return { accessToken: data.accessToken, rawSetCookies };
}
