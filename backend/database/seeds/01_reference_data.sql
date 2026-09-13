-- ==============================================================================
-- Seed Script: 01_reference_data.sql
-- Description: Core reference data: standard roles, permissions, Indian ports,
--              container terminals, pricing surcharges, and chart of accounts.
-- Safe for Production Deployment: Idempotent (ON CONFLICT DO NOTHING)
-- ==============================================================================

-- 1. Standard Roles
INSERT INTO identity.roles (id, code, name, description) VALUES
    ('00000000-0000-0000-0001-000000000001', 'SUPER_ADMIN', 'Super Administrator', 'Full platform operational and administrative authority'),
    ('00000000-0000-0000-0001-000000000002', 'OPS', 'Operations Dispatcher', 'Dispatch, fleet monitoring, exception handling, manual assignment'),
    ('00000000-0000-0000-0001-000000000003', 'SHIPPER', 'Shipper Customer', 'Creates shipment bookings, views quotes, tracks haulage, pays invoices'),
    ('00000000-0000-0000-0001-000000000004', 'DRIVER', 'Commercial Truck Driver', 'Receives trip dispatches, updates milestones, uploads POD'),
    ('00000000-0000-0000-0001-000000000005', 'CARRIER_ADMIN', 'Fleet Carrier Admin', 'Manages fleet vehicles, driver roster, and payout settlements')
ON CONFLICT (code) DO NOTHING;

-- 2. Standard Surcharges and Taxes
INSERT INTO pricing.surcharges (id, code, name, calculation_type, default_rate, is_tax) VALUES
    ('00000000-0000-0000-0002-000000000001', 'GST', 'Goods & Services Tax', 'PERCENTAGE', 18.0000, true),
    ('00000000-0000-0000-0002-000000000002', 'FUEL_SURCHARGE', 'Diesel Price Index Surcharge', 'PERCENTAGE', 12.5000, false),
    ('00000000-0000-0000-0002-000000000003', 'PORT_HANDLING', 'Terminal Gate & Handling Fee', 'FIXED', 350000, false), -- ₹3,500.00
    ('00000000-0000-0000-0002-000000000004', 'TOLL_EXPRESSWAY', 'National Highway Toll Recovery', 'FIXED', 220000, false), -- ₹2,200.00
    ('00000000-0000-0000-0002-000000000005', 'DETENTION_HOURLY', 'Detention Waiting Charge Per Hour', 'PER_HOUR', 50000, false) -- ₹500.00/hr
ON CONFLICT (code) DO NOTHING;

-- 3. Core Chart of Accounts (Double-Entry Ledger Foundation)
INSERT INTO ledger.ledger_accounts (id, account_code, name, account_type, currency, status) VALUES
    ('00000000-0000-0000-0003-000000000001', '1010-CASH', 'Platform Operating Bank Account', 'ASSET', 'INR', 'ACTIVE'),
    ('00000000-0000-0000-0003-000000000002', '1020-CLEARING-GATEWAY', 'Payment Gateway Clearing (Razorpay/PhonePe)', 'ASSET', 'INR', 'ACTIVE'),
    ('00000000-0000-0000-0003-000000000003', '1030-RECEIVABLES-SHIPPER', 'Shipper Accounts Receivable', 'ASSET', 'INR', 'ACTIVE'),
    ('00000000-0000-0000-0003-000000000004', '2010-PAYABLES-CARRIER', 'Carrier Accounts Payable (Disbursements)', 'LIABILITY', 'INR', 'ACTIVE'),
    ('00000000-0000-0000-0003-000000000005', '2020-LIABILITY-GST', 'GST Tax Output Liability', 'LIABILITY', 'INR', 'ACTIVE'),
    ('00000000-0000-0000-0003-000000000006', '4010-REVENUE-FREIGHT', 'Gross Container Haulage Revenue', 'REVENUE', 'INR', 'ACTIVE'),
    ('00000000-0000-0000-0003-000000000007', '5010-EXPENSE-CARRIER', 'Carrier Freight & Haulage Direct Expense', 'EXPENSE', 'INR', 'ACTIVE')
ON CONFLICT (account_code) DO NOTHING;

-- 4. Standard Geographic Locations (Major Indian Ports & Terminals)
-- JNPT (Nhava Sheva, Maharashtra)
INSERT INTO geo.locations (id, code, name, address_line, city, state, postal_code, latitude, longitude, location_type) VALUES
    ('00000000-0000-0000-0004-000000000001', 'LOC-INNSA', 'Jawaharlal Nehru Port Trust (JNPT)', 'Navi Mumbai Port Area', 'Navi Mumbai', 'Maharashtra', '400707', 18.9498000, 72.9510000, 'PORT'),
    ('00000000-0000-0000-0004-000000000002', 'LOC-INMUN', 'Mundra Port', 'APSEZ Mundra', 'Mundra', 'Gujarat', '370421', 22.7441000, 69.7061000, 'PORT'),
    ('00000000-0000-0000-0004-000000000003', 'LOC-INMAA', 'Chennai Port', 'Rajaji Salai', 'Chennai', 'Tamil Nadu', '600001', 13.0844000, 80.2941000, 'PORT'),
    ('00000000-0000-0000-0004-000000000004', 'LOC-PUNE-ICD', 'Inland Container Depot Talegaon', 'Talegaon MIDC', 'Pune', 'Maharashtra', '410507', 18.7340000, 73.6820000, 'TERMINAL'),
    ('00000000-0000-0000-0004-000000000005', 'LOC-AHM-ICD', 'Inland Container Depot Khodiyar', 'Khodiyar Railway Yard', 'Ahmedabad', 'Gujarat', '382421', 23.1670000, 72.5850000, 'TERMINAL')
ON CONFLICT (code) DO NOTHING;

-- Ports
INSERT INTO geo.ports (id, code, name, location_id, status) VALUES
    ('00000000-0000-0000-0005-000000000001', 'INNSA', 'Jawaharlal Nehru Port (JNPT)', '00000000-0000-0000-0004-000000000001', 'ACTIVE'),
    ('00000000-0000-0000-0005-000000000002', 'INMUN', 'Mundra Port', '00000000-0000-0000-0004-000000000002', 'ACTIVE'),
    ('00000000-0000-0000-0005-000000000003', 'INMAA', 'Chennai Port', '00000000-0000-0000-0004-000000000003', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- Terminals at JNPT
INSERT INTO geo.terminals (id, port_id, code, name, location_id, status) VALUES
    ('00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0005-000000000001', 'GTI', 'Gateway Terminals India (APM)', '00000000-0000-0000-0004-000000000001', 'ACTIVE'),
    ('00000000-0000-0000-0006-000000000002', '00000000-0000-0000-0005-000000000001', 'BMCT', 'Bharat Mumbai Container Terminal (PSA)', '00000000-0000-0000-0004-000000000001', 'ACTIVE'),
    ('00000000-0000-0000-0006-000000000003', '00000000-0000-0000-0005-000000000001', 'NSICT', 'Nhava Sheva International Container Terminal', '00000000-0000-0000-0004-000000000001', 'ACTIVE')
ON CONFLICT (port_id, code) DO NOTHING;

-- Key Logistics Corridors (Lanes)
INSERT INTO geo.lanes (id, code, name, origin_location_id, destination_location_id, distance_km, estimated_duration_minutes, status) VALUES
    ('00000000-0000-0000-0007-000000000001', 'LANE-JNPT-PUN', 'JNPT (Nhava Sheva) to Pune ICD', '00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0004-000000000004', 138.50, 240, 'ACTIVE'),
    ('00000000-0000-0000-0007-000000000002', 'LANE-MUN-AHM', 'Mundra Port to Ahmedabad ICD', '00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0004-000000000005', 354.00, 480, 'ACTIVE')
ON CONFLICT (code) DO NOTHING;
