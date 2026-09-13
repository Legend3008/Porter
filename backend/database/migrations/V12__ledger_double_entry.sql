-- ==============================================================================
-- Migration: V12__ledger_double_entry.sql
-- Description: Financial source of truth — immutable double-entry general ledger
--              with strict mathematical balancing (SUM debits == SUM credits).
-- ==============================================================================

-- 1. Ledger Chart of Accounts
CREATE TABLE ledger.ledger_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_code VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(128) NOT NULL,
    account_type VARCHAR(32) NOT NULL CHECK (account_type IN (
        'ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'
    )),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- 2. Ledger Transactions (Financial Event Headers - Immutable)
CREATE TABLE ledger.ledger_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_number VARCHAR(64) UNIQUE NOT NULL,
    transaction_type VARCHAR(64) NOT NULL, -- PAYMENT_RECEIVED, INVOICE_ISSUED, CARRIER_SETTLEMENT, REFUND_ISSUED
    reference_type VARCHAR(64) NOT NULL, -- PAYMENT, INVOICE, SETTLEMENT, REFUND
    reference_id UUID NOT NULL,
    description TEXT NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    posted_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    status VARCHAR(32) NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED', 'REVERSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_ledger_transactions_ref ON ledger.ledger_transactions(reference_type, reference_id);

-- 3. Ledger Entries (Debits and Credits)
CREATE TABLE ledger.ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES ledger.ledger_transactions(id) ON DELETE RESTRICT,
    account_id UUID NOT NULL REFERENCES ledger.ledger_accounts(id) ON DELETE RESTRICT,
    direction VARCHAR(16) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_ledger_entries_account ON ledger.ledger_entries(account_id, created_at);
CREATE INDEX idx_ledger_entries_transaction ON ledger.ledger_entries(transaction_id);

-- 4. Invariant Enforcement: Double-Entry Balancing Verification
CREATE OR REPLACE FUNCTION ledger.verify_transaction_balance(p_transaction_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_debit_total BIGINT;
    v_credit_total BIGINT;
BEGIN
    SELECT 
        COALESCE(SUM(CASE WHEN direction = 'DEBIT' THEN amount_minor ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amount_minor ELSE 0 END), 0)
    INTO v_debit_total, v_credit_total
    FROM ledger.ledger_entries
    WHERE transaction_id = p_transaction_id;

    IF v_debit_total <> v_credit_total THEN
        RAISE EXCEPTION 'Double-entry invariant violated for transaction %: Total debits (%) != Total credits (%)',
            p_transaction_id, v_debit_total, v_credit_total;
    END IF;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- 5. Immutability Protection: Prohibit Updates and Deletions on Ledger Tables
CREATE TRIGGER trg_ledger_transactions_immutable
    BEFORE UPDATE OR DELETE ON ledger.ledger_transactions
    FOR EACH ROW EXECUTE FUNCTION enforce_immutability();

CREATE TRIGGER trg_ledger_entries_immutable
    BEFORE UPDATE OR DELETE ON ledger.ledger_entries
    FOR EACH ROW EXECUTE FUNCTION enforce_immutability();
