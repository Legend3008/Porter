# Porter Container Logistics Platform - Backend API

Production-grade, distributed Node.js/TypeScript REST and WebSocket backend service powering the Porter Container Logistics Platform, connecting shipper mobile clients, driver applications, and operations control centers to a PostgreSQL 16 relational core.

---

## Key Capabilities & Production Guarantees

1. **Deterministic Monetary Accounting (Integer Paise)**:
   - All financial amounts, quotes, surcharges, invoices, and ledger postings use `BIGINT` integer paise (₹1.00 = 100 paise).
   - Zero floating-point drift or truncation.
2. **Double-Click Transactional Idempotency**:
   - Mutating routes utilize `Idempotency-Key` headers backed by `integration.idempotency_keys` in PostgreSQL.
   - Concurrent or replayed requests safely return the cached response without duplicate billing or trip allocation.
3. **High-Concurrency Operational State Machine**:
   - Sequential trip lifecycle transitions (`CREATED` → `ASSIGNED` → `DRIVER_ACCEPTED` → `EN_ROUTE_PICKUP` → `AT_PICKUP` → `LOADING` → `LOADED` → `EN_ROUTE_DELIVERY` → `AT_DELIVERY` → `UNLOADING` → `DELIVERED` → `COMPLETED`).
   - Pessimistic locking (`SELECT FOR UPDATE`) and version increment counters protect against race conditions (e.g. duplicate trip acceptance returning HTTP 409).
4. **Sub-5ms Telemetry & High-Throughput Ingestion**:
   - Monthly partitioned tables (`tracking.location_pings`) combined with cached projection keys (`geo:trip:{id}:latest`) delivering sub-5ms lookup latency (measured at ~1-2ms).
   - Pub/Sub streaming to WebSockets for live driver map updates.
5. **Immutable Double-Entry General Ledger**:
   - Commercial payment capture automatically invokes double-entry ledger balancing with balanced debits (Clearing Gateway) and credits (Freight Revenue + GST Liability).
   - Verified with PostgreSQL function `ledger.verify_transaction_balance(tx_id)` returning `true`.
6. **OpenAPI 3.0 & Interactive Swagger UI**:
   - Full schema and route specification served at `/api/docs`.

---

## Architecture Overview

```
                          [ Android Shipper App / Driver App ]
                                           │
                                           ▼
                     ┌───────────────────────────────────────────┐
                     │          Express / TypeScript API         │
                     │  - Auth (JWT / Refresh / RBAC)            │
                     │  - Idempotency & Rate Limiting            │
                     │  - Structured Pino Logging & Tracing      │
                     └─────────────┬─────────────────────────────┘
                                   │
              ┌────────────────────┼───────────────────────────┐
              ▼                    ▼                           ▼
      [ PostgreSQL 16 ]     [ Redis Cache / Streams ]     [ WebSocket Server ]
   - 16 Schemas & Partitions   - Geo projections (<5ms)    - Live GPS Telemetry
   - Strict State Machine      - Pub/Sub bus               - Shipper Real-Time
   - Double-Entry Ledger       - Memory fallback             Map Updates
```

---

## Directory Structure

```
backend/api/
├── src/
│   ├── app.ts                  # Express application setup & middleware wiring
│   ├── index.ts                # Server bootstrapper & graceful shutdown
│   ├── config/
│   │   ├── database.ts         # pg Connection Pool & withTransaction helper
│   │   ├── env.ts              # Zod-validated environment config
│   │   └── redis.ts            # Redis client with memory fallback
│   ├── common/
│   │   ├── errors/             # Custom AppError, ErrorCodes & ErrorHandler
│   │   ├── middleware/         # Auth, Idempotency, RequestID, RBAC, Validation
│   │   └── types/              # Standardized API response formatters
│   ├── modules/
│   │   ├── auth/               # OTP, Token Refresh, Shipper & Driver Auth
│   │   ├── pricing/            # Quote calculation with 15-min price lock
│   │   ├── booking/            # Drafts & Idempotent Confirmations
│   │   ├── operations/         # Trips, concurrency-safe accept, state transitions
│   │   ├── driver/             # Driver profile, pings, batch GPS, POD
│   │   ├── tracking/           # Sub-5ms latest location & GPS breadcrumb path
│   │   ├── documents/          # Pre-signed upload URLs, SHA-256 integrity
│   │   ├── payments/           # Razorpay order, captured webhook, ledger posting
│   │   ├── billing/            # GST Tax Invoices (subtotal + tax = total)
│   │   ├── settlement/         # Carrier settlements & Driver earnings rollups
│   │   └── admin/              # Operational monitoring & manual overrides
│   ├── websocket/
│   │   └── tracking-server.ts  # Real-time WebSocket server for GPS streaming
│   ├── openapi/
│   │   ├── spec.ts             # Complete OpenAPI 3.0 specification
│   │   └── swagger.ts          # Swagger UI router mounted at /api/docs
│   └── tests/
│       └── run-all-tests.ts    # Comprehensive 10-suite E2E verification
├── run_api_tests.sh            # Automated test runner with isolated Postgres cluster
├── package.json
└── tsconfig.json
```

---

## Getting Started

### Prerequisites
- Node.js 18+ (tested on Node v24)
- PostgreSQL 16 (with PostGIS and UUID extensions)
- Redis 7+ (optional; backend includes transparent in-memory fallback for local dev/testing)

### Environment Variables
Create a `.env` file or export environment variables:
```bash
PORT=3000
DATABASE_URL=postgresql://porter_app:porter_secure_password_2026@localhost:5432/porter_db
DATABASE_POOL_MIN=5
DATABASE_POOL_MAX=25
REDIS_URL=redis://localhost:6379
JWT_SECRET=porter_super_secret_jwt_signing_key_production_2026_x89
JWT_EXPIRES_IN=1h
JWT_REFRESH_SECRET=porter_super_secret_refresh_signing_key_production_2026_y90
JWT_REFRESH_EXPIRES_IN=30d
NODE_ENV=development
```

### Installation
```bash
npm install
```

### Build & Run
```bash
# Compile TypeScript
npm run build

# Start production server
npm start

# Run in watch mode (development)
npm run dev
```

---

## Interactive API Documentation
Once the server is running, navigate to:
```
http://localhost:3000/api/docs
```
Explore endpoints, submit requests, and inspect schemas interactively.

---

## Running Verification Tests
Execute the end-to-end test suite against an automated, isolated PostgreSQL 16 cluster:
```bash
chmod +x run_api_tests.sh
./run_api_tests.sh
```

**Verifies:**
1. Kubernetes Liveness & Readiness probes (`/health/live`, `/health/ready`).
2. OpenAPI 3.0 specification validation.
3. Shipper & Driver OTP authentication with JWT tokens.
4. Guaranteed integer paise quotes with 15-minute countdown locks.
5. Idempotent booking confirmation and automatic trip dispatching.
6. Race-condition prevention (`SELECT FOR UPDATE` returning HTTP 409 on duplicate accept).
7. High-throughput telemetry ingestion with sub-5ms latest location reads.
8. Shipping document registration and Proof of Delivery (POD).
9. Commercial payment webhook processing, GST tax invoicing, and double-entry ledger balancing.
10. Carrier & Driver earnings analytics rollups.
