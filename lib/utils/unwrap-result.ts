"use client";

import { ApiError, FrontApiError } from "@/lib/auth/apiError";

type ApiResultLike<T> = [FrontApiError | null, T | null];

export function unwrapResult<T>([error, data]: ApiResultLike<T>): T {
  if (error || data === null) {
    throw new ApiError(
      error?.status ?? 500,
      error?.statusText ?? "",
      error?.message ?? "Unknown error",
    );
  }
  return data;
}
