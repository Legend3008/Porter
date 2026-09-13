import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { authenticateJwt } from '../../common/middleware/auth';
import { requireRoles } from '../../common/middleware/rbac';
import { query, withTransaction } from '../../config/database';
import { AppError } from '../../common/errors/app-error';
import { ErrorCode } from '../../common/errors/error-codes';
import { successResponse } from '../../common/types/api-response';
import { validateBody } from '../../common/middleware/validate';

export const adminRouter = Router();
adminRouter.use(authenticateJwt);
adminRouter.use(requireRoles(['ADMIN', 'OPERATOR']));

// GET /api/v1/admin/trips
adminRouter.get('/trips', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const status = req.query.status as string;
    const limit = Math.min(Number(req.query.limit) || 50, 100);

    let sql = `
      SELECT 
        t.id,
        t.trip_number,
        t.status,
        t.scheduled_start_at,
        t.actual_start_at,
        t.created_at,
        b.booking_number,
        s.cargo_description,
        s.commodity,
        b.total_amount_minor as total_amount_paise,
        u.full_name as driver_name,
        u.phone as driver_phone,
        v.registration_number as vehicle_number,
        org.display_name as customer_name,
        corg.display_name as carrier_name
      FROM operations.trips t
      JOIN booking.bookings b ON b.id = t.booking_id
      JOIN shipment.shipments s ON s.id = b.shipment_id
      JOIN party.organizations org ON org.id = b.customer_organization_id
      LEFT JOIN fleet.drivers d ON d.id = t.driver_id
      LEFT JOIN identity.users u ON u.id = d.user_id
      LEFT JOIN fleet.vehicles v ON v.id = t.vehicle_id
      LEFT JOIN fleet.carriers c ON c.id = t.carrier_id
      LEFT JOIN party.organizations corg ON corg.id = c.organization_id
    `;
    const params: any[] = [];

    if (status) {
      sql += ` WHERE t.status = $1`;
      params.push(status);
    }

    sql += ` ORDER BY t.created_at DESC LIMIT ${limit}`;

    const result = await query(sql, params);
    return res.json(successResponse(result.rows));
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/admin/exceptions
adminRouter.get('/exceptions', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const status = (req.query.status as string) || 'OPEN';

    const result = await query(
      `SELECT 
        e.*,
        u.full_name as assigned_to_name
       FROM operations.exceptions e
       LEFT JOIN identity.users u ON u.id = e.assigned_to
       WHERE e.status = $1
       ORDER BY e.detected_at DESC LIMIT 50`,
      [status]
    );

    return res.json(successResponse(result.rows));
  } catch (err) {
    next(err);
  }
});

const overrideSchema = z.object({
  new_status: z.enum([
    'CREATED', 'ASSIGNMENT_PENDING', 'ASSIGNED', 'DRIVER_ACCEPTED',
    'EN_ROUTE_PICKUP', 'AT_PICKUP', 'LOADING', 'LOADED',
    'EN_ROUTE_DELIVERY', 'AT_DELIVERY', 'UNLOADING', 'DELIVERED',
    'COMPLETED', 'CANCELLED', 'FAILED'
  ]).optional(),
  reassign_driver_id: z.string().uuid().optional(),
  reassign_vehicle_id: z.string().uuid().optional(),
  reason: z.string().min(5)
});

// POST /api/v1/admin/trips/:id/override
// Administrative state intervention with mandatory audit log trail
adminRouter.post('/trips/:id/override', validateBody(overrideSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tripId = req.params.id;
    const { new_status, reassign_driver_id, reassign_vehicle_id, reason } = req.body;
    const userId = req.user!.userId;

    const overridden = await withTransaction(async (client) => {
      const tripRes = await client.query(
        `SELECT * FROM operations.trips WHERE id = $1 FOR UPDATE`,
        [tripId]
      );

      if (tripRes.rows.length === 0) {
        throw AppError.notFound('Trip not found', ErrorCode.TRIP_NOT_FOUND);
      }

      const beforeData = tripRes.rows[0];

      const updateTrip = await client.query(
        `UPDATE operations.trips
         SET status = COALESCE($2, status),
             driver_id = COALESCE($3, driver_id),
             vehicle_id = COALESCE($4, vehicle_id),
             version = version + 1,
             updated_at = clock_timestamp()
         WHERE id = $1
         RETURNING *`,
        [tripId, new_status || null, reassign_driver_id || null, reassign_vehicle_id || null]
      );

      const afterData = updateTrip.rows[0];

      // Insert Immutable Audit Log
      await client.query(
        `INSERT INTO audit.audit_logs (
          actor_user_id, actor_type, action, entity_type, entity_id,
          request_id, before_data, after_data, metadata
        ) VALUES ($1, 'ADMIN', 'TRIP_OVERRIDE', 'TRIP', $2, $3, $4, $5, $6)`,
        [
          userId,
          tripId,
          req.headers['x-request-id'] || null,
          JSON.stringify(beforeData),
          JSON.stringify(afterData),
          JSON.stringify({ reason })
        ]
      );

      return afterData;
    });

    return res.json(successResponse(overridden));
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/admin/audit-logs
adminRouter.get('/audit-logs', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const entityType = req.query.entity_type as string;
    const entityId = req.query.entity_id as string;
    const limit = Math.min(Number(req.query.limit) || 50, 100);

    let sql = `
      SELECT 
        a.id,
        a.action,
        a.entity_type,
        a.entity_id,
        a.actor_type,
        a.request_id,
        a.metadata,
        a.created_at,
        u.full_name as actor_name,
        u.email as actor_email
      FROM audit.audit_logs a
      LEFT JOIN identity.users u ON u.id = a.actor_user_id
    `;
    const params: any[] = [];

    if (entityType) {
      params.push(entityType);
      sql += ` WHERE a.entity_type = $${params.length}`;
    }

    if (entityId) {
      params.push(entityId);
      sql += params.length > 1 ? ` AND a.entity_id = $${params.length}` : ` WHERE a.entity_id = $${params.length}`;
    }

    sql += ` ORDER BY a.created_at DESC LIMIT ${limit}`;

    const result = await query(sql, params);
    return res.json(successResponse(result.rows));
  } catch (err) {
    next(err);
  }
});
