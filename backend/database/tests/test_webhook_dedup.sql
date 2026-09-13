-- ==============================================================================
-- Test: test_webhook_dedup.sql
-- Description: Asserts that duplicate payment webhooks from gateways
--              (e.g., Razorpay/PhonePe delivering the same event twice)
--              are rejected by the unique constraint (provider, provider_event_id).
-- ==============================================================================

DO $$
DECLARE
    v_provider VARCHAR(32) := 'RAZORPAY';
    v_event_id VARCHAR(128) := 'evt_test_payment_captured_9921';
    v_duplicate_caught BOOLEAN := false;
BEGIN
    -- 1. First webhook insert succeeds
    INSERT INTO payments.payment_webhooks (
        provider, provider_event_id, event_type, signature_verified, payload, processing_status
    ) VALUES (
        v_provider, v_event_id, 'payment.captured', true, '{"amount":5369000, "status":"captured"}'::jsonb, 'PROCESSED'
    );

    -- 2. Duplicate webhook attempt must trigger unique constraint violation
    BEGIN
        INSERT INTO payments.payment_webhooks (
            provider, provider_event_id, event_type, signature_verified, payload, processing_status
        ) VALUES (
            v_provider, v_event_id, 'payment.captured', true, '{"amount":5369000, "status":"captured"}'::jsonb, 'PENDING'
        );
    EXCEPTION WHEN unique_violation THEN
        v_duplicate_caught := true;
    END;

    IF NOT v_duplicate_caught THEN
        RAISE EXCEPTION 'TEST FAILED: Duplicate webhook was not deduplicated!';
    ELSE
        RAISE NOTICE 'TEST PASSED: Webhook deduplication successfully prevented duplicate processing.';
    END IF;
END $$;
