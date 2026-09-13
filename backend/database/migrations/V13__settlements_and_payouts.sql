-- ==============================================================================
-- Migration: V13__settlements_and_payouts.sql
-- Description: Carrier/driver settlements, payout idempotency, rerun safety,
--              and automated financial reconciliation.
-- ==============================================================================

-- 1. Carrier Settlements (Earnings Aggregation)
CREATE TABLE settlement.settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_number VARCHAR(64) UNIQUE NOT NULL,
    carrier_id UUID NOT NULL REFERENCES fleet.carriers(id) ON DELETE RESTRICT,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    gross_amount_minor BIGINT NOT NULL CHECK (gross_amount_minor >= 0),
    deductions_minor BIGINT NOT NULL DEFAULT 0 CHECK (deductions_minor >= 0),
    tds_tax_minor BIGINT NOT NULL DEFAULT 0 CHECK (tds_tax_minor >= 0),
    net_amount_minor BIGINT NOT NULL CHECK (net_amount_minor >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'CALCULATED' CHECK (status IN (
        'DRAFT', 'CALCULATED', 'APPROVED', 'PAID', 'FAILED'
    )),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_settlements_carrier ON settlement.settlements(carrier_id, status);

CREATE TRIGGER trg_settlements_updated_at
    BEFORE UPDATE ON settlement.settlements
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Settlement Line Items (Per-Trip Earnings Breakdown)
CREATE TABLE settlement.settlement_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_id UUID NOT NULL REFERENCES settlement.settlements(id) ON DELETE CASCADE,
    trip_id UUID NOT NULL REFERENCES operations.trips(id) ON DELETE RESTRICT,
    base_freight_minor BIGINT NOT NULL,
    fuel_allowance_minor BIGINT NOT NULL DEFAULT 0,
    toll_reimbursement_minor BIGINT NOT NULL DEFAULT 0,
    detention_allowance_minor BIGINT NOT NULL DEFAULT 0,
    penalty_deduction_minor BIGINT NOT NULL DEFAULT 0,
    total_line_minor BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    -- Invariant: A trip can only ever be included in one settlement line (Prevents double payment)
    UNIQUE (trip_id)
);

CREATE INDEX idx_settlement_lines_settlement ON settlement.settlement_lines(settlement_id);

-- 3. Payouts (Bank Transfers to Carrier/Driver Accounts)
CREATE TABLE settlement.payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_id UUID NOT NULL REFERENCES settlement.settlements(id) ON DELETE RESTRICT,
    provider VARCHAR(32) NOT NULL, -- RAZORPAYX, CASHFREE, BANK_NEFT
    provider_payout_id VARCHAR(128) UNIQUE,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'INITIATED' CHECK (status IN (
        'INITIATED', 'PROCESSING', 'SUCCESS', 'FAILED', 'REVERSED'
    )),
    account_number_encrypted TEXT NOT NULL,
    ifsc_code VARCHAR(16) NOT NULL,
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    completed_at TIMESTAMPTZ,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TRIGGER trg_payouts_updated_at
    BEFORE UPDATE ON settlement.payouts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 4. Reconciliation Runs (Mismatch Detection Engine)
CREATE TABLE settlement.reconciliation_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_type VARCHAR(32) NOT NULL CHECK (run_type IN ('DAILY_GATEWAY', 'DAILY_PAYOUT', 'PERIODIC_LEDGER')),
    run_date DATE NOT NULL,
    total_records INT NOT NULL,
    matched_records INT NOT NULL,
    mismatched_records INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- 5. Reconciliation Discrepancy Items
CREATE TABLE settlement.reconciliation_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES settlement.reconciliation_runs(id) ON DELETE CASCADE,
    entity_type VARCHAR(32) NOT NULL, -- PAYMENT, PAYOUT, LEDGER
    entity_id UUID NOT NULL,
    expected_amount_minor BIGINT NOT NULL,
    actual_amount_minor BIGINT NOT NULL,
    difference_minor BIGINT NOT NULL,
    discrepancy_reason TEXT NOT NULL,
    resolution_status VARCHAR(32) NOT NULL DEFAULT 'UNRESOLVED' CHECK (resolution_status IN (
        'UNRESOLVED', 'INVESTIGATING', 'RESOLVED', 'WRITTEN_OFF'
    )),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
