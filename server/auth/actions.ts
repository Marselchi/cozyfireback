"use server";
import {
  JwtAuthResponse,
  LoginRequest,
  SignupRequest,
} from "@/types/springTypes";
import "server-only";

import { cookies } from "next/headers";
import {
  ACCESS_TOKEN_COOKIE_NAME,
  applyBackendSetCookies,
  decodeToken,
  getCookieSecuritySettings,
  setAccessTokenCookie,
} from "@/lib/auth/authUtils";

export const signUpUser = async (request: SignupRequest): Promise<void> => {
  const res = await fetch(`${process.env.API_BASE_URL}/auth/register`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!res.ok) {
    throw new Error(`Sign up failed: ${res.status} ${res.statusText}`);
  }
};

export const loginUser = async (
  request: LoginRequest,
): Promise<JwtAuthResponse | null> => {
  const backendRes = await fetch(`${process.env.API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    cache: "no-store",
  });

  if (!backendRes.ok) {
    return null;
  }

  const data: JwtAuthResponse = await backendRes.json();
  const cookieStore = await cookies();

  // 1) Ставим accessToken через единую точку (с httpOnly, правильным path и expires)
  setAccessTokenCookie(cookieStore, data.accessToken);

  // 2) Забираем все Set-Cookie хедеры от Spring Boot и применяем их разом
  const setCookies: string[] =
    (backendRes.headers as any).getSetCookie?.() || [];
  applyBackendSetCookies(cookieStore, setCookies);

  return { accessToken: data.accessToken };
};
