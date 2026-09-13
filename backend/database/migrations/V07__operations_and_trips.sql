-- ==============================================================================
-- Migration: V07__operations_and_trips.sql
-- Description: Operational trips, assignments, job offers, multi-stop routes,
--              gate timing events, and operational exception tracking.
-- ==============================================================================

-- 1. Trips (Operational Execution of Bookings)
CREATE TABLE operations.trips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_number VARCHAR(64) UNIQUE NOT NULL,
    booking_id UUID NOT NULL UNIQUE REFERENCES booking.bookings(id) ON DELETE RESTRICT,
    carrier_id UUID REFERENCES fleet.carriers(id) ON DELETE RESTRICT,
    driver_id UUID REFERENCES fleet.drivers(id) ON DELETE RESTRICT,
    vehicle_id UUID REFERENCES fleet.vehicles(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED' CHECK (status IN (
        'CREATED', 'ASSIGNMENT_PENDING', 'ASSIGNED', 'DRIVER_ACCEPTED',
        'EN_ROUTE_PICKUP', 'AT_PICKUP', 'LOADING', 'LOADED',
        'EN_ROUTE_DELIVERY', 'AT_DELIVERY', 'UNLOADING', 'DELIVERED',
        'COMPLETED', 'CANCELLED', 'FAILED'
    )),
    scheduled_start_at TIMESTAMPTZ,
    actual_start_at TIMESTAMPTZ,
    actual_end_at TIMESTAMPTZ,
    current_stop_sequence INT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0, -- Optimistic concurrency lock counter
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_trips_driver_status ON operations.trips(driver_id, status);
CREATE INDEX idx_trips_carrier_status ON operations.trips(carrier_id, status);
CREATE INDEX idx_trips_status_scheduled ON operations.trips(status, scheduled_start_at);

CREATE TRIGGER trg_trips_updated_at
    BEFORE UPDATE ON operations.trips
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Trip Stops (Sequential Route Waypoints)
CREATE TABLE operations.trip_stops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES operations.trips(id) ON DELETE CASCADE,
    sequence INT NOT NULL CHECK (sequence > 0),
    stop_type VARCHAR(32) NOT NULL CHECK (stop_type IN ('ORIGIN', 'DESTINATION', 'INTERMEDIATE')),
    location_id UUID NOT NULL REFERENCES geo.locations(id) ON DELETE RESTRICT,
    planned_arrival_at TIMESTAMPTZ,
    planned_departure_at TIMESTAMPTZ,
    actual_arrival_at TIMESTAMPTZ,
    actual_departure_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ARRIVED', 'DEPARTED', 'SKIPPED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (trip_id, sequence)
);

CREATE TRIGGER trg_trip_stops_updated_at
    BEFORE UPDATE ON operations.trip_stops
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 3. Trip Assignments (Historical Driver & Vehicle Record)
CREATE TABLE operations.trip_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES operations.trips(id) ON DELETE RESTRICT,
    carrier_id UUID NOT NULL REFERENCES fleet.carriers(id) ON DELETE RESTRICT,
    driver_id UUID NOT NULL REFERENCES fleet.drivers(id) ON DELETE RESTRICT,
    vehicle_id UUID NOT NULL REFERENCES fleet.vehicles(id) ON DELETE RESTRICT,
    assignment_status VARCHAR(32) NOT NULL DEFAULT 'OFFERED' CHECK (assignment_status IN (
        'OFFERED', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'COMPLETED'
    )),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    accepted_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- Invariant: Exactly one ACCEPTED assignment allowed per trip at any given time
CREATE UNIQUE INDEX idx_unique_active_trip_assignment 
    ON operations.trip_assignments(trip_id) 
    WHERE assignment_status = 'ACCEPTED';

CREATE TRIGGER trg_trip_assignments_updated_at
    BEFORE UPDATE ON operations.trip_assignments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 4. Job Offers (Broadcast Dispatch & Competition Control)
CREATE TABLE operations.job_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES operations.trips(id) ON DELETE CASCADE,
    driver_id UUID NOT NULL REFERENCES fleet.drivers(id) ON DELETE RESTRICT,
    offered_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OFFERED' CHECK (status IN (
        'OFFERED', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED'
    )),
    responded_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (trip_id, driver_id)
);

-- Concurrency Invariant: Prevent double acceptance among competing drivers
CREATE UNIQUE INDEX idx_unique_accepted_job_offer 
    ON operations.job_offers(trip_id) 
    WHERE status = 'ACCEPTED';

-- 5. Trip Status History (Append-Only Audit)
CREATE TABLE operations.trip_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES operations.trips(id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    actor_id UUID REFERENCES identity.users(id) ON DELETE SET NULL,
    actor_type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    source VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_trip_status_history_trip ON operations.trip_status_history(trip_id, occurred_at ASC);

-- 6. Automatic Trip Status History Capture
CREATE OR REPLACE FUNCTION operations.capture_trip_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO operations.trip_status_history (
            trip_id, from_status, to_status, actor_type, source, reason, occurred_at
        ) VALUES (
            NEW.id, NULL, NEW.status, 'SYSTEM', 'SYSTEM', 'Trip initialized', clock_timestamp()
        );
    ELSIF (TG_OP = 'UPDATE' AND OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO operations.trip_status_history (
            trip_id, from_status, to_status, actor_type, source, reason, occurred_at
        ) VALUES (
            NEW.id, OLD.status, NEW.status, 'SYSTEM', 'SYSTEM', 'Trip status transition', clock_timestamp()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_capture_trip_status
    AFTER INSERT OR UPDATE ON operations.trips
    FOR EACH ROW EXECUTE FUNCTION operations.capture_trip_status_change();

-- 7. Gate Events (CFS / Port Terminal Timestamps for Detention Calculation)
CREATE TABLE operations.gate_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES operations.trips(id) ON DELETE CASCADE,
    stop_id UUID REFERENCES operations.trip_stops(id) ON DELETE SET NULL,
    event_type VARCHAR(64) NOT NULL CHECK (event_type IN (
        'GATE_IN', 'GATE_OUT', 'LOADING_START', 'LOADING_END', 'UNLOADING_START', 'UNLOADING_END'
    )),
    event_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    source VARCHAR(32) NOT NULL DEFAULT 'DRIVER_APP',
    actor_id UUID REFERENCES identity.users(id) ON DELETE SET NULL,
    latitude NUMERIC(10, 7) CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(10, 7) CHECK (longitude BETWEEN -180 AND 180),
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_gate_events_trip ON operations.gate_events(trip_id, event_at ASC);

-- 8. Operations Exceptions (Stuck Trips, SLA Violations, Delayed Gates)
CREATE TABLE operations.exceptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(64) NOT NULL, -- TRIP, BOOKING, CONTAINER
    entity_id UUID NOT NULL,
    exception_type VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'DISMISSED')),
    description TEXT NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    assigned_to UUID REFERENCES identity.users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMPTZ,
    resolution TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_exceptions_status ON operations.exceptions(status, severity);

CREATE TRIGGER trg_exceptions_updated_at
    BEFORE UPDATE ON operations.exceptions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
