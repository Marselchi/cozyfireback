import "server-only";
import { PageableParams } from "@/types/springTypes";
import { ACCESS_TOKEN_COOKIE_NAME, getAccessToken } from "./authUtils";
import { cookies } from "next/headers";
import { cacheTag, cacheLife } from "next/cache";
import { ApiError } from "./apiError";

interface RequestOptions {
  headers?: Record<string, string>;
  expectsJson?: boolean;
}

type ApiResult<T = any> = [ApiError | null, T | null];
type Method = "GET" | "POST" | "PUT" | "DELETE" | "PATCH";

async function request<T>(
  url: string,
  init: RequestInit & { expectsJson?: boolean; auth?: boolean } = {},
): Promise<ApiResult<T>> {
  const { expectsJson = true, auth = false, ...fetchInit } = init;

  let authorizationHeader: string | undefined;

  if (auth) {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get(ACCESS_TOKEN_COOKIE_NAME)?.value;

    if (accessToken) {
      authorizationHeader = `Bearer ${accessToken}`;
    }
  }

  const res = await fetch(`${process.env.API_BASE_URL}${url}`, {
    cache: "no-store",
    ...fetchInit,
    headers: {
      ...(authorizationHeader ? { Authorization: authorizationHeader } : {}),
      ...fetchInit.headers,
    },
  });

  if (!res.ok) {
    return [
      new ApiError(
        res.status,
        res.statusText,
        `API request failed: ${res.status}`,
      ),
      null,
    ];
  }

  const data = expectsJson ? await res.json() : res;
  return [null, data as T];
}

export const getAnon = <T = any>(
  url: string,
  options: RequestOptions = {},
): Promise<ApiResult<T>> => {
  const { expectsJson = true, headers } = options;

  return request<T>(url, {
    method: "GET",
    expectsJson,
    headers: {
      "Content-Type": "application/json",
      ...headers,
    },
  });
};
/**
 * Функция получения данных БЕЗ встроенного кеширования (для клиентского рендера)
 **/
export const getWithAuth = <T = any>(
  url: string,
  options: RequestOptions = {},
): Promise<ApiResult<T>> => {
  return request<T>(url, { method: "GET", auth: true, ...options });
};

/**
 * Функция получения данных с кешем. НЕ ИСПОЛЬЗОВАТЬ в клиентском скопе
 **/
export async function getWithAuthCacheable<T = any>(
  url: string,
  accessToken: string,
  roomId: string,
  options: RequestOptions = {},
): Promise<ApiResult<T>> {
  "use cache";
  cacheTag(`room:${roomId}`);
  cacheLife("minutes");

  return request<T>(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    ...options,
  });
}

export const sendAnon = <T = any>(
  url: string,
  method: Exclude<Method, "GET">,
  body?: any,
  options: RequestOptions = {},
): Promise<ApiResult<T>> => {
  const { expectsJson = true, headers } = options;

  return request<T>(url, {
    method,
    expectsJson,
    headers: {
      "Content-Type": "application/json",
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });
};

export const sendWithAuth = <T = any>(
  url: string,
  method: Exclude<Method, "GET">,
  body?: any,
  options: RequestOptions = {},
): Promise<ApiResult<T>> => {
  const { expectsJson = true, headers } = options;

  return request<T>(url, {
    method,
    auth: true,
    expectsJson,
    headers: {
      "Content-Type": "application/json",
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });
};

export const sendWithAuthNoJson = <T = any>(
  url: string,
  method: Exclude<Method, "GET">,
  body?: any,
  options: RequestOptions = {},
): Promise<ApiResult<T>> => {
  const { expectsJson = true, headers } = options;

  return request<T>(url, {
    method,
    auth: true,
    expectsJson,
    headers: {
      ...headers,
    },
    body: body ?? undefined,
  });
};

export function buildQueryString(
  filter: Record<string, any> = {},
  pageable: PageableParams = {},
): string {
  const params = new URLSearchParams();

  for (const [key, value] of Object.entries(filter)) {
    if (value == null) continue;

    if (Array.isArray(value)) {
      for (const item of value) params.append(key, item.toString());
    } else {
      params.append(key, value.toString());
    }
  }

  if (pageable.page !== undefined)
    params.append("page", pageable.page.toString());
  if (pageable.size !== undefined)
    params.append("size", pageable.size.toString());
  if (pageable.sort?.length)
    pageable.sort.forEach((s) => params.append("sort", s));

  return params.toString();
}
