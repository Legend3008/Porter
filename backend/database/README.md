# Porter Logistics Platform — Production Database Architecture

The authoritative system of record for the **Porter Container Logistics Platform**, engineered in PostgreSQL 16 according to [database.md](file:///Users/ritesh/Desktop/Porter/database.md).

---

## 1. Architecture Summary

- **Engine**: PostgreSQL 16 + Alpine Linux
- **Namespaces**: 17 logical schemas (`identity`, `party`, `fleet`, `geo`, `shipment`, `booking`, `pricing`, `operations`, `tracking`, `documents`, `payments`, `billing`, `ledger`, `settlement`, `notification`, `audit`, `integration`)
- **Monetary Unit**: Integer minor units (**`BIGINT` Paise**, where ₹1.00 = 100 paise). Floating-point numbers are strictly prohibited.
- **Telemetry Partitioning**: `tracking.location_pings` is range-partitioned monthly by `server_received_at` with an automatic trigger maintaining `tracking.latest_locations` for sub-5ms map reads.
- **Accounting**: Strict immutable double-entry ledger (`ledger.ledger_accounts`, `ledger_transactions`, `ledger_entries`) with automated debit/credit balancing verification.
- **Concurrency & Idempotency**: `SELECT FOR UPDATE` critical sections, partial unique indexes for active assignments/offers, and `integration.idempotency_keys` preventing duplicate execution on mobile retry.
- **Event Distribution**: Transactional Outbox Pattern (`integration.outbox_events`) committed in the same transaction as state transitions, guaranteeing 100% reliable push notification delivery.

---

## 2. Directory Layout

```
backend/database/
├── docker-compose.yml                     # Local PostgreSQL 16 + Redis 7 services
├── Makefile                               # Developer automation (up, down, migrate, test)
├── migrations/
│   ├── V01__extensions_and_schemas.sql    # pgcrypto/uuid-ossp, 17 schemas, common types
│   ├── V02__identity_and_party.sql        # Users, roles, permissions, orgs, GSTIN
│   ├── V03__fleet_and_geo.sql             # Carriers, drivers, vehicles, locations, ports, lanes
│   ├── V04__shipment_and_containers.sql   # Shipments, containers, multi-container stops
│   ├── V05__pricing_and_quotes.sql        # Pricing rule sets, versions, quotes, components
│   ├── V06__bookings.sql                  # Bookings, human-readable numbers, status history
│   ├── V07__operations_and_trips.sql      # Trips, stops, assignments, job offers, gate events
│   ├── V08__tracking_partitioned.sql      # Monthly partitioned pings & latest_locations projection
│   ├── V09__documents_and_pod.sql         # Document metadata, versions, POD verification
│   ├── V10__payments_and_refunds.sql      # Payments, gateway attempts, webhooks, refunds
│   ├── V11__billing_and_invoices.sql      # GST tax invoices, invoice lines
│   ├── V12__ledger_double_entry.sql       # Double-entry ledger accounts, transactions, entries
│   ├── V13__settlements_and_payouts.sql   # Carrier settlements, lines, payouts, reconciliation
│   ├── V14__notifications.sql             # Notification channels, preferences, delivery attempts
│   ├── V15__audit_and_integrations.sql    # Idempotency keys, transactional outbox, inbox, audit
│   └── V16__performance_and_triggers.sql # Partial indexes, state transition check triggers
├── seeds/
│   ├── 01_reference_data.sql              # Standard ports, terminals, lanes, surcharges, chart of accounts
│   └── 02_development_data.sql            # Demo shipper, driver, active booking, trip, quote, ledger
└── tests/
    ├── run_verification.sh                # End-to-end automated verification runner
    ├── test_concurrency.sql               # Competing driver assignment race test
    ├── test_idempotency.sql               # Mobile retry idempotency test
    ├── test_ledger_balance.sql            # Double-entry mathematical balance test
    ├── test_gps_partitions.sql            # Partition routing and projection test
    └── test_webhook_dedup.sql             # Gateway webhook deduplication test
```

---

## 3. Running with Docker Compose

Start the PostgreSQL and Redis containers:

```bash
cd backend/database
docker compose up -d
```

The migrations in `migrations/` are automatically mounted to `/docker-entrypoint-initdb.d/migrations` and applied in sequential order on first boot.

To manually connect to PostgreSQL:

```bash
psql -h localhost -p 5432 -U porter_admin -d porter_db
# Password: porter_secure_password_2026
```

---

## 4. Running the Automated Invariant Verification Suite

Execute the standalone verification test runner (spins up an isolated cluster, runs all 16 migrations, applies reference/dev seeds, and executes all invariant test cases):

```bash
./backend/database/tests/run_verification.sh
```

---

## 5. Capacity Planning & Growth Projections

At target scale of **5,000 trips/day**:
- **GPS Telemetry**: 500 concurrent trips × 4 pings/min = 2,000 pings/min = ~2.88 million pings/day.
- **Storage**: ~200 bytes per ping row = ~576 MB/day = ~17.2 GB/month.
- **Partition Lifecycle**: Monthly partitions (`location_pings_YYYY_MM`) keep index sizes well within RAM (~512MB shared buffers), guaranteeing instant index-range scans.
- **Archive Policy**: Telemetry older than 90 days can be moved to cold compressed columnar storage (e.g. Parquet on S3) using `pg_dump` or detached partition tables without locking active tables.
