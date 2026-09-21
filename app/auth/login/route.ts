import { NextResponse } from "next/server";
import {
  setAccessTokenCookie,
  applyBackendSetCookies,
} from "@/lib/auth/authUtils";

export async function POST(request: Request) {
  const url = new URL(request.url);

  const form = await request.formData();
  const username = (form.get("username") as string) ?? "";
  const password = (form.get("password") as string) ?? "";

  const origin = url.origin;
  const callbackUrlRaw = String(form.get("callbackUrl") ?? "/");
  const returnTo = normalizeReturnTo(callbackUrlRaw, origin);

  const backendRes = await fetch(`${process.env.API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
    cache: "no-store",
  });

  if (!backendRes.ok) {
    const loginUrl = new URL("/login", url);
    loginUrl.searchParams.set("callbackUrl", callbackUrlRaw);
    loginUrl.searchParams.set("error", "invalid_credentials");
    return NextResponse.redirect(loginUrl, { status: 303 });
  }

  const data = (await backendRes.json()) as { accessToken: string };

  const res = NextResponse.redirect(new URL(returnTo, origin), { status: 303 });

  // Единая политика для accessToken — та же, что использует middleware при рефреше
  setAccessTokenCookie(res.cookies, data.accessToken);

  // Единая политика для всего, что прислал Spring (refreshToken и что угодно ещё) —
  // Path="/", httpOnly, без ручного split('=')
  const setCookies: string[] =
    (backendRes.headers as any).getSetCookie?.() ?? [];
  applyBackendSetCookies(res.cookies, setCookies);

  return res;
}

function normalizeReturnTo(raw: string | null, origin: string) {
  if (!raw) return "/";

  try {
    let url: URL;

    if (raw.startsWith("http://") || raw.startsWith("https://")) {
      url = new URL(raw);
    } else {
      url = new URL(raw, origin);
    }

    if (url.origin !== origin) {
      console.warn(`Blocked open redirect attempt to: ${url.origin}`);
      return "/";
    }

    const path = url.pathname + url.search;

    if (
      path.startsWith("/login") ||
      path.startsWith("/auth/login") ||
      path.startsWith("/refresh") ||
      path.startsWith("/api/refresh") ||
      path.startsWith("/api/login")
    ) {
      return "/";
    }

    return path.replace(/^\/\//, "/") || "/";
  } catch (e) {
    return "/";
  }
}
