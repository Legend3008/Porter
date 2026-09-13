-- ==============================================================================
-- Migration: V11__billing_and_invoices.sql
-- Description: Tax billing, GST compliant customer invoices, and invoice line items.
-- ==============================================================================

-- 1. Invoices (GST Compliant Tax Invoices)
CREATE TABLE billing.invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(64) UNIQUE NOT NULL,
    customer_organization_id UUID NOT NULL REFERENCES party.organizations(id) ON DELETE RESTRICT,
    booking_id UUID NOT NULL REFERENCES booking.bookings(id) ON DELETE RESTRICT,
    trip_id UUID REFERENCES operations.trips(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ISSUED' CHECK (status IN (
        'DRAFT', 'ISSUED', 'PAID', 'OVERDUE', 'CANCELLED'
    )),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    subtotal_minor BIGINT NOT NULL CHECK (subtotal_minor >= 0),
    cgst_minor BIGINT NOT NULL DEFAULT 0 CHECK (cgst_minor >= 0),
    sgst_minor BIGINT NOT NULL DEFAULT 0 CHECK (sgst_minor >= 0),
    igst_minor BIGINT NOT NULL DEFAULT 0 CHECK (igst_minor >= 0),
    tax_minor BIGINT NOT NULL CHECK (tax_minor >= 0),
    total_minor BIGINT NOT NULL CHECK (total_minor >= 0),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    due_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    download_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_invoices_customer ON billing.invoices(customer_organization_id, issued_at DESC);
CREATE INDEX idx_invoices_booking ON billing.invoices(booking_id);

CREATE TRIGGER trg_invoices_updated_at
    BEFORE UPDATE ON billing.invoices
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Invoice Line Items
CREATE TABLE billing.invoice_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES billing.invoices(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    hsn_sac_code VARCHAR(32) DEFAULT '9965', -- Freight Transport Services
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price_minor BIGINT NOT NULL CHECK (unit_price_minor >= 0),
    amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
    gst_rate NUMERIC(5, 2) NOT NULL DEFAULT 18.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_invoice_lines_invoice ON billing.invoice_lines(invoice_id);
