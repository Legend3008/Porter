import { Router, Request, Response, NextFunction } from "express";
import { z } from "zod";
import { query } from "../../config/database.js";
import { AppError } from "../../common/errors/app-error.js";
import { ErrorCodes } from "../../common/errors/error-codes.js";
import { validate } from "../../common/middleware/validate.js";
import { optionalAuthenticate } from "../../common/middleware/auth.js";
import { formatResponse } from "../../common/types/api-response.js";

export const pricingRouter = Router();

const calculateQuoteSchema = z.object({
  origin_port: z.string().optional(),
  origin_port_code: z.string().optional(),
  destination_port: z.string().optional(),
  destination_city: z.string().optional(),
  container_type: z.string().default("HIGH_CUBE_40FT"),
  container_count: z.number().int().min(1).default(1),
  cargo_weight_kg: z.number().positive().default(18500),
  commodity: z.string().default("General Cargo"),
  is_hazardous: z.boolean().default(false),
  is_overweight: z.boolean().optional(),
  scheduled_date: z.string().optional(),
});

// POST /api/v1/quotes
pricingRouter.post(
  "/quotes",
  optionalAuthenticate,
  validate({ body: calculateQuoteSchema }),
  async (req: Request, res: Response, next: NextFunction) => {
    try {
      const {
        container_type,
        container_count,
        cargo_weight_kg,
        is_hazardous,
        commodity = "General Cargo",
      } = req.body;

      const user = (req as any).user;
      const organizationId = user?.organization_id || "10000000-0000-0000-0001-000000000001";

      // 1. Fetch active pricing rule version
      const versionRes = await query(
        `SELECT id FROM pricing.pricing_rule_versions WHERE status = 'ACTIVE' ORDER BY effective_from DESC LIMIT 1`
      );
      const versionId = versionRes.rows[0]?.id || "60000000-0000-0000-0001-000000000002";

      // 2. Compute components strictly in Integer Paise
      let baseFarePerUnit = 3500000n; // ₹35,000.00
      if (container_type.includes("20")) {
        baseFarePerUnit = 2400000n; // ₹24,000.00
      } else if (container_type.includes("REEFER")) {
        baseFarePerUnit = 5500000n; // ₹55,000.00
      }

      const count = BigInt(container_count);
      const baseFare = baseFarePerUnit * count;
      const portHandling = 350000n * count; // ₹3,500.00
      const tollCharges = 220000n * count;  // ₹2,200.00
      const fuelSurcharge = (baseFare * 1250n) / 10000n; // 12.5% fuel index
      const hazardousSurcharge = is_hazardous ? 500000n * count : 0n;

      const subtotal = baseFare + portHandling + tollCharges + fuelSurcharge + hazardousSurcharge;
      const gstRate = 18.0;
      const gst = (subtotal * 1800n) / 10000n; // 18% GST
      const totalAmount = subtotal + gst;

      // 3. Create fresh shipment draft for this quote
      const refNumber = `SHP-${Date.now().toString().slice(-8)}-${Math.floor(Math.random() * 1000)}`;
      const shipmentInsert = await query(
        `INSERT INTO shipment.shipments (
           customer_organization_id, reference_number, cargo_description,
           cargo_weight_kg, commodity, is_hazardous, status
         ) VALUES ($1, $2, $3, $4, $5, $6, 'DRAFT')
         RETURNING id`,
        [
          organizationId,
          refNumber,
          `Container Haulage: ${commodity}`,
          cargo_weight_kg,
          commodity,
          is_hazardous
        ]
      );
      const shipmentId = shipmentInsert.rows[0].id;

      // 4. Create quote in database with 15-minute countdown validity
      const quoteNumber = `QT-${Date.now().toString().slice(-8)}`;
      const expiresAt = new Date(Date.now() + 15 * 60 * 1000); // 15 mins lock

      const quoteInsertRes = await query(
        `INSERT INTO pricing.quotes (
           quote_number, customer_organization_id, shipment_id, pricing_rule_version_id,
           subtotal_minor, tax_minor, total_minor, currency, status, expires_at
         ) VALUES ($1, $2, $3, $4, $5, $6, $7, 'INR', 'ACTIVE', $8)
         RETURNING id, quote_number, subtotal_minor, tax_minor, total_minor, currency, status, expires_at`,
        [
          quoteNumber,
          organizationId,
          shipmentId,
          versionId,
          subtotal.toString(),
          gst.toString(),
          totalAmount.toString(),
          expiresAt.toISOString(),
        ]
      );
      const quoteRecord = quoteInsertRes.rows[0];

      // 5. Insert line components
      const components = [
        { type: "BASE_FREIGHT", desc: `Base Container Haulage (${container_type})`, amount: baseFare },
        { type: "PORT_HANDLING", desc: "Terminal Gate Handling & Port Infrastructure", amount: portHandling },
        { type: "TOLL", desc: "National Expressway Toll Recovery", amount: tollCharges },
        { type: "FUEL_SURCHARGE", desc: "Diesel Index Fuel Surcharge (12.5%)", amount: fuelSurcharge },
        ...(is_hazardous ? [{ type: "HAZMAT_SURCHARGE", desc: "Hazardous Materials Special Handling", amount: hazardousSurcharge }] : []),
        { type: "GST", desc: `Goods and Services Tax (${gstRate}%)`, amount: gst },
      ];

      for (const comp of components) {
        await query(
          `INSERT INTO pricing.quote_components (quote_id, component_type, description, quantity, unit_price_minor, amount_minor, tax_code)
           VALUES ($1, $2, $3, $4, $5, $6, '9965')`,
          [quoteRecord.id, comp.type, comp.desc, container_count, (comp.amount / count).toString(), comp.amount.toString()]
        );
      }

      // Format response matching Quote.kt Android domain model
      return res.status(201).json(
        formatResponse(req, {
          id: quoteRecord.id,
          quote_id: quoteRecord.id,
          quote_number: quoteRecord.quote_number,
          base_fare_paise: Number(baseFare),
          base_rate_minor: Number(baseFare),
          fuel_surcharge_paise: Number(fuelSurcharge),
          port_handling_paise: Number(portHandling),
          other_surcharge_paise: Number(tollCharges + hazardousSurcharge),
          subtotal_paise: Number(subtotal),
          subtotal_minor: Number(subtotal),
          gst_paise: Number(gst),
          gst_minor: Number(gst),
          total_amount_paise: Number(totalAmount),
          total_amount_minor: Number(totalAmount),
          gst_rate: gstRate,
          valid_until: expiresAt.toISOString(),
          expires_at: expiresAt.toISOString(),
          guaranteed_minutes: 15,
          currency: "INR",
          breakdown: components.map((c) => ({
            label: c.desc,
            amount_paise: Number(c.amount),
            is_gst: c.type === "GST",
          })),
        })
      );
    } catch (error) {
      next(error);
    }
  }
);

// GET /api/v1/quotes/:id
pricingRouter.get("/quotes/:id", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { id } = req.params;
    const quoteRes = await query(
      `SELECT q.*, 
              (SELECT json_agg(c) FROM pricing.quote_components c WHERE c.quote_id = q.id) as components
       FROM pricing.quotes q
       WHERE q.id::text = $1 OR q.quote_number = $1`,
      [id]
    );

    if (quoteRes.rows.length === 0) {
      throw AppError.notFound("Quote not found", ErrorCodes.RESOURCE_NOT_FOUND);
    }

    const q = quoteRes.rows[0];
    return res.json(
      formatResponse(req, {
        id: q.id,
        quote_number: q.quote_number,
        subtotal_paise: Number(q.subtotal_minor),
        tax_paise: Number(q.tax_minor),
        total_amount_paise: Number(q.total_minor),
        currency: q.currency,
        status: q.status,
        valid_until: q.expires_at,
        is_expired: new Date(q.expires_at) < new Date(),
        components: q.components || [],
      })
    );
  } catch (error) {
    next(error);
  }
});
