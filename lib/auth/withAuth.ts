import { cookies } from "next/headers";
import { ACCESS_TOKEN_COOKIE_NAME, getAccessToken, REFRESH_TOKEN_COOKIE_NAME } from "./authUtils";
import { refreshAccessToken } from "./refreshToken";
import { v4 as uuidv4 } from "uuid";


export function withAuth<T extends (...args: any[]) => Promise<any>>(
  serverAction: T
): (...args: Parameters<T>) => Promise<ReturnType<T>> {
  return async function (...args: Parameters<T>): Promise<ReturnType<T>> {
    const accessToken = await getAccessToken();
    const requestId = uuidv4();

    if (!accessToken) {
      const newToken = await refreshAccessToken(requestId);
      if (!newToken) {
        const cookieStore = await cookies();
        cookieStore.delete(ACCESS_TOKEN_COOKIE_NAME);
        cookieStore.delete(REFRESH_TOKEN_COOKIE_NAME);
        throw new Error("Unauthorized: Token refresh failed");
      }
    }

    // Вызов оригинальной функции
    return serverAction(...args);
  };
}