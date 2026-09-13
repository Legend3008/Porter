import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { authenticateJwt } from '../../common/middleware/auth';
import { withTransaction, query } from '../../config/database';
import { AppError } from '../../common/errors/app-error';
import { ErrorCode } from '../../common/errors/error-codes';
import { successResponse } from '../../common/types/api-response';
import { validateBody } from '../../common/middleware/validate';
import { cacheSet, cachePublish } from '../../config/redis';

export const driverRouter = Router();
driverRouter.use(authenticateJwt);

// Helper to look up the driver record for the authenticated user
async function getDriverForUser(userId: string) {
  const res = await query(
    `SELECT d.*, u.full_name, u.phone, c.carrier_code, org.display_name as carrier_name
     FROM fleet.drivers d
     JOIN identity.users u ON u.id = d.user_id
     JOIN fleet.carriers c ON c.id = d.carrier_id
     JOIN party.organizations org ON org.id = c.organization_id
     WHERE d.user_id = $1`,
    [userId]
  );
  if (res.rows.length === 0) {
    throw new AppError('Driver profile not found for this account', 403, ErrorCode.FORBIDDEN);
  }
  return res.rows[0];
}

// GET /api/v1/driver/profile
driverRouter.get('/profile', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const driver = await getDriverForUser(req.user!.userId);

    // Also get vehicle assigned if any
    const vehicleRes = await query(
      `SELECT v.* FROM fleet.vehicles v 
       JOIN operations.trip_assignments ta ON ta.vehicle_id = v.id
       WHERE ta.driver_id = $1 AND ta.assignment_status = 'ACCEPTED'
       ORDER BY ta.assigned_at DESC LIMIT 1`,
      [driver.id]
    );

    return res.json(
      successResponse({
        driver_id: driver.id,
        driver_code: driver.driver_code,
        name: driver.full_name,
        phone: driver.phone,
        carrier: {
          id: driver.carrier_id,
          name: driver.carrier_name,
          code: driver.carrier_code
        },
        status: driver.status,
        rating: Number(driver.rating),
        verification_status: driver.verification_status,
        assigned_vehicle: vehicleRes.rows.length > 0 ? vehicleRes.rows[0] : null
      })
    );
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/driver/trips
driverRouter.get('/trips', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const driver = await getDriverForUser(req.user!.userId);
    const statusFilter = req.query.status as string;

    let queryText = `
      SELECT 
        t.id as trip_id,
        t.trip_number,
        t.status,
        t.scheduled_start_at,
        t.actual_start_at,
        t.actual_end_at,
        b.booking_number,
        s.cargo_description,
        s.commodity,
        b.total_amount_minor as total_amount_paise
      FROM operations.trips t
      JOIN booking.bookings b ON b.id = t.booking_id
      JOIN shipment.shipments s ON s.id = b.shipment_id
      WHERE t.driver_id = $1
    `;
    const params: any[] = [driver.id];

    if (statusFilter === 'active') {
      params.push(['DRIVER_ACCEPTED', 'EN_ROUTE_PICKUP', 'AT_PICKUP', 'LOADING', 'LOADED', 'EN_ROUTE_DELIVERY', 'AT_DELIVERY', 'UNLOADING']);
      queryText += ` AND t.status = ANY($2)`;
    } else if (statusFilter === 'completed') {
      params.push('COMPLETED');
      queryText += ` AND t.status = $2`;
    }

    queryText += ` ORDER BY t.created_at DESC LIMIT 50`;

    const result = await query(queryText, params);
    return res.json(successResponse(result.rows));
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/driver/job-offers
driverRouter.get('/job-offers', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const driver = await getDriverForUser(req.user!.userId);

    const result = await query(
      `SELECT 
        jo.id as offer_id,
        jo.trip_id,
        jo.offered_at,
        jo.expires_at,
        jo.status,
        t.trip_number,
        b.booking_number,
        s.cargo_description,
        s.commodity,
        b.total_amount_minor as payout_estimate_paise
      FROM operations.job_offers jo
      JOIN operations.trips t ON t.id = jo.trip_id
      JOIN booking.bookings b ON b.id = t.booking_id
      JOIN shipment.shipments s ON s.id = b.shipment_id
      WHERE jo.driver_id = $1 
        AND jo.status = 'OFFERED'
        AND jo.expires_at > clock_timestamp()
      ORDER BY jo.offered_at DESC`,
      [driver.id]
    );

    return res.json(successResponse(result.rows));
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/driver/job-offers/:id/accept
driverRouter.post('/job-offers/:id/accept', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const offerId = req.params.id;
    const driver = await getDriverForUser(req.user!.userId);

    const accepted = await withTransaction(async (client) => {
      // 1. Lock job offer
      const offerRes = await client.query(
        `SELECT id, trip_id, status, expires_at FROM operations.job_offers WHERE id = $1 FOR UPDATE`,
        [offerId]
      );

      if (offerRes.rows.length === 0) {
        throw AppError.notFound('Job offer not found', ErrorCode.NOT_FOUND);
      }

      const offer = offerRes.rows[0];
      if (offer.status !== 'OFFERED') {
        throw new AppError(`Job offer is already ${offer.status}`, 409, ErrorCode.CONFLICT);
      }

      if (new Date(offer.expires_at) < new Date()) {
        throw new AppError('Job offer has expired', 410, ErrorCode.VALIDATION_ERROR);
      }

      // 2. Lock trip and check status
      const tripRes = await client.query(
        `SELECT id, status, version FROM operations.trips WHERE id = $1 FOR UPDATE`,
        [offer.trip_id]
      );

      if (tripRes.rows.length === 0) {
        throw AppError.notFound('Trip not found', ErrorCode.TRIP_NOT_FOUND);
      }

      const trip = tripRes.rows[0];
      if (['DRIVER_ACCEPTED', 'EN_ROUTE_PICKUP', 'COMPLETED'].includes(trip.status)) {
        throw new AppError('Trip has already been accepted by another driver', 409, ErrorCode.TRIP_ALREADY_ACCEPTED);
      }

      // 3. Mark offer accepted
      await client.query(
        `UPDATE operations.job_offers 
         SET status = 'ACCEPTED', responded_at = clock_timestamp()
         WHERE id = $1`,
        [offerId]
      );

      // 4. Cancel competing offers for this trip
      await client.query(
        `UPDATE operations.job_offers 
         SET status = 'CANCELLED'
         WHERE trip_id = $1 AND id != $2 AND status = 'OFFERED'`,
        [offer.trip_id, offerId]
      );

      // 5. Update trip
      const updatedTrip = await client.query(
        `UPDATE operations.trips
         SET status = 'DRIVER_ACCEPTED',
             driver_id = $2,
             carrier_id = $3,
             version = version + 1,
             updated_at = clock_timestamp()
         WHERE id = $1
         RETURNING *`,
        [offer.trip_id, driver.id, driver.carrier_id]
      );

      // 6. Update driver status
      await client.query(
        `UPDATE fleet.drivers SET status = 'ON_TRIP' WHERE id = $1`,
        [driver.id]
      );

      return updatedTrip.rows[0];
    });

    return res.json(successResponse(accepted));
  } catch (err) {
    next(err);
  }
});

const locationPingSchema = z.object({
  trip_id: z.string().uuid(),
  vehicle_id: z.string().uuid().optional(),
  latitude: z.number().min(-90).max(90),
  longitude: z.number().min(-180).max(180),
  accuracy_meters: z.number().min(0).optional().default(5),
  speed_mps: z.number().min(0).optional().default(0),
  heading_degrees: z.number().min(0).max(360).optional().default(0),
  device_recorded_at: z.string().datetime().optional().default(() => new Date().toISOString()),
  battery_percent: z.number().int().min(0).max(100).optional()
});

// POST /api/v1/driver/location
driverRouter.post('/location', validateBody(locationPingSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const driver = await getDriverForUser(req.user!.userId);
    const ping = req.body;

    // Resolve vehicle_id if not supplied
    let vehicleId = ping.vehicle_id;
    if (!vehicleId) {
      const tripRes = await query(`SELECT vehicle_id FROM operations.trips WHERE id = $1`, [ping.trip_id]);
      vehicleId = tripRes.rows[0]?.vehicle_id;
      if (!vehicleId) {
        // Fallback to any vehicle of carrier
        const vRes = await query(`SELECT id FROM fleet.vehicles WHERE carrier_id = $1 LIMIT 1`, [driver.carrier_id]);
        vehicleId = vRes.rows[0]?.id;
      }
      if (!vehicleId) {
        const anyV = await query(`SELECT id FROM fleet.vehicles LIMIT 1`);
        vehicleId = anyV.rows[0]?.id;
      }
    }

    if (!vehicleId) {
      throw new AppError('No vehicle assigned to trip', 400, ErrorCode.VALIDATION_ERROR);
    }

    // Insert into partitioned tracking.location_pings
    // Trigger will automatically update tracking.latest_locations
    await query(
      `INSERT INTO tracking.location_pings (
        trip_id, driver_id, vehicle_id, latitude, longitude,
        accuracy_meters, speed_mps, heading_degrees,
        device_recorded_at, battery_percent, server_received_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, clock_timestamp())`,
      [
        ping.trip_id,
        driver.id,
        vehicleId,
        ping.latitude,
        ping.longitude,
        ping.accuracy_meters,
        ping.speed_mps,
        ping.heading_degrees,
        ping.device_recorded_at,
        ping.battery_percent || null
      ]
    );

    // Real-time Redis cache update & pub/sub broadcast for WebSocket streaming
    const payload = JSON.stringify({
      trip_id: ping.trip_id,
      driver_id: driver.id,
      latitude: ping.latitude,
      longitude: ping.longitude,
      speed_mps: ping.speed_mps,
      heading_degrees: ping.heading_degrees,
      recorded_at: ping.device_recorded_at,
      battery_percent: ping.battery_percent
    });

    await cacheSet(`geo:trip:${ping.trip_id}:latest`, payload, 3600);
    await cachePublish(`tracking:trip:${ping.trip_id}`, payload);

    return res.status(201).json(successResponse({ recorded: true, trip_id: ping.trip_id }));
  } catch (err) {
    next(err);
  }
});

const batchLocationSchema = z.object({
  trip_id: z.string().uuid(),
  pings: z.array(
    z.object({
      latitude: z.number().min(-90).max(90),
      longitude: z.number().min(-180).max(180),
      accuracy_meters: z.number().min(0).optional().default(5),
      speed_mps: z.number().min(0).optional().default(0),
      heading_degrees: z.number().min(0).max(360).optional().default(0),
      device_recorded_at: z.string().datetime(),
      battery_percent: z.number().int().min(0).max(100).optional()
    })
  ).min(1).max(500)
});

// POST /api/v1/driver/location/batch
driverRouter.post('/location/batch', validateBody(batchLocationSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const driver = await getDriverForUser(req.user!.userId);
    const { trip_id, pings } = req.body;

    const tripRes = await query(`SELECT vehicle_id FROM operations.trips WHERE id = $1`, [trip_id]);
    let vehicleId = tripRes.rows[0]?.vehicle_id;
    if (!vehicleId) {
      const anyV = await query(`SELECT id FROM fleet.vehicles LIMIT 1`);
      vehicleId = anyV.rows[0]?.id;
    }
    if (!vehicleId) {
      throw new AppError('Vehicle not assigned for trip', 400, ErrorCode.VALIDATION_ERROR);
    }

    await withTransaction(async (client) => {
      for (const p of pings) {
        await client.query(
          `INSERT INTO tracking.location_pings (
            trip_id, driver_id, vehicle_id, latitude, longitude,
            accuracy_meters, speed_mps, heading_degrees,
            device_recorded_at, battery_percent, server_received_at
          ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, clock_timestamp())`,
          [
            trip_id,
            driver.id,
            vehicleId,
            p.latitude,
            p.longitude,
            p.accuracy_meters,
            p.speed_mps,
            p.heading_degrees,
            p.device_recorded_at,
            p.battery_percent || null
          ]
        );
      }
    });

    // Update cache with the latest ping in batch
    const latest = pings[pings.length - 1];
    const payload = JSON.stringify({
      trip_id,
      driver_id: driver.id,
      latitude: latest.latitude,
      longitude: latest.longitude,
      speed_mps: latest.speed_mps,
      heading_degrees: latest.heading_degrees,
      recorded_at: latest.device_recorded_at
    });
    await cacheSet(`geo:trip:${trip_id}:latest`, payload, 3600);
    await cachePublish(`tracking:trip:${trip_id}`, payload);

    return res.status(201).json(successResponse({ count: pings.length, trip_id }));
  } catch (err) {
    next(err);
  }
});

const podSubmissionSchema = z.object({
  receiver_name: z.string().min(2),
  receiver_phone: z.string().optional(),
  signature_storage_key: z.string().optional(),
  document_id: z.string().uuid().optional(),
  latitude: z.number().min(-90).max(90),
  longitude: z.number().min(-180).max(180),
  device_recorded_at: z.string().datetime().optional().default(() => new Date().toISOString())
});

// POST /api/v1/driver/trips/:id/pod
driverRouter.post('/trips/:id/pod', validateBody(podSubmissionSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.id;
    const driver = await getDriverForUser(req.user!.userId);
    const body = req.body;

    const pod = await withTransaction(async (client) => {
      // 1. Verify trip
      const tripRes = await client.query(
        `SELECT id, status, booking_id FROM operations.trips WHERE id = $1 FOR UPDATE`,
        [tripId]
      );

      if (tripRes.rows.length === 0) {
        throw AppError.notFound('Trip not found', ErrorCode.TRIP_NOT_FOUND);
      }

      const trip = tripRes.rows[0];

      // 2. Insert POD Record
      const podRes = await client.query(
        `INSERT INTO documents.pod_records (
          trip_id, document_id, receiver_name, receiver_phone,
          signature_storage_key, latitude, longitude,
          device_recorded_at, captured_by_driver_id, verification_status
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 'ACCEPTED')
        RETURNING *`,
        [
          tripId,
          body.document_id || null,
          body.receiver_name,
          body.receiver_phone || null,
          body.signature_storage_key || null,
          body.latitude,
          body.longitude,
          body.device_recorded_at,
          driver.id
        ]
      );

      // 3. Transition trip status sequentially to DELIVERED
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
        'DELIVERED'
      ];
      const currIdx = statusCycle.indexOf(trip.status);
      const targetIdx = statusCycle.indexOf('DELIVERED');
      if (currIdx !== -1 && currIdx < targetIdx) {
        for (let i = currIdx + 1; i <= targetIdx; i++) {
          const nextStatus = statusCycle[i];
          const isDelivered = nextStatus === 'DELIVERED';
          await client.query(
            `UPDATE operations.trips
             SET status = $1::varchar,
                 actual_end_at = CASE WHEN $2::boolean THEN COALESCE(actual_end_at, clock_timestamp()) ELSE actual_end_at END,
                 version = version + 1,
                 updated_at = clock_timestamp()
             WHERE id = $3`,
            [nextStatus, isDelivered, tripId]
          );
        }
      } else if (trip.status !== 'DELIVERED') {
        await client.query(
          `UPDATE operations.trips
           SET status = 'DELIVERED',
               actual_end_at = COALESCE(actual_end_at, clock_timestamp()),
               version = version + 1,
               updated_at = clock_timestamp()
           WHERE id = $1`,
          [tripId]
        );
      }

      // 4. Update booking status to DELIVERED
      await client.query(
        `UPDATE booking.bookings
         SET status = 'DELIVERED',
             updated_at = clock_timestamp()
         WHERE id = $1`,
        [trip.booking_id]
      );

      // 5. Outbox event
      await client.query(
        `INSERT INTO integration.outbox_events (
          event_type, aggregate_type, aggregate_id, payload
        ) VALUES ($1, $2, $3, $4)`,
        [
          'POD_SUBMITTED',
          'TRIP',
          tripId,
          JSON.stringify({
            trip_id: tripId,
            pod_id: podRes.rows[0].id,
            receiver_name: body.receiver_name,
            delivered_at: new Date().toISOString()
          })
        ]
      );

      return podRes.rows[0];
    });

    return res.status(201).json(successResponse(pod));
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/driver/earnings (direct driver alias)
driverRouter.get('/earnings', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const driver = await getDriverForUser(req.user!.userId);

    const statsRes = await query(
      `SELECT 
        COUNT(t.id) as completed_trips_count,
        COALESCE(SUM(b.total_amount_minor), 0) as total_trip_value_paise
       FROM operations.trips t
       JOIN booking.bookings b ON b.id = t.booking_id
       WHERE t.driver_id = $1 AND t.status IN ('DELIVERED', 'COMPLETED')`,
      [driver.id]
    );

    const stats = statsRes.rows[0];
    const totalValue = BigInt(stats.total_trip_value_paise);
    const estimatedEarningsPaise = (totalValue * 70n) / 100n;

    return res.json(
      successResponse({
        driver_id: driver.id,
        completed_trips_count: Number(stats.completed_trips_count),
        total_trip_value_paise: Number(totalValue),
        estimated_earnings_paise: Number(estimatedEarningsPaise)
      })
    );
  } catch (err) {
    next(err);
  }
});

