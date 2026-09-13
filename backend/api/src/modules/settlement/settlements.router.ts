import { Router, Request, Response, NextFunction } from 'express';
import { authenticateJwt } from '../../common/middleware/auth';
import { query } from '../../config/database';
import { AppError } from '../../common/errors/app-error';
import { ErrorCode } from '../../common/errors/error-codes';
import { successResponse } from '../../common/types/api-response';

export const settlementsRouter = Router();
settlementsRouter.use(authenticateJwt);

// GET /api/v1/carrier/settlements
settlementsRouter.get('/carrier/settlements', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const orgId = req.user?.organizationId;
    const role = req.user?.role;

    let sql = `
      SELECT 
        s.id,
        s.settlement_number,
        s.period_start,
        s.period_end,
        s.gross_amount_minor as gross_amount_paise,
        s.deductions_minor as deductions_paise,
        s.tds_tax_minor as tds_tax_paise,
        s.net_amount_minor as net_amount_paise,
        s.currency,
        s.status,
        s.created_at,
        c.carrier_code,
        org.display_name as carrier_name
      FROM settlement.settlements s
      JOIN fleet.carriers c ON c.id = s.carrier_id
      JOIN party.organizations org ON org.id = c.organization_id
    `;
    const params: any[] = [];

    if (role !== 'ADMIN' && role !== 'OPERATOR' && orgId) {
      sql += ` WHERE c.organization_id = $1`;
      params.push(orgId);
    }

    sql += ` ORDER BY s.period_end DESC LIMIT 50`;

    const result = await query(sql, params);
    return res.json(successResponse(result.rows));
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/carrier/settlements/:id
settlementsRouter.get('/carrier/settlements/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const settlementId = req.params.id;

    const sRes = await query(
      `SELECT s.*, c.carrier_code, org.display_name as carrier_name
       FROM settlement.settlements s
       JOIN fleet.carriers c ON c.id = s.carrier_id
       JOIN party.organizations org ON org.id = c.organization_id
       WHERE s.id = $1`,
      [settlementId]
    );

    if (sRes.rows.length === 0) {
      throw AppError.notFound('Settlement not found', ErrorCode.NOT_FOUND);
    }

    const settlement = sRes.rows[0];

    // Fetch line items
    const lines = await query(
      `SELECT 
        sl.id,
        sl.trip_id,
        sl.base_freight_minor as base_freight_paise,
        sl.fuel_allowance_minor as fuel_allowance_paise,
        sl.toll_reimbursement_minor as toll_reimbursement_paise,
        sl.detention_allowance_minor as detention_allowance_paise,
        sl.penalty_deduction_minor as penalty_deduction_paise,
        sl.total_line_minor as total_line_paise,
        t.trip_number,
        b.booking_number
       FROM settlement.settlement_lines sl
       JOIN operations.trips t ON t.id = sl.trip_id
       JOIN booking.bookings b ON b.id = t.booking_id
       WHERE sl.settlement_id = $1`,
      [settlementId]
    );

    return res.json(
      successResponse({
        id: settlement.id,
        settlement_number: settlement.settlement_number,
        carrier_name: settlement.carrier_name,
        carrier_code: settlement.carrier_code,
        period_start: settlement.period_start,
        period_end: settlement.period_end,
        gross_amount_paise: Number(settlement.gross_amount_minor),
        deductions_paise: Number(settlement.deductions_minor),
        tds_tax_paise: Number(settlement.tds_tax_minor),
        net_amount_paise: Number(settlement.net_amount_minor),
        currency: settlement.currency,
        status: settlement.status,
        lines: lines.rows
      })
    );
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/driver/earnings
settlementsRouter.get('/driver/earnings', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const userId = req.user!.userId;

    const driverRes = await query(
      `SELECT id FROM fleet.drivers WHERE user_id = $1`,
      [userId]
    );

    if (driverRes.rows.length === 0) {
      throw new AppError('Driver profile not found', 403, ErrorCode.FORBIDDEN);
    }

    const driverId = driverRes.rows[0].id;

    // Roll up completed trips
    const statsRes = await query(
      `SELECT 
        COUNT(t.id) as completed_trips_count,
        COALESCE(SUM(b.total_amount_minor), 0) as total_trip_value_paise
       FROM operations.trips t
       JOIN booking.bookings b ON b.id = t.booking_id
       WHERE t.driver_id = $1 AND t.status IN ('DELIVERED', 'COMPLETED')`,
      [driverId]
    );

    const stats = statsRes.rows[0];
    const totalValue = BigInt(stats.total_trip_value_paise);
    // Typical driver commission: 70% of haulage value
    const estimatedEarningsPaise = (totalValue * 70n) / 100n;

    return res.json(
      successResponse({
        driver_id: driverId,
        completed_trips_count: Number(stats.completed_trips_count),
        total_trip_value_paise: Number(totalValue),
        estimated_earnings_paise: Number(estimatedEarningsPaise)
      })
    );
  } catch (err) {
    next(err);
  }
});
