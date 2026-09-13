import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { authenticateJwt } from '../../common/middleware/auth';
import { withTransaction, query } from '../../config/database';
import { AppError } from '../../common/errors/app-error';
import { ErrorCode } from '../../common/errors/error-codes';
import { successResponse } from '../../common/types/api-response';
import { validateBody } from '../../common/middleware/validate';

export const tripsRouter = Router();
tripsRouter.use(authenticateJwt);

// GET /api/v1/trips/:id
tripsRouter.get('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.id;

    const result = await query(
      `SELECT 
        t.id,
        t.trip_number,
        t.status,
        t.booking_id,
        t.scheduled_start_at,
        t.actual_start_at,
        t.actual_end_at,
        t.current_stop_sequence,
        t.version,
        t.created_at,
        t.updated_at,
        -- Booking details
        b.booking_number,
        b.status as booking_status,
        b.total_amount_minor as total_amount_paise,
        -- Driver details
        d.id as driver_id,
        d.driver_code,
        u.full_name as driver_name,
        u.phone as driver_phone,
        -- Vehicle details
        v.id as vehicle_id,
        v.registration_number as vehicle_number,
        v.vehicle_type,
        -- Latest location
        ll.latitude as current_latitude,
        ll.longitude as current_longitude,
        ll.speed_mps,
        ll.heading_degrees,
        ll.device_recorded_at as location_updated_at
      FROM operations.trips t
      JOIN booking.bookings b ON b.id = t.booking_id
      LEFT JOIN fleet.drivers d ON d.id = t.driver_id
      LEFT JOIN identity.users u ON u.id = d.user_id
      LEFT JOIN fleet.vehicles v ON v.id = t.vehicle_id
      LEFT JOIN tracking.latest_locations ll ON ll.trip_id = t.id
      WHERE t.id = $1`,
      [tripId]
    );

    if (result.rows.length === 0) {
      throw AppError.notFound('Trip not found', ErrorCode.TRIP_NOT_FOUND);
    }

    const trip = result.rows[0];

    // Fetch container details
    const containerResult = await query(
      `SELECT 
        c.id,
        c.container_number,
        c.container_type,
        c.iso_code,
        sc.seal_number,
        sc.status
      FROM shipment.containers c
      JOIN shipment.shipment_containers sc ON sc.container_id = c.id
      JOIN booking.bookings b ON b.shipment_id = sc.shipment_id
      WHERE b.id = $1`,
      [trip.booking_id]
    );

    // Fetch stops
    const stopsResult = await query(
      `SELECT 
        ts.id,
        ts.sequence,
        ts.stop_type,
        ts.status,
        ts.planned_arrival_at,
        ts.actual_arrival_at,
        ts.actual_departure_at,
        l.name as location_name,
        l.city,
        l.latitude,
        l.longitude
      FROM operations.trip_stops ts
      JOIN geo.locations l ON l.id = ts.location_id
      WHERE ts.trip_id = $1
      ORDER BY ts.sequence ASC`,
      [tripId]
    );

    return res.json(
      successResponse({
        ...trip,
        containers: containerResult.rows,
        stops: stopsResult.rows
      })
    );
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/trips/:id/timeline
tripsRouter.get('/:id/timeline', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.id;

    const result = await query(
      `SELECT 
        id,
        trip_id,
        from_status,
        to_status,
        actor_type,
        source,
        reason,
        occurred_at
      FROM operations.trip_status_history
      WHERE trip_id = $1
      ORDER BY occurred_at ASC`,
      [tripId]
    );

    return res.json(successResponse(result.rows));
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/trips/:id/accept
// Concurrency protected using SELECT ... FOR UPDATE and version lock
tripsRouter.post('/:id/accept', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.id;
    const userId = req.user!.userId;

    const acceptedTrip = await withTransaction(async (client) => {
      // 1. Lock trip row with FOR UPDATE
      const tripCheck = await client.query(
        `SELECT id, trip_number, status, version, driver_id, carrier_id, booking_id
         FROM operations.trips
         WHERE id = $1
         FOR UPDATE`,
        [tripId]
      );

      if (tripCheck.rows.length === 0) {
        throw AppError.notFound('Trip not found', ErrorCode.TRIP_NOT_FOUND);
      }

      const trip = tripCheck.rows[0];

      // Check if already accepted or in-flight
      if (['DRIVER_ACCEPTED', 'EN_ROUTE_PICKUP', 'AT_PICKUP', 'LOADING', 'LOADED', 'EN_ROUTE_DELIVERY', 'DELIVERED', 'COMPLETED'].includes(trip.status)) {
        throw new AppError('Trip has already been accepted by another driver or is in progress', 409, ErrorCode.TRIP_ALREADY_ACCEPTED, {
          trip_id: tripId,
          current_status: trip.status
        });
      }

      // Find driver associated with requesting user
      const driverRes = await client.query(
        `SELECT id, carrier_id, status FROM fleet.drivers WHERE user_id = $1`,
        [userId]
      );

      let driverId = trip.driver_id;
      let carrierId = trip.carrier_id;

      if (driverRes.rows.length > 0) {
        driverId = driverRes.rows[0].id;
        carrierId = driverRes.rows[0].carrier_id;
      }

      // Update trip to DRIVER_ACCEPTED and increment optimistic version
      const updateTrip = await client.query(
        `UPDATE operations.trips
         SET status = 'DRIVER_ACCEPTED',
             driver_id = COALESCE($2, driver_id),
             carrier_id = COALESCE($3, carrier_id),
             version = version + 1,
             updated_at = clock_timestamp()
         WHERE id = $1 AND version = $4
         RETURNING *`,
        [tripId, driverId, carrierId, trip.version]
      );

      if (updateTrip.rows.length === 0) {
        throw new AppError('Trip was modified concurrently. Please refresh and retry.', 409, ErrorCode.CONCURRENCY_CONFLICT);
      }

      // If there was a job offer for this trip, update it to ACCEPTED
      if (driverId) {
        await client.query(
          `UPDATE operations.job_offers
           SET status = 'ACCEPTED', responded_at = clock_timestamp()
           WHERE trip_id = $1 AND driver_id = $2`,
          [tripId, driverId]
        );

        // Cancel other pending offers for this trip
        await client.query(
          `UPDATE operations.job_offers
           SET status = 'CANCELLED'
           WHERE trip_id = $1 AND driver_id != $2 AND status = 'OFFERED'`,
          [tripId, driverId]
        );

        // Update driver status to ON_TRIP
        await client.query(
          `UPDATE fleet.drivers SET status = 'ON_TRIP' WHERE id = $1`,
          [driverId]
        );
      }

      // Insert transactional outbox event
      await client.query(
        `INSERT INTO integration.outbox_events (
          event_type, aggregate_type, aggregate_id, payload
        ) VALUES ($1, $2, $3, $4)`,
        [
          'TRIP_ACCEPTED',
          'TRIP',
          tripId,
          JSON.stringify({
            trip_id: tripId,
            driver_id: driverId,
            carrier_id: carrierId,
            booking_id: trip.booking_id,
            accepted_at: new Date().toISOString()
          })
        ]
      );

      return updateTrip.rows[0];
    });

    return res.json(
      successResponse({
        trip_id: acceptedTrip.id,
        trip_number: acceptedTrip.trip_number,
        status: acceptedTrip.status,
        version: acceptedTrip.version
      })
    );
  } catch (err) {
    next(err);
  }
});

const transitionTripSchema = z.object({
  reason: z.string().optional(),
  latitude: z.number().min(-90).max(90).optional(),
  longitude: z.number().min(-180).max(180).optional()
});

// POST /api/v1/trips/:id/start
tripsRouter.post('/:id/start', validateBody(transitionTripSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.id;

    const updated = await withTransaction(async (client) => {
      const tripRes = await client.query(
        `SELECT id, status, version, actual_start_at FROM operations.trips WHERE id = $1 FOR UPDATE`,
        [tripId]
      );

      if (tripRes.rows.length === 0) {
        throw AppError.notFound('Trip not found', ErrorCode.TRIP_NOT_FOUND);
      }

      const trip = tripRes.rows[0];
      const validCurrent = ['DRIVER_ACCEPTED', 'ASSIGNED', 'CREATED'];
      if (!validCurrent.includes(trip.status)) {
        throw new AppError(`Cannot start trip in status ${trip.status}`, 400, ErrorCode.INVALID_STATE_TRANSITION);
      }

      const nextStatus = 'EN_ROUTE_PICKUP';
      const result = await client.query(
        `UPDATE operations.trips
         SET status = $1,
             actual_start_at = COALESCE(actual_start_at, clock_timestamp()),
             version = version + 1,
             updated_at = clock_timestamp()
         WHERE id = $2
         RETURNING *`,
        [nextStatus, tripId]
      );

      // Record outbox event
      await client.query(
        `INSERT INTO integration.outbox_events (
          event_type, aggregate_type, aggregate_id, payload
        ) VALUES ($1, $2, $3, $4)`,
        ['TRIP_STARTED', 'TRIP', tripId, JSON.stringify({ trip_id: tripId, status: nextStatus })]
      );

      return result.rows[0];
    });

    return res.json(successResponse(updated));
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/trips/:id/arrive
tripsRouter.post('/:id/arrive', validateBody(transitionTripSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.id;
    const { latitude, longitude } = req.body;

    const updated = await withTransaction(async (client) => {
      const tripRes = await client.query(
        `SELECT id, status, version FROM operations.trips WHERE id = $1 FOR UPDATE`,
        [tripId]
      );

      if (tripRes.rows.length === 0) {
        throw AppError.notFound('Trip not found', ErrorCode.TRIP_NOT_FOUND);
      }

      const trip = tripRes.rows[0];
      let nextStatus = 'AT_PICKUP';
      if (trip.status === 'EN_ROUTE_DELIVERY' || trip.status === 'LOADED') {
        nextStatus = 'AT_DELIVERY';
      }

      const result = await client.query(
        `UPDATE operations.trips
         SET status = $1,
             version = version + 1,
             updated_at = clock_timestamp()
         WHERE id = $2
         RETURNING *`,
        [nextStatus, tripId]
      );

      // Record gate event
      await client.query(
        `INSERT INTO operations.gate_events (
          trip_id, event_type, latitude, longitude
        ) VALUES ($1, $2, $3, $4)`,
        [tripId, 'GATE_IN', latitude || null, longitude || null]
      );

      return result.rows[0];
    });

    return res.json(successResponse(updated));
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/trips/:id/load
tripsRouter.post('/:id/load', validateBody(transitionTripSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.id;
    const updated = await withTransaction(async (client) => {
      const tripRes = await client.query(
        `SELECT id, status FROM operations.trips WHERE id = $1 FOR UPDATE`,
        [tripId]
      );
      if (tripRes.rows.length === 0) throw AppError.notFound('Trip not found', ErrorCode.TRIP_NOT_FOUND);
      const trip = tripRes.rows[0];

      if (trip.status === 'AT_PICKUP') {
        await client.query(`UPDATE operations.trips SET status = 'LOADING', version = version + 1, updated_at = clock_timestamp() WHERE id = $1`, [tripId]);
        const res = await client.query(`UPDATE operations.trips SET status = 'LOADED', version = version + 1, updated_at = clock_timestamp() WHERE id = $1 RETURNING *`, [tripId]);
        return res.rows[0];
      } else if (trip.status === 'LOADING') {
        const res = await client.query(`UPDATE operations.trips SET status = 'LOADED', version = version + 1, updated_at = clock_timestamp() WHERE id = $1 RETURNING *`, [tripId]);
        return res.rows[0];
      } else {
        throw new AppError(`Cannot load trip in status ${trip.status}`, 400, ErrorCode.INVALID_STATE_TRANSITION);
      }
    });
    return res.json(successResponse(updated));
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/trips/:id/depart
tripsRouter.post('/:id/depart', validateBody(transitionTripSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.id;
    const updated = await withTransaction(async (client) => {
      const tripRes = await client.query(
        `SELECT id, status FROM operations.trips WHERE id = $1 FOR UPDATE`,
        [tripId]
      );
      if (tripRes.rows.length === 0) throw AppError.notFound('Trip not found', ErrorCode.TRIP_NOT_FOUND);
      const trip = tripRes.rows[0];

      if (trip.status !== 'LOADED') {
        throw new AppError(`Cannot depart trip in status ${trip.status}. Must be LOADED first.`, 400, ErrorCode.INVALID_STATE_TRANSITION);
      }
      const res = await client.query(
        `UPDATE operations.trips SET status = 'EN_ROUTE_DELIVERY', version = version + 1, updated_at = clock_timestamp() WHERE id = $1 RETURNING *`,
        [tripId]
      );
      return res.rows[0];
    });
    return res.json(successResponse(updated));
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/trips/:id/complete
tripsRouter.post('/:id/complete', validateBody(transitionTripSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.id;

    const completed = await withTransaction(async (client) => {
      const tripRes = await client.query(
        `SELECT id, status, driver_id, booking_id FROM operations.trips WHERE id = $1 FOR UPDATE`,
        [tripId]
      );

      if (tripRes.rows.length === 0) {
        throw AppError.notFound('Trip not found', ErrorCode.TRIP_NOT_FOUND);
      }

      const trip = tripRes.rows[0];

      // Advance sequentially to COMPLETED if not already there
      const statusCycle = [
        'CREATED',
        'ASSIGNMENT_PENDING',
        'ASSIGNED',
        'DRIVER_ACCEPTED',
        'EN_ROUTE_PICKUP',
        'AT_PICKUP',
        'LOADING',
        'LOADED',
        'EN_ROUTE_DELIVERY',
        'AT_DELIVERY',
        'UNLOADING',
        'DELIVERED',
        'COMPLETED'
      ];
      const currIdx = statusCycle.indexOf(trip.status);
      const targetIdx = statusCycle.indexOf('COMPLETED');
      let result;

      if (currIdx !== -1 && currIdx < targetIdx) {
        for (let i = currIdx + 1; i <= targetIdx; i++) {
          const nextStatus = statusCycle[i];
          const isEnd = nextStatus === 'DELIVERED' || nextStatus === 'COMPLETED';
          await client.query(
            `UPDATE operations.trips
             SET status = $1::varchar,
                 actual_end_at = CASE WHEN $2::boolean THEN COALESCE(actual_end_at, clock_timestamp()) ELSE actual_end_at END,
                 version = version + 1,
                 updated_at = clock_timestamp()
             WHERE id = $3`,
            [nextStatus, isEnd, tripId]
          );
        }
      } else {
        result = await client.query(
          `UPDATE operations.trips
           SET status = 'COMPLETED',
               actual_end_at = COALESCE(actual_end_at, clock_timestamp()),
               version = version + 1,
               updated_at = clock_timestamp()
           WHERE id = $1
           RETURNING *`,
          [tripId]
        );
      }

      // Update booking status to COMPLETED
      await client.query(
        `UPDATE booking.bookings
         SET status = 'COMPLETED',
             updated_at = clock_timestamp()
         WHERE id = $1`,
        [trip.booking_id]
      );

      // Free driver
      if (trip.driver_id) {
        await client.query(
          `UPDATE fleet.drivers SET status = 'AVAILABLE' WHERE id = $1`,
          [trip.driver_id]
        );
      }

      // Publish outbox event
      await client.query(
        `INSERT INTO integration.outbox_events (
          event_type, aggregate_type, aggregate_id, payload
        ) VALUES ($1, $2, $3, $4)`,
        [
          'TRIP_COMPLETED',
          'TRIP',
          tripId,
          JSON.stringify({ trip_id: tripId, booking_id: trip.booking_id, completed_at: new Date().toISOString() })
        ]
      );

      const finalRes = await client.query(`SELECT * FROM operations.trips WHERE id = $1`, [tripId]);
      return finalRes.rows[0];
    });

    return res.json(successResponse(completed));
  } catch (err) {
    next(err);
  }
});
