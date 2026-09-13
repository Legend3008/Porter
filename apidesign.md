You are the Principal Backend/API Architect and Senior Production Engineer responsible for designing, implementing, testing, documenting, securing, and optimizing the API platform for this logistics application.

This is NOT a prototype.

Build production-grade APIs that will serve:

1. Shipper Android application
2. Driver/Carrier Android application
3. Operations/Admin responsive web dashboard

The backend is the authoritative business layer between clients and:

- PostgreSQL
- Redis
- object storage
- payment providers
- notification providers
- mapping/location systems
- event bus/queue
- background workers

The API must be:

- secure
- versioned
- predictable
- idempotent
- observable
- performant
- mobile-friendly
- concurrency-safe
- backwards-compatible
- documented
- testable
- production deployable

==================================================
1. FIRST: INSPECT THE EXISTING REPOSITORY
==================================================

DO NOT start coding immediately.

First inspect the entire existing repository.

Determine:

- backend framework
- programming language
- ORM/query builder
- current routes
- current controllers
- current services
- current database models
- current authentication
- current authorization
- current middleware
- current validation
- current error handling
- current API responses
- current WebSocket implementation
- current Redis implementation
- current queues/jobs
- current payment integrations
- current storage integration
- current notification integration
- current tests
- current OpenAPI/Swagger setup
- current logging
- current tracing
- current configuration management

Inspect frontend/mobile API consumers if available.

Map:

Shipper App
    ↓
API

Driver App
    ↓
API

Admin Dashboard
    ↓
API

Do not duplicate existing endpoints.

Do not break existing clients without a migration strategy.

Produce an API audit before making major changes.

==================================================
2. API ARCHITECTURE
==================================================

Use REST as the primary API style.

Use GraphQL only where it materially benefits the admin dashboard.

Mobile applications MUST use REST.

Recommended:

/api/v1/...

Future breaking API changes:

/api/v2/...

Do not create:

/api/users
/api/bookings
/api/trips

without versioning.

Use:

/api/v1/users
/api/v1/bookings
/api/v1/trips

==================================================
3. API CLIENT TYPES
==================================================

All API request and response contracts must be strongly typed.

The backend should have explicit DTO/request/response schemas.

Do not expose raw database entities directly.

Bad:

return databaseModel

Good:

return BookingResponseDTO

This protects the API from database implementation changes.

==================================================
4. API LAYERING
==================================================

Use clear separation:

HTTP Layer
    ↓
Controller/Route
    ↓
Request Validation
    ↓
Authentication
    ↓
Authorization
    ↓
Application Service
    ↓
Domain Logic
    ↓
Repository
    ↓
Database

External integrations should be behind service interfaces.

Example:

PaymentController
    ↓
PaymentService
    ↓
PaymentProviderInterface
    ↓
Razorpay/PhonePe/etc.

Never put business logic directly inside route handlers.

==================================================
5. MODULE STRUCTURE
==================================================

Organize APIs around business domains.

Modules:

identity
party
fleet
geo
shipment
pricing
booking
operations
tracking
documents
payments
billing
ledger
settlement
notification
audit
integration
admin

Each module owns its business logic.

Avoid one giant:

controllers/
services/
models/

structure if it destroys domain boundaries.

==================================================
6. API RESOURCE MAP
==================================================

Primary resources:

Users
Organizations
Drivers
Carriers
Vehicles
Locations
Ports
Terminals
Lanes
Containers
Shipments
Quotes
Bookings
Trips
Assignments
Job Offers
Tracking
Documents
POD
Payments
Refunds
Invoices
Settlements
Payouts
Notifications
Exceptions
Audit Logs

==================================================
7. PUBLIC VS INTERNAL APIs
==================================================

Separate:

PUBLIC MOBILE APIs

ADMIN APIs

INTERNAL SERVICE APIs

WEBHOOK APIs

Do not expose internal administrative endpoints to mobile clients.

Example:

/api/v1/mobile/...

/api/v1/admin/...

/api/v1/webhooks/...

If the project already uses a unified REST namespace, preserve consistency while enforcing authorization.

==================================================
8. AUTHENTICATION
==================================================

Use:

short-lived access tokens
revocable refresh tokens

Access token:

short lifetime

Refresh token:

longer lifetime
revocable
rotatable where appropriate

Do not use permanent JWTs.

Do not put sensitive data inside JWT claims.

JWT should contain only required claims.

Example:

sub
role
organization_id
session_id
iat
exp

==================================================
9. AUTHORIZATION
==================================================

Authentication answers:

"Who are you?"

Authorization answers:

"Are you allowed to do this?"

Every protected endpoint must enforce authorization.

Roles:

SHIPPER
DRIVER
CARRIER_ADMIN
OPS
SUPER_ADMIN

But role checks alone are insufficient.

Also enforce resource ownership.

Example:

GET /bookings/BK123

must verify:

current user
    ↓
belongs to authorized organization
    ↓
owns/has access to booking

Do NOT rely on the booking ID being secret.

==================================================
10. IDOR PROTECTION
==================================================

Prevent:

GET /api/v1/bookings/{another_customer_booking_id}

from returning another customer's data.

Every resource access must verify:

user
organization
resource relationship
role
permission

Do this on the backend.

Never rely on frontend filtering.

==================================================
11. API REQUEST VALIDATION
==================================================

Every input must be validated.

Validate:

type
length
format
range
required fields
enum values
nested objects
arrays
dates
currency
amounts
coordinates

Reject malformed requests before business logic executes.

Never trust:

client status
client price
client payment success
client role
client ownership
client timestamps

==================================================
12. STANDARD API RESPONSE
==================================================

Use predictable response structures.

Success:

{
  "data": {...},
  "meta": {...}
}

List:

{
  "data": [...],
  "pagination": {
    "next_cursor": "...",
    "has_more": true
  }
}

Do not return different pagination formats from different modules.

==================================================
13. STANDARD ERROR FORMAT
==================================================

Every API error must follow a consistent format.

Example:

{
  "error": {
    "code": "TRIP_ALREADY_ASSIGNED",
    "message": "This trip has already been assigned.",
    "request_id": "req_...",
    "details": {}
  }
}

Never return raw stack traces.

Never expose SQL errors.

Never expose internal exception messages in production.

==================================================
14. HTTP STATUS CODES
==================================================

Use HTTP status codes correctly.

200:
successful retrieval/update where appropriate

201:
resource created

202:
accepted for asynchronous processing

204:
successful operation with no body

400:
malformed request

401:
unauthenticated

403:
authenticated but unauthorized

404:
resource not found or intentionally hidden

409:
business/state/concurrency conflict

422:
validation/business input failure where appropriate

429:
rate limited

500:
unexpected server error

502/503/504:
appropriate upstream/service availability failures

Do not return 200 for everything.

==================================================
15. RESOURCE NOT FOUND VS UNAUTHORIZED
==================================================

Be careful about information leakage.

For resources that users should not know exist, consider returning:

404

instead of:

403

when appropriate.

Do not reveal whether another organization's booking exists.

==================================================
16. IDEMPOTENCY
==================================================

All retryable state-changing operations must support:

Idempotency-Key

Examples:

POST /bookings
POST /bookings/{id}/confirm
POST /trips/{id}/accept
POST /payments
POST /payments/{id}/refund
POST /settlements/{id}/payout

Mobile networks are unreliable.

Example:

Client sends:

POST /trips/T123/accept

Server processes request.

Network dies before response.

Client retries.

The second request must NOT create another assignment.

Return the original result where appropriate.

==================================================
17. IDEMPOTENCY IMPLEMENTATION
==================================================

Idempotency must be backed by PostgreSQL.

Store:

key
actor
operation
request_hash
status
response_status
response_body
created_at
expires_at

Rules:

same key + same request:
return previous result

same key + different request:
return conflict

Do not simply cache idempotency in Redis.

==================================================
18. CONCURRENCY
==================================================

Identify every endpoint that can race.

Examples:

driver accepts trip
admin reassigns driver
payment webhook arrives
payment retry arrives
settlement runs twice
two admins modify booking

Use:

transactions
database locks
optimistic locking
unique constraints

as appropriate.

==================================================
19. DRIVER ACCEPT TRIP
==================================================

Endpoint example:

POST /api/v1/driver/trips/{trip_id}/accept

Flow:

1. Authenticate driver.
2. Validate request.
3. Verify trip exists.
4. Verify driver can access trip.
5. Begin transaction.
6. Lock trip or use optimistic versioning.
7. Verify trip state.
8. Verify job offer.
9. Verify driver eligibility.
10. Verify no competing assignment.
11. Create assignment.
12. Update trip state.
13. Create status history.
14. Create audit information if required.
15. Create outbox event.
16. Commit.
17. Return result.

Do not call external services while transaction locks are held.

==================================================
20. STATE TRANSITION APIs
==================================================

Do not expose generic:

PATCH /trips/{id}

allowing:

{
  "status": "COMPLETED"
}

This is dangerous.

Use explicit commands:

POST /trips/{id}/accept
POST /trips/{id}/start
POST /trips/{id}/arrive
POST /trips/{id}/load
POST /trips/{id}/depart
POST /trips/{id}/complete
POST /trips/{id}/cancel

Each command validates the allowed state transition.

==================================================
21. BOOKING APIs
==================================================

Design endpoints similar to:

POST /api/v1/bookings

GET /api/v1/bookings/{id}

GET /api/v1/bookings

POST /api/v1/bookings/{id}/confirm

POST /api/v1/bookings/{id}/cancel

GET /api/v1/bookings/{id}/timeline

GET /api/v1/bookings/{id}/documents

POST /api/v1/bookings/{id}/documents

Do not expose arbitrary database CRUD.

==================================================
22. BOOKING CREATION
==================================================

Booking creation must:

1. Authenticate customer.
2. Validate shipment.
3. Validate origin/destination.
4. Validate quote.
5. Verify quote ownership.
6. Verify quote expiry.
7. Verify pricing version.
8. Create booking.
9. Snapshot required commercial references.
10. Create required operational records.
11. Create history.
12. Create outbox event.
13. Commit.

Return a stable booking representation.

==================================================
23. SHIPMENT APIs
==================================================

Examples:

POST /api/v1/shipments

GET /api/v1/shipments/{id}

GET /api/v1/shipments

PATCH /api/v1/shipments/{id}

POST /api/v1/shipments/{id}/containers

GET /api/v1/shipments/{id}/containers

Do not allow shipment modifications after operational state reaches a protected state unless explicitly supported.

==================================================
24. QUOTE API
==================================================

Example:

POST /api/v1/quotes

GET /api/v1/quotes/{id}

POST /api/v1/quotes/{id}/accept

Quote response should include:

quote_id
quote_number
currency
components
subtotal
tax
discount
total
expires_at
pricing_version

Never allow client to modify:

price
tax
pricing version

==================================================
25. PRICING API
==================================================

Pricing calculation must happen server-side.

Example:

POST /api/v1/quotes

Request:

origin
destination
vehicle_type
container_type
cargo
requested_time

Server:

resolve lane
resolve active pricing version
evaluate rules
calculate charges
calculate taxes
create quote
return quote

Never trust client-provided calculated totals.

==================================================
26. DRIVER APIs
==================================================

Driver endpoints should include:

GET /api/v1/driver/profile

GET /api/v1/driver/trips

GET /api/v1/driver/trips/{id}

GET /api/v1/driver/job-offers

POST /api/v1/driver/job-offers/{id}/accept

POST /api/v1/driver/job-offers/{id}/reject

POST /api/v1/driver/trips/{id}/accept

POST /api/v1/driver/trips/{id}/arrive

POST /api/v1/driver/trips/{id}/start

POST /api/v1/driver/trips/{id}/complete

POST /api/v1/driver/trips/{id}/pod

The backend must determine the driver's actual authorized trips.

Never trust:

driver_id

sent by the client.

Use authenticated identity.

==================================================
27. DRIVER LOCATION API
==================================================

Example:

POST /api/v1/driver/location

Request:

{
  "latitude": ...,
  "longitude": ...,
  "accuracy_meters": ...,
  "speed_mps": ...,
  "heading_degrees": ...,
  "device_recorded_at": "...",
  "sequence_number": ...
}

Validate all fields.

Associate driver based on authenticated session.

Do not trust arbitrary trip_id without verifying driver assignment.

==================================================
28. BATCH LOCATION INGESTION
==================================================

For mobile efficiency support:

POST /api/v1/driver/location/batch

Request:

{
  "points": [...]
}

Limit batch size.

Validate every point.

Return per-point acceptance/rejection if necessary.

Do not allow one malformed point to corrupt the entire batch.

==================================================
29. LOCATION DEDUPLICATION
==================================================

Use:

device ID
sequence number
timestamp

where available.

Repeated GPS points should not create unnecessary duplicate records.

Design ingestion to tolerate retries.

==================================================
30. LIVE TRACKING
==================================================

Use WebSockets for real-time dashboard tracking.

REST remains authoritative for resource state.

WebSocket is a delivery mechanism, not the source of truth.

Example:

Trip status changed
    ↓
database
    ↓
outbox event
    ↓
event processor
    ↓
WebSocket
    ↓
Admin dashboard

If WebSocket disconnects:

client reconnects

then fetches authoritative REST state.

==================================================
31. WEBSOCKET AUTHENTICATION
==================================================

Authenticate WebSocket connections.

Verify:

user
role
organization
requested trip access

Never allow:

wss://.../trip/T123

to expose T123 to unauthorized users.

==================================================
32. WEBSOCKET EVENTS
==================================================

Version events.

Example:

trip.updated.v1
driver.location.updated.v1
booking.updated.v1

Payloads should be small.

Do not send entire database objects unnecessarily.

Example:

{
  "event": "trip.updated",
  "version": 1,
  "trip_id": "...",
  "status": "EN_ROUTE",
  "occurred_at": "..."
}

==================================================
33. ADMIN APIs
==================================================

Admin APIs require stronger authorization.

Examples:

GET /api/v1/admin/trips

GET /api/v1/admin/trips/{id}

POST /api/v1/admin/trips/{id}/assign

POST /api/v1/admin/trips/{id}/reassign

POST /api/v1/admin/trips/{id}/cancel

POST /api/v1/admin/trips/{id}/override

GET /api/v1/admin/drivers

GET /api/v1/admin/vehicles

GET /api/v1/admin/bookings

GET /api/v1/admin/payments

GET /api/v1/admin/settlements

GET /api/v1/admin/exceptions

==================================================
34. ADMIN OVERRIDES
==================================================

Dangerous administrative operations must be explicit.

Example:

POST /api/v1/admin/trips/{id}/override-status

Request:

{
  "target_status": "...",
  "reason": "..."
}

Require:

authorization
reason
audit log
actor identity

Never allow silent administrative state changes.

==================================================
35. ADMIN LIVE MAP
==================================================

The admin map needs:

trip ID
driver
vehicle
current location
trip status
last update
ETA where available
GPS freshness

Do not query millions of historical GPS rows for every map refresh.

Use latest location projection/cache.

==================================================
36. PAGINATION
==================================================

All potentially large collection endpoints must paginate.

Use cursor pagination.

Example:

GET /api/v1/trips?limit=50&cursor=...

Response:

{
  "data": [...],
  "pagination": {
    "next_cursor": "...",
    "has_more": true
  }
}

Set server-side maximum limit.

Example:

requested limit = 10000

server:

limit = 100

==================================================
37. FILTERING
==================================================

Support controlled filters.

Example:

GET /admin/trips
?status=EN_ROUTE
&carrier_id=...
&driver_id=...
&from=...
&to=...

Do not allow arbitrary SQL-like filtering parameters.

Bad:

?sort=some_unvalidated_expression

Whitelist sortable fields.

==================================================
38. SORTING
==================================================

Only allow predefined sorting fields.

Example:

created_at
scheduled_start_at
updated_at

Never concatenate user-provided SQL directly into queries.

==================================================
39. SEARCH
==================================================

Search endpoints must define:

allowed fields
maximum length
rate limits
pagination
matching behavior

For example:

booking_number
container_number
driver_code
vehicle_registration

Do not create uncontrolled wildcard database scans.

==================================================
40. API FILTER DATE HANDLING
==================================================

Use ISO 8601.

Example:

2026-09-14T12:30:00Z

Internally store UTC.

For user-facing timezone-sensitive operations:

accept explicit timezone where necessary.

Never rely on server local timezone.

==================================================
41. API VERSIONING
==================================================

v1 must remain backwards compatible.

Breaking changes require:

v2

or another explicit versioning strategy.

Non-breaking additions may include:

new optional response fields

but clients must tolerate unknown fields.

Do not remove fields casually.

==================================================
42. DEPRECATION
==================================================

When deprecating an endpoint:

1. Mark deprecated.
2. Document replacement.
3. Monitor usage.
4. Communicate migration.
5. Maintain compatibility period.
6. Remove only after usage reaches acceptable threshold.

==================================================
43. API DOCUMENTATION
==================================================

Generate OpenAPI documentation.

Every endpoint must document:

method
path
authentication
authorization
request body
parameters
responses
error codes
examples
pagination
idempotency
rate limits where relevant

The OpenAPI document must be generated from or validated against the implementation.

Never allow documentation to drift from actual behavior.

==================================================
44. API CONTRACT TESTING
==================================================

Create contract tests between:

backend
shipper app
driver app
admin dashboard

Verify:

request schema
response schema
error schema
status codes
required fields

Breaking API changes must fail CI.

==================================================
45. MOBILE NETWORK RESILIENCE
==================================================

Mobile clients operate under:

poor network
timeouts
duplicate requests
offline mode
slow connections
app restarts
background execution

API design must therefore support:

idempotency
retry-safe commands
batching
pagination
small responses
incremental synchronization
timestamps
version numbers
conflict detection

==================================================
46. SYNC API
==================================================

Consider a driver synchronization endpoint.

Example:

GET /api/v1/driver/sync?cursor=...

Return changes relevant to the driver since the cursor.

Potential data:

job offers
trip changes
status changes
notifications
documents

Do not return unrelated organization data.

==================================================
47. ETAG / CONDITIONAL REQUESTS
==================================================

For suitable read-heavy resources support:

ETag
If-None-Match

or version-based caching.

Especially useful for:

configuration
reference data
driver profile
vehicle data

Do not over-engineer every endpoint.

==================================================
48. RESPONSE SIZE
==================================================

Do not return enormous nested objects by default.

Example:

GET /trips

should not automatically return:

all GPS history
all documents
all payments
all audit logs
all events

Use:

GET /trips/{id}

for detailed resource

and dedicated endpoints for expensive collections.

==================================================
49. FIELD EXPANSION
==================================================

If necessary support controlled expansion:

?include=driver,vehicle

Whitelist allowed expansions.

Do not allow arbitrary database graph traversal.

==================================================
50. RATE LIMITING
==================================================

Implement rate limits at gateway and/or application layers.

Different limits for:

authentication
booking creation
GPS ingestion
admin APIs
payment endpoints
public APIs

GPS ingestion may need high throughput.

Login/payment endpoints require stricter abuse protection.

==================================================
51. BRUTE FORCE PROTECTION
==================================================

Protect:

login
OTP
password reset
refresh token
payment initiation

Use:

rate limits
attempt limits
temporary lockouts where appropriate
monitoring

Do not leak whether an account exists.

==================================================
52. REQUEST SIZE LIMITS
==================================================

Set request size limits.

Especially:

JSON
multipart
document metadata
batch GPS

Large documents should use signed object-storage uploads rather than passing binary data through the API server.

==================================================
53. FILE UPLOAD API
==================================================

Do NOT send large files through normal application servers if avoidable.

Recommended:

POST /documents/upload-url

Server returns:

signed upload URL

Client uploads directly to object storage.

Then:

POST /documents/complete

Backend verifies:

object exists
size
mime type
checksum
owner
document type

==================================================
54. DOCUMENT ACCESS
==================================================

Documents should be downloaded using short-lived signed URLs.

Do not expose raw bucket paths.

Example:

GET /api/v1/documents/{id}/download-url

Backend:

authenticate
authorize
generate short-lived URL

==================================================
55. PAYMENT APIs
==================================================

Examples:

POST /api/v1/payments

GET /api/v1/payments/{id}

POST /api/v1/payments/{id}/refund

GET /api/v1/bookings/{id}/payments

Payment creation must be idempotent.

==================================================
56. PAYMENT FLOW
==================================================

Correct flow:

Client
  ↓
Create payment request
  ↓
Backend
  ↓
Payment provider
  ↓
Provider response
  ↓
Client
  ↓
Provider webhook
  ↓
Backend verifies signature
  ↓
Database transaction
  ↓
Payment finalized
  ↓
Ledger
  ↓
Outbox
  ↓
Notifications

Do not finalize financial truth solely from the synchronous provider response.

==================================================
57. WEBHOOK APIs
==================================================

Webhook endpoints must:

1. Verify signature.
2. Identify provider.
3. Deduplicate event ID.
4. Validate payload.
5. Begin transaction.
6. Update authoritative state.
7. Create ledger records if required.
8. Create outbox event.
9. Mark webhook processed.
10. Commit.
11. Return provider-compatible success.

Never process the same webhook twice.

==================================================
58. WEBHOOK TIMEOUT
==================================================

Webhook handlers should be fast.

If processing is expensive:

verify and durably store event
    ↓
enqueue processing
    ↓
return provider success

Do not hold webhook HTTP requests for long processing operations.

==================================================
59. REFUNDS
==================================================

Refund endpoint:

POST /api/v1/payments/{id}/refund

Require:

authorization
amount
reason
idempotency

Validate:

refund <= refundable amount

Do not allow duplicate refund.

Financial correction must flow through ledger/reconciliation.

==================================================
60. INVOICE APIs
==================================================

Examples:

GET /api/v1/invoices/{id}

GET /api/v1/invoices

GET /api/v1/bookings/{id}/invoice

POST /api/v1/admin/invoices/{id}/void

Issued invoices must not be freely editable.

==================================================
61. SETTLEMENT APIs
==================================================

Examples:

GET /api/v1/carrier/settlements

GET /api/v1/carrier/settlements/{id}

GET /api/v1/driver/earnings

GET /api/v1/admin/settlements

POST /api/v1/admin/settlements/{id}/approve

POST /api/v1/admin/settlements/{id}/payout

All financial commands require idempotency.

==================================================
62. DRIVER EARNINGS
==================================================

Driver earnings should be derived from authoritative settlement/ledger data.

Do not let the driver app calculate its own balance as truth.

API should expose:

gross
deductions
adjustments
tax where applicable
net
currency
status
period

==================================================
63. NOTIFICATION APIs
==================================================

Mobile clients may need:

GET /api/v1/notifications

POST /api/v1/notifications/{id}/read

POST /api/v1/notifications/read-all

Notification delivery itself should normally be asynchronous.

==================================================
64. EXCEPTION APIs
==================================================

Ops dashboard should expose:

GET /api/v1/admin/exceptions

GET /api/v1/admin/exceptions/{id}

POST /api/v1/admin/exceptions/{id}/assign

POST /api/v1/admin/exceptions/{id}/resolve

Examples:

GPS_STALE
DRIVER_NO_SHOW
PAYMENT_MISMATCH
SETTLEMENT_MISMATCH
STUCK_TRIP
DOCUMENT_MISSING

==================================================
65. AUDIT API
==================================================

Audit logs should generally NOT be editable through normal APIs.

Admins may have read access:

GET /api/v1/admin/audit-logs

Support filtering:

actor
entity
action
date range
request ID

Do not expose sensitive before/after fields to users without permission.

==================================================
66. GRAPHQL
==================================================

GraphQL may be used for admin dashboard aggregation.

Example dashboard requires:

trip
driver
vehicle
latest location
status
exception

Instead of many REST calls, GraphQL may aggregate them.

However:

GraphQL must have:

authentication
authorization
depth limits
complexity limits
pagination
query timeout
field allowlists where appropriate

Do not allow unrestricted GraphQL queries.

==================================================
67. REST REMAINS AUTHORITATIVE
==================================================

GraphQL must call application/domain services.

Do not put separate business logic in GraphQL resolvers.

Same business service:

REST
  ↓
BookingService

GraphQL
  ↓
BookingService

==================================================
68. API CACHING
==================================================

Use Redis for appropriate read caching.

Good candidates:

reference data
pricing configuration
short-lived quote data
latest location
rate limiting
session-related ephemeral data

Bad candidates:

ledger truth
payment truth
settlement truth
booking truth

Always define TTL.

Cache invalidation must be explicit.

==================================================
69. CACHE FAILURE
==================================================

If Redis goes down:

critical API functionality should continue where safely possible.

Example:

latest location may become slower

but:

booking
payment
ledger

must not depend on Redis availability for correctness.

==================================================
70. ASYNCHRONOUS OPERATIONS
==================================================

Use background jobs for:

notifications
event publishing
document processing
analytics aggregation
reconciliation
settlement generation
GPS aggregation
cleanup
large exports

Do not make user requests wait for unnecessary background work.

==================================================
71. JOB STATUS
==================================================

For operations that are asynchronous, return:

202 Accepted

with operation/job reference where appropriate.

Example:

POST /admin/exports

Response:

{
  "data": {
    "job_id": "...",
    "status": "QUEUED"
  }
}

Then:

GET /api/v1/admin/jobs/{job_id}

==================================================
72. RETRIES
==================================================

Retry only transient failures.

Use:

exponential backoff
jitter
maximum attempts

Do not retry permanent validation errors.

==================================================
73. DEAD LETTER QUEUE
==================================================

Failed background jobs/events must eventually enter a DLQ.

Ops should be able to inspect:

job
attempt count
last error
timestamps
payload reference
status

Do not silently discard failures.

==================================================
74. API OBSERVABILITY
==================================================

Every request must have:

request_id

Prefer distributed:

trace_id

Log:

method
route
status
duration
request_id
trace_id
actor
organization
error code

Do NOT log secrets.

==================================================
75. STRUCTURED LOGGING
==================================================

Use JSON logs.

Example:

{
  "level": "INFO",
  "request_id": "...",
  "route": "/api/v1/trips/{id}/accept",
  "status": 200,
  "duration_ms": 83,
  "actor_id": "...",
  "trip_id": "..."
}

Do not log:

password
access token
refresh token
payment credentials
signed URLs
document contents

==================================================
76. METRICS
==================================================

Track:

request count
error rate
latency
p50
p95
p99
rate-limit events
authentication failures
DB latency
Redis latency
queue lag
webhook failures
payment success/failure
GPS ingestion rate
WebSocket connections

Also track business metrics:

bookings/day
trips/day
active trips
completed trips
driver acceptance rate
payment success rate
settlement backlog

==================================================
77. API SLO TARGETS
==================================================

Establish targets such as:

read API p99 < 300 ms

normal write API p99 < 800 ms

Do not apply these blindly to:

large exports
complex reports
file processing
external provider operations

Define endpoint classes.

==================================================
78. HEALTH ENDPOINTS
==================================================

Implement:

/health/live

/liveness

Checks whether process is alive.

And:

/health/ready

Checks whether application can safely receive traffic.

Readiness may verify:

database connectivity
required dependencies
migration compatibility
critical configuration

Do not make liveness depend on database.

==================================================
79. GRACEFUL SHUTDOWN
==================================================

On shutdown:

stop accepting new requests
finish safe in-flight requests
stop accepting new jobs
finish/acknowledge safe jobs
close connections
close Redis
close WebSocket connections gracefully

==================================================
80. SECURITY HEADERS
==================================================

Configure appropriate HTTP security headers.

Use TLS.

Configure CORS explicitly.

Never:

Access-Control-Allow-Origin: *

for authenticated production APIs unless specifically justified.

==================================================
81. CORS
==================================================

Allow only known frontend origins.

Separate:

development
staging
production

origins.

Do not allow arbitrary origins in production.

==================================================
82. CSRF
==================================================

If browser authentication uses cookies:

implement CSRF protection.

If authentication is purely bearer-token based:

evaluate CSRF exposure accordingly.

Do not blindly copy a generic configuration.

==================================================
83. SECRETS
==================================================

Never put secrets in:

source code
Git
API responses
logs
OpenAPI examples

Use environment/secret management.

==================================================
84. API GATEWAY
==================================================

API gateway may handle:

TLS termination
authentication integration
rate limiting
routing
request IDs
WAF
IP controls

But authorization must ALSO be enforced in application services.

Never assume gateway authorization alone is sufficient.

==================================================
85. REQUEST CORRELATION
==================================================

Accept/generate:

X-Request-ID

Propagate it through:

API
database context where useful
queue
events
notifications
external integrations

==================================================
86. API TIMEOUTS
==================================================

Every upstream request must have a timeout.

Do not allow:

API request
    ↓
payment provider
    ↓
wait indefinitely

Use bounded timeouts.

==================================================
87. CIRCUIT BREAKERS
==================================================

For unreliable external providers consider:

timeouts
circuit breakers
backoff
fallback behavior

Examples:

payment provider
SMS
email
mapping service

Never retry endlessly.

==================================================
88. API ERROR CATALOG
==================================================

Create a centralized error-code catalog.

Examples:

AUTH_REQUIRED
FORBIDDEN
RESOURCE_NOT_FOUND
VALIDATION_ERROR
IDEMPOTENCY_CONFLICT
CONCURRENT_UPDATE
BOOKING_EXPIRED
QUOTE_EXPIRED
TRIP_INVALID_STATE
TRIP_ALREADY_ASSIGNED
DRIVER_NOT_ELIGIBLE
PAYMENT_FAILED
PAYMENT_ALREADY_CAPTURED
REFUND_LIMIT_EXCEEDED
DOCUMENT_NOT_FOUND
GPS_INVALID
GPS_STALE
RATE_LIMITED

Error codes must be stable.

Human-readable messages can change.

==================================================
89. API CONTRACT OWNERSHIP
==================================================

Every endpoint must have an owner module.

Example:

POST /bookings

Owner:

booking module

GET /driver/trips

Owner:

operations module

POST /payments

Owner:

payments module

Do not allow random modules to mutate another module's state.

==================================================
90. CROSS-MODULE OPERATIONS
==================================================

Use application services.

Example:

Booking confirmation may involve:

BookingService
PricingService
TripService
PaymentService

Do not perform random cross-module database updates from controllers.

==================================================
91. EVENT-DRIVEN SIDE EFFECTS
==================================================

Example:

Booking confirmed

Database transaction:

booking updated
history created
outbox event created

After commit:

notification service
analytics
WebSocket
other consumers

Do not make all side effects synchronous.

==================================================
92. API TRANSACTION RULE
==================================================

Critical business state changes must be atomic.

Example:

Trip acceptance must not result in:

assignment created

but trip remaining unassigned.

Either both happen or neither happens.

==================================================
93. READ CONSISTENCY
==================================================

After a successful write, the immediate response should reflect committed authoritative state.

Do not return:

"trip accepted"

while database still says:

ASSIGNMENT_PENDING

==================================================
94. EVENTUAL CONSISTENCY
==================================================

Clearly distinguish:

strongly consistent data

from:

eventually consistent projections.

Example:

Trip status:
authoritative PostgreSQL

Latest location:
projection/cache

Analytics:
eventually consistent

API documentation must not mislead clients.

==================================================
95. SECURITY OF ADMIN EXPORTS
==================================================

Large exports must be asynchronous.

Do not:

GET /admin/export

and hold HTTP connection for 20 minutes.

Instead:

POST /admin/exports
    ↓
job
    ↓
generate
    ↓
store securely
    ↓
short-lived download URL

Authorize export creation and download separately.

==================================================
96. API TESTING
==================================================

Create:

unit tests
integration tests
API contract tests
authorization tests
security tests
concurrency tests
load tests
failure tests
webhook tests
idempotency tests
pagination tests

==================================================
97. AUTHORIZATION TEST MATRIX
==================================================

Test combinations:

SHIPPER → own booking
SHIPPER → another booking

DRIVER → assigned trip
DRIVER → another driver's trip

CARRIER_ADMIN → own carrier
CARRIER_ADMIN → another carrier

OPS → operational resources

SUPER_ADMIN → privileged operations

Every unauthorized case must fail correctly.

==================================================
98. IDOR TESTS
==================================================

Create two organizations.

Create resources for each.

Attempt:

Organization A token
    ↓
Organization B resource

Expected:

access denied / hidden.

Test:

bookings
shipments
documents
payments
invoices
trips
settlements

==================================================
99. CONCURRENCY TESTS
==================================================

Test:

100 simultaneous trip acceptance requests.

Expected:

1 successful assignment.

Test:

100 duplicate payment webhooks.

Expected:

1 financial effect.

Test:

100 identical idempotent booking requests.

Expected:

1 booking.

==================================================
100. RATE LIMIT TESTS
==================================================

Verify:

login protection
OTP protection
GPS ingestion
payment endpoints
admin endpoints

Rate limits must be tested under realistic load.

==================================================
101. API LOAD TESTING
==================================================

Load test realistic traffic.

Examples:

booking creation
trip lookup
driver trip list
admin trip map
GPS ingestion
payment status
notifications

Measure:

throughput
p50
p95
p99
errors
DB load
Redis load
queue lag

==================================================
102. GPS LOAD TEST
==================================================

Model:

concurrent trips
GPS frequency
batch size
network retries

Example:

500 active trips
4 points/minute

approximately:

2,000 GPS points/minute

or:

2.88 million/day.

Use actual expected values from the product.

==================================================
103. API PERFORMANCE
==================================================

Optimize using:

proper indexes
pagination
projection queries
caching
connection pooling
batching
async processing

Do NOT solve performance by blindly adding caches.

==================================================
104. N+1 QUERIES
==================================================

Detect and prevent:

GET /trips

query trips once

then:

query driver once per trip
query vehicle once per trip
query location once per trip

This is an N+1 problem.

Use:

joins where appropriate
batch queries
data loaders
projection queries

while respecting module boundaries.

==================================================
105. DATABASE QUERY PROJECTION
==================================================

Do not retrieve:

SELECT *

for API responses.

Retrieve only required columns.

Admin list:

return summary fields.

Detail endpoint:

return detailed fields.

==================================================
106. API RESOURCE SHAPES
==================================================

Define separate DTOs:

TripListItem
TripDetail
TripDriverView
AdminTripView
DriverTripView

Do not use one enormous TripResponse everywhere.

==================================================
107. MOBILE API DESIGN
==================================================

Mobile APIs should minimize:

payload size
round trips
nested unnecessary data

Prefer:

summary endpoint
detail endpoint
sync endpoint
batch endpoint

where appropriate.

==================================================
108. OFFLINE DRIVER SUPPORT
==================================================

Driver app may operate offline.

API commands must support:

client command ID
idempotency key
created_at
device timestamp
server timestamp

When reconnecting:

client retries command.

Server determines whether it already happened.

==================================================
109. COMMAND RESULT
==================================================

For important driver commands return:

command/result identity
current resource state
server timestamp
version

Example:

{
  "data": {
    "trip_id": "...",
    "status": "ARRIVED",
    "version": 8,
    "updated_at": "..."
  }
}

This helps mobile synchronize state.

==================================================
110. CLOCK SAFETY
==================================================

Never trust device clock as authoritative.

Store:

device timestamp
server timestamp

Business rules should use server timestamps unless device time is specifically required as evidence.

==================================================
111. API DOCUMENTATION FOR AGENTS
==================================================

Generate machine-readable OpenAPI.

Include examples.

AI agents and frontend developers should be able to generate clients from the contract.

==================================================
112. API CHANGE PROCESS
==================================================

Any endpoint change must update:

OpenAPI
DTOs
tests
frontend types
documentation
changelog if externally relevant

CI should detect contract drift.

==================================================
113. FEATURE FLAGS
==================================================

Potentially gate incomplete APIs behind feature flags.

Example:

new pricing engine
new payment provider
new tracking algorithm

Do not expose unfinished functionality accidentally.

==================================================
114. API DEPLOYMENT
==================================================

Deployment must support:

development
staging
production

Use:

CI
automated tests
migration checks
security scans
container builds

Do not deploy untested API changes directly to production.

==================================================
115. BACKWARD COMPATIBILITY
==================================================

Before deployment ask:

Will old mobile app versions still work?

This is especially important because users do not update mobile apps instantly.

Do not make mandatory API changes that immediately break older mobile versions.

==================================================
116. MOBILE API COMPATIBILITY WINDOW
==================================================

Design API responses so older clients can continue operating.

Prefer additive changes.

For breaking behavior:

version
feature flag
minimum supported app version

where necessary.

==================================================
117. API REQUEST SIGNING
==================================================

For highly sensitive integrations consider request signatures where justified.

Do not invent cryptographic protocols unnecessarily.

Use established standards.

==================================================
118. WEBHOOK SECURITY
==================================================

Webhook endpoints should verify:

signature
timestamp where supported
provider identity
event ID
payload schema

Reject replayed events where provider protocol supports replay protection.

==================================================
119. AUDITABILITY
==================================================

For sensitive API operations record:

actor
organization
action
resource
before
after
request ID
timestamp
source

Especially:

refund
settlement adjustment
trip override
driver verification
role changes
manual payment adjustment

==================================================
120. API ABUSE MONITORING
==================================================

Detect:

rapid login attempts
high booking creation
GPS floods
repeated payment attempts
mass resource enumeration
suspicious admin behavior
excessive document downloads

Do not wait for a database outage to discover abuse.

==================================================
121. PRODUCTION READINESS
==================================================

The API layer is NOT production-ready until:

[ ] OpenAPI exists
[ ] Authentication works
[ ] Authorization works
[ ] IDOR protection tested
[ ] Validation implemented
[ ] Error model standardized
[ ] Idempotency implemented
[ ] Concurrency tested
[ ] State machines enforced
[ ] Pagination implemented
[ ] Rate limiting implemented
[ ] Webhooks secured
[ ] Payment flow tested
[ ] Ledger integration tested
[ ] Settlement integration tested
[ ] GPS ingestion load tested
[ ] WebSocket authentication tested
[ ] Redis failure tested
[ ] Database failure tested
[ ] External provider timeout tested
[ ] Retry behavior tested
[ ] DLQ implemented
[ ] Logging implemented
[ ] Metrics implemented
[ ] Tracing implemented
[ ] Health endpoints implemented
[ ] Graceful shutdown implemented
[ ] Security scanning passed
[ ] Contract tests passed
[ ] Load tests passed
[ ] Mobile backward compatibility verified
[ ] Deployment pipeline verified

==================================================
122. IMPLEMENTATION ORDER
==================================================

Do NOT implement the API randomly.

Implement in this order.

PHASE 1
API foundation

- server configuration
- routing
- versioning
- middleware
- request IDs
- error handling
- validation
- OpenAPI
- health endpoints

PHASE 2
Identity

- login
- refresh
- logout
- users
- roles
- organizations
- authorization

PHASE 3
Fleet

- carriers
- drivers
- vehicles

PHASE 4
Geo

- locations
- ports
- terminals
- lanes

PHASE 5
Shipment

- shipments
- containers
- stops

PHASE 6
Pricing

- pricing configuration
- quotes
- quote components

PHASE 7
Bookings

- create
- list
- detail
- confirm
- cancel
- history

PHASE 8
Operations

- trips
- trip details
- job offers
- assignments
- state transitions
- exceptions

PHASE 9
Driver APIs

- driver dashboard
- trip workflow
- location ingestion
- batch sync
- POD

PHASE 10
Tracking

- latest location
- history
- WebSockets
- live map APIs

PHASE 11
Documents

- upload URL
- completion
- download URL
- POD

PHASE 12
Payments

- payment creation
- status
- webhook
- refunds

PHASE 13
Billing

- invoices
- invoice details

PHASE 14
Settlement

- earnings
- settlements
- approval
- payout

PHASE 15
Notifications

- notification list
- read state
- delivery infrastructure

PHASE 16
Admin

- operational dashboard
- assignments
- overrides
- exceptions
- audit
- exports

PHASE 17
Reliability

- idempotency
- outbox
- inbox
- retries
- DLQ

PHASE 18
Performance

- caching
- query optimization
- load testing
- connection tuning

PHASE 19
Security

- penetration testing
- authorization tests
- abuse tests
- dependency scanning

==================================================
123. AGENT WORKFLOW
==================================================

For every phase:

1. Inspect current code.
2. Identify dependencies.
3. Explain intended changes.
4. Implement.
5. Add/update database interactions.
6. Add validation.
7. Add authorization.
8. Add tests.
9. Update OpenAPI.
10. Run unit tests.
11. Run integration tests.
12. Run API contract tests.
13. Run lint/type checks.
14. Inspect generated SQL.
15. Test failure cases.
16. Test concurrency where relevant.
17. Test backward compatibility.
18. Report exactly what changed.

Do not silently modify unrelated systems.

==================================================
124. REQUIRED API DOCUMENTATION
==================================================

Produce:

1. API architecture
2. Endpoint catalog
3. Authentication model
4. Authorization model
5. Error catalog
6. Request/response schemas
7. OpenAPI
8. Pagination specification
9. Idempotency specification
10. WebSocket specification
11. Webhook specification
12. Rate-limit specification
13. Versioning strategy
14. Mobile synchronization strategy
15. API security model
16. API observability model
17. API testing strategy

==================================================
125. REQUIRED FINAL API REPORT
==================================================

After implementation report:

A. Existing API audit
B. New architecture
C. Endpoint catalog
D. Authentication
E. Authorization
F. Database integration
G. Idempotency
H. Concurrency
I. Payments
J. GPS
K. WebSockets
L. Webhooks
M. Notifications
N. Documents
O. Admin APIs
P. GraphQL if used
Q. Caching
R. Queues
S. Observability
T. Security
U. Testing
V. Load-test results
W. Known issues
X. Production readiness

Classify unresolved issues:

BLOCKER
HIGH
MEDIUM
LOW

Never claim production readiness if BLOCKER or HIGH issues remain.

==================================================
126. FINAL PRINCIPLE
==================================================

Build APIs for unreliable real-world clients, not ideal clients.

Assume:

mobile devices lose connectivity
users double-tap buttons
requests timeout
requests are retried
webhooks arrive twice
webhooks arrive out of order
drivers go offline
GPS points arrive late
GPS points arrive duplicated
admins make mistakes
payment providers fail
Redis fails
queues fail
databases temporarily fail
old mobile versions remain installed
external APIs become slow

The API must remain correct under these conditions.

Correctness > convenience.

Security > speed of implementation.

Explicit contracts > implicit behavior.

Idempotency > assumptions.

Database integrity > cached state.

Auditability > destructive updates.

Build this as a production logistics API platform that real customers, drivers, operators and financial systems can depend on.