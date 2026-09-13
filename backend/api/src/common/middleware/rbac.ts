import { Request, Response, NextFunction } from "express";
import { AppError } from "../errors/app-error.js";
import { ErrorCodes } from "../errors/error-codes.js";

export function requireRole(...allowedRoles: string[]) {
  return (req: Request, _res: Response, next: NextFunction) => {
    const user = (req as any).user;
    if (!user) {
      return next(AppError.unauthorized("Authentication required", ErrorCodes.AUTH_REQUIRED));
    }

    if (!allowedRoles.includes(user.role) && user.role !== "SUPER_ADMIN") {
      return next(AppError.forbidden(`Role '${user.role}' is not authorized to access this resource`));
    }

    next();
  };
}

export function requireRoles(roles: string[]) {
  return requireRole(...roles);
}
