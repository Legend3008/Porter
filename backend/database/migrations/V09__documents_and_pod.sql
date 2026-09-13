-- ==============================================================================
-- Migration: V09__documents_and_pod.sql
-- Description: Shipping documents, revision history, and tamper-evident
--              Proof of Delivery (POD) records with cryptographic checksums.
-- ==============================================================================

-- 1. Shipping Documents Metadata (Object storage keys + relational metadata)
CREATE TABLE documents.documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES booking.bookings(id) ON DELETE RESTRICT,
    trip_id UUID REFERENCES operations.trips(id) ON DELETE SET NULL,
    document_type VARCHAR(64) NOT NULL CHECK (document_type IN (
        'BILL_OF_LADING', 'DELIVERY_ORDER', 'GATE_PASS', 'COMMERCIAL_INVOICE',
        'PACKING_LIST', 'CUSTOMS_DECLARATION', 'PROOF_OF_DELIVERY', 'OTHER'
    )),
    status VARCHAR(32) NOT NULL DEFAULT 'UPLOADED' CHECK (status IN (
        'UPLOADED', 'VERIFIED', 'REJECTED', 'EXPIRED'
    )),
    storage_key TEXT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0),
    checksum_sha256 VARCHAR(64) NOT NULL,
    uploaded_by UUID REFERENCES identity.users(id) ON DELETE SET NULL,
    is_verified BOOLEAN NOT NULL DEFAULT false,
    verified_by UUID REFERENCES identity.users(id) ON DELETE SET NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_documents_booking ON documents.documents(booking_id);
CREATE INDEX idx_documents_trip ON documents.documents(trip_id);

CREATE TRIGGER trg_documents_updated_at
    BEFORE UPDATE ON documents.documents
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Document Revision History (Immutable Versions)
CREATE TABLE documents.document_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents.documents(id) ON DELETE CASCADE,
    version_number INT NOT NULL CHECK (version_number > 0),
    storage_key TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    uploaded_by UUID REFERENCES identity.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (document_id, version_number)
);

-- 3. Proof of Delivery (POD) Records
CREATE TABLE documents.pod_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL UNIQUE REFERENCES operations.trips(id) ON DELETE RESTRICT,
    document_id UUID REFERENCES documents.documents(id) ON DELETE SET NULL,
    receiver_name VARCHAR(255) NOT NULL,
    receiver_phone VARCHAR(32),
    signature_storage_key TEXT,
    latitude NUMERIC(10, 7) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(10, 7) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    device_recorded_at TIMESTAMPTZ NOT NULL,
    server_received_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    captured_by_driver_id UUID NOT NULL REFERENCES fleet.drivers(id) ON DELETE RESTRICT,
    verification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING', 'ACCEPTED', 'DISPUTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_pod_records_driver ON documents.pod_records(captured_by_driver_id);
