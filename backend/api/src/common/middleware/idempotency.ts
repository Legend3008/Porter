import { Request, Response, NextFunction } from "express";
import crypto from "crypto";
import { query } from "../../config/database.js";
import { AppError } from "../errors/app-error.js";
import { ErrorCodes } from "../errors/error-codes.js";

export function idempotencyMiddleware(req: Request, res: Response, next: NextFunction) {
  // Only apply to mutating methods
  if (!["POST", "PUT", "PATCH"].includes(req.method.toUpperCase())) {
    return next();
  }

  const idempotencyKey = req.headers["idempotency-key"] as string | undefined;
  if (!idempotencyKey) {
    return next();
  }

  const actorId = (req as any).user?.id || "00000000-0000-0000-0000-000000000000";
  const operation = `${req.method.toUpperCase()} ${req.baseUrl}${req.path}`;
  const requestHash = crypto
    .createHash("sha256")
    .update(JSON.stringify(req.body || {}))
    .digest("hex");

  // Check if idempotency key exists
  query(
    `SELECT status, request_hash, response_status, response_body 
     FROM integration.idempotency_keys 
     WHERE actor_id = $1 AND operation = $2 AND idempotency_key = $3`,
    [actorId, operation, idempotencyKey]
  )
    .then(async (result) => {
      if (result.rows.length > 0) {
        const existing = result.rows[0];

        // Conflict check: key reused with different request payload
        if (existing.request_hash !== requestHash) {
          return next(
            AppError.conflict(
              "Idempotency key was previously used with a different request payload.",
              ErrorCodes.IDEMPOTENCY_CONFLICT
            )
          );
        }

        // In progress check
        if (existing.status === "IN_PROGRESS") {
          return next(
            AppError.conflict(
              "A request with this idempotency key is already currently executing.",
              ErrorCodes.IDEMPOTENCY_CONFLICT
            )
          );
        }

        // Return cached completed response
        if (existing.status === "COMPLETED" && existing.response_status) {
          res.setHeader("X-Cache-Lookup", "HIT-IDEMPOTENT");
          return res.status(existing.response_status).json(existing.response_body);
        }
      }

      // Record in progress
      await query(
        `INSERT INTO integration.idempotency_keys (
           idempotency_key, actor_id, operation, request_hash, status, expires_at
         ) VALUES ($1, $2, $3, $4, 'IN_PROGRESS', clock_timestamp() + interval '24 hours')
         ON CONFLICT (actor_id, operation, idempotency_key) DO NOTHING`,
        [idempotencyKey, actorId, operation, requestHash]
      );

      // Intercept res.json to record response on completion
      const originalJson = res.json.bind(res);
      res.json = (body: any) => {
        const statusCode = res.statusCode;
        query(
          `UPDATE integration.idempotency_keys 
           SET status = 'COMPLETED', response_status = $1, response_body = $2, completed_at = clock_timestamp()
           WHERE actor_id = $3 AND operation = $4 AND idempotency_key = $5`,
          [statusCode, JSON.stringify(body), actorId, operation, idempotencyKey]
        )
          .catch(() => {})
          .finally(() => {
            originalJson(body);
          });
        return res;
      };

      next();
    })
    .catch((err) => next(err));
}
