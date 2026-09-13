import { Router, Request, Response, NextFunction } from 'express';
import { authenticateJwt } from '../../common/middleware/auth';
import { query } from '../../config/database';
import { AppError } from '../../common/errors/app-error';
import { ErrorCode } from '../../common/errors/error-codes';
import { successResponse } from '../../common/types/api-response';

export const invoicesRouter = Router();
invoicesRouter.use(authenticateJwt);

// GET /api/v1/invoices
invoicesRouter.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const orgId = req.user?.organizationId;
    const role = req.user?.role;
    const status = req.query.status as string;

    let sql = `
      SELECT 
        i.id,
        i.invoice_number,
        i.status,
        i.currency,
        i.subtotal_minor as subtotal_paise,
        i.cgst_minor as cgst_paise,
        i.sgst_minor as sgst_paise,
        i.igst_minor as igst_paise,
        i.tax_minor as tax_paise,
        i.total_minor as total_paise,
        i.issued_at,
        i.paid_at,
        b.booking_number,
        org.display_name as customer_name
      FROM billing.invoices i
      JOIN booking.bookings b ON b.id = i.booking_id
      JOIN party.organizations org ON org.id = i.customer_organization_id
    `;
    const params: any[] = [];

    if (role !== 'ADMIN' && role !== 'OPERATOR' && orgId) {
      params.push(orgId);
      sql += ` WHERE i.customer_organization_id = $1`;
    }

    if (status) {
      sql += params.length > 0 ? ` AND i.status = $${params.length + 1}` : ` WHERE i.status = $1`;
      params.push(status);
    }

    sql += ` ORDER BY i.issued_at DESC LIMIT 50`;

    const result = await query(sql, params);
    return res.json(successResponse(result.rows));
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/invoices/:id
invoicesRouter.get('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const invoiceId = req.params.id;

    const invRes = await query(
      `SELECT 
        i.*,
        b.booking_number,
        s.cargo_description,
        org.display_name as customer_name,
        org.legal_name
      FROM billing.invoices i
      JOIN booking.bookings b ON b.id = i.booking_id
      JOIN shipment.shipments s ON s.id = b.shipment_id
      JOIN party.organizations org ON org.id = i.customer_organization_id
      WHERE i.id = $1`,
      [invoiceId]
    );

    if (invRes.rows.length === 0) {
      throw AppError.notFound('Invoice not found', ErrorCode.NOT_FOUND);
    }

    const invoice = invRes.rows[0];

    // Fetch line items
    const linesRes = await query(
      `SELECT 
        id,
        description,
        hsn_sac_code,
        quantity,
        unit_price_minor as unit_price_paise,
        amount_minor as amount_paise,
        gst_rate
       FROM billing.invoice_lines
       WHERE invoice_id = $1
       ORDER BY created_at ASC`,
      [invoiceId]
    );

    return res.json(
      successResponse({
        id: invoice.id,
        invoice_number: invoice.invoice_number,
        status: invoice.status,
        currency: invoice.currency,
        subtotal_paise: Number(invoice.subtotal_minor),
        cgst_paise: Number(invoice.cgst_minor),
        sgst_paise: Number(invoice.sgst_minor),
        igst_paise: Number(invoice.igst_minor),
        tax_paise: Number(invoice.tax_minor),
        total_paise: Number(invoice.total_minor),
        issued_at: invoice.issued_at,
        paid_at: invoice.paid_at,
        customer: {
          name: invoice.customer_name,
          legal_name: invoice.legal_name
        },
        booking: {
          id: invoice.booking_id,
          booking_number: invoice.booking_number,
          cargo_description: invoice.cargo_description
        },
        line_items: linesRes.rows
      })
    );
  } catch (err) {
    next(err);
  }
});
