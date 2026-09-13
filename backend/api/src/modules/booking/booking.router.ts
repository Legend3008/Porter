import { Router, Request, Response, NextFunction } from "express";
import { z } from "zod";
import { query, withTransaction } from "../../config/database.js";
import { AppError } from "../../common/errors/app-error.js";
import { ErrorCodes } from "../../common/errors/error-codes.js";
import { validate } from "../../common/middleware/validate.js";
import { optionalAuthenticate, authenticate } from "../../common/middleware/auth.js";
import { idempotencyMiddleware } from "../../common/middleware/idempotency.js";
import { formatResponse } from "../../common/types/api-response.js";

export const bookingRouter = Router();

const createDraftSchema = z.object({
  origin_port: z.string().default("JNPT (Nhava Sheva)"),
  destination_port: z.string().default("Talegaon ICD, Pune"),
  pickup_address: z.object({
    line1: z.string(),
    city: z.string(),
    state: z.string(),
    pincode: z.string(),
  }).optional(),
  delivery_address: z.object({
    line1: z.string(),
    city: z.string(),
    state: z.string(),
    pincode: z.string(),
  }).optional(),
  container_type: z.string().default("HIGH_CUBE_40FT"),
  container_count: z.number().int().min(1).default(1),
  cargo_weight_kg: z.number().positive().default(18500),
  commodity: z.string().default("General Cargo"),
  is_hazardous: z.boolean().default(false),
});

const confirmBookingSchema = z.object({
  draft_id: z.string().optional(),
  booking_id: z.string().optional(),
  quote_id: z.string().optional(),
  payment_method: z.string().default("NETBANKING"),
});

// GET /api/v1/bookings
bookingRouter.get("/", optionalAuthenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const user = (req as any).user;
    const organizationId = user?.organization_id || "10000000-0000-0000-0001-000000000001";
    const { status } = req.query;

    let sql = `
      SELECT b.id, b.booking_number as "referenceNumber", b.status,
             b.total_amount_minor as "totalAmountPaise", b.paid_amount_minor as "paidAmountPaise",
             b.created_at as "createdAt", b.updated_at as "updatedAt",
             s.cargo_description, s.commodity, s.cargo_weight_kg,
             t.id as "assignedTripId",
             c.container_type as "containerType"
      FROM booking.bookings b
      JOIN shipment.shipments s ON s.id = b.shipment_id
      LEFT JOIN shipment.shipment_containers sc ON sc.shipment_id = s.id
      LEFT JOIN shipment.containers c ON c.id = sc.container_id
      LEFT JOIN operations.trips t ON t.booking_id = b.id
      WHERE b.customer_organization_id = $1
    `;
    const params: any[] = [organizationId];

    if (status) {
      sql += ` AND b.status = $2`;
      params.push(status);
    }

    sql += ` ORDER BY b.created_at DESC LIMIT 50`;

    const result = await query(sql, params);

    // Map to Booking.kt compatible items
    const items = result.rows.map((r) => ({
      id: r.id,
      referenceNumber: r.referenceNumber,
      status: r.status,
      originPort: "JNPT (Nhava Sheva)",
      destinationPort: "Talegaon ICD, Pune",
      containerType: r.containerType || "HIGH_CUBE_40FT",
      containerCount: 1,
      totalAmountPaise: Number(r.totalAmountPaise),
      paidAmountPaise: Number(r.paidAmountPaise),
      assignedTripId: r.assignedTripId,
      createdAt: r.createdAt,
      updatedAt: r.updatedAt,
      estimatedDeliveryAt: new Date(Date.now() + 86400000).toISOString(),
    }));

    return res.json(formatResponse(req, items));
  } catch (error) {
    next(error);
  }
});

// GET /api/v1/bookings/:id
bookingRouter.get("/:id", optionalAuthenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { id } = req.params;
    const user = (req as any).user;
    const organizationId = user?.organization_id || "10000000-0000-0000-0001-000000000001";

    const result = await query(
      `SELECT b.id, b.booking_number as "referenceNumber", b.status, b.quote_id,
              b.total_amount_minor as "totalAmountPaise", b.paid_amount_minor as "paidAmountPaise",
              b.created_at as "createdAt", b.updated_at as "updatedAt",
              s.cargo_description, s.commodity, s.cargo_weight_kg as "totalWeightKg", s.is_hazardous as "isHazardous",
              t.id as "assignedTripId"
       FROM booking.bookings b
       JOIN shipment.shipments s ON s.id = b.shipment_id
       LEFT JOIN operations.trips t ON t.booking_id = b.id
       WHERE (b.id = $1 OR b.booking_number = $1) AND b.customer_organization_id = $2`,
      [id, organizationId]
    );

    if (result.rows.length === 0) {
      throw AppError.notFound("Booking not found", ErrorCodes.RESOURCE_NOT_FOUND);
    }

    const b = result.rows[0];
    const bookingData = {
      id: b.id,
      referenceNumber: b.referenceNumber,
      status: b.status,
      quoteId: b.quote_id,
      totalAmountPaise: Number(b.totalAmountPaise),
      paidAmountPaise: Number(b.paidAmountPaise),
      assignedTripId: b.assignedTripId,
      createdAt: b.createdAt,
      updatedAt: b.updatedAt,
      estimatedPickupAt: new Date(Date.now() + 14400000).toISOString(),
      estimatedDeliveryAt: new Date(Date.now() + 86400000).toISOString(),
      shipmentDetails: {
        originPort: "JNPT (Nhava Sheva)",
        destinationPort: "Talegaon ICD, Pune",
        pickupAddress: {
          line1: "GTI Terminal Gate 2, Nhava Sheva",
          city: "Navi Mumbai",
          state: "Maharashtra",
          pincode: "400707",
          country: "India",
        },
        deliveryAddress: {
          line1: "Plot 42, Talegaon MIDC Auto Cluster",
          city: "Pune",
          state: "Maharashtra",
          pincode: "410507",
          country: "India",
        },
        containerType: "HIGH_CUBE_40FT",
        containerCount: 1,
        totalWeightKg: Number(b.totalWeightKg),
        commodity: b.commodity,
        isHazardous: b.isHazardous,
        scheduledDate: new Date().toISOString(),
      },
    };

    return res.json(formatResponse(req, bookingData));
  } catch (error) {
    next(error);
  }
});

// POST /api/v1/bookings/drafts
bookingRouter.post(
  "/drafts",
  optionalAuthenticate,
  idempotencyMiddleware,
  validate({ body: createDraftSchema }),
  async (req: Request, res: Response, next: NextFunction) => {
    try {
      const user = (req as any).user;
      const organizationId = user?.organization_id || "10000000-0000-0000-0001-000000000001";
      const { cargo_weight_kg, commodity, is_hazardous } = req.body;

      return await withTransaction(async (client) => {
        // 1. Create shipment draft
        const refNumber = `SHP-${Date.now().toString().slice(-8)}`;
        const shipmentRes = await client.query(
          `INSERT INTO shipment.shipments (
             customer_organization_id, reference_number, cargo_description,
             cargo_weight_kg, commodity, is_hazardous, status
           ) VALUES ($1, $2, $3, $4, $5, $6, 'DRAFT')
           RETURNING id`,
          [organizationId, refNumber, `Container Cargo: ${commodity}`, cargo_weight_kg, commodity, is_hazardous]
        );
        const shipmentId = shipmentRes.rows[0].id;

        // 2. Fetch active pricing rule version
        const versionRes = await client.query(
          `SELECT id FROM pricing.pricing_rule_versions WHERE status = 'ACTIVE' LIMIT 1`
        );
        const versionId = versionRes.rows[0]?.id || "60000000-0000-0000-0001-000000000002";

        // 3. Create quote (₹45,500 subtotal + ₹8,190 GST = ₹53,690 in paise)
        const quoteNumber = `QT-${Date.now().toString().slice(-8)}`;
        const expiresAt = new Date(Date.now() + 15 * 60 * 1000); // 15 mins lock

        const quoteRes = await client.query(
          `INSERT INTO pricing.quotes (
             quote_number, customer_organization_id, shipment_id, pricing_rule_version_id,
             subtotal_minor, tax_minor, total_minor, currency, status, expires_at
           ) VALUES ($1, $2, $3, $4, 4550000, 819000, 5369000, 'INR', 'ACTIVE', $5)
           RETURNING id, quote_number, subtotal_minor, tax_minor, total_minor, expires_at`,
          [quoteNumber, organizationId, shipmentId, versionId, expiresAt.toISOString()]
        );
        const quote = quoteRes.rows[0];

        // 4. Create Booking in DRAFT status
        const bookingNumber = `BK-${Date.now().toString().slice(-8)}`;
        const bookingRes = await client.query(
          `INSERT INTO booking.bookings (
             booking_number, customer_organization_id, shipment_id, quote_id,
             status, total_amount_minor, paid_amount_minor, currency
           ) VALUES ($1, $2, $3, $4, 'DRAFT', 5369000, 0, 'INR')
           RETURNING id, booking_number, status`,
          [bookingNumber, organizationId, shipmentId, quote.id]
        );
        const booking = bookingRes.rows[0];

        return res.status(201).json(
          formatResponse(req, {
            draft_id: booking.id,
            booking_id: booking.id,
            reference_number: booking.booking_number,
            quote_id: quote.id,
            quote: {
              id: quote.id,
              booking_draft_id: booking.id,
              base_fare_paise: 3500000,
              fuel_surcharge_paise: 480000,
              port_handling_paise: 350000,
              other_surcharge_paise: 220000,
              subtotal_paise: 4550000,
              gst_paise: 819000,
              total_amount_paise: 5369000,
              gst_rate: 18.0,
              valid_until: quote.expires_at,
            },
          })
        );
      });
    } catch (error) {
      next(error);
    }
  }
);

// GET /api/v1/bookings/drafts/:draftId/quote
bookingRouter.get("/drafts/:draftId/quote", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { draftId } = req.params;
    const resQuote = await query(
      `SELECT q.*, b.id as booking_id
       FROM booking.bookings b
       JOIN pricing.quotes q ON q.id = b.quote_id
       WHERE b.id = $1`,
      [draftId]
    );

    if (resQuote.rows.length === 0) {
      throw AppError.notFound("Quote for draft not found", ErrorCodes.RESOURCE_NOT_FOUND);
    }

    const q = resQuote.rows[0];
    return res.json(
      formatResponse(req, {
        id: q.id,
        bookingDraftId: q.booking_id,
        baseFarePaise: 3500000,
        fuelSurchargePaise: 480000,
        portHandlingPaise: 350000,
        otherSurchargePaise: 220000,
        subtotalPaise: Number(q.subtotal_minor),
        gstPaise: Number(q.tax_minor),
        totalAmountPaise: Number(q.total_minor),
        gstRate: 18.0,
        validUntil: q.expires_at,
        currency: "INR",
      })
    );
  } catch (error) {
    next(error);
  }
});

// POST /api/v1/bookings/confirm
bookingRouter.post(
  "/confirm",
  optionalAuthenticate,
  idempotencyMiddleware,
  validate({ body: confirmBookingSchema }),
  async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { draft_id, booking_id, quote_id } = req.body;
      let targetId = booking_id || draft_id;

      return await withTransaction(async (client) => {
        if (!targetId && quote_id) {
          const bCheck = await client.query(
            `SELECT id FROM booking.bookings WHERE quote_id = $1 LIMIT 1`,
            [quote_id]
          );
          if (bCheck.rows.length > 0) {
            targetId = bCheck.rows[0].id;
          } else {
            // Fetch quote
            const qRes = await client.query(`SELECT * FROM pricing.quotes WHERE id = $1`, [quote_id]);
            if (qRes.rows.length === 0) {
              throw AppError.notFound("Quote not found", ErrorCodes.RESOURCE_NOT_FOUND);
            }
            const q = qRes.rows[0];
            let shipmentId = q.shipment_id;
            const boundCheck = await client.query(`SELECT id FROM booking.bookings WHERE shipment_id = $1`, [shipmentId]);
            if (boundCheck.rows.length > 0) {
              const refNumber = `SHP-${Date.now().toString().slice(-8)}-${Math.floor(Math.random() * 1000)}`;
              const newShipment = await client.query(
                `INSERT INTO shipment.shipments (
                   customer_organization_id, reference_number, cargo_description,
                   cargo_weight_kg, commodity, is_hazardous, status
                 ) VALUES ($1, $2, $3, $4, $5, $6, 'BOOKED')
                 RETURNING id`,
                [q.customer_organization_id, refNumber, 'Container Cargo', 24000, 'Freight', false]
              );
              shipmentId = newShipment.rows[0].id;
            }

            const bookingNumber = `BK-${Date.now().toString().slice(-8)}`;
            const createB = await client.query(
              `INSERT INTO booking.bookings (
                booking_number, customer_organization_id, shipment_id, quote_id,
                status, total_amount_minor, paid_amount_minor, currency
              ) VALUES ($1, $2, $3, $4, 'DRAFT', $5, 0, 'INR')
              RETURNING id`,
              [bookingNumber, q.customer_organization_id, shipmentId, quote_id, q.total_minor]
            );
            targetId = createB.rows[0].id;
          }
        }

        if (!targetId) {
          throw AppError.badRequest("booking_id, draft_id, or quote_id is required", ErrorCodes.VALIDATION_ERROR);
        }

        // 1. Lock booking and verify
        const bookRes = await client.query(
          `SELECT b.*, q.expires_at, q.status as quote_status
           FROM booking.bookings b
           JOIN pricing.quotes q ON q.id = b.quote_id
           WHERE b.id = $1 FOR UPDATE`,
          [targetId]
        );

        if (bookRes.rows.length === 0) {
          throw AppError.notFound("Booking not found", ErrorCodes.RESOURCE_NOT_FOUND);
        }

        const b = bookRes.rows[0];

        // Verify quote not expired
        if (new Date(b.expires_at) < new Date()) {
          throw AppError.badRequest("Quote has expired. Please refresh freight quote.", ErrorCodes.QUOTE_EXPIRED);
        }

        // 2. Update booking to CONFIRMED
        await client.query(
          `UPDATE booking.bookings 
           SET status = 'CONFIRMED', confirmed_at = clock_timestamp(), updated_at = clock_timestamp()
           WHERE id = $1`,
          [targetId]
        );

        // 3. Create or update Trip record
        const tripNumber = `TR-${Date.now().toString().slice(-8)}`;
        const tripRes = await client.query(
          `INSERT INTO operations.trips (
             trip_number, booking_id, status, scheduled_start_at
           ) VALUES ($1, $2, 'ASSIGNED', clock_timestamp() + interval '2 hours')
           ON CONFLICT (booking_id) DO UPDATE SET status = 'ASSIGNED'
           RETURNING id, trip_number`,
          [tripNumber, targetId]
        );
        const trip = tripRes.rows[0];

        // Ensure origin and destination trip stops exist
        const originLocRes = await client.query(`SELECT id FROM geo.locations WHERE code = 'LOC-INNSA' LIMIT 1`);
        const destLocRes = await client.query(`SELECT id FROM geo.locations WHERE code = 'LOC-PUNE-ICD' LIMIT 1`);
        const originId = originLocRes.rows[0]?.id;
        const destId = destLocRes.rows[0]?.id;

        if (originId && destId) {
          await client.query(
            `INSERT INTO operations.trip_stops (trip_id, sequence, stop_type, location_id, status)
             VALUES 
               ($1, 1, 'ORIGIN', $2, 'PENDING'),
               ($1, 2, 'DESTINATION', $3, 'PENDING')
             ON CONFLICT (trip_id, sequence) DO NOTHING`,
            [trip.id, originId, destId]
          );
        }

        // Ensure at least one container is linked to shipment
        const cCheck = await client.query(
          `SELECT id FROM shipment.shipment_containers WHERE shipment_id = $1 LIMIT 1`,
          [b.shipment_id]
        );
        if (cCheck.rows.length === 0) {
          const containerRes = await client.query(`SELECT id FROM shipment.containers LIMIT 1`);
          const cId = containerRes.rows[0]?.id;
          if (cId) {
            await client.query(
              `INSERT INTO shipment.shipment_containers (shipment_id, container_id, sequence, seal_number, status)
               VALUES ($1, $2, 1, $3, 'ASSIGNED')
               ON CONFLICT DO NOTHING`,
              [b.shipment_id, cId, `SEAL-${Date.now().toString().slice(-6)}`]
            );
          }
        }

        // 4. Insert Transactional Outbox Event
        await client.query(
          `INSERT INTO integration.outbox_events (
             event_type, aggregate_type, aggregate_id, payload, status
           ) VALUES (
             'booking.confirmed.v1', 'BOOKING', $1,
             $2::jsonb, 'PENDING'
           )`,
          [targetId, JSON.stringify({ booking_id: targetId, trip_id: trip.id, status: "CONFIRMED" })]
        );

        return res.status(201).json(
          formatResponse(req, {
            booking_id: targetId,
            reference_number: b.booking_number,
            status: "CONFIRMED",
            payment_status: "PENDING",
            trip_id: trip.id,
            trip_number: trip.trip_number,
            message: "Booking confirmed successfully. Haulage trip dispatched.",
          })
        );
      });
    } catch (error) {
      next(error);
    }
  }
);
