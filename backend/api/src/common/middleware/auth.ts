import { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";
import { env } from "../../config/env.js";
import { AppError } from "../errors/app-error.js";
import { ErrorCodes } from "../errors/error-codes.js";

export interface AuthenticatedUser {
  id: string;
  userId: string;
  role: string;
  organization_id: string;
  organizationId: string;
  phone?: string;
  email?: string;
}

declare global {
  namespace Express {
    interface Request {
      user?: AuthenticatedUser;
      requestId?: string;
    }
  }
}

export function authenticate(req: Request, _res: Response, next: NextFunction) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return next(AppError.unauthorized("Missing or invalid Authorization header", ErrorCodes.AUTH_REQUIRED));
  }

  const token = authHeader.substring(7);
  try {
    const raw = jwt.verify(token, env.JWT_ACCESS_SECRET) as any;
    const payload: AuthenticatedUser = {
      id: raw.id || raw.userId,
      userId: raw.userId || raw.id,
      role: raw.role,
      organization_id: raw.organization_id || raw.organizationId || "",
      organizationId: raw.organizationId || raw.organization_id || "",
      phone: raw.phone,
      email: raw.email,
    };
    req.user = payload;
    next();
  } catch (err: any) {
    if (err.name === "TokenExpiredError") {
      return next(new AppError(401, ErrorCodes.TOKEN_EXPIRED, "Access token has expired"));
    }
    return next(AppError.unauthorized("Invalid access token", ErrorCodes.AUTH_REQUIRED));
  }
}

export const authenticateJwt = authenticate;

export function optionalAuthenticate(req: Request, _res: Response, next: NextFunction) {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith("Bearer ")) {
    const token = authHeader.substring(7);
    try {
      const raw = jwt.verify(token, env.JWT_ACCESS_SECRET) as any;
      const payload: AuthenticatedUser = {
        id: raw.id || raw.userId,
        userId: raw.userId || raw.id,
        role: raw.role,
        organization_id: raw.organization_id || raw.organizationId || "",
        organizationId: raw.organizationId || raw.organization_id || "",
        phone: raw.phone,
        email: raw.email,
      };
      req.user = payload;
    } catch {
      // Ignore invalid optional token
    }
  }
  next();
}
