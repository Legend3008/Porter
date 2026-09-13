-- ==============================================================================
-- Test: test_gps_partitions.sql
-- Description: Asserts that inserting telemetry with various server_received_at
--              dates correctly routes to the respective monthly partition tables
--              and updates the latest_locations projection.
-- ==============================================================================

DO $$
DECLARE
    v_trip_id UUID := '70000000-0000-0000-0001-000000000002';
    v_driver_id UUID := '40000000-0000-0000-0001-000000000001';
    v_vehicle_id UUID := '40000000-0000-0000-0001-000000000002';
    v_count_aug INT;
    v_count_sep INT;
    v_latest_lat NUMERIC(10, 7);
BEGIN
    -- 1. Insert an August 2026 ping
    INSERT INTO tracking.location_pings (
        trip_id, driver_id, vehicle_id, latitude, longitude,
        device_recorded_at, server_received_at
    ) VALUES (
        v_trip_id, v_driver_id, v_vehicle_id, 18.9400000, 72.9400000,
        '2026-08-15 10:00:00+00', '2026-08-15 10:00:01+00'
    );

    -- 2. Insert a September 2026 ping
    INSERT INTO tracking.location_pings (
        trip_id, driver_id, vehicle_id, latitude, longitude,
        device_recorded_at, server_received_at
    ) VALUES (
        v_trip_id, v_driver_id, v_vehicle_id, 18.9555000, 72.9555000,
        '2026-09-14 02:30:00+00', '2026-09-14 02:30:01+00'
    );

    -- 3. Verify August partition received the row
    SELECT COUNT(*) INTO v_count_aug FROM tracking.location_pings_2026_08 WHERE trip_id = v_trip_id;
    IF v_count_aug < 1 THEN
        RAISE EXCEPTION 'TEST FAILED: August ping was not routed to location_pings_2026_08!';
    END IF;

    -- 4. Verify September partition received the row
    SELECT COUNT(*) INTO v_count_sep FROM tracking.location_pings_2026_09 WHERE trip_id = v_trip_id;
    IF v_count_sep < 1 THEN
        RAISE EXCEPTION 'TEST FAILED: September ping was not routed to location_pings_2026_09!';
    END IF;

    -- 5. Verify latest_locations projection updated to the newer September coordinate
    SELECT latitude INTO v_latest_lat FROM tracking.latest_locations WHERE trip_id = v_trip_id;
    IF v_latest_lat <> 18.9555000 THEN
        RAISE EXCEPTION 'TEST FAILED: latest_locations projection not updated to newest coordinates (Got: %, Expected: 18.9555000)', v_latest_lat;
    END IF;

    RAISE NOTICE 'TEST PASSED: Partition routing and latest_locations projection verified.';
END $$;
