-- ==============================================================================
-- Migration: V15__audit_and_integrations.sql
-- Description: Mission-critical platform infrastructure: API idempotency tracking,
--              transactional outbox pattern, event deduplication, and append-only audit.
-- ==============================================================================

-- 1. Idempotency Keys (Network Timeout & Double-Click Protection)
CREATE TABLE integration.idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(128) NOT NULL,
    actor_id UUID NOT NULL,
    operation VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    response_status INT,
    response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    UNIQUE (actor_id, operation, idempotency_key)
);

CREATE INDEX idx_idempotency_lookup ON integration.idempotency_keys(actor_id, operation, idempotency_key);
CREATE INDEX idx_idempotency_expiry ON integration.idempotency_keys(expires_at);

-- 2. Transactional Outbox Events (Atomic Event Publishing)
CREATE TABLE integration.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    event_type VARCHAR(128) NOT NULL,
    event_version INT NOT NULL DEFAULT 1,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    published_at TIMESTAMPTZ,
    attempt_count INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'PUBLISHED', 'FAILED', 'DEAD_LETTER'
    )),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_outbox_events_pending ON integration.outbox_events(status, created_at ASC) 
    WHERE status = 'PENDING';

-- 3. Consumer Inbox Events (At-Least-Once Event Deduplication)
CREATE TABLE integration.inbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    consumer VARCHAR(128) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    processed_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSED' CHECK (status IN ('PROCESSED', 'FAILED', 'IGNORED')),
    failure_reason TEXT,
    UNIQUE (event_id, consumer)
);

-- 4. Audit Logs (Immutable Historical & Security Trail)
CREATE TABLE audit.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES identity.users(id) ON DELETE SET NULL,
    actor_type VARCHAR(32) NOT NULL DEFAULT 'USER',
    action VARCHAR(128) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    request_id VARCHAR(128),
    ip_address INET,
    user_agent TEXT,
    before_data JSONB,
    after_data JSONB,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_audit_logs_entity ON audit.audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_logs_actor ON audit.audit_logs(actor_user_id, created_at DESC);

-- Audit logs are strictly append-only
CREATE TRIGGER trg_audit_logs_immutable
    BEFORE UPDATE OR DELETE ON audit.audit_logs
    FOR EACH ROW EXECUTE FUNCTION enforce_immutability();
