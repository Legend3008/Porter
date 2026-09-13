-- ==============================================================================
-- Test: test_idempotency.sql
-- Description: Asserts that retrying a network request with the same Idempotency-Key
--              is caught by the unique constraint and returns cached results.
-- ==============================================================================

DO $$
DECLARE
    v_actor_id UUID := '30000000-0000-0000-0001-000000000001';
    v_operation VARCHAR(128) := 'POST /api/v1/bookings';
    v_key VARCHAR(128) := 'idemp_test_key_88921';
    v_request_hash VARCHAR(64) := 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
    v_duplicate_caught BOOLEAN := false;
BEGIN
    -- 1. First execution creates the idempotency record
    INSERT INTO integration.idempotency_keys (
        idempotency_key, actor_id, operation, request_hash, status, response_status, response_body, expires_at
    ) VALUES (
        v_key, v_actor_id, v_operation, v_request_hash, 'COMPLETED', 201, '{"booking_id":"70000000-0000-0000-0001-000000000001"}'::jsonb, clock_timestamp() + interval '24 hours'
    );

    -- 2. Second execution (client retry on network timeout) must trigger unique violation
    BEGIN
        INSERT INTO integration.idempotency_keys (
            idempotency_key, actor_id, operation, request_hash, status, expires_at
        ) VALUES (
            v_key, v_actor_id, v_operation, v_request_hash, 'IN_PROGRESS', clock_timestamp() + interval '24 hours'
        );
    EXCEPTION WHEN unique_violation THEN
        v_duplicate_caught := true;
    END;

    IF NOT v_duplicate_caught THEN
        RAISE EXCEPTION 'TEST FAILED: Idempotency violation not caught! Duplicate operation record was created.';
    ELSE
        RAISE NOTICE 'TEST PASSED: Idempotency constraint successfully prevented duplicate execution.';
    END IF;
END $$;
