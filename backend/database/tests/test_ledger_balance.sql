-- ==============================================================================
-- Test: test_ledger_balance.sql
-- Description: Asserts that every posted ledger transaction strictly balances
--              (Total Debits == Total Credits in minor units).
-- ==============================================================================

DO $$
DECLARE
    v_tx_id UUID := gen_random_uuid();
    v_account_cash UUID := '00000000-0000-0000-0003-000000000001';
    v_account_rev UUID := '00000000-0000-0000-0003-000000000006';
    v_is_balanced BOOLEAN;
    v_unbalanced_tx_id UUID := gen_random_uuid();
    v_error_caught BOOLEAN := false;
BEGIN
    -- 1. Create a balanced transaction
    INSERT INTO ledger.ledger_transactions (id, transaction_number, transaction_type, reference_type, reference_id, description)
    VALUES (v_tx_id, 'TX-TEST-BALANCED', 'PAYMENT_RECEIVED', 'PAYMENT', gen_random_uuid(), 'Test balanced transaction');

    INSERT INTO ledger.ledger_entries (transaction_id, account_id, direction, amount_minor)
    VALUES 
        (v_tx_id, v_account_cash, 'DEBIT', 100000),   -- ₹1,000.00 debit
        (v_tx_id, v_account_rev, 'CREDIT', 100000);   -- ₹1,000.00 credit

    v_is_balanced := ledger.verify_transaction_balance(v_tx_id);
    IF NOT v_is_balanced THEN
        RAISE EXCEPTION 'TEST FAILED: Balanced transaction was reported as unbalanced.';
    END IF;

    -- 2. Create an unbalanced transaction and verify verification function raises exception
    INSERT INTO ledger.ledger_transactions (id, transaction_number, transaction_type, reference_type, reference_id, description)
    VALUES (v_unbalanced_tx_id, 'TX-TEST-UNBALANCED', 'PAYMENT_RECEIVED', 'PAYMENT', gen_random_uuid(), 'Test unbalanced transaction');

    INSERT INTO ledger.ledger_entries (transaction_id, account_id, direction, amount_minor)
    VALUES 
        (v_unbalanced_tx_id, v_account_cash, 'DEBIT', 100000),   -- ₹1,000.00 debit
        (v_unbalanced_tx_id, v_account_rev, 'CREDIT', 80000);    -- ₹800.00 credit (Lacks ₹200)

    BEGIN
        PERFORM ledger.verify_transaction_balance(v_unbalanced_tx_id);
    EXCEPTION WHEN OTHERS THEN
        v_error_caught := true;
    END;

    IF NOT v_error_caught THEN
        RAISE EXCEPTION 'TEST FAILED: Unbalanced transaction was not rejected by verify_transaction_balance!';
    ELSE
        RAISE NOTICE 'TEST PASSED: Double-entry ledger mathematical balance invariant verified.';
    END IF;
END $$;
