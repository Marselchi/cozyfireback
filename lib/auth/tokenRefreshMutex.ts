type RefreshPromise = Promise<string> | null;
const requestContext = new Map<string, RefreshPromise>();

export const acquireRefreshMutex = (requestId: string): { shouldRefresh: boolean; refreshPromise: RefreshPromise } => {
  const existing = requestContext.get(requestId);
  if (existing) {
    return { shouldRefresh: false, refreshPromise: existing };
  }
  return { shouldRefresh: true, refreshPromise: null };
};

export const setRefreshPromise = (requestId: string, promise: RefreshPromise): void => {
  requestContext.set(requestId, promise);
};

export const clearRefreshPromise = (requestId: string): void => {
  requestContext.delete(requestId);
};