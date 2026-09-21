import { NextResponse, type NextRequest } from "next/server";
import {
  ACCESS_TOKEN_COOKIE_NAME,
  isExpiringSoon,
  setAccessTokenCookie,
  applyBackendSetCookies,
} from "./lib/auth/authUtils";
import { performTokenRefresh } from "./lib/auth/tokenRefresh";

const publicPaths = [
  "/auth/login",
  "/login",
  "/signup",
  "/api/v1/auth",
  "/refresh",
];

function buildCookieHeaderWithOverride(
  request: NextRequest,
  overrides: Record<string, string>,
): string {
  const merged = new Map(
    request.cookies.getAll().map((c) => [c.name, c.value]),
  );
  for (const [name, value] of Object.entries(overrides)) {
    merged.set(name, value);
  }
  return Array.from(merged.entries())
    .map(([name, value]) => `${name}=${value}`)
    .join("; ");
}

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isApiPath = pathname.startsWith("/api");
  const isPublicPath = publicPaths.some((p) => pathname.startsWith(p));

  if (isPublicPath) {
    if (pathname === "/login") {
      const token = request.cookies.get(ACCESS_TOKEN_COOKIE_NAME)?.value;
      if (token && !isExpiringSoon(token)) {
        return NextResponse.redirect(new URL("/", request.url));
      }
    }
    return NextResponse.next();
  }

  const currentToken = request.cookies.get(ACCESS_TOKEN_COOKIE_NAME)?.value;

  if (currentToken && !isExpiringSoon(currentToken)) {
    return NextResponse.next();
  }

  // Токена нет либо он скоро истечёт/истёк — пробуем рефрешнуть
  const cookieHeader = request.cookies
    .getAll()
    .map((c) => `${c.name}=${c.value}`)
    .join("; ");

  const result = await performTokenRefresh(cookieHeader);

  if (!result) {
    // refreshToken невалиден — реальный конец сессии
    if (isApiPath) {
      return NextResponse.json({ error: "session_expired" }, { status: 401 });
    }
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("callbackUrl", request.url);
    return NextResponse.redirect(loginUrl);
  }

  // Подменяем cookie-заголовок в ЗАПРОСЕ — чтобы Route Handler/RSC,
  // который выполнится сразу после middleware В ЭТОМ ЖЕ цикле,
  // увидел уже свежий accessToken, а не старый истёкший.
  const requestHeaders = new Headers(request.headers);
  requestHeaders.set(
    "cookie",
    buildCookieHeaderWithOverride(request, {
      [ACCESS_TOKEN_COOKIE_NAME]: result.accessToken,
    }),
  );

  const response = NextResponse.next({ request: { headers: requestHeaders } });

  // И ставим cookie в ОТВЕТ — чтобы браузер обновил её на будущее
  setAccessTokenCookie(response.cookies, result.accessToken);
  applyBackendSetCookies(response.cookies, result.rawSetCookies);

  return response;
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|icon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp|woff|woff2|ttf|eot)$).*)",
  ],
};
