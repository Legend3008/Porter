-- ==============================================================================
-- Migration: V08__tracking_partitioned.sql
-- Description: High-volume GPS telemetry table partitioned by month
--              and dedicated latest_locations projection for sub-5ms app queries.
-- ==============================================================================

-- 1. Location Pings Partitioned Table (Range partitioned by server_received_at)
CREATE TABLE tracking.location_pings (
    id BIGSERIAL,
    trip_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    latitude NUMERIC(10, 7) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(10, 7) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    accuracy_meters NUMERIC(6, 2) CHECK (accuracy_meters >= 0),
    speed_mps NUMERIC(6, 2) CHECK (speed_mps >= 0),
    heading_degrees NUMERIC(5, 2) CHECK (heading_degrees BETWEEN 0 AND 360),
    device_recorded_at TIMESTAMPTZ NOT NULL,
    server_received_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    source VARCHAR(32) NOT NULL DEFAULT 'DRIVER_APP',
    sequence_number BIGINT,
    battery_percent SMALLINT CHECK (battery_percent BETWEEN 0 AND 100),
    metadata JSONB DEFAULT '{}'::jsonb,
    PRIMARY KEY (id, server_received_at)
) PARTITION BY RANGE (server_received_at);

-- 2. Pre-created Monthly Partitions
CREATE TABLE tracking.location_pings_2026_08 PARTITION OF tracking.location_pings
    FOR VALUES FROM ('2026-08-01 00:00:00+00') TO ('2026-09-01 00:00:00+00');

CREATE TABLE tracking.location_pings_2026_09 PARTITION OF tracking.location_pings
    FOR VALUES FROM ('2026-09-01 00:00:00+00') TO ('2026-10-01 00:00:00+00');

CREATE TABLE tracking.location_pings_2026_10 PARTITION OF tracking.location_pings
    FOR VALUES FROM ('2026-10-01 00:00:00+00') TO ('2026-11-01 00:00:00+00');

CREATE TABLE tracking.location_pings_2026_11 PARTITION OF tracking.location_pings
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');

CREATE TABLE tracking.location_pings_2026_12 PARTITION OF tracking.location_pings
    FOR VALUES FROM ('2026-12-01 00:00:00+00') TO ('2027-01-01 00:00:00+00');

CREATE TABLE tracking.location_pings_2027_01 PARTITION OF tracking.location_pings
    FOR VALUES FROM ('2027-01-01 00:00:00+00') TO ('2027-02-01 00:00:00+00');

CREATE TABLE tracking.location_pings_default PARTITION OF tracking.location_pings DEFAULT;

-- Partition indexes (propagated to each partition table)
CREATE INDEX idx_location_pings_trip ON tracking.location_pings(trip_id, device_recorded_at DESC);
CREATE INDEX idx_location_pings_driver ON tracking.location_pings(driver_id, device_recorded_at DESC);

-- 3. Latest Locations Table (Unpartitioned 1-row-per-trip projection for fast reads)
CREATE TABLE tracking.latest_locations (
    trip_id UUID PRIMARY KEY REFERENCES operations.trips(id) ON DELETE CASCADE,
    driver_id UUID NOT NULL REFERENCES fleet.drivers(id) ON DELETE RESTRICT,
    vehicle_id UUID NOT NULL REFERENCES fleet.vehicles(id) ON DELETE RESTRICT,
    latitude NUMERIC(10, 7) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(10, 7) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    accuracy_meters NUMERIC(6, 2),
    speed_mps NUMERIC(6, 2),
    heading_degrees NUMERIC(5, 2),
    device_recorded_at TIMESTAMPTZ NOT NULL,
    server_received_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    sequence_number BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- 4. Projection Trigger: Update latest_locations on new ping
CREATE OR REPLACE FUNCTION tracking.update_latest_location_projection()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO tracking.latest_locations (
        trip_id, driver_id, vehicle_id, latitude, longitude,
        accuracy_meters, speed_mps, heading_degrees,
        device_recorded_at, server_received_at, sequence_number, updated_at
    ) VALUES (
        NEW.trip_id, NEW.driver_id, NEW.vehicle_id, NEW.latitude, NEW.longitude,
        NEW.accuracy_meters, NEW.speed_mps, NEW.heading_degrees,
        NEW.device_recorded_at, NEW.server_received_at, NEW.sequence_number, clock_timestamp()
    )
    ON CONFLICT (trip_id) DO UPDATE SET
        driver_id = EXCLUDED.driver_id,
        vehicle_id = EXCLUDED.vehicle_id,
        latitude = EXCLUDED.latitude,
        longitude = EXCLUDED.longitude,
        accuracy_meters = EXCLUDED.accuracy_meters,
        speed_mps = EXCLUDED.speed_mps,
        heading_degrees = EXCLUDED.heading_degrees,
        device_recorded_at = EXCLUDED.device_recorded_at,
        server_received_at = EXCLUDED.server_received_at,
        sequence_number = EXCLUDED.sequence_number,
        updated_at = clock_timestamp()
    WHERE EXCLUDED.device_recorded_at >= tracking.latest_locations.device_recorded_at;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_location_pings_projection
    AFTER INSERT ON tracking.location_pings
    FOR EACH ROW EXECUTE FUNCTION tracking.update_latest_location_projection();
