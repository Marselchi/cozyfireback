export class ApiError extends Error {
  status: number;
  statusText: string;

  constructor(status: number, statusText: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.statusText = statusText;
  }
}

export type ApiResult<T = any> = [ApiError | null, T | null];

// Явный shape для данных, прошедших через RSC-границу.
// Named type — а не анонимная структура — чтобы было видно намерение в сигнатурах.
export interface FrontApiError {
  status: number;
  statusText: string;
  message: string;
}

export function toFrontApiError(e: ApiError | Error): FrontApiError {
  if (e instanceof ApiError) {
    return { status: e.status, statusText: e.statusText, message: e.message };
  }
  return {
    status: 500,
    statusText: "",
    message: e instanceof Error ? e.message : "Unknown error",
  };
}
