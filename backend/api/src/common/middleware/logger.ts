import { Request, Response, NextFunction } from "express";

export function structuredLogger(req: Request, res: Response, next: NextFunction) {
  const start = Date.now();
  const requestId = (req as any).requestId;

  res.on("finish", () => {
    const durationMs = Date.now() - start;
    const logEntry = {
      level: res.statusCode >= 500 ? "ERROR" : res.statusCode >= 400 ? "WARN" : "INFO",
      request_id: requestId,
      method: req.method,
      route: req.originalUrl || req.url,
      status: res.statusCode,
      duration_ms: durationMs,
      ip: req.ip || req.socket.remoteAddress,
      user_agent: req.headers["user-agent"],
      actor_id: (req as any).user?.id,
    };

    console.log(JSON.stringify(logEntry));
  });

  next();
}

export const requestLogger = structuredLogger;
