import { ErrorCode, ErrorCodes } from "./error-codes.js";

export class AppError extends Error {
  public readonly statusCode: number;
  public readonly code: ErrorCode;
  public readonly details?: Record<string, any>;

  constructor(statusCode: number, code: ErrorCode, message: string, details?: Record<string, any>);
  constructor(message: string, statusCode?: number, code?: ErrorCode, details?: Record<string, any>);
  constructor(
    arg1: number | string,
    arg2?: ErrorCode | number,
    arg3?: string | ErrorCode,
    arg4?: Record<string, any>
  ) {
    let statusCode = 500;
    let code: ErrorCode = ErrorCodes.INTERNAL_SERVER_ERROR;
    let message = "An error occurred";
    let details: Record<string, any> | undefined;

    if (typeof arg1 === "number") {
      statusCode = arg1;
      code = (arg2 as ErrorCode) || ErrorCodes.INTERNAL_SERVER_ERROR;
      message = (arg3 as string) || "An error occurred";
      details = arg4;
    } else {
      message = arg1;
      statusCode = typeof arg2 === "number" ? arg2 : 500;
      code = (arg3 as ErrorCode) || (typeof arg2 === "string" ? (arg2 as ErrorCode) : ErrorCodes.INTERNAL_SERVER_ERROR);
      details = arg4;
    }

    super(message);
    this.name = "AppError";
    this.statusCode = statusCode;
    this.code = code;
    this.details = details;
    Object.setPrototypeOf(this, new.target.prototype);
  }

  static badRequest(message: string, code: ErrorCode = ErrorCodes.VALIDATION_ERROR, details?: Record<string, any>) {
    return new AppError(400, code, message, details);
  }

  static unauthorized(message: string = "Authentication required", code: ErrorCode = ErrorCodes.AUTH_REQUIRED) {
    return new AppError(401, code, message);
  }

  static forbidden(message: string = "Access denied", code: ErrorCode = ErrorCodes.FORBIDDEN) {
    return new AppError(403, code, message);
  }

  static notFound(message: string = "Resource not found", code: ErrorCode = ErrorCodes.RESOURCE_NOT_FOUND) {
    return new AppError(404, code, message);
  }

  static conflict(message: string, code: ErrorCode = ErrorCodes.CONCURRENT_UPDATE, details?: Record<string, any>) {
    return new AppError(409, code, message, details);
  }

  static unprocessable(message: string, code: ErrorCode = ErrorCodes.VALIDATION_ERROR, details?: Record<string, any>) {
    return new AppError(422, code, message, details);
  }

  static internal(message: string = "An unexpected error occurred", details?: Record<string, any>) {
    return new AppError(500, ErrorCodes.INTERNAL_SERVER_ERROR, message, details);
  }
}
