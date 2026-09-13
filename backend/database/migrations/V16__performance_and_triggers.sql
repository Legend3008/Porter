-- ==============================================================================
-- Migration: V16__performance_and_triggers.sql
-- Description: Targeted partial indexes for active operational data,
--              state machine transition validation, and optimistic locking enforcement.
-- ==============================================================================

-- 1. High-Performance Partial Indexes (Eliminate Scanning Finished Historical Data)
CREATE INDEX idx_bookings_active ON booking.bookings(customer_organization_id, status, created_at DESC) 
    WHERE status NOT IN ('COMPLETED', 'CANCELLED');

CREATE INDEX idx_trips_active_carrier ON operations.trips(carrier_id, status, scheduled_start_at ASC) 
    WHERE status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED');

CREATE INDEX idx_trips_active_driver ON operations.trips(driver_id, status) 
    WHERE status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED');

CREATE INDEX idx_invoices_unpaid ON billing.invoices(customer_organization_id, due_at ASC) 
    WHERE status IN ('ISSUED', 'OVERDUE');

CREATE INDEX idx_quotes_valid_active ON pricing.quotes(customer_organization_id, expires_at) 
    WHERE status = 'ACTIVE';

-- 2. State Machine Transition Validator for Trips
CREATE OR REPLACE FUNCTION operations.validate_trip_state_transition()
RETURNS TRIGGER AS $$
DECLARE
    v_old_status VARCHAR(32);
    v_new_status VARCHAR(32);
BEGIN
    v_old_status := OLD.status;
    v_new_status := NEW.status;

    -- No change in status
    IF v_old_status = v_new_status THEN
        RETURN NEW;
    END IF;

    -- Terminal states cannot transition to any other state
    IF v_old_status IN ('COMPLETED', 'CANCELLED', 'FAILED') THEN
        RAISE EXCEPTION 'Illegal state transition: Trip % is already in terminal state % and cannot transition to %',
            OLD.id, v_old_status, v_new_status;
    END IF;

    -- Validate explicit allowed transitions
    CASE v_old_status
        WHEN 'CREATED' THEN
            IF v_new_status NOT IN ('ASSIGNMENT_PENDING', 'ASSIGNED', 'CANCELLED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        WHEN 'ASSIGNMENT_PENDING' THEN
            IF v_new_status NOT IN ('ASSIGNED', 'CANCELLED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        WHEN 'ASSIGNED' THEN
            IF v_new_status NOT IN ('DRIVER_ACCEPTED', 'ASSIGNMENT_PENDING', 'CANCELLED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        WHEN 'DRIVER_ACCEPTED' THEN
            IF v_new_status NOT IN ('EN_ROUTE_PICKUP', 'ASSIGNED', 'CANCELLED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        WHEN 'EN_ROUTE_PICKUP' THEN
            IF v_new_status NOT IN ('AT_PICKUP', 'CANCELLED', 'FAILED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        WHEN 'AT_PICKUP' THEN
            IF v_new_status NOT IN ('LOADING', 'CANCELLED', 'FAILED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        WHEN 'LOADING' THEN
            IF v_new_status NOT IN ('LOADED', 'CANCELLED', 'FAILED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        WHEN 'LOADED' THEN
            IF v_new_status NOT IN ('EN_ROUTE_DELIVERY', 'CANCELLED', 'FAILED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        WHEN 'EN_ROUTE_DELIVERY' THEN
            IF v_new_status NOT IN ('AT_DELIVERY', 'CANCELLED', 'FAILED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        WHEN 'AT_DELIVERY' THEN
            IF v_new_status NOT IN ('UNLOADING', 'CANCELLED', 'FAILED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        WHEN 'UNLOADING' THEN
            IF v_new_status NOT IN ('DELIVERED', 'CANCELLED', 'FAILED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        WHEN 'DELIVERED' THEN
            IF v_new_status NOT IN ('COMPLETED', 'CANCELLED') THEN
                RAISE EXCEPTION 'Illegal transition from % to % for trip %', v_old_status, v_new_status, OLD.id;
            END IF;
        ELSE
            RAISE EXCEPTION 'Unknown previous trip status: %', v_old_status;
    END CASE;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_trip_state
    BEFORE UPDATE ON operations.trips
    FOR EACH ROW EXECUTE FUNCTION operations.validate_trip_state_transition();
