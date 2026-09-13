import { Request } from "express";

export interface ApiResponseMeta {
  request_id?: string;
  timestamp: string;
  [key: string]: any;
}

export interface PaginationMeta {
  next_cursor?: string | null;
  has_more: boolean;
  limit: number;
}

export function formatResponse<T>(req: Request, data: T) {
  const requestId = (req as any).requestId || "req_unknown";
  return {
    data,
    meta: {
      request_id: requestId,
      timestamp: new Date().toISOString(),
    },
  };
}

export function successResponse<T>(data: T, meta?: Record<string, any>) {
  return {
    data,
    meta: {
      timestamp: new Date().toISOString(),
      ...meta,
    },
  };
}

export function formatPaginatedResponse<T>(
  req: Request,
  data: T[],
  pagination: PaginationMeta
) {
  const requestId = (req as any).requestId || "req_unknown";
  return {
    data,
    pagination,
    meta: {
      request_id: requestId,
      timestamp: new Date().toISOString(),
    },
  };
}
