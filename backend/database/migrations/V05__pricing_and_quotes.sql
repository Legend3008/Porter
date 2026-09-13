-- ==============================================================================
-- Migration: V05__pricing_and_quotes.sql
-- Description: Configurable pricing engine, versioned rules, quotes,
--              and itemized quote breakdown components (all in BIGINT paise).
-- ==============================================================================

-- 1. Pricing Rule Sets
CREATE TABLE pricing.pricing_rule_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- 2. Pricing Rule Versions (Immutable Historical Pricing)
CREATE TABLE pricing.pricing_rule_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_set_id UUID NOT NULL REFERENCES pricing.pricing_rule_sets(id) ON DELETE RESTRICT,
    version_number INT NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('DRAFT', 'ACTIVE', 'SUPERSEDED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (rule_set_id, version_number)
);

-- 3. Pricing Rules
CREATE TABLE pricing.pricing_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL REFERENCES pricing.pricing_rule_versions(id) ON DELETE CASCADE,
    rule_type VARCHAR(64) NOT NULL,
    container_type VARCHAR(32),
    lane_id UUID REFERENCES geo.lanes(id) ON DELETE CASCADE,
    base_amount_minor BIGINT NOT NULL DEFAULT 0 CHECK (base_amount_minor >= 0),
    per_km_amount_minor BIGINT NOT NULL DEFAULT 0 CHECK (per_km_amount_minor >= 0),
    per_ton_amount_minor BIGINT NOT NULL DEFAULT 0 CHECK (per_ton_amount_minor >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- 4. Surcharges (Fuel, Port, Toll, Waiting, GST)
CREATE TABLE pricing.surcharges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(128) NOT NULL,
    calculation_type VARCHAR(32) NOT NULL CHECK (calculation_type IN ('FIXED', 'PERCENTAGE', 'PER_HOUR', 'PER_KM')),
    default_rate NUMERIC(12, 4) NOT NULL, -- e.g. 18.00 for GST, or fixed paise
    is_tax BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- 5. Quotes (Commercial Snapshot - Immutable once accepted)
CREATE TABLE pricing.quotes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quote_number VARCHAR(64) UNIQUE NOT NULL,
    customer_organization_id UUID NOT NULL REFERENCES party.organizations(id) ON DELETE RESTRICT,
    shipment_id UUID NOT NULL REFERENCES shipment.shipments(id) ON DELETE RESTRICT,
    lane_id UUID REFERENCES geo.lanes(id) ON DELETE SET NULL,
    pricing_rule_version_id UUID NOT NULL REFERENCES pricing.pricing_rule_versions(id) ON DELETE RESTRICT,
    subtotal_minor BIGINT NOT NULL CHECK (subtotal_minor >= 0),
    tax_minor BIGINT NOT NULL CHECK (tax_minor >= 0),
    discount_minor BIGINT NOT NULL DEFAULT 0 CHECK (discount_minor >= 0),
    total_minor BIGINT NOT NULL CHECK (total_minor >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN (
        'DRAFT', 'ACTIVE', 'EXPIRED', 'ACCEPTED', 'CANCELLED'
    )),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_quotes_customer ON pricing.quotes(customer_organization_id, created_at DESC);
CREATE INDEX idx_quotes_status ON pricing.quotes(status, expires_at);

CREATE TRIGGER trg_quotes_updated_at
    BEFORE UPDATE ON pricing.quotes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 6. Quote Components (Transparent Cost Breakdown)
CREATE TABLE pricing.quote_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quote_id UUID NOT NULL REFERENCES pricing.quotes(id) ON DELETE CASCADE,
    component_type VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity NUMERIC(10, 2) NOT NULL DEFAULT 1,
    unit_price_minor BIGINT NOT NULL,
    amount_minor BIGINT NOT NULL,
    tax_code VARCHAR(32), -- SAC 9965 (Goods Transport Services)
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_quote_components_quote ON pricing.quote_components(quote_id);
