import { Router, Request, Response, NextFunction } from 'express';
import { authenticateJwt } from '../../common/middleware/auth';
import { query } from '../../config/database';
import { cacheGet, cacheSet } from '../../config/redis';
import { AppError } from '../../common/errors/app-error';
import { ErrorCode } from '../../common/errors/error-codes';
import { successResponse } from '../../common/types/api-response';

export const trackingRouter = Router();
trackingRouter.use(authenticateJwt);

// GET /api/v1/tracking/:tripId/latest
// Sub-5ms fast path using Redis cache, falling back to tracking.latest_locations projection
trackingRouter.get('/:tripId/latest', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.tripId;

    // 1. Check Redis Cache
    const cached = await cacheGet(`geo:trip:${tripId}:latest`);
    if (cached) {
      try {
        const parsed = JSON.parse(cached);
        return res.json(successResponse({ ...parsed, cached: true }));
      } catch {
        // Fallback on parse failure
      }
    }

    // 2. Fallback to PostgreSQL latest_locations projection (single index seek)
    const result = await query(
      `SELECT 
        ll.trip_id,
        ll.driver_id,
        ll.vehicle_id,
        ll.latitude,
        ll.longitude,
        ll.accuracy_meters,
        ll.speed_mps,
        ll.heading_degrees,
        ll.device_recorded_at,
        ll.server_received_at,
        u.full_name as driver_name,
        u.phone as driver_phone,
        v.registration_number as vehicle_number
      FROM tracking.latest_locations ll
      JOIN fleet.drivers d ON d.id = ll.driver_id
      JOIN identity.users u ON u.id = d.user_id
      JOIN fleet.vehicles v ON v.id = ll.vehicle_id
      WHERE ll.trip_id = $1`,
      [tripId]
    );

    if (result.rows.length === 0) {
      // Check if trip exists
      const tripCheck = await query(`SELECT id, status FROM operations.trips WHERE id = $1`, [tripId]);
      if (tripCheck.rows.length === 0) {
        throw AppError.notFound('Trip not found', ErrorCode.TRIP_NOT_FOUND);
      }
      return res.json(
        successResponse({
          trip_id: tripId,
          status: tripCheck.rows[0].status,
          latitude: null,
          longitude: null,
          message: 'No location telemetry recorded yet'
        })
      );
    }

    const latest = result.rows[0];

    // Cache back in Redis
    await cacheSet(`geo:trip:${tripId}:latest`, JSON.stringify(latest), 3600);

    return res.json(successResponse({ ...latest, cached: false }));
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/tracking/:tripId/history
trackingRouter.get('/:tripId/history', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.tripId;
    const limit = Math.min(Number(req.query.limit) || 100, 1000);
    const since = req.query.since as string;

    let sql = `
      SELECT 
        id,
        trip_id,
        latitude,
        longitude,
        speed_mps,
        heading_degrees,
        accuracy_meters,
        device_recorded_at,
        server_received_at
      FROM tracking.location_pings
      WHERE trip_id = $1
    `;
    const params: any[] = [tripId];

    if (since) {
      params.push(since);
      sql += ` AND device_recorded_at > $2`;
    }

    sql += ` ORDER BY device_recorded_at ASC LIMIT ${limit}`;

    const result = await query(sql, params);
    return res.json(successResponse(result.rows));
  } catch (err) {
    next(err);
  }
});
