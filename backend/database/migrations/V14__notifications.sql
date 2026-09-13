-- ==============================================================================
-- Migration: V14__notifications.sql
-- Description: Asynchronous alert delivery (Push, SMS, Email) decoupled from
--              authoritative commercial transactions.
-- ==============================================================================

-- 1. Notification Preferences
CREATE TABLE notification.notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES identity.users(id) ON DELETE CASCADE,
    push_enabled BOOLEAN NOT NULL DEFAULT true,
    sms_enabled BOOLEAN NOT NULL DEFAULT true,
    email_enabled BOOLEAN NOT NULL DEFAULT true,
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TRIGGER trg_notif_prefs_updated_at
    BEFORE UPDATE ON notification.notification_preferences
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Notification Dispatch Queue
CREATE TABLE notification.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    channel VARCHAR(16) NOT NULL CHECK (channel IN ('PUSH', 'SMS', 'EMAIL', 'WHATSAPP')),
    priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED')),
    reference_type VARCHAR(64), -- BOOKING, TRIP, PAYMENT
    reference_id UUID,
    payload JSONB DEFAULT '{}'::jsonb,
    scheduled_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    sent_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_notifications_pending ON notification.notifications(status, scheduled_at) 
    WHERE status = 'PENDING';
CREATE INDEX idx_notifications_recipient ON notification.notifications(recipient_user_id, created_at DESC);

-- 3. Notification Delivery Attempts (Provider Telemetry)
CREATE TABLE notification.delivery_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notification.notifications(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL, -- FCM, TWILIO, AWS_SNS, SENDGRID
    provider_message_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    response_payload JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_delivery_attempts_notification ON notification.delivery_attempts(notification_id);
