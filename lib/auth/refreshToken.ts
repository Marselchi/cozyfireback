import { cookies, headers } from "next/headers";
import { ACCESS_TOKEN_COOKIE_NAME, decodeToken, getCookieSecuritySettings } from "./authUtils";
import { acquireRefreshMutex, setRefreshPromise, clearRefreshPromise } from "./tokenRefreshMutex";

export const refreshAccessToken = async (requestId: string): Promise<string | null> => {
  const { shouldRefresh, refreshPromise } = acquireRefreshMutex(requestId);

  if (refreshPromise) {
    return refreshPromise;
  }

  if (!shouldRefresh) {
    return null;
  }

  const refreshExec = async (): Promise<string> => {
    const cookieStore = await cookies();
    const allCookies = cookieStore.getAll();

    const requestHeaders = await headers();
    let cookieHeader = allCookies
      .map(cookie => `${cookie.name}=${encodeURIComponent(cookie.value)}`)
      .join('; ');


    const incomingCookie = requestHeaders.get('cookie');
    if (incomingCookie) {
      cookieHeader = incomingCookie;
    }


    const res = await fetch(`${process.env.API_BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(cookieHeader ? { Cookie: cookieHeader } : {}),
      },
    });

    if (!res.ok) {
      cookieStore.delete(ACCESS_TOKEN_COOKIE_NAME);
      throw new Error(`Failed to refresh token: ${res.status} ${await res.text()}`);
    }

    const data = await res.json();
    const newAccessToken: string = data.accessToken;

    const decodedNewToken = decodeToken(newAccessToken);
    let expires: Date | undefined;
    if (decodedNewToken?.exp) {
      expires = new Date(decodedNewToken.exp * 1000);
    }

    const securitySettings = getCookieSecuritySettings();

    cookieStore.set(ACCESS_TOKEN_COOKIE_NAME, newAccessToken, {
      httpOnly: false,
      ...securitySettings,
      path: "/",
      ...(expires ? { expires } : {}),
    });

    return newAccessToken;
  };

  const promise = refreshExec();
  setRefreshPromise(requestId, promise);

  try {
    const newToken = await promise;
    clearRefreshPromise(requestId);
    return newToken;
  } catch (error) {
    console.error("Token refresh failed:", error);
    clearRefreshPromise(requestId);
    return null;
  }
};