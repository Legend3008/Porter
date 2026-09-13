-- ==============================================================================
-- Migration: V03__fleet_and_geo.sql
-- Description: Geographic topology (locations, ports, terminals, corridors)
--              and Fleet supply (carriers, drivers, vehicles, assignments).
-- ==============================================================================

-- 1. Geo Locations
CREATE TABLE geo.locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) UNIQUE,
    name VARCHAR(255) NOT NULL,
    address_line TEXT,
    city VARCHAR(128) NOT NULL,
    district VARCHAR(128),
    state VARCHAR(128) NOT NULL,
    country VARCHAR(64) NOT NULL DEFAULT 'India',
    postal_code VARCHAR(32),
    latitude NUMERIC(10, 7) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(10, 7) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    location_type VARCHAR(32) NOT NULL CHECK (location_type IN ('PORT', 'TERMINAL', 'WAREHOUSE', 'CUSTOMER_SITE', 'DEPOT', 'OTHER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_locations_city ON geo.locations(city);
CREATE INDEX idx_locations_state ON geo.locations(state);
CREATE INDEX idx_locations_type ON geo.locations(location_type);

CREATE TRIGGER trg_locations_updated_at
    BEFORE UPDATE ON geo.locations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Ports (Configurable Container Gateways)
CREATE TABLE geo.ports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(32) UNIQUE NOT NULL, -- e.g. INNSA, INMUN, INMAA
    name VARCHAR(255) NOT NULL,
    location_id UUID NOT NULL REFERENCES geo.locations(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TRIGGER trg_ports_updated_at
    BEFORE UPDATE ON geo.ports
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 3. Port Terminals / CFS
CREATE TABLE geo.terminals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    port_id UUID NOT NULL REFERENCES geo.ports(id) ON DELETE RESTRICT,
    code VARCHAR(64) NOT NULL, -- GTI, NSICT, BMCT, APM
    name VARCHAR(255) NOT NULL,
    location_id UUID NOT NULL REFERENCES geo.locations(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (port_id, code)
);

CREATE TRIGGER trg_terminals_updated_at
    BEFORE UPDATE ON geo.terminals
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 4. Lanes (Logistics Corridors)
CREATE TABLE geo.lanes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) UNIQUE NOT NULL, -- e.g. INNSA-PUN-01
    name VARCHAR(255) NOT NULL,
    origin_location_id UUID NOT NULL REFERENCES geo.locations(id) ON DELETE RESTRICT,
    destination_location_id UUID NOT NULL REFERENCES geo.locations(id) ON DELETE RESTRICT,
    distance_km NUMERIC(8, 2) NOT NULL CHECK (distance_km > 0),
    estimated_duration_minutes INT NOT NULL CHECK (estimated_duration_minutes > 0),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_lanes_origin ON geo.lanes(origin_location_id);
CREATE INDEX idx_lanes_dest ON geo.lanes(destination_location_id);

CREATE TRIGGER trg_lanes_updated_at
    BEFORE UPDATE ON geo.lanes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 5. Lane Stops / Toll checkpoints
CREATE TABLE geo.lane_stops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lane_id UUID NOT NULL REFERENCES geo.lanes(id) ON DELETE CASCADE,
    location_id UUID NOT NULL REFERENCES geo.locations(id) ON DELETE RESTRICT,
    sequence INT NOT NULL CHECK (sequence > 0),
    is_toll_point BOOLEAN NOT NULL DEFAULT false,
    toll_cost_minor BIGINT NOT NULL DEFAULT 0 CHECK (toll_cost_minor >= 0),
    UNIQUE (lane_id, sequence)
);

-- 6. Carriers (Supply Fleet Operators)
CREATE TABLE fleet.carriers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL UNIQUE REFERENCES party.organizations(id) ON DELETE RESTRICT,
    carrier_code VARCHAR(64) UNIQUE NOT NULL,
    fleet_size INT NOT NULL DEFAULT 0 CHECK (fleet_size >= 0),
    rating NUMERIC(3, 2) DEFAULT 5.00 CHECK (rating BETWEEN 0 AND 5),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ONBOARDING', 'ACTIVE', 'SUSPENDED', 'BLOCKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TRIGGER trg_carriers_updated_at
    BEFORE UPDATE ON fleet.carriers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 7. Drivers
CREATE TABLE fleet.drivers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    carrier_id UUID NOT NULL REFERENCES fleet.carriers(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL UNIQUE REFERENCES identity.users(id) ON DELETE RESTRICT,
    driver_code VARCHAR(64) UNIQUE NOT NULL,
    license_number_encrypted TEXT NOT NULL,
    license_expiry_date DATE NOT NULL,
    verification_status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED' CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    verified_at TIMESTAMPTZ,
    rating NUMERIC(3, 2) DEFAULT 5.00 CHECK (rating BETWEEN 0 AND 5),
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'ON_TRIP', 'OFF_DUTY', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_drivers_carrier ON fleet.drivers(carrier_id);
CREATE INDEX idx_drivers_status ON fleet.drivers(status);

CREATE TRIGGER trg_drivers_updated_at
    BEFORE UPDATE ON fleet.drivers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 8. Vehicles (Commercial Trailers / Heavy Haulers)
CREATE TABLE fleet.vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    carrier_id UUID NOT NULL REFERENCES fleet.carriers(id) ON DELETE RESTRICT,
    registration_number VARCHAR(32) UNIQUE NOT NULL,
    vehicle_type VARCHAR(64) NOT NULL,
    capacity_kg INT NOT NULL CHECK (capacity_kg > 0),
    capacity_volume NUMERIC(8, 2),
    manufacture_year INT CHECK (manufacture_year BETWEEN 1990 AND 2030),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_vehicles_carrier ON fleet.vehicles(carrier_id);

CREATE TRIGGER trg_vehicles_updated_at
    BEFORE UPDATE ON fleet.vehicles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 9. Driver-Vehicle Assignments (With Historical Tracking)
CREATE TABLE fleet.driver_vehicle_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id UUID NOT NULL REFERENCES fleet.drivers(id) ON DELETE RESTRICT,
    vehicle_id UUID NOT NULL REFERENCES fleet.vehicles(id) ON DELETE RESTRICT,
    assigned_from TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    assigned_until TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'REVOKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- Critical concurrency rule: only 1 active assignment per driver and per vehicle
CREATE UNIQUE INDEX idx_unique_active_driver_assignment 
    ON fleet.driver_vehicle_assignments(driver_id) 
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX idx_unique_active_vehicle_assignment 
    ON fleet.driver_vehicle_assignments(vehicle_id) 
    WHERE status = 'ACTIVE';
