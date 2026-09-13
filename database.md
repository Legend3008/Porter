Yes. Since you want to **feed this to an AI coding agent**, the database specification should not just say *“create these tables.”* It needs to tell the agent:

* what the database is for
* what every entity means
* how entities relate
* what rules must never be violated
* how PostgreSQL should be structured
* how migrations should work
* how concurrency/idempotency should work
* what indexes and partitions are required
* what must never be stored
* how the database interacts with the backend modules
* how to test it
* how to verify it is production-ready

Below is a **copy-paste-ready master database implementation prompt** designed for your Porter-style logistics platform.

---

# MASTER AI AGENT PROMPT — PRODUCTION DATABASE ENGINEERING

```text
You are the Principal Database Architect and Senior PostgreSQL Engineer responsible for designing and implementing the production database for this logistics platform.

Do NOT treat this as a demo project.

The database will become the system of record for:

- customers/shippers
- users
- drivers
- carriers
- fleet
- vehicles
- ports
- terminals
- locations
- lanes
- containers
- shipments
- bookings
- quotes
- pricing
- trip assignments
- trips
- trip state
- GPS tracking
- documents
- payments
- invoices
- accounting
- settlements
- notifications
- audit logs
- integrations
- asynchronous events
- idempotency
- operational exceptions

The application is India-first and is intended to scale from approximately:

200 trips/day at launch

to approximately:

5,000 trips/day within 12 months.

The database must therefore be designed for correctness first, then scalability, maintainability, observability and operational safety.

==================================================
1. IMPORTANT: FIRST UNDERSTAND THE SYSTEM
==================================================

Before writing any migration or SQL:

1. Inspect the entire existing backend repository.
2. Inspect the frontend/mobile repositories if available.
3. Inspect API contracts.
4. Inspect existing models/entities.
5. Inspect existing migrations.
6. Inspect configuration files.
7. Inspect authentication/authorization logic.
8. Inspect payment integrations.
9. Inspect event definitions.
10. Inspect tracking/location ingestion.
11. Inspect booking/pricing logic.
12. Inspect settlement/accounting logic.
13. Identify what already exists.
14. Do NOT duplicate existing functionality.
15. Do NOT blindly overwrite existing migrations.
16. Do NOT invent relationships that contradict the existing application architecture.

Produce a database implementation plan BEFORE modifying the database.

The plan must identify:

- current database technology
- current ORM/query builder
- current migration system
- current tables
- current models
- existing relationships
- missing tables
- dangerous existing schema decisions
- required migrations
- indexes currently missing
- data integrity problems
- compatibility risks

If something is ambiguous, inspect the code and configuration first.

Do not guess when repository evidence exists.

==================================================
2. DATABASE TECHNOLOGY
==================================================

Use PostgreSQL as the authoritative transactional database.

PostgreSQL is the source of truth.

Use Redis only for ephemeral/cache/read-acceleration use cases.

Use object storage such as S3-compatible storage for large documents and media.

Do NOT store large binary documents directly in PostgreSQL unless there is a specific justified requirement.

The architecture should be:

Application
    |
    v
PostgreSQL
    |
    +---- Redis
    |
    +---- Object Storage
    |
    +---- Message Queue/Event Bus

PostgreSQL owns durable business state.

Redis must NEVER become the authoritative source for:

- bookings
- trips
- payments
- ledger entries
- settlements
- invoices
- customer balances
- driver earnings
- compliance records

==================================================
3. DATABASE DESIGN PHILOSOPHY
==================================================

Follow these principles:

1. Strong relational integrity.
2. Explicit foreign keys where appropriate.
3. Transactions for business-critical state changes.
4. Immutable financial records.
5. Append-only audit records.
6. Idempotent write operations.
7. Optimistic concurrency where appropriate.
8. No silent data corruption.
9. No application-generated balances as the source of truth.
10. No floating-point money.
11. UTC timestamps in PostgreSQL.
12. Database timestamps use TIMESTAMPTZ.
13. Explicit status transitions.
14. Soft deletion only where genuinely appropriate.
15. Avoid unnecessary polymorphic foreign keys.
16. Avoid giant generic tables.
17. Avoid JSONB for fields that should be relational.
18. JSONB is allowed for metadata and provider payloads.
19. Use database constraints wherever possible.
20. Keep module ownership explicit.

==================================================
4. DATABASE MODULE/SHEMA ORGANIZATION
==================================================

Organize tables into logical PostgreSQL schemas.

Recommended schemas:

identity
party
fleet
geo
shipment
booking
pricing
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
platform

The exact schema names may be adjusted if the existing repository already has conventions.

Each backend module owns its tables.

Example:

identity owns:
- users
- roles
- permissions
- sessions

fleet owns:
- drivers
- carriers
- vehicles
- vehicle_documents

shipment owns:
- shipments
- shipment_items
- shipment_containers

booking owns:
- bookings
- booking_status_history

pricing owns:
- pricing_rule_sets
- pricing_rule_versions
- quotes
- quote_components

operations owns:
- trips
- trip_stops
- trip_assignments
- trip_events
- job_offers

tracking owns:
- location_pings
- latest_locations

payments owns:
- payments
- payment_attempts
- payment_webhooks
- refunds

ledger owns:
- ledger_accounts
- ledger_transactions
- ledger_entries

settlement owns:
- settlements
- settlement_lines
- payouts

audit owns:
- audit_logs

integration owns:
- outbox_events
- inbox_events
- idempotency_keys

Do not allow application modules to directly query another module's tables.

Cross-module access should happen through service interfaces/repositories.

Database foreign keys may still exist where they materially protect integrity.

==================================================
5. IDENTIFIERS
==================================================

Use UUIDs for externally visible business entities.

Prefer UUIDv7 or another time-sortable UUID strategy where the application stack supports it.

Examples:

user_id
customer_id
driver_id
carrier_id
vehicle_id
shipment_id
booking_id
quote_id
trip_id
payment_id
invoice_id
settlement_id

For extremely high-volume append-only telemetry such as GPS pings, evaluate BIGINT identity keys for storage efficiency.

Never expose sequential database IDs publicly if they enable easy enumeration of business resources.

Public IDs must not reveal sensitive information.

==================================================
6. COMMON COLUMN STANDARD
==================================================

Where applicable, tables should use:

id
created_at
updated_at

Use:

TIMESTAMPTZ NOT NULL DEFAULT now()

For mutable records.

Do not blindly add updated_at to immutable tables.

For records representing actors, also capture:

created_by
updated_by

when operationally appropriate.

For important state changes capture:

actor_id
actor_type
source
occurred_at

Example source values:

USER
DRIVER_APP
SHIPPER_APP
OPS_DASHBOARD
SYSTEM
WEBHOOK
IMPORT
INTEGRATION

==================================================
7. TENANCY / ORGANIZATION MODEL
==================================================

Design the system so multi-tenant behavior is possible even if the initial deployment has a simple tenant model.

Create a party/organization abstraction.

A party may represent:

- customer
- carrier
- platform
- other business organization

Recommended:

party.organizations

Fields:

id
legal_name
display_name
party_type
status
created_at
updated_at

Possible party types:

CUSTOMER
CARRIER
PLATFORM
PARTNER

Do not create separate unrelated copies of organization identity.

Users should belong to organizations through membership tables.

Example:

identity.organization_memberships

Fields:

id
organization_id
user_id
role_id
status
created_at
updated_at

==================================================
8. IDENTITY DATABASE
==================================================

Create:

identity.users

Fields:

id
email
phone
password_hash
full_name
status
email_verified_at
phone_verified_at
last_login_at
created_at
updated_at

Never store plaintext passwords.

Use a strong password hashing algorithm in the application.

Normalize email/phone carefully.

Add appropriate unique indexes.

Do not assume phone numbers are globally unique if the product requirements do not guarantee that.

==================================================
9. ROLES AND PERMISSIONS
==================================================

Create:

identity.roles

identity.permissions

identity.role_permissions

identity.user_roles

Roles must support:

SHIPPER
DRIVER
CARRIER_ADMIN
OPS
SUPER_ADMIN

Do not rely solely on frontend role checks.

Authorization must be enforced by the backend.

Database design must support resource-level authorization.

Example:

A shipper should not be able to access another shipper's:

bookings
shipments
documents
payments
invoices

A driver should not be able to access another driver's private information.

==================================================
10. SESSION / REFRESH TOKEN STORAGE
==================================================

Create:

identity.refresh_tokens

Store:

id
user_id
token_hash
expires_at
revoked_at
created_at
last_used_at
device_id
ip_address
user_agent

Never store raw long-lived refresh tokens if avoidable.

Store hashes.

Support token revocation.

==================================================
11. PARTY / CUSTOMER DATA
==================================================

Create customer organization records.

Recommended:

party.organizations

party.organization_addresses

party.organization_contacts

party.tax_registrations

Tax registration data should support Indian business requirements without hardcoding assumptions into application logic.

Example fields:

tax_registration_number
registration_type
legal_name
state_code
status
verified_at

Do not use tax registration number as the primary key.

==================================================
12. FLEET DATABASE
==================================================

Create:

fleet.carriers

fleet.drivers

fleet.vehicles

fleet.vehicle_documents

fleet.driver_documents

fleet.driver_vehicle_assignments

A carrier can have many drivers.

A carrier can have many vehicles.

A driver can be associated with a carrier.

A vehicle can have historical driver assignments.

Do not overwrite assignment history.

Use effective dates.

Example:

driver_vehicle_assignments

id
driver_id
vehicle_id
assigned_from
assigned_until
status
created_at

Prevent overlapping active assignments when business rules require it.

==================================================
13. DRIVER TABLE
==================================================

Recommended fields:

id
carrier_id
user_id
driver_code
status
license_number_encrypted
license_expiry_date
verification_status
verified_at
created_at
updated_at

Sensitive identity fields should use appropriate application-level encryption/tokenization.

Do not store unnecessary sensitive information.

==================================================
14. VEHICLE TABLE
==================================================

Recommended:

id
carrier_id
registration_number
vehicle_type
capacity_kg
capacity_volume
status
manufacture_year
created_at
updated_at

Registration numbers should be normalized.

Use unique constraints where business rules require.

==================================================
15. GEO DATABASE
==================================================

Create:

geo.locations

geo.ports

geo.terminals

geo.lanes

geo.lane_stops

A location should support:

id
name
address_line
city
district
state
country
postal_code
latitude
longitude
location_type

Location types may include:

PORT
TERMINAL
WAREHOUSE
CUSTOMER_SITE
DEPOT
OTHER

Latitude/longitude should use an appropriate numeric/geospatial representation.

If PostGIS is available and justified, use PostGIS.

Otherwise use DECIMAL columns with validation.

==================================================
16. PORTS
==================================================

Create:

geo.ports

Fields:

id
code
name
location_id
status
metadata
created_at
updated_at

Ports must be data/configuration.

Do NOT hardcode ports into application business logic.

==================================================
17. LANES
==================================================

Create:

geo.lanes

Fields:

id
code
name
origin_location_id
destination_location_id
distance_km
estimated_duration_minutes
status
created_at
updated_at

A lane represents a configurable logistics corridor.

Example:

JNPT → Navi Mumbai

But this must be configuration.

Do not create code such as:

if origin == JNPT:
    price = ...

Pricing belongs in the pricing module.

==================================================
18. CONTAINER DATABASE
==================================================

Create:

shipment.containers

Fields:

id
container_number
container_type
iso_code
status
created_at
updated_at

Container number must have normalization and uniqueness rules.

Create:

shipment.container_events

Fields:

id
container_id
event_type
event_at
location_id
source
metadata
created_at

Do not overwrite important container history.

==================================================
19. SHIPMENT DATABASE
==================================================

A shipment represents the cargo/logistics requirement.

Create:

shipment.shipments

Fields:

id
customer_organization_id
reference_number
cargo_description
cargo_weight_kg
cargo_volume
cargo_value_minor
currency
status
special_instructions
created_at
updated_at

Money must use integer minor units.

Example:

₹1,250.50

must NOT be stored as:

1250.50 FLOAT

Use:

125050 paisa

with:

currency = INR

or an appropriate monetary representation.

==================================================
20. SHIPMENT CONTAINERS
==================================================

Create:

shipment.shipment_containers

Fields:

shipment_id
container_id
sequence
status
created_at

This allows:

one shipment → multiple containers

and preserves relational integrity.

==================================================
21. SHIPMENT STOPS
==================================================

Create:

shipment.shipment_stops

Fields:

id
shipment_id
sequence
stop_type
location_id
planned_arrival_at
planned_departure_at
created_at
updated_at

Stop types:

ORIGIN
DESTINATION
INTERMEDIATE

Use sequence instead of assuming only origin/destination.

==================================================
22. BOOKING DATABASE
==================================================

A booking represents a customer's commercial request/order for transportation.

Create:

booking.bookings

Fields:

id
booking_number
customer_organization_id
shipment_id
quote_id
status
requested_at
confirmed_at
cancelled_at
cancellation_reason
created_by
created_at
updated_at

Booking numbers can be human-readable.

Example:

BK-2026-000123

But the UUID remains the actual primary key.

==================================================
23. BOOKING STATUS
==================================================

Define explicit states.

Example:

DRAFT
REQUESTED
QUOTED
CONFIRMED
ASSIGNED
IN_PROGRESS
COMPLETED
CANCELLED
FAILED

Do not let arbitrary strings enter the database.

Use either:

- lookup tables
- constrained text
- carefully managed PostgreSQL enums

Prefer lookup/config tables when statuses may evolve frequently.

==================================================
24. BOOKING STATUS HISTORY
==================================================

Create:

booking.booking_status_history

Fields:

id
booking_id
from_status
to_status
changed_by
actor_type
source
reason
occurred_at
created_at

Never lose historical state transitions.

Current state lives on bookings.status.

History lives separately.

==================================================
25. PRICING ENGINE DATABASE
==================================================

Pricing must be configuration-driven.

Create:

pricing.pricing_rule_sets

pricing.pricing_rule_versions

pricing.pricing_rules

pricing.surcharges

pricing.quotes

pricing.quote_components

Pricing must support:

base freight
distance
vehicle type
lane
container type
weight
time
toll
fuel surcharge
port surcharge
detention
waiting
GST/tax
discount
other configured charges

Do not hardcode commercial pricing into SQL or application branches.

==================================================
26. PRICING VERSIONING
==================================================

Every quote must identify the pricing version used.

Example:

quote_id
pricing_rule_version_id

A historical quote must remain reproducible.

If pricing changes tomorrow, yesterday's booking must not silently change.

Never recalculate a historical booking using the current pricing rules.

==================================================
27. QUOTES
==================================================

Create:

pricing.quotes

Fields:

id
quote_number
customer_organization_id
shipment_id
lane_id
pricing_rule_version_id
subtotal_minor
tax_minor
discount_minor
total_minor
currency
status
expires_at
created_at

Quote states:

DRAFT
ACTIVE
EXPIRED
ACCEPTED
CANCELLED

The backend must verify quote ownership and expiry before booking.

==================================================
28. QUOTE COMPONENTS
==================================================

Create:

pricing.quote_components

Fields:

id
quote_id
component_type
description
quantity
unit_price_minor
amount_minor
tax_code
metadata
created_at

Examples:

BASE_FREIGHT
FUEL_SURCHARGE
TOLL
PORT_FEE
DETENTION
GST
DISCOUNT

This makes quotes explainable.

==================================================
29. TRIP DATABASE
==================================================

A trip is the operational execution of a booking.

Create:

operations.trips

Fields:

id
trip_number
booking_id
carrier_id
driver_id
vehicle_id
status
scheduled_start_at
actual_start_at
actual_end_at
current_stop_sequence
version
created_at
updated_at

The trip must reference the booking.

Do not duplicate customer/payment truth unnecessarily.

==================================================
30. TRIP STATE MACHINE
==================================================

Implement a strict state machine.

Possible example:

CREATED
ASSIGNMENT_PENDING
ASSIGNED
DRIVER_ACCEPTED
EN_ROUTE_TO_ORIGIN
AT_ORIGIN
LOADING
LOADED
EN_ROUTE
AT_DESTINATION
UNLOADING
COMPLETED
CANCELLED
FAILED

Allowed transitions must be explicit.

Example:

CREATED
    -> ASSIGNMENT_PENDING

ASSIGNMENT_PENDING
    -> ASSIGNED

ASSIGNED
    -> DRIVER_ACCEPTED
    -> CANCELLED

DRIVER_ACCEPTED
    -> EN_ROUTE_TO_ORIGIN

etc.

Do NOT allow arbitrary:

UPDATE trips SET status = 'COMPLETED'

from any state.

The application must use a state transition service.

==================================================
31. TRIP STATUS HISTORY
==================================================

Create:

operations.trip_status_history

Fields:

id
trip_id
from_status
to_status
actor_id
actor_type
source
reason
occurred_at
created_at

This is essential for:

- operations
- debugging
- SLA analysis
- detention
- dispute resolution
- analytics
- audit

==================================================
32. TRIP STOPS
==================================================

Create:

operations.trip_stops

Fields:

id
trip_id
sequence
stop_type
location_id
planned_arrival_at
planned_departure_at
actual_arrival_at
actual_departure_at
status
created_at
updated_at

This supports multi-stop trips.

==================================================
33. DRIVER ASSIGNMENTS
==================================================

Create:

operations.trip_assignments

Fields:

id
trip_id
driver_id
vehicle_id
carrier_id
assignment_status
assigned_at
accepted_at
rejected_at
completed_at
created_at
updated_at

Maintain assignment history.

Do not simply overwrite driver_id when a reassignment occurs.

The current driver may be available on the trip record for fast reads, while assignment history remains authoritative for history.

==================================================
34. JOB OFFERS
==================================================

Create:

operations.job_offers

Fields:

id
trip_id
driver_id
offered_at
expires_at
status
responded_at
rejection_reason
created_at

Statuses:

OFFERED
ACCEPTED
REJECTED
EXPIRED
CANCELLED

Use database constraints/transactions to prevent multiple drivers from becoming the accepted assignment.

==================================================
35. CONCURRENCY CONTROL
==================================================

This is critical.

Two drivers may attempt to accept the same trip simultaneously.

The system must prevent:

Driver A → ACCEPTED

and

Driver B → ACCEPTED

for the same trip.

Use a transaction with appropriate locking or optimistic concurrency.

Possible approach:

BEGIN;

SELECT trip
FROM operations.trips
WHERE id = ?
FOR UPDATE;

verify current status

verify offer

verify driver eligibility

create assignment

update trip

COMMIT;

Use row locking only around the critical section.

Do not hold locks while calling external APIs.

==================================================
36. GPS TRACKING DATABASE
==================================================

GPS data is high-volume.

Do NOT put GPS history into the main trips table.

Create:

tracking.location_pings

Fields:

id
trip_id
driver_id
vehicle_id
latitude
longitude
accuracy_meters
speed_mps
heading_degrees
device_recorded_at
server_received_at
source
sequence_number
battery_percent
metadata

Important distinction:

device_recorded_at
=
when the device says the location occurred

server_received_at
=
when our server received it

Never replace the device timestamp with server time.

==================================================
37. GPS VALIDATION
==================================================

Validate:

latitude between -90 and +90

longitude between -180 and +180

accuracy within sane limits

speed within sane limits

timestamp not absurdly far in the future

timestamp not outside acceptable historical window

sequence numbers where available

Reject or quarantine obviously corrupt points.

Never silently accept impossible coordinates.

==================================================
38. GPS PARTITIONING
==================================================

Partition:

tracking.location_pings

by RANGE on server_received_at or another carefully selected time column.

Recommended initial strategy:

monthly partitions.

Example:

location_pings_2026_09
location_pings_2026_10
location_pings_2026_11

Evaluate partition size based on real ingestion volume.

Create indexes per partition:

(trip_id, device_recorded_at DESC)

(driver_id, device_recorded_at DESC)

server_received_at

Do not create unnecessary indexes because GPS tables can become enormous.

==================================================
39. LATEST LOCATION
==================================================

Do NOT query the entire GPS history every time the dashboard wants the driver's latest position.

Create:

tracking.latest_locations

Fields:

trip_id
driver_id
vehicle_id
latitude
longitude
accuracy_meters
speed_mps
heading_degrees
device_recorded_at
server_received_at
sequence_number
updated_at

Use Redis as an acceleration layer.

PostgreSQL remains durable source for the latest location projection if the product requires recovery.

==================================================
40. LOCATION RETENTION
==================================================

GPS history must have an explicit retention policy.

Define:

hot retention
archive retention
deletion policy

Do not allow GPS history to grow indefinitely without a lifecycle strategy.

Old telemetry may be:

- compressed
- archived
- aggregated
- deleted according to policy

Do not delete operational/legal data without an approved retention policy.

==================================================
41. GATE EVENTS
==================================================

Detention and operational timing depend on reliable events.

Create:

operations.gate_events

Fields:

id
trip_id
stop_id
event_type
event_at
source
actor_id
metadata
created_at

Examples:

GATE_IN
GATE_OUT
LOADING_START
LOADING_END
UNLOADING_START
UNLOADING_END

Do not derive important operational timestamps from frontend assumptions.

The backend is authoritative.

==================================================
42. DETENTION
==================================================

Detention must be calculated server-side.

Store the underlying facts:

arrival
gate-in
loading start
loading end
gate-out
etc.

Then calculate detention according to the active pricing/business rule version.

Never allow the mobile client to directly set:

detention_amount = 5000

without server validation.

==================================================
43. DOCUMENT DATABASE
==================================================

Documents themselves belong in object storage.

PostgreSQL stores metadata.

Create:

documents.documents

Fields:

id
owner_type
owner_id
document_type
status
storage_key
file_name
mime_type
file_size_bytes
checksum
uploaded_by
created_at
updated_at

However, avoid uncontrolled polymorphic references.

For critical document relationships, prefer explicit link tables.

==================================================
44. DOCUMENT VERSIONS
==================================================

Create:

documents.document_versions

Fields:

id
document_id
version_number
storage_key
checksum
file_size_bytes
uploaded_by
created_at

Never silently overwrite important compliance documents.

Maintain versions.

==================================================
45. POD
==================================================

Proof of Delivery is operationally important.

Support:

pod document
captured_at
captured_by
signature metadata
receiver name
receiver contact if necessary
GPS coordinates if captured
device metadata
checksum
verification status

POD must be tamper-evident.

Do not trust only a client-side timestamp.

==================================================
46. PAYMENTS
==================================================

Create:

payments.payments

payments.payment_attempts

payments.payment_webhooks

payments.refunds

A payment represents the commercial payment intent/result.

An attempt represents a particular gateway attempt.

A webhook represents an external provider event.

Do not merge all of these into one table.

==================================================
47. PAYMENT TABLE
==================================================

Recommended:

payments.payments

id
booking_id
customer_organization_id
amount_minor
currency
status
provider
provider_payment_id
created_at
updated_at
completed_at

Statuses:

CREATED
PENDING
AUTHORIZED
CAPTURED
FAILED
CANCELLED
REFUNDED
PARTIALLY_REFUNDED

Do not assume frontend success means payment success.

==================================================
48. PAYMENT ATTEMPTS
==================================================

Create:

payments.payment_attempts

id
payment_id
provider
provider_order_id
provider_payment_id
amount_minor
status
failure_code
failure_message
attempted_at
created_at

A payment may have multiple attempts.

==================================================
49. PAYMENT WEBHOOKS
==================================================

Create:

payments.payment_webhooks

Fields:

id
provider
provider_event_id
event_type
signature_verified
payload_encrypted_or_protected
received_at
processed_at
processing_status
failure_reason

Create a unique constraint on:

provider + provider_event_id

This prevents duplicate webhook processing.

==================================================
50. PAYMENT SECURITY
==================================================

Never store:

CVV
raw card number
bank credentials
payment passwords

Use gateway-hosted/tokenized flows.

Verify webhook signatures.

Do not trust payment status sent by mobile clients.

==================================================
51. INVOICES
==================================================

Create:

billing.invoices

billing.invoice_lines

Fields should support:

invoice_number
organization_id
booking_id
status
currency
subtotal_minor
tax_minor
total_minor
issued_at
due_at
paid_at
cancelled_at

Invoice numbers must be unique.

Invoice history must be preserved.

Do not modify issued invoices casually.

Use credit/debit note mechanisms for corrections where required by business/accounting rules.

==================================================
52. MONEY MODEL
==================================================

This rule is NON-NEGOTIABLE.

Never use:

FLOAT
DOUBLE PRECISION

for monetary values.

Use:

BIGINT minor units

or

NUMERIC with explicit precision

depending on accounting requirements.

For INR:

1 rupee = 100 paise

Example:

₹100.50

stored as:

10050

currency:

INR

All financial tables must carry currency where ambiguity is possible.

==================================================
53. LEDGER
==================================================

The ledger is the financial source of truth.

Create:

ledger.ledger_accounts

ledger.ledger_transactions

ledger.ledger_entries

This must be double-entry accounting.

Every transaction must balance.

Example:

Customer pays ₹10,000.

Debit:
Cash/Clearing ₹10,000

Credit:
Customer Receivable ₹10,000

Never simply update:

customer.balance = customer.balance + 10000

as the financial source of truth.

Balances are derived from ledger entries.

==================================================
54. LEDGER ACCOUNTS
==================================================

Account types:

ASSET
LIABILITY
EQUITY
REVENUE
EXPENSE
RECEIVABLE
PAYABLE
CLEARING
TAX

Example accounts:

Platform Cash
Payment Gateway Clearing
Customer Receivables
Carrier Payables
Freight Revenue
Tax Payable
Refund Liability

==================================================
55. LEDGER TRANSACTIONS
==================================================

Create:

ledger.ledger_transactions

Fields:

id
transaction_number
transaction_type
reference_type
reference_id
effective_at
posted_at
description
status
created_at

A ledger transaction is immutable once posted.

Corrections must be represented by reversing/correcting transactions.

Never edit historical posted ledger entries.

==================================================
56. LEDGER ENTRIES
==================================================

Create:

ledger.ledger_entries

Fields:

id
transaction_id
account_id
direction
amount_minor
currency
metadata
created_at

Direction:

DEBIT
CREDIT

Each transaction must satisfy:

SUM(DEBITS) = SUM(CREDITS)

Implement validation in the application transaction and, where practical, database-level protection.

==================================================
57. LEDGER IMMUTABILITY
==================================================

Once a financial transaction is POSTED:

DO NOT UPDATE IT.

DO NOT DELETE IT.

To correct it:

create a reversal transaction

then create the corrected transaction.

This preserves financial history.

==================================================
58. SETTLEMENTS
==================================================

Create:

settlement.settlements

settlement.settlement_lines

settlement.payouts

A settlement represents money owed to a carrier/driver.

Settlement lines should identify the source trip/earning.

Fields may include:

trip_id
gross_earnings_minor
deductions_minor
tax_minor
adjustment_minor
net_amount_minor

But ledger remains authoritative for financial accounting.

==================================================
59. SETTLEMENT RERUN SAFETY
==================================================

Settlement generation must be safely rerunnable.

If a settlement job runs twice:

it must NOT pay the carrier twice.

Use:

idempotency keys
unique business references
database constraints
transaction boundaries

Do not rely only on application checks such as:

if not exists:

because two workers can race.

==================================================
60. PAYOUTS
==================================================

Create:

settlement.payouts

Fields:

id
settlement_id
provider
provider_payout_id
amount_minor
currency
status
initiated_at
completed_at
failure_reason
created_at
updated_at

Provider payout IDs must be unique where applicable.

==================================================
61. RECONCILIATION
==================================================

Create reconciliation support for:

payments
payouts
ledger
settlements

Possible:

settlement.reconciliation_runs

settlement.reconciliation_items

The system should detect:

payment exists but ledger missing
ledger exists but payment missing
settlement marked paid but provider failed
provider says success but internal state pending
duplicate payment
duplicate payout

Never hide financial mismatches.

==================================================
62. NOTIFICATIONS
==================================================

Create:

notification.notification_preferences

notification.notifications

notification.delivery_attempts

Support channels:

PUSH
SMS
EMAIL
WHATSAPP if later integrated

Notifications should have:

status
attempt count
provider
provider message ID
sent_at
failed_at
failure_reason

Do not make notification delivery part of the booking transaction.

Booking succeeds even if notification provider temporarily fails.

Queue notification delivery asynchronously.

==================================================
63. AUDIT LOG
==================================================

Create:

audit.audit_logs

This is append-only.

Fields:

id
actor_user_id
actor_type
action
entity_type
entity_id
request_id
ip_address
user_agent
before_data
after_data
metadata
created_at

Audit logs should record sensitive administrative actions such as:

booking modification
trip override
payment adjustment
refund
settlement adjustment
role changes
driver verification
document changes
manual status override

Do not store unnecessary secrets in audit payloads.

==================================================
64. IDEMPOTENCY
==================================================

Create:

integration.idempotency_keys

Fields:

id
idempotency_key
actor_id
operation
request_hash
status
response_status
response_body
created_at
expires_at
completed_at

Unique identity should include the appropriate actor/context.

Example:

actor_id + operation + idempotency_key

This is critical for mobile networks.

Example:

Driver presses "Accept Trip".

Request succeeds.

Mobile network times out.

Driver presses again.

The backend must return the previous result instead of creating a second assignment.

==================================================
65. IDEMPOTENCY RULES
==================================================

Every state-changing mobile/API operation that may be retried must support idempotency.

Especially:

create booking
confirm booking
accept trip
reject trip
start trip
arrive
complete trip
upload POD metadata
create payment
refund
submit settlement
etc.

Idempotency must be implemented at the database transaction boundary.

==================================================
66. OUTBOX EVENTS
==================================================

Create:

integration.outbox_events

Fields:

id
event_id
event_type
event_version
aggregate_type
aggregate_id
payload
occurred_at
published_at
attempt_count
status
last_error

The transactional outbox solves:

database transaction succeeds
but event publishing fails.

Business transaction and outbox insert happen in the same database transaction.

A worker publishes the event later.

==================================================
67. INBOX / EVENT DEDUPLICATION
==================================================

Create:

integration.inbox_events

Fields:

event_id
consumer
received_at
processed_at
status
failure_reason

Unique:

event_id + consumer

Consumers must be idempotent.

The system uses at-least-once event delivery.

Never assume exactly-once delivery.

==================================================
68. EVENT VERSIONING
==================================================

Events must be versioned.

Example:

trip.completed.v1
trip.completed.v2

Never silently change the meaning of an existing event.

Old consumers must remain compatible during migration.

==================================================
69. EXTERNAL INTEGRATIONS
==================================================

Where external provider identifiers are required, store them explicitly.

Examples:

payment provider IDs
SMS provider IDs
mapping provider IDs
compliance provider IDs

Never assume external IDs are the same as internal IDs.

Use unique constraints where provider guarantees uniqueness.

==================================================
70. COMPLIANCE DATA
==================================================

The system is India-first.

Design compliance data to support requirements such as:

tax registration
GST-related information
e-way bill-related information
transport/commercial documents
invoice references
vehicle documentation

Do not hardcode legal rules into database structure unless absolutely necessary.

Prefer versioned configuration and service-layer rules.

==================================================
71. DATABASE CONSTRAINTS
==================================================

Use constraints aggressively.

Examples:

NOT NULL
CHECK
UNIQUE
FOREIGN KEY

Examples:

latitude >= -90
latitude <= 90

longitude >= -180
longitude <= 180

amount_minor >= 0

capacity_kg > 0

expires_at > created_at

Do not rely entirely on application validation.

The database is the final integrity boundary.

==================================================
72. FOREIGN KEYS
==================================================

Use foreign keys for important relationships.

Examples:

booking.shipment_id
trip.booking_id
trip.driver_id
trip.vehicle_id
payment.booking_id
settlement.settlement_id
ledger_entry.transaction_id

Use ON DELETE behavior deliberately.

Do NOT use:

ON DELETE CASCADE

blindly on business-critical financial or operational data.

For most important business records:

RESTRICT

or controlled application-level deletion is safer.

==================================================
73. DELETE POLICY
==================================================

Financial records:

NEVER DELETE.

Audit records:

NEVER DELETE except through formally approved retention mechanisms.

Completed trips:

normally retain.

Payments:

retain.

Ledger:

retain.

Documents:

retain according to compliance policy.

Users:

use status/deactivation rather than destructive deletion when history must remain.

Do not implement "delete everything" casually.

==================================================
74. INDEXING
==================================================

Every index must support an actual query pattern.

Do not create indexes on every column.

Important indexes include:

users(email)
users(phone)

organization_memberships(user_id)
organization_memberships(organization_id)

bookings(customer_organization_id, created_at DESC)
bookings(status, created_at DESC)

trips(status, scheduled_start_at)
trips(driver_id, status)
trips(carrier_id, status)

trip_assignments(driver_id, status)

quotes(customer_organization_id, created_at DESC)
quotes(expires_at)

payments(booking_id)
payments(provider, provider_payment_id)
payments(status, created_at)

settlements(carrier_id, status, created_at)

ledger_entries(account_id, created_at)

audit_logs(entity_type, entity_id, created_at DESC)

outbox_events(status, created_at)

==================================================
75. PARTIAL INDEXES
==================================================

Use partial indexes for active records where beneficial.

Examples:

active trip assignments

active job offers

pending payments

unpublished outbox events

Do not index historical rows unnecessarily.

==================================================
76. UNIQUE CONSTRAINTS
==================================================

Examples:

booking_number
invoice_number
trip_number
quote_number
container_number
provider + provider_event_id
provider + provider_payment_id
idempotency actor + operation + key
event_id + consumer

Use database uniqueness.

Never rely only on:

SELECT first
then INSERT

because of race conditions.

==================================================
77. CONCURRENCY
==================================================

Identify all operations that can race.

Examples:

two drivers accepting a trip
two workers processing a webhook
two workers creating a settlement
two payment retries
two admins changing a booking
two systems updating trip state

Use:

transactions
SELECT FOR UPDATE
unique constraints
optimistic locking
version columns

where appropriate.

==================================================
78. OPTIMISTIC LOCKING
==================================================

For frequently updated operational records such as trips:

include:

version BIGINT NOT NULL DEFAULT 0

Update pattern:

UPDATE trips
SET
    status = ?,
    version = version + 1
WHERE
    id = ?
    AND version = ?

If affected rows = 0:

the record was concurrently modified.

Reload and handle appropriately.

==================================================
79. TRANSACTIONS
==================================================

Transactions must be short.

Good:

BEGIN
lock required rows
validate state
modify records
insert outbox event
COMMIT

Bad:

BEGIN
call payment provider
wait 10 seconds
call external API
wait
COMMIT

Never hold database locks while waiting for external services.

==================================================
80. JSONB POLICY
==================================================

JSONB is allowed for:

provider webhook payloads
metadata
integration-specific attributes
unstructured device metadata

Do NOT use JSONB for core relational fields such as:

customer_id
booking_id
trip_id
status
amount
currency
driver_id

If the application frequently filters/sorts/joins on a field, it should usually be a real column.

==================================================
81. DATABASE SECURITY
==================================================

Use:

TLS
encryption at rest
managed secrets
least-privilege DB roles

Create separate database users/roles where practical:

application_read_write
migration_admin
readonly_reporting

The runtime application should NOT have unrestricted database administration privileges.

==================================================
82. MIGRATIONS
==================================================

Use a proper migration system compatible with the existing backend stack.

Every schema change must be represented by a migration.

Never manually modify production schema without a migration.

Migrations must be:

- versioned
- reviewable
- reversible where practical
- tested
- idempotent where appropriate
- safe for deployment

==================================================
83. ZERO-DOWNTIME MIGRATIONS
==================================================

For production:

DO NOT perform dangerous migrations in one step.

For example, when adding a required column:

Step 1:
add nullable column

Step 2:
deploy code that writes it

Step 3:
backfill existing data

Step 4:
verify

Step 5:
add NOT NULL constraint

Use expand/contract migration patterns.

==================================================
84. LARGE TABLE MIGRATIONS
==================================================

Do not perform massive blocking operations during peak traffic.

For large tables:

- batch backfills
- monitor locks
- create indexes concurrently where supported
- partition carefully
- avoid table rewrites where possible

==================================================
85. CONNECTION POOLING
==================================================

Use connection pooling.

Set:

maximum connections
minimum idle connections
connection timeout
idle timeout
statement timeout

Do not allow one traffic spike to exhaust PostgreSQL connections.

==================================================
86. QUERY PERFORMANCE
==================================================

Set sensible statement timeouts.

Identify slow queries.

Monitor:

p50
p95
p99

for database queries.

Investigate:

sequential scans
large joins
bad cardinality estimates
missing indexes
connection waits
lock contention

Never solve a slow query by blindly adding indexes.

Use EXPLAIN ANALYZE.

==================================================
87. DATABASE OBSERVABILITY
==================================================

Monitor:

CPU
memory
disk
IOPS
connections
connection pool usage
transaction rate
locks
deadlocks
slow queries
replication lag
cache hit ratio
table growth
index growth
autovacuum health
bloat

Also monitor business metrics:

active trips
pending bookings
payment failures
unprocessed webhooks
outbox lag
settlement backlog

==================================================
88. AUTOVACUUM
==================================================

Tune autovacuum for high-write tables.

Especially:

location_pings
outbox_events
inbox_events
audit_logs
job_offers

Do not assume PostgreSQL defaults are optimal for every workload.

==================================================
89. TABLE PARTITIONING
==================================================

Do not partition everything.

Initially partition only tables where size/write rate/retention justifies it.

Likely candidates:

tracking.location_pings

potentially:

audit.audit_logs

integration.outbox_events

if real-world volume requires it.

Document why each table is partitioned.

==================================================
90. BACKUPS
==================================================

Production PostgreSQL must have:

automated backups
point-in-time recovery
retention policy
multi-zone or equivalent durability
restore testing

A backup that has never been restored is not considered verified.

==================================================
91. DISASTER RECOVERY
==================================================

Define:

RPO
RTO

Document:

database failure
region failure
corruption
accidental deletion
bad migration

Create and test restore procedures.

==================================================
92. DATABASE SEEDING
==================================================

Create separate seed categories.

Development seed:

fake users
fake drivers
fake vehicles
fake lanes
fake pricing
fake bookings

Production seed:

only controlled reference/configuration data.

NEVER run fake development data against production.

==================================================
93. REFERENCE DATA
==================================================

Reference/configuration tables may include:

vehicle_types
container_types
document_types
trip_status definitions
booking_status definitions
payment providers
currencies
tax codes
lane configurations
pricing configurations

Reference data should be version-controlled where appropriate.

==================================================
94. NO HARDCODED BUSINESS CONFIGURATION
==================================================

Do not hardcode:

port names
lane prices
vehicle prices
fuel surcharge
waiting charge
detention rate
tax configuration

unless it is a true immutable system constant.

Business configuration should be data-driven.

==================================================
95. DATA CONSISTENCY
==================================================

For every critical operation define:

authoritative record
derived records
events
side effects

Example booking confirmation:

AUTHORITATIVE:
booking

DERIVED:
trip creation

EVENT:
booking.confirmed

SIDE EFFECT:
notification

Do not make notification success part of booking transaction success.

==================================================
96. DATABASE STATE VS CACHE
==================================================

For every field ask:

Is this authoritative?

If yes:
PostgreSQL.

If derived:
can be rebuilt.

If ephemeral:
Redis.

Never make a cache impossible to rebuild.

==================================================
97. REPORTING
==================================================

Do not run heavy analytics queries against primary transactional tables during peak traffic.

Prepare for:

read replicas
reporting database
warehouse
materialized views
ETL/event streams

as traffic increases.

Operational dashboard queries must be carefully optimized.

==================================================
98. ADMIN QUERY SAFETY
==================================================

Admin dashboards can generate expensive queries.

Use:

pagination
cursor pagination
filters
date ranges
indexed fields
query limits

Never allow an admin API to accidentally request millions of rows.

==================================================
99. PAGINATION
==================================================

For large datasets prefer cursor/keyset pagination.

Avoid:

OFFSET 500000

for high-volume tables.

Use stable ordering such as:

created_at DESC, id DESC

and a cursor.

==================================================
100. DATA RETENTION
==================================================

Create a documented retention matrix.

For each data class specify:

hot retention
archive retention
deletion rules
legal hold behavior

Examples:

GPS telemetry
audit logs
payment webhooks
documents
financial ledger
notifications

Financial records generally require much longer retention than temporary operational data.

Do not invent legal retention periods.

Make them configurable and documented.

==================================================
101. PRIVACY
==================================================

Minimize PII.

Encrypt sensitive values where required.

Never log:

passwords
access tokens
refresh tokens
payment credentials
sensitive document contents

Audit access to sensitive resources.

==================================================
102. REQUEST TRACEABILITY
==================================================

Important records should be traceable through:

request_id
actor_id
source
created_at

Example:

API request
    ↓
booking transaction
    ↓
outbox event
    ↓
notification
    ↓
audit event

A production engineer should be able to trace what happened.

==================================================
103. EXCEPTION MODEL
==================================================

Create:

operations.exceptions

Fields:

id
entity_type
entity_id
exception_type
severity
status
description
detected_at
assigned_to
resolved_at
resolution
created_at
updated_at

Examples:

DRIVER_NO_SHOW
GPS_STALE
PAYMENT_MISMATCH
SETTLEMENT_MISMATCH
STUCK_TRIP
DOCUMENT_MISSING
WEBHOOK_FAILURE

This allows Ops to manage failures explicitly.

==================================================
104. STUCK WORK DETECTION
==================================================

Database design must support detecting:

booking stuck in requested
trip stuck in assigned
driver offer expired
payment pending too long
outbox event not published
webhook not processed
settlement pending
GPS stale

Use timestamps and status history.

Do not require manual database inspection.

==================================================
105. HEALTH / RECONCILIATION QUERIES
==================================================

Create internal queries/jobs that verify invariants.

Examples:

booking without trip where trip is required

completed trip without required POD

captured payment without ledger transaction

settlement marked paid without payout

outbox event stuck

duplicate active assignment

trip with impossible status transition

negative inventory/capacity where prohibited

==================================================
106. TEST DATABASE
==================================================

Create automated tests for:

foreign keys
unique constraints
check constraints
state transitions
idempotency
concurrency
ledger balancing
webhook deduplication
settlement reruns
quote expiry
GPS validation
partition routing
audit generation

==================================================
107. CONCURRENCY TESTS
==================================================

Write tests specifically for race conditions.

Example:

100 simultaneous requests

attempting to accept

the same trip.

Expected:

exactly one successful acceptance.

All others must receive a deterministic conflict/already-assigned result.

==================================================
108. LEDGER TESTS
==================================================

Test:

every posted transaction balances

no posted entry can be modified

duplicate financial event is rejected/deduplicated

refund creates correct reversal/accounting behavior

settlement is not paid twice

payment reconciliation works

==================================================
109. PAYMENT TESTS
==================================================

Test:

duplicate webhook

out-of-order webhook

invalid signature

unknown provider event

payment timeout

payment retry

partial refund

full refund

provider success but internal timeout

internal success but webhook arrives later

==================================================
110. MIGRATION TESTS
==================================================

Every migration must be tested against:

empty database

fresh database

database containing realistic production-like data

upgrade from previous schema

rollback if rollback is supported

==================================================
111. REALISTIC LOAD TEST DATA
==================================================

Generate realistic data for:

customers
drivers
vehicles
bookings
trips
GPS pings
payments
settlements

Do not benchmark using 10 rows.

Test realistic scale.

==================================================
112. GPS LOAD MODEL
==================================================

Estimate GPS volume as:

concurrent_trips
×
pings_per_trip_per_minute
×
60
×
minutes_per_day

Example:

500 concurrent trips
1 ping every 15 seconds

=

500 × 4 × 60 × 24

=

2,880,000 GPS points/day.

Use actual expected product settings when known.

Design storage based on real numbers rather than assumptions.

==================================================
113. DATABASE CAPACITY PLANNING
==================================================

Estimate:

rows/day
row size
indexes
WAL generation
storage growth
backup growth
IOPS
CPU
connections

Calculate at:

launch

6 months

12 months

3 years

Do not design only for today's traffic.

==================================================
114. PRODUCTION ENVIRONMENTS
==================================================

Maintain separate:

development
test
staging
production

Never let development tools connect to production.

Use separate credentials.

==================================================
115. PRODUCTION DATABASE ACCESS
==================================================

Production database access must be restricted.

Application developers should not casually run:

DELETE
TRUNCATE
DROP

against production.

Use controlled access and audit logging.

==================================================
116. SCHEMA DOCUMENTATION
==================================================

Generate documentation containing:

table
purpose
columns
types
nullable
default
constraints
foreign keys
indexes
ownership
retention
PII classification
authoritative/derived classification

==================================================
117. ENTITY RELATIONSHIP DIAGRAM
==================================================

Create an ERD.

At minimum represent:

Organization
User
Driver
Carrier
Vehicle
Location
Port
Lane
Container
Shipment
Shipment Stop
Booking
Quote
Trip
Trip Stop
Trip Assignment
Location Ping
Document
Payment
Invoice
Ledger Account
Ledger Transaction
Ledger Entry
Settlement
Payout
Audit Log
Outbox Event
Idempotency Key

Show cardinality.

==================================================
118. CORE RELATIONSHIPS
==================================================

The conceptual relationship should resemble:

Organization
   |
   +---- Users
   |
   +---- Drivers
   |
   +---- Vehicles
   |
   +---- Shipments
             |
             +---- Containers
             |
             +---- Stops
             |
             +---- Booking
                    |
                    +---- Quote
                    |
                    +---- Trip
                           |
                           +---- Driver
                           +---- Vehicle
                           +---- Stops
                           +---- Assignments
                           +---- GPS
                           +---- Events

Booking
   |
   +---- Payment
   |
   +---- Invoice
   |
   +---- Ledger references

Carrier
   |
   +---- Settlement
           |
           +---- Settlement Lines
           |
           +---- Payout

All critical changes
   |
   +---- Audit
   +---- Outbox

==================================================
119. DATABASE NAMING
==================================================

Use consistent naming.

Tables:

snake_case plural

Examples:

bookings
trip_assignments
location_pings

Columns:

snake_case

Primary key:

id

Foreign key:

<entity>_id

Timestamp:

created_at
updated_at
occurred_at

Do not mix:

camelCase
snake_case
PascalCase

==================================================
120. SQL STYLE
==================================================

SQL must be readable.

Use explicit column lists.

Avoid:

SELECT *

in production application queries.

Use explicit joins.

Use parameterized queries.

Never construct SQL using unsafe string interpolation.

==================================================
121. ORM RULES
==================================================

If using an ORM:

Do not allow the ORM to hide critical database behavior.

Explicitly define:

foreign keys
indexes
unique constraints
transactions
locking
cascade behavior
decimal/money handling
timestamps

Review generated SQL for critical queries.

==================================================
122. DATABASE TRANSACTION BOUNDARIES
==================================================

Document transaction boundaries for:

booking creation
booking confirmation
trip assignment
driver acceptance
trip status transition
payment finalization
refund
ledger posting
settlement creation
payout initiation
webhook processing

==================================================
123. CRITICAL TRANSACTION EXAMPLE
==================================================

Driver accepts trip:

BEGIN

1. Lock trip.
2. Verify trip is assignable.
3. Lock/verify job offer.
4. Verify driver is eligible.
5. Verify no competing active assignment.
6. Create assignment.
7. Update trip status.
8. Add status history.
9. Add audit event if required.
10. Insert outbox event.

COMMIT

Only after COMMIT should external notification/event publishing occur.

==================================================
124. BOOKING TRANSACTION EXAMPLE
==================================================

BEGIN

1. Validate customer ownership.
2. Lock quote.
3. Verify quote is active.
4. Verify quote has not expired.
5. Create booking.
6. Snapshot commercial references.
7. Create required operational records.
8. Write booking history.
9. Write outbox event.
10. Commit.

Do not call payment gateway while holding database locks.

==================================================
125. FINANCIAL TRANSACTION EXAMPLE
==================================================

When payment is verified:

BEGIN

1. Deduplicate provider event.
2. Lock payment.
3. Verify payment state.
4. Update payment.
5. Create ledger transaction.
6. Create balanced ledger entries.
7. Record reconciliation information.
8. Insert outbox event.
9. Mark webhook processed.

COMMIT

If any critical step fails:

ROLLBACK

Do not mark payment successful without the required financial records.

==================================================
126. DATABASE FAILURE HANDLING
==================================================

The application must correctly handle:

connection timeout
deadlock
serialization failure
unique violation
foreign key violation
statement timeout
database unavailable

Do not convert all database errors into HTTP 500.

Map expected conflicts correctly.

Retry only safe transient errors.

Do not blindly retry arbitrary transactions.

==================================================
127. DEADLOCK HANDLING
==================================================

Use consistent lock ordering.

Example:

Always lock:

booking

then trip

then assignment

rather than sometimes:

trip

then booking

and elsewhere:

booking

then trip

This reduces deadlock risk.

==================================================
128. SECURITY TESTING
==================================================

Test:

SQL injection
IDOR
cross-tenant access
privilege escalation
mass assignment
unauthorized updates
sensitive data leakage
unsafe exports

Database constraints are not a replacement for authorization.

==================================================
129. FINAL DATABASE DELIVERABLES
==================================================

You must produce:

1. Database architecture document.
2. ERD.
3. Schema/module map.
4. Full migration set.
5. Seed scripts.
6. Index strategy.
7. Partition strategy.
8. Constraint strategy.
9. Transaction-boundary documentation.
10. Idempotency implementation.
11. Outbox/inbox implementation.
12. Ledger implementation.
13. Audit implementation.
14. Backup/restore documentation.
15. Database performance documentation.
16. Data retention documentation.
17. Test suite.
18. Load test dataset.
19. Database health queries.
20. Production readiness checklist.

==================================================
130. IMPLEMENTATION ORDER
==================================================

Do NOT build everything randomly.

Implement in this order:

PHASE 1
Database foundation

- PostgreSQL setup
- extensions
- migration framework
- naming conventions
- common types
- base schemas
- database roles

PHASE 2
Identity and organizations

- users
- roles
- permissions
- organizations
- memberships
- sessions

PHASE 3
Fleet and geography

- carriers
- drivers
- vehicles
- locations
- ports
- terminals
- lanes

PHASE 4
Shipment

- shipments
- containers
- shipment containers
- shipment stops

PHASE 5
Pricing

- pricing configuration
- pricing versions
- quotes
- quote components

PHASE 6
Bookings

- bookings
- booking status history
- booking constraints

PHASE 7
Operations

- trips
- trip stops
- assignments
- job offers
- trip events
- trip status history
- exceptions

PHASE 8
Tracking

- location pings
- partitions
- latest locations
- retention

PHASE 9
Documents

- documents
- versions
- POD metadata

PHASE 10
Payments

- payments
- attempts
- webhooks
- refunds

PHASE 11
Billing

- invoices
- invoice lines

PHASE 12
Ledger

- accounts
- transactions
- entries
- reconciliation

PHASE 13
Settlement

- settlements
- settlement lines
- payouts
- reconciliation

PHASE 14
Notifications

- notifications
- delivery attempts
- preferences

PHASE 15
Platform infrastructure

- idempotency
- outbox
- inbox
- audit
- integrations

PHASE 16
Performance

- indexes
- partition optimization
- query tuning
- connection pooling
- vacuum tuning

PHASE 17
Testing

- unit
- integration
- concurrency
- financial
- load
- failure
- restore

==================================================
131. AGENT EXECUTION RULE
==================================================

For every phase:

1. Inspect existing code.
2. Explain what will change.
3. Create migration.
4. Update models/entities.
5. Update repositories.
6. Update service layer.
7. Update tests.
8. Run migrations against clean database.
9. Run migrations against existing database.
10. Run test suite.
11. Inspect generated SQL.
12. Verify indexes.
13. Verify constraints.
14. Verify rollback/recovery strategy.
15. Report exactly what changed.

Do not silently modify unrelated modules.

==================================================
132. NEVER DO THESE THINGS
==================================================

NEVER:

- use FLOAT for money
- store plaintext passwords
- store raw card credentials
- trust client payment status
- trust client trip completion timestamps blindly
- delete financial history
- overwrite ledger records
- hardcode pricing
- hardcode ports
- use Redis as source of truth
- store unlimited GPS data without retention
- create arbitrary cross-module SQL joins
- use SELECT * in production APIs
- add indexes blindly
- disable foreign keys to make development easier
- use cascading deletes blindly
- ignore unique constraints
- ignore concurrency
- assume exactly-once event processing
- assume webhooks arrive once
- hold DB transactions while calling external APIs
- manually edit production schema
- run destructive migrations without a plan
- expose internal sequential IDs unnecessarily
- put all data into JSONB
- create one giant "everything" table

==================================================
133. PRODUCTION ACCEPTANCE CRITERIA
==================================================

The database is NOT considered production-ready until:

[ ] All migrations run successfully.
[ ] Existing data survives migration.
[ ] All critical foreign keys work.
[ ] Unique constraints are tested.
[ ] State transitions are tested.
[ ] Concurrent trip acceptance is tested.
[ ] Payment webhook deduplication is tested.
[ ] Idempotency is tested.
[ ] Ledger balancing is tested.
[ ] Settlement reruns are tested.
[ ] GPS partitioning works.
[ ] Retention process works.
[ ] Outbox processing works.
[ ] Inbox deduplication works.
[ ] Audit logs are generated.
[ ] Backup succeeds.
[ ] Restore succeeds.
[ ] Point-in-time recovery is tested where configured.
[ ] Slow queries are identified.
[ ] Indexes are validated.
[ ] Connection pool is load-tested.
[ ] Database load test passes.
[ ] No critical data integrity violations exist.
[ ] No known critical security vulnerabilities exist.
[ ] Production database permissions are restricted.
[ ] Migration rollback/forward recovery strategy is documented.
[ ] Monitoring is configured.
[ ] Alerts are configured.
[ ] Runbooks exist.
[ ] On-call engineers understand the database.

==================================================
134. REQUIRED FINAL REPORT
==================================================

After implementation, provide a final report containing:

A. Architecture

B. PostgreSQL schemas

C. Tables

D. Relationships

E. ERD

F. Indexes

G. Constraints

H. Partitioning

I. Migration strategy

J. Transaction boundaries

K. Concurrency strategy

L. Idempotency strategy

M. Event/outbox strategy

N. Payment data model

O. Ledger model

P. Settlement model

Q. Audit model

R. Backup/recovery

S. Performance

T. Security

U. Testing

V. Known limitations

W. Future scaling plan

X. Production readiness status

For every unresolved issue, clearly label:

BLOCKER
HIGH
MEDIUM
LOW

Do not claim production readiness if any BLOCKER or HIGH issue remains.

==================================================
135. MOST IMPORTANT PRINCIPLE
==================================================

This database is not merely a place to store application objects.

It is the system of record.

When deciding between:

"easy for the application"

and

"safe and correct for production"

choose correctness.

When deciding between:

"fast implementation"

and

"maintainable architecture"

choose maintainability.

When deciding between:

"convenient mutable data"

and

"auditable historical data"

choose auditability for business-critical records.

When deciding between:

"cache"

and

"source of truth"

choose PostgreSQL for durable business state.

Build this as if thousands of real customers, drivers, vehicles, payments and financial transactions will depend on it.

==================================================
END OF DATABASE ENGINEERING SPECIFICATION
==================================================
```

## How you should actually use this with your AI coding agent

Don't dump this prompt into the agent and tell it **“build everything.”** For a project this large, the agent can make a huge mess if it starts generating hundreds of migrations at once.

Use a controlled process.

### Step 1 — Give the agent the project

Your repository should ideally look roughly like:

```text
logistics-platform/
│
├── backend/
│   ├── src/
│   ├── migrations/
│   ├── tests/
│   ├── ...
│
├── shipper-app/
│
├── driver-app/
│
├── admin-dashboard/
│
├── docs/
│
└── ...
```

The database agent should have access to the backend first.

---

# Step 2 — First tell the AI NOT to code

Your first message should be something like:

Before writing any database code, inspect the entire repository and understand the existing backend architecture.

Do NOT create migrations yet.

Analyze:

1. Backend framework
2. ORM/query builder
3. Existing database
4. Existing migrations
5. Existing models/entities
6. API endpoints
7. Authentication
8. Booking flow
9. Pricing flow
10. Trip/driver assignment flow
11. GPS tracking
12. Payment integration
13. Invoice/billing logic
14. Settlement logic
15. Event system
16. Redis usage
17. Object storage
18. Background jobs
19. Existing tests

Then produce:

A. Current architecture
B. Existing database schema
C. Existing tables
D. Existing relationships
E. Missing tables
F. Problems in current schema
G. Recommended target architecture
H. Migration plan
I. Risks
J. Questions/blockers

Do not modify any files during this step.

Do not guess when repository evidence exists.

Wait for my approval before implementation.

This is important.

**Make the AI understand the existing project before allowing it to touch the database.**

---

# Step 3 — Make the agent produce the ERD

After discovery, tell it:

Using your repository analysis and the production database specification I provided, design the complete target database.

Do not implement it yet.

Create:

1. Module/schema map
2. Complete entity list
3. Every table
4. Every column
5. Data type for every column
6. Nullable/non-nullable status
7. Primary keys
8. Foreign keys
9. Unique constraints
10. Check constraints
11. Indexes
12. Relationships/cardinality
13. Partitioning strategy
14. Retention strategy
15. Ownership of every table
16. Authoritative vs derived classification

Then create a complete ERD using Mermaid.

For every important relationship explain why it exists.

Pay special attention to:

Booking → Shipment
Booking → Quote
Booking → Trip
Trip → Driver
Trip → Vehicle
Trip → Assignment
Trip → GPS
Booking → Payment
Payment → Ledger
Trip → Settlement
Settlement → Payout
Document → POD
Outbox → Domain events
Idempotency → API operations

Do not write migrations yet.

Identify contradictions with the existing repository.

Wait for approval.

---

# Step 4 — Review the ERD before implementation

This is where **you** and the AI should catch mistakes.

For example, you should be able to see something like:

```text
                    ORGANIZATION
                         │
          ┌──────────────┼──────────────┐
          │              │              │
        USERS          DRIVERS        VEHICLES
          │              │              │
          │              └──────┬───────┘
          │                     │
          │                ASSIGNMENTS
          │                     │
          │                     ▼
SHIPMENT ───────────────► BOOKING ─────────► TRIP
   │                         │                │
   │                         │                ├── TRIP STOPS
   │                         │                ├── STATUS HISTORY
   │                         │                ├── GPS
   │                         │                ├── EVENTS
   │                         │                └── DOCUMENTS
   │                         │
   │                         ├────────► PAYMENT
   │                         │              │
   │                         │              ▼
   │                         │           LEDGER
   │                         │
   │                         └────────► INVOICE
   │
   └────────► CONTAINERS

TRIP ─────────► SETTLEMENT ─────────► PAYOUT
```

If the AI's proposed design doesn't make sense at this level, **don't let it start coding yet.**

---

# Step 5 — Then implement foundation

Tell the agent to implement only:

```text
Phase 1
    ↓
PostgreSQL
    ↓
Schemas
    ↓
Extensions
    ↓
Migration framework
    ↓
DB roles
    ↓
Common conventions
```

Then test.

Only after that:

```text
Phase 2
Identity + Organizations
```

Then:

```text
Phase 3
Fleet + Geography
```

And so on.

This is much safer than asking an agent to create 100+ tables simultaneously.

---

# The most important concept: don't think of it as "tables"

Think of the database as **layers**.

### Layer 1 — Identity

```text
Users
Organizations
Roles
Permissions
Sessions
```

### Layer 2 — Supply

```text
Carriers
Drivers
Vehicles
```

### Layer 3 — Geography

```text
Locations
Ports
Terminals
Lanes
```

### Layer 4 — Demand

```text
Shipments
Containers
Bookings
```

### Layer 5 — Commercial

```text
Pricing
Quotes
Payments
Invoices
```

### Layer 6 — Operations

```text
Trips
Assignments
Stops
Job Offers
Trip Events
```

### Layer 7 — Tracking

```text
GPS
Latest Locations
```

### Layer 8 — Financial

```text
Ledger
Settlements
Payouts
Reconciliation
```

### Layer 9 — Platform infrastructure

```text
Audit
Idempotency
Outbox
Inbox
Notifications
Exceptions
```

That separation is what keeps the system maintainable when it grows.

---

# One particularly important design decision

Your system should **not** do this:

```text
trip
 ├── driver
 ├── vehicle
 ├── payment
 ├── gps
 ├── invoice
 ├── settlement
 ├── documents
 ├── notifications
 └── everything else
```

That becomes a giant operational table and eventually becomes painful.

Instead:

```text
                         TRIP
                          │
       ┌──────────────────┼──────────────────┐
       │                  │                  │
   ASSIGNMENT           STOPS              EVENTS
       │                                     │
 DRIVER + VEHICLE                         HISTORY
       │
       └────────────── GPS

BOOKING
   │
   ├── QUOTE
   ├── PAYMENT
   ├── INVOICE
   └── TRIP

TRIP
   │
   └── SETTLEMENT
           │
           └── PAYOUT

PAYMENT / SETTLEMENT / INVOICE
           │
           ▼
         LEDGER
```

This is the architecture I would strongly recommend for the project.

---

# And there are 5 tables/systems you should treat as especially important

If you're new to databases, these might initially look unnecessary.

They aren't.

### 1. `idempotency_keys`

Protects you from:

> User taps twice → two bookings/payments/assignments.

---

### 2. `outbox_events`

Protects you from:

> Database transaction succeeds → event publishing fails.

---

### 3. `inbox_events`

Protects you from:

> Event delivered twice → operation executed twice.

---

### 4. `ledger`

Protects you from:

> Financial history becoming impossible to reconcile.

---

### 5. `status_history`

Protects you from:

> “Why did this trip become completed at 3:41 PM?”

You can actually reconstruct what happened.

---

# Your final database architecture should look approximately like this

```text
                         ┌──────────────────┐
                         │    PostgreSQL    │
                         │  SOURCE OF TRUTH │
                         └────────┬─────────┘
                                  │
       ┌──────────────────────────┼──────────────────────────┐
       │                          │                          │
       ▼                          ▼                          ▼
   IDENTITY                    OPERATIONS                FINANCIAL
       │                          │                          │
       ├─ Users                   ├─ Trips                  ├─ Payments
       ├─ Roles                   ├─ Assignments            ├─ Invoices
       └─ Organizations           ├─ Stops                  ├─ Ledger
                                  ├─ Events                 ├─ Settlements
                                  └─ Exceptions             └─ Payouts
       │
       ▼
    FLEET
       │
       ├─ Drivers
       ├─ Vehicles
       └─ Carriers

       │
       ▼
    SHIPMENT
       │
       ├─ Shipments
       ├─ Containers
       └─ Stops

       │
       ▼
    PRICING
       │
       ├─ Rules
       ├─ Versions
       ├─ Quotes
       └─ Components

       │
       ▼
    TRACKING
       │
       ├─ Latest Location
       └─ GPS History

       │
       ▼
    DOCUMENTS
       │
       ├─ Document metadata
       ├─ Versions
       └─ POD

       │
       ▼
    PLATFORM
       │
       ├─ Idempotency
       ├─ Outbox
       ├─ Inbox
       ├─ Audit
       └─ Notifications


                    ┌─────────────┐
                    │    Redis    │
                    │    CACHE    │
                    └─────────────┘

                    ┌─────────────┐
                    │ S3 / Object │
                    │   Storage   │
                    └─────────────┘

                    ┌─────────────┐
                    │ Queue/Event │
                    │    Bus      │
                    └─────────────┘
```

The key principle is:

**PostgreSQL stores the truth. Redis accelerates it. Object storage stores files. The event system distributes changes.**

And your AI agent should build the database **incrementally, migration-by-migration, with tests after every module**, rather than generating a giant schema and hoping it works.     
