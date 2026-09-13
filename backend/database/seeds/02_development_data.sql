-- ==============================================================================
-- Seed Script: 02_development_data.sql
-- Description: Realistic development & sandbox dataset:
--              Acme Logistics (Shipper), Western Freight (Carrier),
--              active booking, quote breakdown, assigned trip, and balanced ledger entries.
-- ==============================================================================

-- 1. Shipper Organization (Acme Logistics Pvt Ltd)
INSERT INTO party.organizations (id, legal_name, display_name, party_type, status) VALUES
    ('10000000-0000-0000-0001-000000000001', 'Acme Logistics India Private Limited', 'Acme Logistics', 'CUSTOMER', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- Shipper Tax Registration (Maharashtra GSTIN)
INSERT INTO party.tax_registrations (id, organization_id, tax_registration_number, registration_type, legal_name, state_code, status, verified_at) VALUES
    ('10000000-0000-0000-0001-000000000002', '10000000-0000-0000-0001-000000000001', '27AABCU9603R1ZM', 'GSTIN', 'Acme Logistics India Private Limited', '27', 'VERIFIED', clock_timestamp())
ON CONFLICT (registration_type, tax_registration_number) DO NOTHING;

-- 2. Carrier Organization (Western Freight Haulers)
INSERT INTO party.organizations (id, legal_name, display_name, party_type, status) VALUES
    ('20000000-0000-0000-0001-000000000001', 'Western Freightlines & Logistics LLP', 'Western Haulers', 'CARRIER', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO fleet.carriers (id, organization_id, carrier_code, fleet_size, rating, status) VALUES
    ('20000000-0000-0000-0001-000000000002', '20000000-0000-0000-0001-000000000001', 'WF-MUM-01', 25, 4.85, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 3. Users: Shipper and Driver
INSERT INTO identity.users (id, email, phone, full_name, status, email_verified_at, phone_verified_at) VALUES
    ('30000000-0000-0000-0001-000000000001', 'ops@acmelogistics.in', '+919876543210', 'Ramesh Kumar', 'ACTIVE', clock_timestamp(), clock_timestamp()),
    ('30000000-0000-0000-0001-000000000002', 'driver.vikram@porter.com', '+919812345678', 'Vikram Singh', 'ACTIVE', clock_timestamp(), clock_timestamp())
ON CONFLICT (id) DO NOTHING;

-- Memberships
INSERT INTO identity.organization_memberships (organization_id, user_id, role_id, status) VALUES
    ('10000000-0000-0000-0001-000000000001', '30000000-0000-0000-0001-000000000001', '00000000-0000-0000-0001-000000000003', 'ACTIVE'),
    ('20000000-0000-0000-0001-000000000001', '30000000-0000-0000-0001-000000000002', '00000000-0000-0000-0001-000000000004', 'ACTIVE')
ON CONFLICT (organization_id, user_id) DO NOTHING;

-- 4. Driver & Vehicle
INSERT INTO fleet.drivers (id, carrier_id, user_id, driver_code, license_number_encrypted, license_expiry_date, verification_status, verified_at, rating, status) VALUES
    ('40000000-0000-0000-0001-000000000001', '20000000-0000-0000-0001-000000000002', '30000000-0000-0000-0001-000000000002', 'DRV-1002', 'enc:MH0320140019284', '2030-05-15', 'VERIFIED', clock_timestamp(), 4.90, 'ON_TRIP')
ON CONFLICT (id) DO NOTHING;

INSERT INTO fleet.vehicles (id, carrier_id, registration_number, vehicle_type, capacity_kg, capacity_volume, manufacture_year, status) VALUES
    ('40000000-0000-0000-0001-000000000002', '20000000-0000-0000-0001-000000000002', 'MH46BB9876', '40FT_TRAILER', 32000, 67.50, 2022, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- Active driver-vehicle pairing
INSERT INTO fleet.driver_vehicle_assignments (driver_id, vehicle_id, assigned_from, status) VALUES
    ('40000000-0000-0000-0001-000000000001', '40000000-0000-0000-0001-000000000002', clock_timestamp() - interval '2 days', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- 5. Physical Container & Shipment
INSERT INTO shipment.containers (id, container_number, container_type, iso_code, tare_weight_kg, max_payload_kg, status) VALUES
    ('50000000-0000-0000-0001-000000000001', 'MSCU8291024', 'HIGH_CUBE_40FT', '40HC', 3820, 28680, 'IN_USE')
ON CONFLICT (container_number) DO NOTHING;

INSERT INTO shipment.shipments (id, customer_organization_id, reference_number, cargo_description, cargo_weight_kg, cargo_volume_cbm, cargo_value_minor, currency, commodity, is_hazardous, status) VALUES
    ('50000000-0000-0000-0001-000000000002', '10000000-0000-0000-0001-000000000001', 'SHP-202609-00101', 'Precision Automotive Transmissions & Parts', 18500.00, 58.00, 850000000, 'INR', 'Automotive Components', false, 'BOOKED')
ON CONFLICT (reference_number) DO NOTHING;

INSERT INTO shipment.shipment_containers (shipment_id, container_id, sequence, seal_number, status) VALUES
    ('50000000-0000-0000-0001-000000000002', '50000000-0000-0000-0001-000000000001', 1, 'SEAL-MAERSK-98124', 'LOADED')
ON CONFLICT (shipment_id, container_id) DO NOTHING;

-- 6. Pricing Rules & Active Quote
INSERT INTO pricing.pricing_rule_sets (id, code, name, description) VALUES
    ('60000000-0000-0000-0001-000000000001', 'STD-PORT-2026', 'Standard Port Logistics Tariff 2026', 'Tariffs for Western Maharashtra corridors')
ON CONFLICT (code) DO NOTHING;

INSERT INTO pricing.pricing_rule_versions (id, rule_set_id, version_number, effective_from, status) VALUES
    ('60000000-0000-0000-0001-000000000002', '60000000-0000-0000-0001-000000000001', 1, '2026-01-01 00:00:00+00', 'ACTIVE')
ON CONFLICT (rule_set_id, version_number) DO NOTHING;

-- Quote: ₹45,500.00 total (₹35,000 base + ₹3,500 port + ₹2,200 toll + ₹4,800 fuel + ₹6,939 GST) -> Let's format cleanly
INSERT INTO pricing.quotes (id, quote_number, customer_organization_id, shipment_id, lane_id, pricing_rule_version_id, subtotal_minor, tax_minor, discount_minor, total_minor, currency, status, expires_at) VALUES
    ('60000000-0000-0000-0001-000000000003', 'QT-202609-00101', '10000000-0000-0000-0001-000000000001', '50000000-0000-0000-0001-000000000002', '00000000-0000-0000-0007-000000000001', '60000000-0000-0000-0001-000000000002', 4550000, 819000, 0, 5369000, 'INR', 'ACCEPTED', clock_timestamp() + interval '30 days')
ON CONFLICT (quote_number) DO NOTHING;

INSERT INTO pricing.quote_components (quote_id, component_type, description, quantity, unit_price_minor, amount_minor, tax_code) VALUES
    ('60000000-0000-0000-0001-000000000003', 'BASE_FREIGHT', 'Haulage JNPT to Pune ICD (40ft High Cube)', 1, 3500000, 3500000, '9965'),
    ('60000000-0000-0000-0001-000000000003', 'PORT_HANDLING', 'Terminal gate handling & documentation', 1, 350000, 350000, '9965'),
    ('60000000-0000-0000-0001-000000000003', 'TOLL', 'Mumbai-Pune Expressway Toll recovery', 1, 220000, 220000, '9965'),
    ('60000000-0000-0000-0001-000000000003', 'FUEL_SURCHARGE', 'Diesel fuel price adjustment', 1, 480000, 480000, '9965'),
    ('60000000-0000-0000-0001-000000000003', 'GST', 'Integrated Goods and Services Tax (18%)', 1, 819000, 819000, '9965')
ON CONFLICT DO NOTHING;

-- 7. Booking & Assigned Trip
INSERT INTO booking.bookings (id, booking_number, customer_organization_id, shipment_id, quote_id, status, total_amount_minor, paid_amount_minor, currency, requested_at, confirmed_at) VALUES
    ('70000000-0000-0000-0001-000000000001', 'BK-202609-00101', '10000000-0000-0000-0001-000000000001', '50000000-0000-0000-0001-000000000002', '60000000-0000-0000-0001-000000000003', 'ASSIGNED', 5369000, 5369000, 'INR', clock_timestamp() - interval '4 hours', clock_timestamp() - interval '3 hours')
ON CONFLICT (booking_number) DO NOTHING;

INSERT INTO operations.trips (id, trip_number, booking_id, carrier_id, driver_id, vehicle_id, status, scheduled_start_at, actual_start_at, current_stop_sequence, version) VALUES
    ('70000000-0000-0000-0001-000000000002', 'TR-202609-00101', '70000000-0000-0000-0001-000000000001', '20000000-0000-0000-0001-000000000002', '40000000-0000-0000-0001-000000000001', '40000000-0000-0000-0001-000000000002', 'DRIVER_ACCEPTED', clock_timestamp() - interval '2 hours', clock_timestamp() - interval '1 hour', 1, 1)
ON CONFLICT (trip_number) DO NOTHING;

-- Active accepted assignment
INSERT INTO operations.trip_assignments (trip_id, carrier_id, driver_id, vehicle_id, assignment_status, assigned_at, accepted_at) VALUES
    ('70000000-0000-0000-0001-000000000002', '20000000-0000-0000-0001-000000000002', '40000000-0000-0000-0001-000000000001', '40000000-0000-0000-0001-000000000002', 'ACCEPTED', clock_timestamp() - interval '2 hours', clock_timestamp() - interval '1 hour')
ON CONFLICT DO NOTHING;

-- 8. Telemetry Projection (Truck live at JNPT Gate 2)
INSERT INTO tracking.latest_locations (trip_id, driver_id, vehicle_id, latitude, longitude, accuracy_meters, speed_mps, heading_degrees, device_recorded_at, server_received_at, sequence_number) VALUES
    ('70000000-0000-0000-0001-000000000002', '40000000-0000-0000-0001-000000000001', '40000000-0000-0000-0001-000000000002', 18.9512000, 72.9534000, 4.50, 8.20, 112.50, clock_timestamp(), clock_timestamp(), 1042)
ON CONFLICT (trip_id) DO NOTHING;

-- 9. Commercial Payment & Invoice
INSERT INTO payments.payments (id, booking_id, customer_organization_id, amount_minor, currency, status, provider, provider_payment_id, method, completed_at) VALUES
    ('80000000-0000-0000-0001-000000000001', '70000000-0000-0000-0001-000000000001', '10000000-0000-0000-0001-000000000001', 5369000, 'INR', 'CAPTURED', 'RAZORPAY', 'pay_Ox9821hNq98a1', 'NETBANKING', clock_timestamp() - interval '3 hours')
ON CONFLICT (id) DO NOTHING;

INSERT INTO billing.invoices (id, invoice_number, customer_organization_id, booking_id, trip_id, status, currency, subtotal_minor, cgst_minor, sgst_minor, igst_minor, tax_minor, total_minor, paid_at) VALUES
    ('80000000-0000-0000-0001-000000000002', 'INV-202609-00101', '10000000-0000-0000-0001-000000000001', '70000000-0000-0000-0001-000000000001', '70000000-0000-0000-0001-000000000002', 'PAID', 'INR', 4550000, 0, 0, 819000, 819000, 5369000, clock_timestamp() - interval '3 hours')
ON CONFLICT (invoice_number) DO NOTHING;

-- 10. Balanced Double-Entry Financial Ledger Posting
-- Debit Gateway Clearing ₹53,690.00 | Credit Freight Revenue ₹45,500.00 | Credit GST Payable ₹8,190.00
INSERT INTO ledger.ledger_transactions (id, transaction_number, transaction_type, reference_type, reference_id, description, effective_at, posted_at, status) VALUES
    ('90000000-0000-0000-0001-000000000001', 'TX-202609-00101', 'PAYMENT_RECEIVED', 'PAYMENT', '80000000-0000-0000-0001-000000000001', 'Online collection via Razorpay for Booking BK-202609-00101', clock_timestamp() - interval '3 hours', clock_timestamp() - interval '3 hours', 'POSTED')
ON CONFLICT (transaction_number) DO NOTHING;

INSERT INTO ledger.ledger_entries (transaction_id, account_id, direction, amount_minor, currency) VALUES
    ('90000000-0000-0000-0001-000000000001', '00000000-0000-0000-0003-000000000002', 'DEBIT', 5369000, 'INR'),
    ('90000000-0000-0000-0001-000000000001', '00000000-0000-0000-0003-000000000006', 'CREDIT', 4550000, 'INR'),
    ('90000000-0000-0000-0001-000000000001', '00000000-0000-0000-0003-000000000005', 'CREDIT', 819000, 'INR')
ON CONFLICT DO NOTHING;

-- 11. Transactional Outbox Event (Guaranteed Delivery)
INSERT INTO integration.outbox_events (event_id, event_type, event_version, aggregate_type, aggregate_id, payload, status) VALUES
    ('a0000000-0000-0000-0001-000000000001', 'booking.assigned.v1', 1, 'BOOKING', '70000000-0000-0000-0001-000000000001', '{"booking_id":"70000000-0000-0000-0001-000000000001", "trip_id":"70000000-0000-0000-0001-000000000002", "driver_id":"40000000-0000-0000-0001-000000000001"}'::jsonb, 'PUBLISHED')
ON CONFLICT (event_id) DO NOTHING;
