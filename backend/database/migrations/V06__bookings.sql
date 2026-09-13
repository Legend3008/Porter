-- ==============================================================================
-- Migration: V06__bookings.sql
-- Description: Bookings, human-readable booking numbers, commercial snapshots,
--              and automatic append-only booking status transitions.
-- ==============================================================================

-- 1. Bookings (Commercial Transportation Order)
CREATE TABLE booking.bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_number VARCHAR(64) UNIQUE NOT NULL,
    customer_organization_id UUID NOT NULL REFERENCES party.organizations(id) ON DELETE RESTRICT,
    shipment_id UUID NOT NULL UNIQUE REFERENCES shipment.shipments(id) ON DELETE RESTRICT,
    quote_id UUID NOT NULL REFERENCES pricing.quotes(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
        'DRAFT', 'PENDING_PAYMENT', 'PAYMENT_PROCESSING', 'CONFIRMED',
        'ASSIGNED', 'IN_TRANSIT', 'AT_PORT', 'LOADED', 'DELIVERED',
        'COMPLETED', 'CANCELLED', 'PAYMENT_FAILED'
    )),
    total_amount_minor BIGINT NOT NULL CHECK (total_amount_minor >= 0),
    paid_amount_minor BIGINT NOT NULL DEFAULT 0 CHECK (paid_amount_minor >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,
    created_by UUID REFERENCES identity.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_bookings_customer ON booking.bookings(customer_organization_id, created_at DESC);
CREATE INDEX idx_bookings_status ON booking.bookings(status, created_at DESC);

CREATE TRIGGER trg_bookings_updated_at
    BEFORE UPDATE ON booking.bookings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Booking Status History (Immutable State Transition Audit)
CREATE TABLE booking.booking_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES booking.bookings(id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    changed_by UUID REFERENCES identity.users(id) ON DELETE SET NULL,
    actor_type VARCHAR(32) NOT NULL DEFAULT 'USER', -- USER, DRIVER, OPS, SYSTEM, WEBHOOK
    source VARCHAR(32) NOT NULL DEFAULT 'SHIPPER_APP',
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_booking_status_history_booking ON booking.booking_status_history(booking_id, occurred_at ASC);

-- 3. Automatic Audit Trigger for Booking Status Transitions
CREATE OR REPLACE FUNCTION booking.capture_booking_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO booking.booking_status_history (
            booking_id, from_status, to_status, actor_type, source, reason, occurred_at
        ) VALUES (
            NEW.id, NULL, NEW.status, 'SYSTEM', 'SYSTEM', 'Initial booking creation', clock_timestamp()
        );
    ELSIF (TG_OP = 'UPDATE' AND OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO booking.booking_status_history (
            booking_id, from_status, to_status, actor_type, source, reason, occurred_at
        ) VALUES (
            NEW.id, OLD.status, NEW.status, 'SYSTEM', 'SYSTEM', 'State transition', clock_timestamp()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_capture_booking_status
    AFTER INSERT OR UPDATE ON booking.bookings
    FOR EACH ROW EXECUTE FUNCTION booking.capture_booking_status_change();
