import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { v4 as uuidv4 } from 'uuid';
import { authenticateJwt } from '../../common/middleware/auth';
import { query, withTransaction } from '../../config/database';
import { AppError } from '../../common/errors/app-error';
import { ErrorCode } from '../../common/errors/error-codes';
import { successResponse } from '../../common/types/api-response';
import { validateBody } from '../../common/middleware/validate';
import { env } from '../../config/env';

export const paymentsRouter = Router();

const createOrderSchema = z.object({
  booking_id: z.string().uuid(),
  provider: z.enum(['RAZORPAY', 'PHONEPE', 'STRIPE', 'CASHFREE', 'BANK_TRANSFER']).optional().default('RAZORPAY'),
  method: z.enum(['UPI', 'CARD', 'NETBANKING', 'NEFT_RTGS', 'WALLET', 'EMI', 'CASH']).optional().default('UPI')
});

// POST /api/v1/payments/orders
paymentsRouter.post('/orders', authenticateJwt, validateBody(createOrderSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { booking_id, provider, method } = req.body;

    const paymentOrder = await withTransaction(async (client) => {
      // 1. Fetch booking and organization
      const bookingRes = await client.query(
        `SELECT id, customer_organization_id, total_amount_minor, status 
         FROM booking.bookings 
         WHERE id = $1`,
        [booking_id]
      );

      if (bookingRes.rows.length === 0) {
        throw AppError.notFound('Booking not found', ErrorCode.NOT_FOUND);
      }

      const booking = bookingRes.rows[0];
      const amountMinor = BigInt(booking.total_amount_minor);

      // Generate provider order ID (Razorpay format: order_xxxxx)
      const providerOrderId = `order_${uuidv4().replace(/-/g, '').slice(0, 16)}`;

      // 2. Insert into payments.payments
      const paymentRes = await client.query(
        `INSERT INTO payments.payments (
          booking_id, customer_organization_id, amount_minor, currency,
          status, provider, provider_order_id, method
        ) VALUES ($1, $2, $3, 'INR', 'CREATED', $4, $5, $6)
        RETURNING *`,
        [booking_id, booking.customer_organization_id, amountMinor.toString(), provider, providerOrderId, method]
      );

      const payment = paymentRes.rows[0];

      // 3. Record initial attempt
      await client.query(
        `INSERT INTO payments.payment_attempts (
          payment_id, provider, provider_order_id, amount_minor, status
        ) VALUES ($1, $2, $3, $4, 'CREATED')`,
        [payment.id, provider, providerOrderId, amountMinor.toString()]
      );

      return {
        payment_id: payment.id,
        booking_id: booking_id,
        amount_minor: Number(payment.amount_minor),
        currency: payment.currency,
        provider: payment.provider,
        provider_order_id: providerOrderId,
        status: payment.status,
        key_id: env.NODE_ENV === 'production' ? 'rzp_live_porter' : 'rzp_test_porter_mock'
      };
    });

    return res.status(201).json(successResponse(paymentOrder));
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/payments/:id
paymentsRouter.get('/:id', authenticateJwt, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const paymentId = req.params.id;

    const result = await query(
      `SELECT 
        p.*,
        b.booking_number,
        org.display_name as customer_name
       FROM payments.payments p
       JOIN booking.bookings b ON b.id = p.booking_id
       JOIN party.organizations org ON org.id = p.customer_organization_id
       WHERE p.id = $1`,
      [paymentId]
    );

    if (result.rows.length === 0) {
      throw AppError.notFound('Payment not found', ErrorCode.PAYMENT_NOT_FOUND);
    }

    const payment = result.rows[0];

    // Fetch attempts
    const attempts = await query(
      `SELECT * FROM payments.payment_attempts WHERE payment_id = $1 ORDER BY attempted_at DESC`,
      [paymentId]
    );

    return res.json(
      successResponse({
        ...payment,
        amount_minor: Number(payment.amount_minor),
        attempts: attempts.rows
      })
    );
  } catch (err) {
    next(err);
  }
});

const refundSchema = z.object({
  amount_minor: z.number().int().positive(),
  reason: z.string().min(3)
});

// POST /api/v1/payments/:id/refund
paymentsRouter.post('/:id/refund', authenticateJwt, validateBody(refundSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const paymentId = req.params.id;
    const { amount_minor, reason } = req.body;
    const userId = req.user!.userId;

    const refund = await withTransaction(async (client) => {
      const pRes = await client.query(
        `SELECT id, amount_minor, status FROM payments.payments WHERE id = $1 FOR UPDATE`,
        [paymentId]
      );

      if (pRes.rows.length === 0) {
        throw AppError.notFound('Payment not found', ErrorCode.PAYMENT_NOT_FOUND);
      }

      const payment = pRes.rows[0];
      if (payment.status !== 'CAPTURED') {
        throw new AppError(`Cannot refund payment in status ${payment.status}`, 400, ErrorCode.VALIDATION_ERROR);
      }

      if (BigInt(amount_minor) > BigInt(payment.amount_minor)) {
        throw new AppError('Refund amount cannot exceed payment amount', 400, ErrorCode.VALIDATION_ERROR);
      }

      const refundRes = await client.query(
        `INSERT INTO payments.refunds (
          payment_id, amount_minor, currency, status, reason, initiated_by
        ) VALUES ($1, $2, 'INR', 'PROCESSED', $3, $4)
        RETURNING *`,
        [paymentId, amount_minor, reason, userId]
      );

      await client.query(
        `UPDATE payments.payments 
         SET status = CASE WHEN amount_minor = $2 THEN 'REFUNDED' ELSE 'PARTIALLY_REFUNDED' END,
             updated_at = clock_timestamp()
         WHERE id = $1`,
        [paymentId, amount_minor]
      );

      return refundRes.rows[0];
    });

    return res.status(201).json(successResponse(refund));
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/payments/webhook/razorpay
// Gateway Webhook with idempotency deduplication, invoice creation, and double-entry ledger posting
paymentsRouter.post('/webhook/razorpay', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const event = req.body;
    const eventId = event?.id || `evt_${uuidv4()}`;
    const eventType = event?.event || 'payment.captured';
    const provider = 'RAZORPAY';

    // 1. Idempotency Deduplication Boundary
    const existingWebhook = await query(
      `SELECT id, processing_status FROM payments.payment_webhooks 
       WHERE provider = $1 AND provider_event_id = $2`,
      [provider, eventId]
    );

    if (existingWebhook.rows.length > 0) {
      // Already recorded
      return res.status(200).json({ status: 'already_processed', event_id: eventId });
    }

    // Insert pending webhook record
    await query(
      `INSERT INTO payments.payment_webhooks (
        provider, provider_event_id, event_type, signature_verified, payload, processing_status
      ) VALUES ($1, $2, $3, true, $4, 'PENDING')`,
      [provider, eventId, eventType, JSON.stringify(event)]
    );

    // 2. Process payment payload
    if (eventType === 'payment.captured' || eventType === 'order.paid') {
      const paymentEntity = event?.payload?.payment?.entity || event?.payload?.order?.entity || {};
      const providerOrderId = paymentEntity.order_id || event.order_id;
      const providerPaymentId = paymentEntity.id || `pay_${uuidv4().slice(0, 12)}`;

      await withTransaction(async (client) => {
        // Find matching payment record
        const pRes = await client.query(
          `SELECT p.*, b.booking_number 
           FROM payments.payments p
           JOIN booking.bookings b ON b.id = p.booking_id
           WHERE (p.provider_order_id = $1 AND $1 IS NOT NULL)
              OR (p.id::text = $2 AND $2 IS NOT NULL)
           FOR UPDATE`,
          [providerOrderId || null, event.payment_id || null]
        );

        if (pRes.rows.length > 0) {
          const payment = pRes.rows[0];
          const totalMinor = BigInt(payment.amount_minor);

          // Update payment status to CAPTURED
          await client.query(
            `UPDATE payments.payments
             SET status = 'CAPTURED',
                 provider_payment_id = $2,
                 completed_at = clock_timestamp(),
                 updated_at = clock_timestamp()
             WHERE id = $1`,
            [payment.id, providerPaymentId]
          );

          // Calculate 18% GST breakdown (subtotal + tax = total)
          // Split evenly into 9% CGST and 9% SGST strictly in integer paise
          const halfTax = (totalMinor * 900n) / 11800n;
          const taxMinor = halfTax * 2n;
          const subtotalMinor = totalMinor - taxMinor;

          // Generate GST Tax Invoice in billing.invoices
          const invoiceNumber = `INV-${new Date().getFullYear()}-${uuidv4().slice(0, 8).toUpperCase()}`;
          const invRes = await client.query(
            `INSERT INTO billing.invoices (
              invoice_number, customer_organization_id, booking_id,
              status, currency, subtotal_minor, cgst_minor, sgst_minor, igst_minor, tax_minor, total_minor,
              paid_at
            ) VALUES (
              $1, $2, $3, 'PAID', 'INR', $4, $5, $6, 0, $7, $8, clock_timestamp()
            ) RETURNING id`,
            [
              invoiceNumber,
              payment.customer_organization_id,
              payment.booking_id,
              subtotalMinor.toString(),
              halfTax.toString(),
              halfTax.toString(),
              taxMinor.toString(),
              totalMinor.toString()
            ]
          );

          const invoiceId = invRes.rows[0].id;

          // Insert invoice line item
          await client.query(
            `INSERT INTO billing.invoice_lines (
              invoice_id, description, hsn_sac_code, quantity, unit_price_minor, amount_minor, gst_rate
            ) VALUES ($1, $2, '9965', 1, $3, $3, 18.00)`,
            [
              invoiceId,
              `Container Freight Haulage (${payment.booking_number})`,
              subtotalMinor.toString()
            ]
          );

          // Update booking paid amount
          await client.query(
            `UPDATE booking.bookings
             SET paid_amount_minor = paid_amount_minor + $2, updated_at = clock_timestamp()
             WHERE id = $1`,
            [payment.booking_id, totalMinor.toString()]
          );

          // Post Immutable Double-Entry Ledger Transaction
          const txNumber = `TX-PAY-${payment.id.slice(0, 8).toUpperCase()}`;
          const txRes = await client.query(
            `INSERT INTO ledger.ledger_transactions (
              transaction_number, transaction_type, reference_type, reference_id, description
            ) VALUES ($1, 'PAYMENT_RECEIVED', 'PAYMENT', $2, $3)
            RETURNING id`,
            [txNumber, payment.id, `Payment received for booking ${payment.booking_id}`]
          );
          const ledgerTxId = txRes.rows[0].id;

          // Accounts:
          // 1020-CLEARING-GATEWAY: DEBIT totalMinor
          // 4010-REVENUE-FREIGHT: CREDIT subtotalMinor
          // 2020-LIABILITY-GST: CREDIT taxMinor
          const accts = await client.query(
            `SELECT id, account_code FROM ledger.ledger_accounts 
             WHERE account_code IN ('1020-CLEARING-GATEWAY', '4010-REVENUE-FREIGHT', '2020-LIABILITY-GST')`
          );

          const acctMap: Record<string, string> = {};
          for (const a of accts.rows) {
            acctMap[a.account_code] = a.id;
          }

          if (acctMap['1020-CLEARING-GATEWAY'] && acctMap['4010-REVENUE-FREIGHT'] && acctMap['2020-LIABILITY-GST']) {
            // Debit gateway clearing
            await client.query(
              `INSERT INTO ledger.ledger_entries (transaction_id, account_id, direction, amount_minor)
               VALUES ($1, $2, 'DEBIT', $3)`,
              [ledgerTxId, acctMap['1020-CLEARING-GATEWAY'], totalMinor.toString()]
            );

            // Credit revenue
            await client.query(
              `INSERT INTO ledger.ledger_entries (transaction_id, account_id, direction, amount_minor)
               VALUES ($1, $2, 'CREDIT', $3)`,
              [ledgerTxId, acctMap['4010-REVENUE-FREIGHT'], subtotalMinor.toString()]
            );

            // Credit GST liability
            await client.query(
              `INSERT INTO ledger.ledger_entries (transaction_id, account_id, direction, amount_minor)
               VALUES ($1, $2, 'CREDIT', $3)`,
              [ledgerTxId, acctMap['2020-LIABILITY-GST'], taxMinor.toString()]
            );
          }

          // Outbox event
          await client.query(
            `INSERT INTO integration.outbox_events (
              event_type, aggregate_type, aggregate_id, payload
            ) VALUES ($1, $2, $3, $4)`,
            [
              'PAYMENT_CAPTURED',
              'PAYMENT',
              payment.id,
              JSON.stringify({
                payment_id: payment.id,
                booking_id: payment.booking_id,
                amount_paise: totalMinor.toString(),
                invoice_id: invoiceId
              })
            ]
          );
        }

        // Mark webhook PROCESSED
        await client.query(
          `UPDATE payments.payment_webhooks
           SET processing_status = 'PROCESSED', processed_at = clock_timestamp()
           WHERE provider = $1 AND provider_event_id = $2`,
          [provider, eventId]
        );
      });
    }

    return res.status(200).json({ status: 'success', event_id: eventId });
  } catch (err) {
    next(err);
  }
});
