import { Request, Response, NextFunction } from "express";
import { AppError } from "./app-error.js";
import { ErrorCodes } from "./error-codes.js";
import { ZodError } from "zod";

export function errorHandler(
  err: any,
  req: Request,
  res: Response,
  _next: NextFunction
) {
  const requestId = (req as any).requestId || req.headers["x-request-id"] || "req_unknown";

  // Handle AppError
  if (err instanceof AppError) {
    return res.status(err.statusCode).json({
      error: {
        code: err.code,
        message: err.message,
        request_id: requestId,
        status: err.statusCode,
        details: err.details || {},
      },
    });
  }

  // Handle Zod validation errors
  if (err instanceof ZodError) {
    const fieldErrors = err.errors.map((e) => ({
      field: e.path.join("."),
      message: e.message,
    }));
    return res.status(400).json({
      error: {
        code: ErrorCodes.VALIDATION_ERROR,
        message: "Request validation failed",
        request_id: requestId,
        status: 400,
        details: { fields: fieldErrors },
      },
    });
  }

  // Handle PostgreSQL unique violations
  if (err.code === "23505") {
    return res.status(409).json({
      error: {
        code: ErrorCodes.CONCURRENT_UPDATE,
        message: "A resource with these details already exists or conflict occurred.",
        request_id: requestId,
        status: 409,
        details: { constraint: err.constraint },
      },
    });
  }

  // Log unexpected errors
  console.error(
    JSON.stringify({
      level: "ERROR",
      request_id: requestId,
      url: req.url,
      method: req.method,
      error_message: err.message,
      stack: process.env.NODE_ENV !== "production" ? err.stack : undefined,
    })
  );

  return res.status(500).json({
    error: {
      code: ErrorCodes.INTERNAL_SERVER_ERROR,
      message: "Internal server error",
      request_id: requestId,
      status: 500,
    },
  });
}
