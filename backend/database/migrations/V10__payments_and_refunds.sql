-- ==============================================================================
-- Migration: V10__payments_and_refunds.sql
-- Description: Commercial payments, gateway attempts, webhook deduplication,
--              and refund tracking (all in BIGINT paise).
-- ==============================================================================

-- 1. Payments (Commercial Payment Intent & Result)
CREATE TABLE payments.payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES booking.bookings(id) ON DELETE RESTRICT,
    customer_organization_id UUID NOT NULL REFERENCES party.organizations(id) ON DELETE RESTRICT,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED' CHECK (status IN (
        'CREATED', 'PENDING', 'PROCESSING', 'AUTHORIZED', 'CAPTURED',
        'FAILED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED'
    )),
    provider VARCHAR(32) NOT NULL, -- RAZORPAY, PHONEPE, STRIPE, CASHFREE, BANK_TRANSFER
    provider_order_id VARCHAR(128),
    provider_payment_id VARCHAR(128),
    method VARCHAR(32) CHECK (method IN (
        'UPI', 'CARD', 'NETBANKING', 'NEFT_RTGS', 'WALLET', 'EMI', 'CASH'
    )),
    failure_code VARCHAR(64),
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_payments_booking ON payments.payments(booking_id);
CREATE INDEX idx_payments_customer ON payments.payments(customer_organization_id, created_at DESC);
CREATE INDEX idx_payments_provider_ref ON payments.payments(provider, provider_payment_id);

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments.payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Payment Attempts (Multiple attempts per payment session)
CREATE TABLE payments.payment_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments.payments(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_order_id VARCHAR(128),
    provider_payment_id VARCHAR(128),
    amount_minor BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    failure_code VARCHAR(64),
    failure_message TEXT,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_payment_attempts_payment ON payments.payment_attempts(payment_id);

-- 3. Payment Webhooks (Deduplication boundary for payment gateways)
CREATE TABLE payments.payment_webhooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(32) NOT NULL,
    provider_event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    signature_verified BOOLEAN NOT NULL DEFAULT false,
    payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    processed_at TIMESTAMPTZ,
    processing_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (processing_status IN (
        'PENDING', 'PROCESSED', 'FAILED', 'IGNORED'
    )),
    failure_reason TEXT,
    UNIQUE (provider, provider_event_id)
);

CREATE INDEX idx_payment_webhooks_pending ON payments.payment_webhooks(processing_status) 
    WHERE processing_status = 'PENDING';

-- 4. Refunds (Full & Partial Reversals)
CREATE TABLE payments.refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments.payments(id) ON DELETE RESTRICT,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED')),
    provider_refund_id VARCHAR(128),
    reason TEXT NOT NULL,
    initiated_by UUID REFERENCES identity.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_refunds_payment ON payments.refunds(payment_id);
