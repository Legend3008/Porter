# Production Backend Build Prompt

## Full Production-Grade Backend for a Container Logistics Platform

---

# 0. MASTER ENGINEERING DIRECTIVE

You are a **principal backend/platform engineering organization** responsible for designing, implementing, testing, securing, deploying, operating, and documenting the complete production backend for a Porter-style container logistics platform.

This backend is the **authoritative system of record** for:

* identity
* customers
* drivers
* carriers
* vehicles
* ports
* lanes
* containers
* shipments
* bookings
* pricing
* assignments
* trips
* GPS/location data
* status transitions
* documents
* payments
* refunds
* invoices
* settlements
* notifications
* operational exceptions
* audit records
* analytics-facing operational data

The backend serves:

1. Shipper Android application
2. Driver/Carrier Android application
3. Ops/Admin web dashboard
4. Internal operational tooling
5. Payment providers
6. Notification providers
7. Mapping/location services
8. Future integrations

This is **not an MVP**.

Do not optimize for:

* fastest demo
* fewest files
* minimum infrastructure
* mocked business logic
* hardcoded workflows
* premature microservices
* UI-driven validation
* manual operational processes

Optimize for:

**correctness, financial integrity, security, resilience, observability, auditability, scalability, maintainability, recoverability, and operational simplicity.**

---

# 1. SOURCE OF TRUTH

The backend is the only authoritative source for:

```text
pricing
booking state
trip state
payment state
ledger state
settlement state
assignment state
delivery state
document state
permissions
```

Clients are untrusted consumers.

Never trust:

* client-calculated prices
* client-supplied earnings
* client-supplied detention duration
* client-supplied delivery status
* client-reported payment success
* client-reported authorization
* client timestamps when server timestamps are required
* client-generated financial totals
* client-generated state transitions

All authoritative business rules must execute server-side.

---

# 2. BUSINESS SCALE

Initial market:

```text
India
```

Initial payment ecosystem may include:

```text
Razorpay
PhonePe
```

and must remain architecturally capable of supporting additional gateways.

Initial corridor may be:

```text
JNPT → Mumbai/Navi Mumbai
```

but corridor-specific assumptions must never exist in business logic.

The system must support configuration-driven:

```text
ports
terminals
lanes
routes
distance bands
pricing rules
surcharges
tax rules
detention policies
vehicle types
container types
operating hours
holidays
```

Target growth:

```text
~200 trips/day launch
→ ~5,000 trips/day within 12 months
```

Design with sufficient headroom beyond the target.

---

# 3. ARCHITECTURAL STRATEGY

Start as a **modular monolith**.

Do not introduce microservices merely because the product is production-grade.

However, module boundaries must be designed as future service boundaries.

Required modules:

```text
identity
customer
driver
carrier
fleet
port
lane
container
shipment
booking
pricing
assignment
trip
tracking
document
payment
ledger
billing
settlement
notification
audit
admin
reporting
```

Each module must have:

* owned domain model
* owned persistence model
* public application interface
* domain services
* validation
* authorization checks
* events
* tests

---

# 4. MODULE OWNERSHIP RULE

Each module owns its database tables.

Cross-module access must happen through:

```text
application service interfaces
domain events
explicit query interfaces
```

Never allow arbitrary modules to perform:

```text
direct SQL against another module's tables
```

Avoid cross-module database joins.

If reporting requires cross-domain data:

```text
operational query model
read model
materialized view
analytics pipeline
```

may be introduced deliberately.

---

# 5. DOMAIN MODULE CONTRACTS

Before implementing each module, define:

```text
entities
value objects
commands
queries
state transitions
events
invariants
authorization rules
database ownership
external dependencies
failure modes
```

Every module must have a documented contract.

Example:

```text
booking
├── CreateBooking
├── GetBooking
├── CancelBooking
├── ConfirmBooking
├── AssignTrip
├── BookingCreated
├── BookingConfirmed
└── BookingCancelled
```

---

# 6. DOMAIN-DRIVEN DESIGN

Use domain-driven design where it improves correctness.

Distinguish:

```text
Entity
Value Object
Aggregate
Domain Event
Command
Query
Policy
Invariant
```

Do not turn every database table into a domain object.

Identify true aggregates and enforce transactional invariants at the aggregate boundary.

---

# 7. DATABASE ARCHITECTURE

Primary database:

```text
PostgreSQL
```

Managed production deployment:

```text
Aurora/RDS/Cloud SQL equivalent
```

Requirements:

* Multi-AZ
* encryption at rest
* TLS
* connection pooling
* automated backups
* PITR
* replica capability
* monitoring
* migration system
* schema ownership
* least-privilege database users

---

# 8. DATABASE TRANSACTION RULES

Every critical business operation must have explicit transaction boundaries.

Examples:

```text
Create booking
Reserve quote
Create payment intent
Record ledger transaction
Accept driver assignment
Transition trip
Create settlement
```

Do not rely on application-level assumptions such as:

```text
check
→ then write
```

when concurrent requests can invalidate the check.

Use appropriate:

```text
row locking
unique constraints
optimistic concurrency
serializable transactions
advisory locks
atomic updates
```

where necessary.

---

# 9. DATABASE CONSTRAINTS

Important invariants must be enforced at both:

```text
application layer
database layer
```

where practical.

Examples:

```text
unique booking reference
unique payment provider transaction ID
unique idempotency key per operation scope
unique active assignment where applicable
non-negative quantities where applicable
valid foreign keys
valid enum/domain values
```

Never rely exclusively on frontend validation.

---

# 10. CONCURRENCY CONTROL

Explicitly design for simultaneous operations.

Examples:

```text
two drivers accepting one job
two admins modifying one trip
duplicate payment callbacks
duplicate webhook delivery
two booking requests for one resource
simultaneous cancellation + assignment
simultaneous trip completion
```

The system must produce one deterministic authoritative result.

Test every critical race.

---

# 11. OPTIMISTIC CONCURRENCY

Where entities can be edited concurrently, use:

```text
version
revision
updated_at
ETag
```

or equivalent.

Example:

```text
PATCH trip
If-Version: 14
```

If current version is 15:

```text
409 CONFLICT
```

Return authoritative state where appropriate.

Never silently overwrite another operator's changes.

---

# 12. API ARCHITECTURE

Primary external API:

```text
REST
/api/v1/...
```

Documentation:

```text
OpenAPI 3
```

Requirements:

* versioning
* consistent response envelope where appropriate
* standardized errors
* pagination
* filtering
* sorting
* validation
* correlation IDs
* idempotency
* rate limiting
* authentication
* authorization
* audit metadata

---

# 13. API VERSIONING

Never make silent breaking changes.

Allowed:

```text
add optional field
add endpoint
add enum only where clients safely tolerate unknown values
```

Potentially breaking:

```text
remove field
rename field
change meaning
change requiredness
change data type
remove enum value
```

Breaking changes require:

```text
new API version
migration plan
deprecation period
client compatibility plan
```

---

# 14. STANDARD API ERROR MODEL

Define a consistent error structure.

Example conceptual shape:

```json
{
  "error": {
    "code": "QUOTE_EXPIRED",
    "message": "The quote has expired.",
    "request_id": "req_...",
    "details": {}
  }
}
```

Never expose internal stack traces.

Error codes must be stable enough for clients to implement deterministic behavior.

---

# 15. HTTP STATUS SEMANTICS

Use HTTP semantics consistently:

```text
200 success
201 created
202 accepted/async processing
204 success/no content
400 malformed request
401 unauthenticated
403 unauthorized
404 not found
409 business/concurrency conflict
422 validation/business validation
429 rate limited
500 internal error
502 upstream failure
503 temporarily unavailable
```

Do not return `200` for every business failure.

---

# 16. IDEMPOTENCY

Every retryable state-changing operation must support idempotency.

Required examples:

```text
POST booking
POST payment
POST refund
POST assignment acceptance
POST trip transition
POST POD
POST document upload/finalization
POST settlement
POST notification action
```

Idempotency record must contain at minimum:

```text
key
actor
endpoint/operation
request fingerprint
status
response
created_at
expires_at
```

If the same key is reused with a different request body:

```text
409 IDEMPOTENCY_KEY_REUSE
```

If the original operation completed:

```text
return original result
```

If it is still processing:

```text
return deterministic processing state
```

---

# 17. OUTBOX PATTERN

Never perform:

```text
database transaction
+
event publish
```

as two independent operations.

Use a transactional outbox.

Example:

```text
BEGIN
    update booking
    insert BookingConfirmed event into outbox
COMMIT
```

A publisher then delivers the event.

This prevents:

```text
database updated
but event lost
```

---

# 18. INBOX / EVENT DEDUPLICATION

Every event consumer must tolerate at-least-once delivery.

Store processed event IDs where required:

```text
consumer
event_id
processed_at
```

Duplicate event:

```text
ignore safely
```

Never assume exactly-once delivery.

---

# 19. EVENT ARCHITECTURE

Use a managed queue initially:

```text
SQS
Pub/Sub
RabbitMQ
```

Choose one actual production technology.

Do not design the implementation around all three simultaneously.

Events must be:

```text
versioned
schema-defined
idempotent
traceable
replayable where appropriate
```

---

# 20. EVENT ENVELOPE

Define a common event envelope.

Conceptually:

```json
{
  "event_id": "evt_123",
  "event_type": "TripStarted",
  "event_version": 1,
  "occurred_at": "...",
  "producer": "trip",
  "aggregate_type": "trip",
  "aggregate_id": "trip_123",
  "correlation_id": "req_123",
  "causation_id": "evt_122",
  "payload": {}
}
```

Never depend exclusively on event ordering across distributed consumers.

---

# 21. EVENT EVOLUTION

Never silently modify the meaning of an existing event.

Use:

```text
TripStarted.v1
TripStarted.v2
```

or equivalent schema versioning.

Maintain backward compatibility for consumers during migration.

---

# 22. EVENT REPLAY

Operational tooling must allow authorized engineers to:

```text
inspect event
identify consumer
replay event
```

where technically safe.

Replay must be idempotent.

Never provide unrestricted event replay to normal admin users.

---

# 23. REAL-TIME TRACKING

GPS ingestion must be separate from the core transactional booking path.

Pipeline:

```text
GPS device/app
↓
ingestion API
↓
validation
↓
queue/buffer
↓
tracking processor
↓
latest-position Redis state
↓
WebSocket
```

Historical location storage should not overload transactional tables unnecessarily.

---

# 24. GPS INGESTION

Validate:

```text
latitude
longitude
accuracy
timestamp
speed
heading
driver
vehicle
trip
```

Reject impossible values.

Detect suspicious:

```text
teleportation
impossible speed
future timestamps
ancient timestamps
invalid coordinates
```

Do not necessarily discard suspicious data; mark it according to the operational model.

---

# 25. LOCATION TIMESTAMP TRUST

Record separately:

```text
device_timestamp
server_received_at
processed_at
```

Never confuse these.

Server time is authoritative for:

```text
audit
billing
detention
settlement
```

unless explicitly defined otherwise.

---

# 26. LOCATION STORAGE

Store:

```text
latest position
historical position
```

separately.

Redis:

```text
latest active trip position
```

PostgreSQL/time-series/partitioned storage:

```text
historical positions
```

Partition/index by:

```text
trip
timestamp
```

according to measured workload.

---

# 27. TRACKING RETENTION

Define explicit retention policies for location history.

Example policy must specify:

```text
hot storage retention
archive retention
legal retention
deletion process
anonymization strategy
```

Do not retain precise location indefinitely without a business/legal reason.

---

# 28. TRIP STATE MACHINE

Trip state transitions must be explicitly modeled.

Example:

```text
CREATED
↓
ASSIGNED
↓
DRIVER_ACCEPTED
↓
EN_ROUTE_PICKUP
↓
AT_PICKUP
↓
LOADING
↓
PICKED_UP
↓
GATE_OUT
↓
IN_TRANSIT
↓
AT_DESTINATION
↓
UNLOADING
↓
DELIVERED
↓
COMPLETED
```

Actual states must be generated from the product's authoritative domain model.

Every transition defines:

```text
allowed actor
required conditions
required evidence
side effects
events
audit entry
```

---

# 29. STATE MACHINE INVARIANTS

Reject:

```text
backward transition
skipped mandatory state
duplicate transition
unauthorized transition
transition without prerequisites
transition after terminal state
```

unless an explicitly defined correction workflow exists.

Corrections must be audited.

---

# 30. ADMIN OVERRIDES

Administrative overrides are not normal transitions.

Require:

```text
permission
reason
actor
timestamp
old state
new state
correlation ID
```

Where possible, distinguish:

```text
normal transition
corrective transition
administrative override
```

Never mutate state directly through an admin SQL command.

---

# 31. PRICING ENGINE

Pricing must be data-driven.

Do not hardcode:

```text
lane = Mumbai
price = X
```

Instead model:

```text
lane
port
origin
destination
vehicle type
container type
distance band
base price
fuel surcharge
night surcharge
weekend surcharge
reefer surcharge
detention
tax
minimum charge
effective_from
effective_until
priority
```

---

# 32. PRICING VERSIONING

Every quote must reference the exact pricing rule/version used.

Store:

```text
pricing_version
rule IDs
inputs
calculated components
tax components
currency
quote expiration
```

This makes historical pricing reproducible.

---

# 33. QUOTE IMMUTABILITY

A quote, once issued, must retain its exact calculation inputs and result.

Do not recompute historical quotes against today's pricing rules.

---

# 34. QUOTE EXPIRATION

Quotes have server-defined TTL.

On booking:

```text
validate quote existence
validate ownership
validate expiry
validate version
validate amount
```

If invalid:

```text
QUOTE_EXPIRED
QUOTE_INVALID
PRICE_CHANGED
```

according to business rules.

---

# 35. MONEY REPRESENTATION

Never use floating-point arithmetic for money.

Use:

```text
integer minor units
```

or PostgreSQL `NUMERIC` according to the financial architecture.

Every monetary amount must have:

```text
amount
currency
```

Never assume INR forever in database schemas if multi-currency may eventually matter.

---

# 36. TAX

Tax calculation must be server-side.

For India, support the applicable GST concepts required by the business.

Tax calculations must record:

```text
tax type
rate
taxable amount
tax amount
jurisdiction
rule/version
```

Do not derive historical tax from today's configuration.

---

# 37. FINANCIAL LEDGER

The financial subsystem must be ledger-first.

Never use:

```text
balance = balance + amount
```

as the authoritative financial operation.

Use immutable ledger entries.

Conceptual model:

```text
LedgerAccount
LedgerTransaction
LedgerEntry
```

A transaction should balance according to the accounting model.

---

# 38. DOUBLE-ENTRY PRINCIPLE

Where applicable, use double-entry accounting:

```text
debit
credit
```

with invariant:

```text
sum(debits) == sum(credits)
```

The exact chart of accounts must be defined for the business.

---

# 39. LEDGER IMMUTABILITY

Never update or delete posted financial entries.

Corrections happen through:

```text
reversal
adjustment
compensating transaction
```

Every correction references the original transaction.

---

# 40. FINANCIAL AUDITABILITY

Every financial transaction must be traceable:

```text
customer
booking
trip
payment provider
payment ID
invoice
ledger transaction
settlement
payout
refund
```

An engineer/auditor must be able to reconstruct why a balance exists.

---

# 41. PAYMENT LIFECYCLE

Model payment as a state machine.

Example:

```text
CREATED
↓
INITIATED
↓
PENDING
↓
AUTHORIZED
↓
CAPTURED
```

Failure paths:

```text
FAILED
CANCELLED
EXPIRED
REFUNDED
PARTIALLY_REFUNDED
```

Actual provider semantics must be respected.

---

# 42. PAYMENT WEBHOOKS

Payment confirmation must come from verified provider callbacks.

Webhook pipeline:

```text
provider
↓
TLS
↓
signature verification
↓
raw event persistence
↓
deduplication
↓
business processing
↓
ledger transaction
↓
domain event
```

Never trust client callbacks as authoritative payment confirmation.

---

# 43. WEBHOOK SECURITY

Validate:

```text
signature
timestamp where supported
event ID
provider account
payload schema
```

Protect against:

```text
replay attacks
duplicate callbacks
forged callbacks
malformed payloads
```

Persist raw webhook payload securely for reconciliation/audit according to retention policy.

---

# 44. WEBHOOK IDEMPOTENCY

Provider event IDs must be deduplicated.

Repeated webhook:

```text
same final state
no duplicate ledger transaction
no duplicate refund
no duplicate notification
```

---

# 45. PAYMENT RECONCILIATION

Build automated reconciliation.

Compare:

```text
internal payment records
gateway transactions
ledger
bank/payout records
```

Identify:

```text
missing internal payment
missing gateway payment
amount mismatch
status mismatch
duplicate
unknown transaction
```

Create an operational exception rather than silently modifying money.

---

# 46. REFUNDS

Refunds must be:

```text
idempotent
ledger-backed
audited
gateway-reconciled
```

Partial refunds must never exceed the refundable amount.

Use database constraints/transactions to prevent race-condition over-refunding.

---

# 47. SETTLEMENT ENGINE

Carrier settlement pipeline:

```text
trip completion
↓
eligible-for-settlement
↓
settlement calculation
↓
review/approval where required
↓
ledger posting
↓
payout initiation
↓
provider confirmation
↓
reconciliation
↓
settled
```

Settlement calculation must be reproducible from immutable source data.

---

# 48. SETTLEMENT SAFETY

A settlement job may fail at any point.

It must be safely rerunnable.

Example:

```text
calculate
→ crash
→ restart
→ detect existing settlement
→ continue
```

Never pay twice because a worker restarted.

---

# 49. PAYMENT/SETTLEMENT OUTAGE

If payment provider is unavailable:

```text
do not mark payment successful
```

If payout provider is unavailable:

```text
retain payable state
retry safely
```

Financial correctness is more important than immediate UI success.

---

# 50. INVOICING

Invoices must reference authoritative financial records.

Invoice must preserve:

```text
invoice number
customer
booking
line items
tax
subtotal
total
currency
issued_at
status
```

Historical invoices must not silently change because pricing rules later changed.

---

# 51. DOCUMENT SERVICE

Store documents in S3-compatible object storage.

Never expose public buckets.

Use:

```text
signed upload URLs
signed download URLs
short expiration
authorization checks
```

---

# 52. DOCUMENT METADATA

Database stores:

```text
document ID
owner
entity
type
object key
size
MIME type
checksum
created_at
uploaded_at
status
```

The object store holds the binary.

---

# 53. DOCUMENT SECURITY

Protect against:

```text
malware
oversized files
wrong MIME type
extension spoofing
unauthorized access
path traversal
public exposure
```

Where required, integrate malware scanning before marking documents trusted.

---

# 54. POD INTEGRITY

POD records must be immutable after finalization except through explicit correction workflows.

Store:

```text
photo
signature
recipient
timestamp
GPS context where legally/business appropriate
driver
trip
document checksum
```

Never let a client overwrite finalized evidence without audit.

---

# 55. IDENTITY

Implement:

```text
shipper
driver
carrier admin
ops
finance
support
super-admin
```

as explicit roles/permissions.

Avoid giant boolean checks such as:

```text
isAdmin
```

for complex authorization.

---

# 56. RBAC

Permissions should be granular.

Example:

```text
trip.read
trip.update
trip.override
driver.read
driver.assign
pricing.read
pricing.write
payment.read
refund.create
settlement.approve
audit.read
```

Roles map to permissions.

---

# 57. AUTHORIZATION

Authorization must be checked:

```text
API/gateway
service layer
resource level
```

Gateway authorization is not sufficient.

Example:

A shipper may have:

```text
trip.read
```

but only for:

```text
their own trips
```

---

# 58. RESOURCE-LEVEL AUTHORIZATION

Every sensitive query must enforce ownership/scope.

Never:

```text
GET /trips/:id
→ fetch trip
→ return it
```

without checking whether the caller is allowed to access it.

Prevent IDOR/BOLA vulnerabilities.

---

# 59. AUTHENTICATION

Use:

```text
short-lived access token
revocable refresh token
```

Support:

```text
token rotation
revocation
session listing
logout
forced logout
device/session tracking where appropriate
```

---

# 60. PASSWORD SECURITY

If passwords are used:

* use Argon2id or equivalent modern password hashing
* never store plaintext passwords
* rate-limit authentication
* detect credential stuffing
* avoid account enumeration
* support secure reset
* invalidate appropriate sessions after password changes

---

# 61. OTP SECURITY

If OTP login exists:

```text
short expiration
attempt limit
rate limit
single-use
purpose binding
device/IP abuse protection
```

Never log OTP values.

---

# 62. SESSION SECURITY

Track:

```text
session ID
user
device
created
last active
revoked
expiration
```

Sensitive sessions must be revocable.

---

# 63. ADMIN SECURITY

Admin accounts require stronger controls.

Support:

```text
shorter sessions
strong authentication
MFA where appropriate
IP/device controls where appropriate
privileged action auditing
```

Super-admin operations should receive additional protection.

---

# 64. SECRETS

Never store secrets in Git.

Use:

```text
AWS Secrets Manager
GCP Secret Manager
Azure Key Vault
Vault
```

or equivalent.

Rotate:

```text
database credentials
API keys
webhook secrets
signing keys
service credentials
```

according to a defined policy.

---

# 65. ENCRYPTION

Use:

```text
TLS externally
TLS internally where appropriate
database encryption at rest
object-storage encryption
managed key systems
```

Sensitive fields may require application-level encryption depending on threat model.

---

# 66. PERSONAL DATA

Classify data:

```text
public
internal
confidential
sensitive
highly sensitive
```

Define:

```text
access rules
retention
encryption
logging policy
deletion policy
```

---

# 67. PRIVACY

Support applicable privacy requirements.

Implement:

```text
data minimization
purpose limitation
retention policies
access logging
deletion requests
export/access requests where applicable
consent/notice where required
```

Do not automatically claim GDPR applicability; determine the legal obligations for actual operating jurisdictions.

---

# 68. DATA DELETION

Deletion must respect:

```text
financial records
tax records
legal retention
audit requirements
fraud prevention
```

If data cannot be physically deleted due to legal obligations:

```text
retain minimum required fields
restrict access
document retention basis
```

---

# 69. RATE LIMITING

Rate-limit by:

```text
IP
user
device
API key
client type
endpoint
operation
```

Apply stricter limits to:

```text
login
OTP
password reset
payment
booking creation
document upload
GPS ingestion
admin mutations
```

---

# 70. ABUSE PREVENTION

Detect suspicious patterns:

```text
rapid account creation
OTP abuse
booking spam
payment retries
GPS flooding
document flooding
job acceptance automation
API scraping
```

Introduce throttling/blocking mechanisms where appropriate.

---

# 71. INPUT VALIDATION

Validate every external input.

Use schema validation for:

```text
JSON
query parameters
path parameters
headers
GraphQL input
webhooks
file metadata
GPS payloads
```

Never trust client-provided IDs without authorization checks.

---

# 72. SQL SECURITY

Use parameterized queries/ORM-safe mechanisms.

Never concatenate user input into SQL.

Use:

```text
least-privilege DB users
```

Separate:

```text
application user
migration user
read-only reporting user
```

where practical.

---

# 73. GRAPHQL SECURITY

GraphQL is admin-facing.

Implement:

```text
depth limits
complexity limits
query timeout
pagination
authorization
field-level access
introspection policy
rate limits
```

Prevent expensive arbitrary queries from taking down the database.

---

# 74. ADMIN QUERY PERFORMANCE

Never allow unrestricted queries across millions of records.

Require:

```text
pagination
bounded date ranges
indexed filters
query limits
timeouts
```

Use dedicated read models where operational queries become expensive.

---

# 75. CACHING

Redis may be used for:

```text
latest GPS
sessions
rate limits
short-lived quotes
hot configuration
distributed locks where justified
```

Redis must never become the sole source of financial/business truth.

---

# 76. CACHE FAILURE

If Redis fails:

```text
financial correctness must survive
booking correctness must survive
authorization must remain safe
```

Use fallback behavior according to each use case.

Do not blindly fail open.

---

# 77. DISTRIBUTED LOCKS

Use distributed locking only where necessary.

Locks must have:

```text
owner/token
expiration
safe release
bounded duration
```

Never rely on an indefinite distributed lock.

Database constraints are preferable for many uniqueness problems.

---

# 78. NOTIFICATIONS

Build notification infrastructure supporting:

```text
FCM
email
SMS
future channels
```

Use asynchronous jobs.

Notification delivery must not block core booking/payment transactions.

---

# 79. NOTIFICATION IDEMPOTENCY

A domain event may generate multiple processing attempts.

Ensure:

```text
one logical notification
```

does not accidentally become dozens of messages.

---

# 80. BACKGROUND JOBS

Use a durable job/queue system.

Jobs require:

```text
job ID
attempt count
visibility timeout
retry policy
dead-letter queue
backoff
deduplication where necessary
```

---

# 81. DEAD-LETTER QUEUES

Failed jobs must become visible.

Provide operational tooling for:

```text
inspect
retry
discard
replay
```

with authorization and audit logging.

Never silently discard failed financial jobs.

---

# 82. RETRY STRATEGY

Retries must distinguish:

```text
transient
permanent
business conflict
authentication
authorization
rate limit
```

Use:

```text
exponential backoff
jitter
maximum attempts
dead-lettering
```

Never infinite retry.

---

# 83. EXTERNAL PROVIDER RESILIENCE

For payment, SMS, email, maps, and other providers:

Implement:

```text
timeout
retry
circuit breaker where appropriate
fallback
provider status handling
observability
```

Never allow one external provider to indefinitely block core workers.

---

# 84. TIME HANDLING

Store timestamps in UTC.

Represent timezone explicitly when business logic depends on local time.

Never use server-local timezone implicitly.

All scheduled rules must specify:

```text
timezone
effective date
```

---

# 85. CLOCK SKEW

Do not trust client clocks for financial/audit decisions.

Use server timestamps.

For GPS:

```text
device timestamp
server receive timestamp
```

must remain separate.

---

# 86. SCHEDULING

Scheduled operations such as:

```text
quote expiration
detention calculation
settlement
invoice generation
notification
```

must be safe under:

```text
duplicate execution
worker restart
clock changes
deployment
partial failure
```

---

# 87. DETENTION ENGINE

Detention must be calculated from authoritative timestamped events.

Inputs may include:

```text
gate-in
loading start
loading end
gate-out
destination arrival
unloading
```

Never trust:

```text
driver says 3 hours
```

as a financial fact.

---

# 88. DETENTION RULE VERSIONING

Persist the rule version used.

Historical detention must remain reproducible.

---

# 89. E-WAY BILL / COMPLIANCE

India-specific compliance integrations must be isolated behind interfaces.

Do not spread GST/e-way-bill logic across booking/trip code.

Example:

```text
ComplianceService
├── validate
├── generate
├── update
└── cancel
```

External regulatory integrations must be:

```text
auditable
retryable
idempotent
versioned
```

---

# 90. SEARCH

Do not introduce OpenSearch merely because it exists.

Start with PostgreSQL search where sufficient.

Introduce a search engine when:

```text
query complexity
scale
full-text requirements
operational latency
```

justify it.

Search indexes are derived data and must be rebuildable.

---

# 91. ANALYTICS

Separate operational transactions from heavy analytics.

Do not execute expensive analytical queries against primary transactional tables during peak operations.

Potential architecture:

```text
PostgreSQL
↓
events/CDC
↓
analytics storage
↓
BI/reporting
```

Introduce only when justified.

---

# 92. DATA PARTITIONING

Partition high-volume tables where measured workload requires it.

Likely candidates:

```text
GPS position history
audit events
event records
large notification tables
```

Partition by:

```text
time
entity
```

based on actual query/write patterns.

---

# 93. DATABASE INDEX STRATEGY

Every index must correspond to real query patterns.

Monitor:

```text
slow queries
index hit rate
unused indexes
write amplification
table bloat
```

Do not create dozens of speculative indexes.

---

# 94. CONNECTION POOLING

Use bounded connection pools.

Monitor:

```text
active connections
waiting connections
pool saturation
query duration
transaction duration
```

One runaway endpoint must not consume the entire database pool.

---

# 95. SLOW QUERY PROTECTION

Establish:

```text
statement timeout
transaction timeout
API timeout
worker timeout
```

according to operation class.

Never allow an accidental query to run indefinitely.

---

# 96. API PERFORMANCE TARGETS

At target load:

```text
Read API p50 < 100ms
Read API p95 < 200ms
Read API p99 < 300ms

Critical writes p95 < 500ms
Critical writes p99 < 800ms
```

Exact targets may be adjusted based on measured production requirements.

Async operations should return:

```text
202
```

where appropriate rather than holding HTTP connections unnecessarily.

---

# 97. GPS PERFORMANCE

Capacity must be based on:

```text
peak concurrent active trips
×
GPS frequency
×
expected retry/duplicate factor
×
headroom
```

Do not size infrastructure using daily average traffic.

---

# 98. HORIZONTAL SCALING

Define scaling signals for:

```text
API
workers
GPS ingestion
real-time processor
WebSocket gateway
queue consumers
```

Signals may include:

```text
CPU
memory
request rate
p95/p99 latency
queue depth
consumer lag
active WebSocket connections
```

---

# 99. WEBSOCKET ARCHITECTURE

WebSocket infrastructure must support:

```text
authentication
authorization
subscription management
connection limits
heartbeat
disconnect detection
reconnection
horizontal scaling
backpressure
```

Do not assume a single server process owns all subscriptions.

Use a shared pub/sub mechanism when horizontally scaling.

---

# 100. WEBSOCKET AUTHORIZATION

A user may subscribe only to entities they are authorized to view.

For example:

```text
shipper
→ own bookings/trips

driver
→ own active trip

ops
→ authorized operational scope
```

Never trust a client-supplied trip ID.

---

# 101. BACKPRESSURE

If event production exceeds client consumption:

```text
coalesce GPS updates
drop superseded intermediate location updates where safe
preserve critical domain events
```

Never drop:

```text
payment state
booking state
trip status transition
settlement state
```

merely because the stream is busy.

---

# 102. REAL-TIME CONSISTENCY

WebSocket events are acceleration, not the ultimate source of truth.

After reconnect:

```text
resynchronize authoritative state
```

Clients must be able to recover from missed events.

---

# 103. AUDIT SYSTEM

Audit all sensitive mutations:

```text
who
what
when
where
old value
new value
reason
request ID
source
```

Sources may include:

```text
mobile
web
admin
system
webhook
worker
integration
```

---

# 104. AUDIT IMMUTABILITY

Audit records should be append-only.

Normal application users must not be able to edit/delete audit history.

---

# 105. REQUEST CORRELATION

Every request gets:

```text
request_id
trace_id
```

Propagate through:

```text
API
services
database calls where observable
queue messages
workers
external calls
events
webhooks
```

---

# 106. STRUCTURED LOGGING

Logs must be JSON structured.

Include:

```text
timestamp
severity
service
environment
request_id
trace_id
actor_id where safe
operation
duration
error_code
```

Never include:

```text
password
OTP
access token
refresh token
card data
payment secrets
raw sensitive documents
```

---

# 107. DISTRIBUTED TRACING

Use:

```text
OpenTelemetry
```

Trace:

```text
gateway
→ API
→ service
→ database
→ queue
→ worker
→ external provider
```

Trace sampling must be configurable.

---

# 108. METRICS

Expose metrics for:

### API

```text
requests/sec
latency
p50/p95/p99
error rate
4xx
5xx
```

### Database

```text
connections
pool saturation
slow queries
locks
replication lag
```

### Queue

```text
depth
consumer lag
processing latency
failed jobs
DLQ size
```

### Tracking

```text
GPS ingestion rate
invalid GPS rate
processing latency
active trips
WebSocket connections
```

### Payments

```text
success rate
failure rate
pending count
webhook latency
reconciliation mismatch count
```

### Business

```text
bookings/day
trips/day
completion rate
cancellation rate
assignment success rate
```

---

# 109. SLOs

Define service-level objectives before launch.

Example:

```text
API availability ≥ 99.9%
Booking availability ≥ 99.95%
Payment webhook processing ≥ 99.99%
Real-time event processing within target latency ≥ 99%
```

Exact values must be agreed with the business.

---

# 110. ALERTING

Alerts must be actionable.

Page on-call for conditions such as:

```text
payment success collapse
booking error spike
database unavailable
queue lag beyond threshold
settlement failure
GPS ingestion outage
WebSocket infrastructure failure
critical security event
```

Avoid alerting on every minor error.

---

# 111. ERROR BUDGET

Track SLO error budgets.

When reliability degrades:

```text
feature velocity
```

may need to slow until reliability recovers.

---

# 112. HEALTH ENDPOINTS

Provide:

```text
/liveness
/readiness
```

and dependency-aware health checks.

Do not make liveness depend on every external service.

A temporarily unavailable payment provider should not necessarily cause the API process to be restarted.

---

# 113. GRACEFUL SHUTDOWN

Every service must support:

```text
SIGTERM
stop accepting new requests
finish in-flight safe work
stop consumers
commit/rollback transactions
close connections
exit
```

Workers must not lose jobs during deployment.

---

# 114. DEPLOYMENT

Containerize services.

Choose one cloud platform:

```text
AWS ECS/Fargate
OR
AWS EKS
OR
GCP Cloud Run/GKE
OR
Azure Container Apps/AKS
```

Do not prematurely support all clouds.

---

# 115. INFRASTRUCTURE AS CODE

Use:

```text
Terraform
```

or equivalent.

Everything required for production must be reproducible:

```text
network
database
cache
queues
storage
secrets
IAM
monitoring
DNS
certificates
compute
autoscaling
```

No critical production infrastructure should depend on undocumented console clicks.

---

# 116. ENVIRONMENTS

Maintain isolated:

```text
development
staging
production
```

Never share production credentials with staging.

Never allow staging workloads to accidentally target production.

---

# 117. CI PIPELINE

Every PR must run:

```text
format
lint
static analysis
unit tests
integration tests
schema validation
dependency scan
security scan
build
```

Required branches must pass all relevant gates.

---

# 118. CD PIPELINE

Deployment:

```text
commit
↓
CI
↓
artifact
↓
staging
↓
integration/E2E
↓
approval
↓
production
```

Production artifact must be the same immutable artifact tested in staging.

---

# 119. DEPLOYMENT STRATEGY

Critical components:

```text
booking
payment
ledger
settlement
```

require:

```text
canary
blue-green
or equivalent safe deployment
```

Do not deploy a financial change directly to 100% of traffic without a safety mechanism.

---

# 120. DATABASE MIGRATIONS

Migrations must be:

```text
forward compatible
backward compatible where required
automated
tested
observable
rollback-aware
```

Prefer:

```text
expand
→ migrate
→ contract
```

for zero/low-downtime schema changes.

---

# 121. DATA MIGRATION

Never perform large production migrations as one unbounded transaction.

Use:

```text
batching
progress tracking
retries
idempotency
verification
```

---

# 122. FEATURE FLAGS

Use feature flags for risky releases.

Flags may control:

```text
pricing engine version
payment provider
assignment algorithm
tracking implementation
notification provider
new API behavior
```

Backend must remain safe when flag configuration is unavailable.

---

# 123. CONFIGURATION

Separate:

```text
configuration
secrets
code
```

Configuration should support:

```text
environment
versioning
validation
safe defaults
audit
```

---

# 124. BACKUPS

PostgreSQL:

```text
automated backups
PITR
cross-AZ resilience
```

Object storage:

```text
versioning where appropriate
backup/replication strategy
```

Critical configuration:

```text
backup/export
```

---

# 125. DISASTER RECOVERY

Define:

```text
RTO
RPO
```

Example:

```text
RTO ≤ 1 hour
RPO ≤ 5 minutes
```

These are targets, not assumptions.

Business stakeholders must approve them.

---

# 126. RESTORE TEST

At least periodically:

```text
restore database
restore required objects
verify schema
verify data integrity
run application
run critical queries
execute critical workflow
```

An untested backup is not considered production-ready.

---

# 127. FAILURE SCENARIOS

Explicitly test:

```text
database failure
Redis failure
queue failure
worker crash
API process crash
WebSocket process crash
payment provider outage
SMS provider outage
object storage outage
network partition
DNS failure
deployment failure
credential rotation
```

---

# 128. CHAOS TESTING

Test:

```text
kill tracking worker
duplicate events
delay events
reorder events
kill payment worker
drop webhook responses
restart API instances
```

Expected:

```text
no double payment
no double settlement
no illegal state transition
no silent data loss
```

---

# 129. FINANCIAL CHAOS TEST

Simulate:

```text
payment request sent
→ response lost
→ retry

webhook duplicated

webhook delayed

worker crashes after ledger write

worker crashes before ledger write

settlement worker crashes after payout initiation
```

The final financial state must remain correct.

---

# 130. SECURITY TESTING

CI/release process must include:

```text
SAST
DAST
dependency scanning
container scanning
secret scanning
IaC scanning
API security tests
authorization tests
```

Perform periodic penetration testing before major production milestones.

---

# 131. THREAT MODELING

Threat-model at minimum:

```text
authentication
authorization
payment
GPS
documents
admin operations
webhooks
file uploads
API abuse
tenant/resource isolation
```

Document:

```text
threat
impact
likelihood
mitigation
residual risk
```

---

# 132. CONTAINER SECURITY

Production images must:

```text
run as non-root where possible
use minimal base images
pin dependencies
contain no secrets
be scanned
have SBOMs
```

Sign production artifacts where infrastructure supports it.

---

# 133. SUPPLY-CHAIN SECURITY

Track:

```text
dependencies
versions
licenses
vulnerabilities
build provenance
container images
```

Use automated dependency update tooling with controlled review.

---

# 134. API CONTRACT TESTING

Generate client contracts from OpenAPI.

CI must detect:

```text
breaking API changes
schema drift
event schema drift
GraphQL schema breaking changes
```

---

# 135. MODULE CONTRACT TESTING

Every module interface requires contract tests.

Example:

```text
pricing → booking
payment → booking
trip → tracking
trip → settlement
```

A contract change must fail dependent tests before merge.

---

# 136. INTEGRATION TESTING

Use real infrastructure for critical integration tests.

Especially:

```text
PostgreSQL
queue
Redis
object storage emulator where appropriate
payment sandbox
```

Do not mock every dependency in every test.

---

# 137. TEST DATABASE

Integration tests must use isolated databases/schema instances.

Tests must clean up deterministically.

Avoid test suites whose correctness depends on execution order.

---

# 138. E2E TESTING

Automate:

```text
signup
login
quote
booking
payment
assignment
trip
tracking
POD
delivery
invoice
settlement
```

Run against staging before production releases.

---

# 139. LOAD TESTING

Load-test:

```text
booking creation
quote generation
GPS ingestion
WebSocket fanout
admin GraphQL
payment webhook ingestion
settlement processing
```

Test at:

```text
launch scale
12-month scale
peak scale
failure scale
```

---

# 140. CAPACITY MODEL

Document calculations for:

```text
requests/sec
GPS messages/sec
queue throughput
DB writes/sec
DB storage growth
Redis memory
WebSocket connections
object storage growth
```

Include:

```text
average
p95
peak
burst
headroom
```

---

# 141. PERFORMANCE REGRESSION

Performance tests must have thresholds.

A build that introduces a significant regression should fail or require explicit approval.

---

# 142. DATA RETENTION

Define retention for:

```text
GPS
audit logs
events
webhooks
documents
notifications
application logs
financial records
```

Each retention period must have:

```text
business reason
legal basis
storage tier
deletion/archive mechanism
```

---

# 143. DATA ARCHIVAL

Move cold data to cheaper storage when appropriate.

Archived data must remain:

```text
discoverable
authorized
integrity-protected
recoverable
```

---

# 144. OBSERVABILITY OF BUSINESS FLOWS

Create traces/metrics that follow:

```text
booking
→ payment
→ assignment
→ trip
→ delivery
→ invoice
→ settlement
```

Operations should be able to answer:

> What happened to this booking?

from one correlation trail.

---

# 145. OPERATIONAL EXCEPTIONS

Build a first-class exception model.

Examples:

```text
payment mismatch
assignment conflict
GPS anomaly
POD upload failure
settlement mismatch
compliance failure
stuck trip
stale location
```

An exception must have:

```text
status
severity
owner
created_at
resolved_at
resolution
```

---

# 146. STUCK-WORK DETECTION

Create scheduled detection for abnormal states.

Examples:

```text
booking stuck awaiting payment
trip stuck at pickup
driver assignment pending too long
payment pending too long
settlement pending too long
POD missing
```

Create operational alerts rather than relying on humans to discover these manually.

---

# 147. DATA RECONCILIATION

Periodically reconcile:

```text
booking ↔ trip
trip ↔ assignment
trip ↔ POD
booking ↔ payment
payment ↔ ledger
ledger ↔ invoice
settlement ↔ payout
gateway ↔ internal records
```

Discrepancies become explicit exceptions.

---

# 148. SELF-HEALING

Where safe, automated reconciliation may repair:

```text
derived caches
missing read models
stale subscriptions
failed notifications
search indexes
```

Never automatically repair ambiguous financial records without controlled logic.

---

# 149. ADMIN OPERATIONS

Create safe administrative operations for:

```text
retry event
retry webhook
reprocess settlement
rebuild read model
inspect job
inspect payment
inspect trip
```

Every privileged action must be:

```text
authorized
confirmed
audited
idempotent
```

---

# 150. SUPPORTABILITY

Every customer-facing failure should be traceable using:

```text
request ID
booking ID
trip ID
payment ID
```

Support personnel should not need direct database access for routine investigation.

---

# 151. INTERNAL ADMIN API

Separate internal/admin permissions from customer APIs where appropriate.

Never expose operational endpoints merely by hiding them from navigation.

Authorization must exist at the endpoint/service layer.

---

# 152. DATA EXPORTS

Exports must be:

```text
authorized
bounded
audited
asynchronous for large datasets
```

Never generate a massive CSV synchronously inside an HTTP request.

---

# 153. REPORTING

Large reports should use:

```text
async job
→ generated artifact
→ signed download
```

rather than blocking API workers.

---

# 154. API PAGINATION

Use deterministic pagination.

Prefer cursor pagination for large/high-churn datasets.

Avoid:

```text
OFFSET 500000
```

on high-volume tables.

---

# 155. FILTERING

Filters must be:

```text
validated
indexed
bounded
authorization-aware
```

Never permit arbitrary SQL-like filters from clients.

---

# 156. SORTING

Only allow whitelisted sort fields.

Never pass arbitrary client field names directly into SQL.

---

# 157. MULTI-TENANCY READINESS

Even if the initial business operates as one company, structure authorization and data ownership so future support for:

```text
carrier
customer
organization
business account
```

does not require redesign.

Do not prematurely build full multi-tenant infrastructure if not needed.

---

# 158. ORGANIZATION SCOPING

Where applicable:

```text
user
→ organization
→ resources
```

must be enforced server-side.

No cross-organization access.

---

# 159. DRIVER/FLEET INTEGRITY

Maintain authoritative relationships:

```text
carrier
→ driver
→ vehicle
→ trip
```

Validate:

```text
driver eligibility
vehicle eligibility
document validity
assignment constraints
```

before assignment/start where required.

---

# 160. DRIVER DOCUMENT EXPIRATION

Support expiry tracking for:

```text
license
vehicle registration
insurance
permits
other required compliance documents
```

Create proactive operational alerts.

---

# 161. ASSIGNMENT ENGINE

Assignment must be backend-driven.

Potential inputs:

```text
availability
location
vehicle type
capacity
route
eligibility
carrier
operating area
driver status
```

The exact algorithm must be configurable/evolvable.

---

# 162. ASSIGNMENT CONSISTENCY

An active job must never have multiple authoritative drivers unless the domain explicitly supports multi-driver trips.

Enforce this with transactional constraints.

---

# 163. CANCELLATION

Cancellation must define:

```text
who can cancel
when
fees
refund
driver compensation
state transition
audit
notification
```

Never implement cancellation as:

```text
status = CANCELLED
```

alone.

---

# 164. RESCHEDULING

If supported:

```text
original booking
new schedule
pricing implications
driver assignment implications
audit
notifications
```

must be explicit.

---

# 165. NOTIFICATION CONSISTENCY

Critical notifications should be triggered from authoritative domain events.

Example:

```text
TripStarted
→ notification
```

not:

```text
mobile app pressed button
→ notification
```

The domain event confirms that the operation actually occurred.

---

# 166. EVENTUAL CONSISTENCY

Document which operations are:

```text
strongly consistent
eventually consistent
```

Examples:

```text
ledger posting
→ strong

search index
→ eventual

analytics
→ eventual

live GPS
→ eventual/near-real-time
```

Do not let users interpret eventual data as authoritative.

---

# 167. TRANSACTION BOUNDARIES

Keep transactions short.

Never perform:

```text
external HTTP call
```

inside a long-running database transaction.

Use:

```text
transaction
→ outbox
→ async external operation
```

where appropriate.

---

# 168. EXTERNAL API TIMEOUTS

Every external call must define:

```text
connect timeout
read timeout
overall timeout
retry policy
```

No infinite HTTP calls.

---

# 169. CIRCUIT BREAKERS

Use circuit breakers where repeated external failure could cascade into backend resource exhaustion.

Monitor:

```text
open
half-open
closed
```

states.

---

# 170. BACKPRESSURE

Queues should absorb bursts.

Consumers scale according to:

```text
queue depth
processing latency
```

rather than allowing API requests to synchronously absorb all downstream work.

---

# 171. DATA INTEGRITY CHECKS

Scheduled integrity jobs should detect:

```text
orphan records
invalid state combinations
unbalanced ledger transactions
missing events
duplicate assignments
missing POD
payment/ledger mismatches
```

---

# 172. PRODUCTION RUNBOOKS

Create runbooks for:

```text
API outage
database outage
Redis outage
queue outage
payment outage
GPS outage
WebSocket outage
notification outage
settlement mismatch
bad deployment
security incident
data corruption
backup restore
```

Each runbook must include:

```text
symptoms
diagnostics
mitigation
rollback
verification
communication
post-incident actions
```

---

# 173. INCIDENT MANAGEMENT

Define:

```text
severity levels
on-call ownership
escalation
incident commander
communications
postmortem process
```

Critical incidents require blameless postmortems.

---

# 174. ROLLBACK

Every production release must have a rollback strategy.

For database changes, prefer backward-compatible migrations so application rollback remains possible.

---

# 175. SECURITY INCIDENT RESPONSE

Have procedures for:

```text
credential leak
token compromise
payment compromise
PII exposure
malicious document
admin account compromise
API abuse
```

Include:

```text
revocation
containment
forensics
notification
rotation
recovery
```

---

# 176. LOG RETENTION

Logs must have:

```text
retention
access control
redaction
cost controls
```

Do not retain sensitive logs forever.

---

# 177. COST OBSERVABILITY

Track infrastructure cost drivers:

```text
database
GPS ingestion
WebSockets
queue
object storage
network egress
logging
observability
payment fees
```

Create alerts for unexpected cost spikes.

---

# 178. FINOPS

Production architecture should expose enough metrics to answer:

```text
cost per trip
cost per active driver
cost per GPS million events
cost per booking
cost per GB stored
```

where useful.

---

# 179. API DOCUMENTATION

OpenAPI must include:

```text
authentication
schemas
errors
examples
pagination
idempotency
rate limits
authorization
```

Do not document only happy paths.

---

# 180. DEVELOPER DOCUMENTATION

Provide:

```text
README
architecture
module contracts
local setup
environment variables
database setup
migration guide
API guide
event guide
testing guide
deployment guide
incident guide
```

---

# 181. ARCHITECTURE DECISION RECORDS

Document major decisions:

```text
modular monolith
PostgreSQL
Redis
queue technology
payment provider
ledger architecture
WebSocket architecture
cloud
IaC
authentication
event schema strategy
```

---

# 182. NO MAGIC NUMBERS

Centralize configuration for:

```text
timeouts
TTL
retry counts
GPS thresholds
rate limits
detention thresholds
payment expiration
session duration
```

---

# 183. SAFE DEFAULTS

If configuration is missing:

```text
fail closed for security
fail safe for money
fail visibly for operations
```

Never silently invent production behavior.

---

# 184. MIGRATION FROM MOCKS

If any prototype/mock implementation exists:

```text
identify
replace
test
remove
```

No mock business logic may accidentally remain active in production.

---

# 185. TESTING PYRAMID

Maintain:

```text
many unit tests
↓
integration tests
↓
contract tests
↓
fewer E2E tests
↓
targeted load/chaos tests
```

Tests must verify behavior, not merely code coverage.

---

# 186. CRITICAL INVARIANT TESTS

Automated tests must prove:

```text
one booking cannot become two
one payment cannot become two ledger entries
one payout cannot become two payouts
one job cannot have two winners
illegal trip transitions are rejected
unauthorized resources are inaccessible
duplicate events are harmless
duplicate webhooks are harmless
```

---

# 187. PROPERTY-BASED TESTING

Where useful, test invariants over many generated inputs.

Especially:

```text
pricing
ledger
state machines
idempotency
pagination
```

---

# 188. SECURITY REGRESSION TESTS

Maintain tests for previously discovered vulnerabilities.

Every security bug should produce:

```text
regression test
```

before being considered fixed.

---

# 189. LOAD TEST ACCEPTANCE

At minimum demonstrate target-scale behavior:

```text
5,000 trips/day
peak concurrent active trips
peak GPS rate
peak booking rate
admin query load
payment webhook burst
```

with agreed headroom.

---

# 190. FAILURE ACCEPTANCE

At target load, simulate:

```text
worker crash
database failover
queue consumer restart
Redis restart
duplicate events
provider outage
```

Expected:

```text
no financial corruption
no irreversible data loss
no illegal state transition
recoverable processing
observable failure
```

---

# 191. SECURITY ACCEPTANCE

Before production:

```text
no critical vulnerabilities
no committed secrets
authorization tests pass
webhook verification passes
file upload security passes
dependency scan acceptable
container scan acceptable
TLS verified
secret rotation tested
```

---

# 192. DATA RECOVERY ACCEPTANCE

Perform an actual:

```text
backup
→ destroy/isolated failure simulation
→ restore
→ verify
→ application startup
→ critical transaction test
```

Record evidence.

---

# 193. PAYMENT ACCEPTANCE

Demonstrate:

```text
successful payment
failed payment
pending payment
duplicate webhook
late webhook
invalid webhook
refund
partial refund
payment retry
worker crash
reconciliation mismatch
```

No scenario may create duplicate financial entries.

---

# 194. SETTLEMENT ACCEPTANCE

Demonstrate:

```text
normal settlement
settlement retry
worker crash
payout timeout
duplicate payout callback
reconciliation
manual correction
```

Final carrier balance must remain correct.

---

# 195. BOOKING ACCEPTANCE

Demonstrate:

```text
normal booking
duplicate request
expired quote
invalid quote
concurrent booking
payment timeout
booking cancellation
```

No duplicate booking or stale price usage.

---

# 196. TRACKING ACCEPTANCE

Demonstrate:

```text
GPS ingestion
duplicate GPS
out-of-order GPS
invalid coordinates
offline buffering
processor crash
Redis restart
WebSocket reconnect
```

Latest known position remains correct.

---

# 197. SECURITY/PRIVACY ACCEPTANCE

Verify:

```text
shipper A cannot access shipper B
driver A cannot access driver B's trip
carrier cannot access unauthorized carriers
normal admin cannot perform super-admin actions
expired tokens are rejected
revoked sessions stop working
documents cannot be downloaded without authorization
```

---

# 198. PRODUCTION READINESS GATE

Do not declare production-ready until:

```text
[ ] Architecture documented
[ ] Module boundaries documented
[ ] API contracts generated
[ ] Event schemas versioned
[ ] Idempotency implemented
[ ] Outbox implemented
[ ] Event deduplication implemented
[ ] Ledger implemented
[ ] Payment reconciliation implemented
[ ] Settlement rerun tested
[ ] RBAC implemented
[ ] Resource authorization tested
[ ] Security scanning complete
[ ] Secrets managed
[ ] Database backups enabled
[ ] Restore tested
[ ] Monitoring active
[ ] Alerting active
[ ] On-call defined
[ ] Runbooks written
[ ] Load testing complete
[ ] Chaos testing complete
[ ] E2E tests pass
[ ] CI/CD operational
[ ] Canary/rollback tested
[ ] Data retention defined
[ ] Privacy controls implemented
[ ] Production environment isolated
[ ] Critical financial invariants tested
```

---

# 199. FINAL PRODUCTION DEFINITION

The backend is production-ready only when it can survive:

```text
duplicate requests
duplicate events
duplicate webhooks
lost responses
delayed responses
out-of-order events
worker crashes
API crashes
database failover
Redis failure
queue failure
external provider outage
network instability
concurrent operations
malicious requests
expired sessions
invalid documents
GPS anomalies
deployment failures
```

while preserving:

```text
financial correctness
state-machine correctness
authorization correctness
auditability
data integrity
recoverability
observability
```

---

# 200. REQUIRED FINAL DELIVERABLE

At the end of implementation, produce the following evidence rather than simply claiming success.

## A. Architecture

Provide:

```text
system architecture diagram
module architecture
database architecture
event architecture
real-time architecture
deployment architecture
security architecture
```

## B. Repository

Provide the actual repository tree.

## C. API

Provide:

```text
OpenAPI specification
endpoint inventory
authentication model
authorization matrix
idempotency matrix
error-code catalog
```

## D. Events

Provide:

```text
event catalog
schema versions
producer
consumers
delivery guarantees
deduplication strategy
replay strategy
```

## E. Database

Provide:

```text
schema
migration strategy
index strategy
partition strategy
backup strategy
retention strategy
```

## F. Financial System

Provide:

```text
chart of accounts
ledger model
payment lifecycle
refund lifecycle
settlement lifecycle
reconciliation model
```

## G. Security

Provide:

```text
threat model
RBAC matrix
resource authorization model
secret-management design
security test results
```

## H. Reliability

Provide:

```text
SLOs
SLIs
alert thresholds
RTO
RPO
DR procedure
restore test evidence
```

## I. Testing

Provide:

```text
unit test results
integration test results
contract test results
E2E results
load-test results
chaos-test results
security-test results
```

## J. Deployment

Provide:

```text
CI pipeline
CD pipeline
infrastructure code
environment strategy
migration procedure
rollback procedure
release checklist
```

## K. Operations

Provide:

```text
runbooks
incident procedures
on-call model
reconciliation procedures
DLQ procedures
backup/restore procedures
```

## L. Known Limitations

Explicitly list anything not implemented.

Never claim:

```text
production-ready
secure
scalable
fault-tolerant
```

unless the corresponding behavior has actually been implemented and verified.

---

# 201. EXECUTION ORDER

Implement in this order:

```text
PHASE 01
Inspect repository and existing backend contracts

PHASE 02
Establish architecture and module boundaries

PHASE 03
Define domain model and invariants

PHASE 04
Establish PostgreSQL schema and migrations

PHASE 05
Implement identity/auth/RBAC

PHASE 06
Implement API foundation

PHASE 07
Implement idempotency infrastructure

PHASE 08
Implement booking

PHASE 09
Implement pricing

PHASE 10
Implement assignment/fleet

PHASE 11
Implement trip state machine

PHASE 12
Implement tracking/GPS ingestion

PHASE 13
Implement event/outbox infrastructure

PHASE 14
Implement WebSocket infrastructure

PHASE 15
Implement documents/POD

PHASE 16
Implement payment integration

PHASE 17
Implement ledger

PHASE 18
Implement invoicing

PHASE 19
Implement settlement

PHASE 20
Implement reconciliation

PHASE 21
Implement notifications

PHASE 22
Implement admin/operational APIs

PHASE 23
Implement observability

PHASE 24
Implement security hardening

PHASE 25
Implement backups/DR

PHASE 26
Implement CI/CD/IaC

PHASE 27
Run integration tests

PHASE 28
Run E2E tests

PHASE 29
Run load tests

PHASE 30
Run chaos tests

PHASE 31
Run security testing

PHASE 32
Execute restore test

PHASE 33
Execute financial reconciliation tests

PHASE 34
Execute production deployment rehearsal

PHASE 35
Production readiness review
```

For every phase:

```text
design
→ implement
→ test
→ observe
→ attack/fail it
→ fix
→ document
→ continue
```

Do not postpone reliability, security, observability, or testing until the end.

---

# 202. ABSOLUTE ENGINEERING RULE

The backend must be designed around this invariant:

> **Anything that can be retried will eventually be retried. Anything that can be duplicated will eventually be duplicated. Anything that can fail halfway through will eventually fail halfway through. Anything involving money will eventually encounter a timeout, duplicate callback, or partial failure.**

Therefore:

```text
Idempotency
+
Transactions
+
Constraints
+
Outbox
+
Inbox/deduplication
+
Immutable ledger
+
Reconciliation
+
Auditability
+
Observability
+
Recovery
```

are not optional enhancements.

They are part of the core architecture.

---

# 203. FINAL SUCCESS CONDITION

The finished backend must not merely process the happy path.

It must maintain correct system state when:

```text
Client sends request twice
+
Network drops
+
Worker crashes
+
Webhook arrives twice
+
Events arrive out of order
+
Database temporarily fails
+
Redis disappears
+
Payment provider times out
+
Driver goes offline
+
Two drivers accept simultaneously
+
Two admins edit simultaneously
+
A deployment occurs during active trips
```

The final authoritative state must remain:

**correct, recoverable, auditable, secure, observable, and financially consistent.**

The objective is not to build a backend that works when everything works.

The objective is to build a backend that **remains correct when everything does not work.**
