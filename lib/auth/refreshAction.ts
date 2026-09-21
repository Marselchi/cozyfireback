'use server';

import 'server-only';
import { cookies } from 'next/headers';
import {
  ACCESS_TOKEN_COOKIE_NAME,
  decodeToken,
  getCookieSecuritySettings,
} from './authUtils';

let refreshPromise: Promise<string | null> | null = null;

export async function refreshTokens(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const cookieStore = await cookies();

      const cookieHeader = cookieStore
        .getAll()
        .map(c => `${c.name}=${c.value}`)
        .join('; ');

      const res = await fetch(`${process.env.API_BASE_URL}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(cookieHeader ? { cookie: cookieHeader } : {}),
        },
        cache: 'no-store',
      });

      if (!res.ok) return null;

      const data = await res.json();
      const accessToken = data.accessToken;

      const decoded = decodeToken(accessToken);
      const expires = decoded?.exp
        ? new Date(decoded.exp * 1000)
        : undefined;

      cookieStore.set(ACCESS_TOKEN_COOKIE_NAME, accessToken, {
        httpOnly: false,
        path: '/',
        ...getCookieSecuritySettings(),
        ...(expires ? { expires } : {}),
      });

      const rawSetCookies =
        (res.headers as any).getSetCookie?.() ?? [];

      for (const raw of rawSetCookies) {
        const [cookiePart] = raw.split(';');
        const [name, value] = cookiePart.split('=');

        if (name && value) {
          cookieStore.set(name.trim(), value.trim());
        }
      }

      return accessToken;
    })().finally(() => {
      refreshPromise = null;
    });
  }

  return refreshPromise;
}