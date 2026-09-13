-- ==============================================================================
-- Migration: V02__identity_and_party.sql
-- Description: Identity, authentication, RBAC, tenancy, and tax registrations.
-- ==============================================================================

-- 1. Organizations (Party Namespace - Tenancy & Business Entities)
CREATE TABLE party.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    party_type VARCHAR(32) NOT NULL CHECK (party_type IN ('CUSTOMER', 'CARRIER', 'PLATFORM', 'PARTNER')),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TRIGGER trg_organizations_updated_at
    BEFORE UPDATE ON party.organizations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Users (Identity Namespace)
CREATE TABLE identity.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(32) NOT NULL,
    password_hash TEXT,
    full_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'DEACTIVATED')),
    email_verified_at TIMESTAMPTZ,
    phone_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_users_phone ON identity.users (phone);
CREATE INDEX idx_users_email ON identity.users (email);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON identity.users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 3. Roles and Permissions (RBAC)
CREATE TABLE identity.roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE identity.permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(128) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE identity.role_permissions (
    role_id UUID NOT NULL REFERENCES identity.roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES identity.permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE identity.user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES identity.roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (user_id, role_id)
);

-- 4. Organization Memberships (Multi-Tenant Mapping)
CREATE TABLE identity.organization_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES party.organizations(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE RESTRICT,
    role_id UUID NOT NULL REFERENCES identity.roles(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INVITED', 'SUSPENDED', 'REMOVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_memberships_user ON identity.organization_memberships(user_id);
CREATE INDEX idx_memberships_org ON identity.organization_memberships(organization_id);

CREATE TRIGGER trg_org_memberships_updated_at
    BEFORE UPDATE ON identity.organization_memberships
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 5. Refresh Tokens (Session Security)
CREATE TABLE identity.refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    last_used_at TIMESTAMPTZ,
    device_id VARCHAR(128),
    ip_address INET,
    user_agent TEXT
);

CREATE INDEX idx_refresh_tokens_user ON identity.refresh_tokens(user_id) WHERE revoked_at IS NULL;

-- 6. Organization Addresses
CREATE TABLE party.organization_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES party.organizations(id) ON DELETE CASCADE,
    address_type VARCHAR(32) NOT NULL DEFAULT 'REGISTERED' CHECK (address_type IN ('REGISTERED', 'OPERATIONAL', 'BILLING', 'BRANCH')),
    line1 TEXT NOT NULL,
    line2 TEXT,
    city VARCHAR(128) NOT NULL,
    state VARCHAR(128) NOT NULL,
    postal_code VARCHAR(32) NOT NULL,
    country VARCHAR(64) NOT NULL DEFAULT 'India',
    latitude NUMERIC(10, 7) CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(10, 7) CHECK (longitude BETWEEN -180 AND 180),
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TRIGGER trg_org_addresses_updated_at
    BEFORE UPDATE ON party.organization_addresses
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 7. Organization Contacts
CREATE TABLE party.organization_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES party.organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(32) NOT NULL,
    designation VARCHAR(128),
    is_primary BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TRIGGER trg_org_contacts_updated_at
    BEFORE UPDATE ON party.organization_contacts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 8. Tax Registrations (GSTIN / PAN Compliance)
CREATE TABLE party.tax_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES party.organizations(id) ON DELETE RESTRICT,
    tax_registration_number VARCHAR(64) NOT NULL,
    registration_type VARCHAR(32) NOT NULL DEFAULT 'GSTIN' CHECK (registration_type IN ('GSTIN', 'PAN', 'TAN')),
    legal_name VARCHAR(255) NOT NULL,
    state_code VARCHAR(8) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED' CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (registration_type, tax_registration_number)
);

CREATE INDEX idx_tax_registrations_org ON party.tax_registrations(organization_id);

CREATE TRIGGER trg_tax_registrations_updated_at
    BEFORE UPDATE ON party.tax_registrations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
