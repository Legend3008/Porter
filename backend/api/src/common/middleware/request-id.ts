import { Request, Response, NextFunction } from "express";
import { v4 as uuidv4 } from "uuid";

export function requestIdMiddleware(req: Request, res: Response, next: NextFunction) {
  const incomingId = req.headers["x-request-id"] as string | undefined;
  const requestId = incomingId && incomingId.trim() !== "" ? incomingId : `req_${uuidv4().replace(/-/g, "")}`;
  
  (req as any).requestId = requestId;
  res.setHeader("X-Request-ID", requestId);
  next();
}
