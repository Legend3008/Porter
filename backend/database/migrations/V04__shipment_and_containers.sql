-- ==============================================================================
-- Migration: V04__shipment_and_containers.sql
-- Description: Containers, shipments, multi-container linking, routing stops,
--              and container telemetry/gate events.
-- ==============================================================================

-- 1. Physical Containers (ISO Shipping Units)
CREATE TABLE shipment.containers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    container_number VARCHAR(32) UNIQUE NOT NULL,
    container_type VARCHAR(32) NOT NULL CHECK (container_type IN (
        'DRY_20FT', 'DRY_40FT', 'HIGH_CUBE_40FT', 'REEFER_20FT', 'REEFER_40FT', 'FLAT_RACK_20FT'
    )),
    iso_code VARCHAR(16) NOT NULL, -- 20G1, 40G1, 40HC, 20R1
    tare_weight_kg INT,
    max_payload_kg INT,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'IN_USE', 'DAMAGED', 'DECOMMISSIONED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TRIGGER trg_containers_updated_at
    BEFORE UPDATE ON shipment.containers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Shipments (Commercial Logistics Requirement)
CREATE TABLE shipment.shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_organization_id UUID NOT NULL REFERENCES party.organizations(id) ON DELETE RESTRICT,
    reference_number VARCHAR(64) UNIQUE NOT NULL,
    cargo_description TEXT NOT NULL,
    cargo_weight_kg NUMERIC(10, 2) NOT NULL CHECK (cargo_weight_kg > 0),
    cargo_volume_cbm NUMERIC(8, 2),
    cargo_value_minor BIGINT NOT NULL DEFAULT 0 CHECK (cargo_value_minor >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    commodity VARCHAR(128) NOT NULL,
    is_hazardous BOOLEAN NOT NULL DEFAULT false,
    hazardous_class VARCHAR(32),
    un_number VARCHAR(16),
    special_instructions TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED' CHECK (status IN (
        'DRAFT', 'CREATED', 'BOOKED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED'
    )),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_shipments_customer ON shipment.shipments(customer_organization_id);
CREATE INDEX idx_shipments_status ON shipment.shipments(status);

CREATE TRIGGER trg_shipments_updated_at
    BEFORE UPDATE ON shipment.shipments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 3. Shipment-Containers (Multi-Container Support per Shipment)
CREATE TABLE shipment.shipment_containers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipment.shipments(id) ON DELETE CASCADE,
    container_id UUID NOT NULL REFERENCES shipment.containers(id) ON DELETE RESTRICT,
    sequence INT NOT NULL DEFAULT 1 CHECK (sequence > 0),
    seal_number VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED' CHECK (status IN (
        'ASSIGNED', 'LOADED', 'GATE_IN', 'DISCHARGED', 'RETURNED'
    )),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (shipment_id, container_id)
);

CREATE INDEX idx_shipment_containers_shipment ON shipment.shipment_containers(shipment_id);

-- 4. Shipment Routing Stops
CREATE TABLE shipment.shipment_stops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipment.shipments(id) ON DELETE CASCADE,
    sequence INT NOT NULL CHECK (sequence > 0),
    stop_type VARCHAR(32) NOT NULL CHECK (stop_type IN ('ORIGIN', 'DESTINATION', 'INTERMEDIATE')),
    location_id UUID NOT NULL REFERENCES geo.locations(id) ON DELETE RESTRICT,
    planned_arrival_at TIMESTAMPTZ,
    planned_departure_at TIMESTAMPTZ,
    actual_arrival_at TIMESTAMPTZ,
    actual_departure_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (shipment_id, sequence)
);

CREATE TRIGGER trg_shipment_stops_updated_at
    BEFORE UPDATE ON shipment.shipment_stops
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 5. Container Lifecycle History Events (Append-Only)
CREATE TABLE shipment.container_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    container_id UUID NOT NULL REFERENCES shipment.containers(id) ON DELETE RESTRICT,
    event_type VARCHAR(64) NOT NULL,
    event_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    location_id UUID REFERENCES geo.locations(id) ON DELETE SET NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_container_events_container ON shipment.container_events(container_id, event_at DESC);
