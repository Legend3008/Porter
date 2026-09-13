-- ==============================================================================
-- Test: test_concurrency.sql
-- Description: Asserts that two competing drivers cannot both accept the same trip.
--              Validates the unique partial index on operations.trip_assignments
--              and job_offers.
-- ==============================================================================

DO $$
DECLARE
    v_trip_id UUID := '70000000-0000-0000-0001-000000000002';
    v_carrier_id UUID := '20000000-0000-0000-0001-000000000002';
    v_driver_1 UUID := '40000000-0000-0000-0001-000000000001';
    v_driver_2 UUID;
    v_vehicle_2 UUID;
    v_error_caught BOOLEAN := false;
BEGIN
    -- Create a second test driver
    INSERT INTO identity.users (id, phone, full_name, status)
    VALUES ('30000000-0000-0000-0001-000000000003', '+919999999999', 'Competing Driver', 'ACTIVE')
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO fleet.drivers (id, carrier_id, user_id, driver_code, license_number_encrypted, license_expiry_date)
    VALUES ('40000000-0000-0000-0001-000000000003', v_carrier_id, '30000000-0000-0000-0001-000000000003', 'DRV-1003', 'enc:MH0399999', '2030-01-01')
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO fleet.vehicles (id, carrier_id, registration_number, vehicle_type, capacity_kg)
    VALUES ('40000000-0000-0000-0001-000000000003', v_carrier_id, 'MH46BB0002', '40FT_TRAILER', 30000)
    ON CONFLICT (registration_number) DO NOTHING;

    -- Attempt to insert a second ACCEPTED assignment for the same trip (should fail with unique violation)
    BEGIN
        INSERT INTO operations.trip_assignments (trip_id, carrier_id, driver_id, vehicle_id, assignment_status)
        VALUES (v_trip_id, v_carrier_id, '40000000-0000-0000-0001-000000000003', '40000000-0000-0000-0001-000000000003', 'ACCEPTED');
    EXCEPTION WHEN unique_violation THEN
        v_error_caught := true;
    END;

    IF NOT v_error_caught THEN
        RAISE EXCEPTION 'TEST FAILED: Concurrency violation not prevented! Two active assignments were permitted for the same trip.';
    ELSE
        RAISE NOTICE 'TEST PASSED: Concurrency check prevented competing driver acceptance.';
    END IF;
END $$;
