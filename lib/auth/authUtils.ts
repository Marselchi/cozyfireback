import { cookies } from "next/headers";
import { jwtDecode } from "jwt-decode";
import setCookieParser, {
  type Cookie as ParsedCookie,
} from "set-cookie-parser";

export const ACCESS_TOKEN_COOKIE_NAME = "accessToken";
export const REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

export const getCookieSecuritySettings = () => {
  const isTest = process.env.NODE_ENV === "test";
  return {
    sameSite: "lax" as const,
    secure: !isTest,
  };
};

export const getAccessToken = async (): Promise<string | null> => {
  const cookieStore = await cookies();
  return cookieStore.get(ACCESS_TOKEN_COOKIE_NAME)?.value || null;
};

export const decodeToken = (token: string): DecodedToken | null => {
  try {
    return jwtDecode<DecodedToken>(token);
  } catch (e) {
    console.error("Error decoding token:", e);
    return null;
  }
};

export const isExpiringSoon = (token: string, bufferSeconds = 60): boolean => {
  const decoded = decodeToken(token);
  if (!decoded?.exp) return true; // не смогли декодировать — считаем протухшим, безопасный дефолт
  return decoded.exp * 1000 - bufferSeconds * 1000 < Date.now();
};

// Общий минимальный интерфейс для cookies() ReadonlyRequestCookies,
// NextRequest.cookies (RequestCookies) — оба имеют .getAll().
interface CookieReader {
  getAll(): { name: string; value: string }[];
}

export function buildCookieHeaderFromStore(cookieStore: CookieReader): string {
  return cookieStore
    .getAll()
    .map((c) => `${c.name}=${c.value}`)
    .join("; ");
}

/**
 * Минимальный интерфейс, общий для NextResponse.cookies и cookies() (ResponseCookies).
 */
interface CookieSetter {
  set(name: string, value: string, options?: Record<string, unknown>): void;
}

/**
 * Единая точка установки accessToken-cookie — используется и в /refresh route,
 * и в middleware, чтобы политика (httpOnly/path/security) не расходилась.
 */
export function setAccessTokenCookie(
  target: CookieSetter,
  accessToken: string,
) {
  const decoded = decodeToken(accessToken);
  const expires = decoded?.exp ? new Date(decoded.exp * 1000) : undefined;

  target.set(ACCESS_TOKEN_COOKIE_NAME, accessToken, {
    httpOnly: true, // клиенту JS больше не нужен доступ к значению — BFF-модель
    path: "/",
    ...getCookieSecuritySettings(),
    ...(expires ? { expires } : {}),
  });
}

type CookiePolicy = (parsed: ParsedCookie) => Record<string, unknown>;

const cookiePolicies: Record<string, CookiePolicy> = {
  [REFRESH_TOKEN_COOKIE_NAME]: (parsed) => ({
    httpOnly: true,
    // Path="/" обязателен — middleware проверяет /api/** тоже и должен
    // получать refreshToken на любом защищённом пути. Сужение Path сломает
    // проактивный рефреш там, где он реально нужен.
    path: "/",
    ...getCookieSecuritySettings(),
    ...(parsed.maxAge !== undefined ? { maxAge: parsed.maxAge } : {}),
    ...(parsed.expires ? { expires: parsed.expires } : {}),
  }),
};

const defaultPolicy: CookiePolicy = (parsed) => ({
  httpOnly: true,
  path: "/",
  ...getCookieSecuritySettings(),
  ...(parsed.maxAge !== undefined ? { maxAge: parsed.maxAge } : {}),
  ...(parsed.expires ? { expires: parsed.expires } : {}),
});

export function applyBackendSetCookies(
  target: CookieSetter,
  rawSetCookies: string[],
) {
  if (!rawSetCookies.length) return;

  const parsedCookies = setCookieParser.parse(rawSetCookies, {
    decodeValues: false,
  });

  for (const parsed of parsedCookies) {
    const policy = cookiePolicies[parsed.name] ?? defaultPolicy;
    try {
      target.set(parsed.name, parsed.value, policy(parsed));
    } catch (e) {
      console.error(
        `applyBackendSetCookies: не удалось установить cookie "${parsed.name}"`,
        e,
      );
    }
  }
}

interface DecodedToken {
  exp?: number;
  [key: string]: any;
}
