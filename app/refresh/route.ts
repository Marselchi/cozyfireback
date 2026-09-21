import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import {
  ACCESS_TOKEN_COOKIE_NAME,
  REFRESH_TOKEN_COOKIE_NAME,
  buildCookieHeaderFromStore,
  applyBackendSetCookies,
  setAccessTokenCookie,
} from "@/lib/auth/authUtils";
import { performTokenRefresh } from "@/lib/auth/tokenRefresh";

export async function POST() {
  const cookieStore = await cookies();
  const cookieHeader = buildCookieHeaderFromStore(cookieStore);

  const result = await performTokenRefresh(cookieHeader);

  if (!result) {
    cookieStore.delete(ACCESS_TOKEN_COOKIE_NAME);
    cookieStore.delete(REFRESH_TOKEN_COOKIE_NAME);
    return NextResponse.json({ error: "session_expired" }, { status: 401 });
  }

  setAccessTokenCookie(cookieStore, result.accessToken);
  applyBackendSetCookies(cookieStore, result.rawSetCookies);

  return NextResponse.json({ ok: true });
}
