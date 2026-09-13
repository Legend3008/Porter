# Production Frontend Build Prompt

## Shipper Android + Driver/Carrier Android + Ops/Admin Web Platform

---

# 0. MASTER INSTRUCTION

You are a **principal-level frontend/mobile engineering organization** responsible for designing, implementing, testing, securing, and production-hardening three client applications for a Porter-style, port-to-destination container logistics platform:

1. **Shipper App** — native Android
2. **Driver/Carrier App** — native Android
3. **Ops/Admin Dashboard** — production web application

This is not a prototype, mockup, tutorial, proof of concept, or UI-only implementation.

Build software intended to survive:

* unreliable mobile networks
* intermittent connectivity at ports
* background execution restrictions
* Android process death
* GPS inaccuracies
* battery constraints
* duplicate requests
* race conditions
* delayed WebSocket events
* stale caches
* payment-provider delays
* thousands of simultaneous operational records
* multiple operators modifying the same entity
* backend/API evolution
* large datasets
* partial failures
* application crashes
* app upgrades
* malicious or malformed client input
* real production traffic

The backend described by the existing backend production specification is the **authoritative system of record**.

The frontend must never invent business truth.

---

# 1. PRIMARY OBJECTIVE

Deliver three production-quality applications that provide a coherent end-to-end experience:

```text
SHIPPER
Register/Login
    ↓
Create shipment
    ↓
Get server quote
    ↓
Book container movement
    ↓
Payment
    ↓
Booking confirmation
    ↓
Driver assignment
    ↓
Live tracking
    ↓
Pickup
    ↓
Transit
    ↓
Delivery
    ↓
POD
    ↓
Delivery confirmation
    ↓
Invoice / payment history
```

```text
DRIVER
Login
    ↓
Availability
    ↓
Job offer
    ↓
Accept job
    ↓
Navigate to pickup
    ↓
Arrive
    ↓
Pickup
    ↓
Transit
    ↓
Delivery
    ↓
POD capture
    ↓
Complete trip
    ↓
Earnings
```

```text
OPS
Authenticate
    ↓
Operations dashboard
    ↓
Monitor active trips
    ↓
Inspect containers
    ↓
Inspect drivers
    ↓
Inspect customers
    ↓
Handle exceptions
    ↓
Reassign / override when authorized
    ↓
Audit action
    ↓
Analytics / reporting
```

All three clients must represent the same backend state consistently.

---

# 2. NON-NEGOTIABLE ENGINEERING PRINCIPLES

## 2.1 Backend is authoritative

Never calculate or infer authoritative business state locally.

The frontend may temporarily display optimistic state for UX, but:

```text
Server response > WebSocket event > local optimistic state > local cache
```

The client must reconcile when authoritative data arrives.

Never calculate:

* final trip price
* driver earnings
* commissions
* taxes
* invoice totals
* balances
* payment success
* delivery completion
* trip completion
* assignment ownership
* authoritative ETA
* authoritative status transitions

from client-side assumptions.

---

# 3. STALE-DATA SAFETY

Any screen showing live operational information must distinguish:

```text
LIVE
UPDATED X SECONDS AGO
RECONNECTING
OFFLINE
STALE
UNKNOWN
```

Never silently freeze:

```text
Vehicle position
Trip status
Driver location
ETA
Assignment
Payment state
Delivery state
```

and make it appear current.

Define a common freshness model:

```text
LIVE
    event received within freshness threshold

RECENT
    no event yet but within acceptable tolerance

STALE
    data older than configured threshold

DISCONNECTED
    transport connection unavailable

OFFLINE
    device/network unavailable

UNKNOWN
    freshness cannot be established
```

Thresholds must be configurable rather than scattered as magic numbers.

---

# 4. REPOSITORY ARCHITECTURE

Use a monorepo unless the existing backend/repository architecture explicitly requires otherwise.

Recommended:

```text
/
├── android/
│   ├── shipper/
│   └── driver/
│
├── web/
│   └── ops-dashboard/
│
├── shared/
│   ├── api-contracts/
│   ├── schemas/
│   ├── generated/
│   └── design-tokens/
│
├── docs/
│   ├── architecture/
│   ├── api/
│   ├── offline/
│   ├── security/
│   └── release/
│
├── scripts/
├── .github/
│   └── workflows/
└── README.md
```

Do not duplicate API models manually between applications.

---

# 5. API CONTRACT INTEGRATION

Consume the backend's:

```text
REST /api/v1
GraphQL
WebSocket/event stream
Authentication system
```

Generate strongly typed clients from the backend OpenAPI/schema.

Requirements:

* generated request models
* generated response models
* generated enums
* generated API clients
* schema validation where appropriate
* compile-time type safety
* version compatibility
* API contract tests

Never manually recreate backend DTOs in three applications.

If the backend contract conflicts with this document:

**backend contract wins.**

Document the discrepancy rather than silently inventing behavior.

---

# 6. SHARED DOMAIN MODEL

Create a shared conceptual model for:

```text
User
Shipper
Driver
Carrier
Container
Shipment
Booking
Trip
Assignment
Port
Location
Quote
Payment
Invoice
POD
Document
Notification
Earning
StatusEvent
AuditEvent
```

Represent backend enums exactly.

Example:

```text
TripStatus
├── CREATED
├── ASSIGNED
├── DRIVER_EN_ROUTE
├── AT_PICKUP
├── LOADING
├── IN_TRANSIT
├── AT_DELIVERY
├── UNLOADING
├── DELIVERED
├── COMPLETED
├── CANCELLED
└── FAILED
```

Do not assume these exact values if the backend defines different ones.

Generate them from the backend contract.

---

# 7. GLOBAL UI STATE MODEL

Every network-dependent screen must explicitly model:

```text
INITIAL
LOADING
SUCCESS
EMPTY
REFRESHING
OFFLINE
STALE
ERROR
RETRYING
PERMISSION_REQUIRED
UNAUTHORIZED
FORBIDDEN
CONFLICT
RATE_LIMITED
SERVER_ERROR
```

Never use:

```text
loading = true
```

as the entire application state model.

Never show an infinite spinner.

Every failure must have a recoverable UX where possible.

---

# 8. ERROR-HANDLING STANDARD

Build a centralized error system.

Map:

```text
HTTP/API errors
GraphQL errors
WebSocket errors
timeout
DNS/network failure
authentication expiry
authorization failure
validation failure
conflict
rate limit
server failure
unknown failure
```

into user-safe messages.

Do not expose:

* stack traces
* internal exception names
* database errors
* tokens
* internal IDs where inappropriate
* infrastructure details

Provide:

```text
Retry
Refresh
Go Back
Work Offline
Contact Support
```

only when those actions actually make sense.

---

# 9. RETRY POLICY

Every network operation must have a declared retry strategy.

For example:

```text
GET:
    retry transient failures

POST:
    retry only with idempotency key where supported

PUT/PATCH:
    retry according to idempotency contract

DELETE:
    retry according to API semantics

Upload:
    resumable/retryable where supported

WebSocket:
    reconnect using exponential backoff + jitter
```

Never retry:

```text
4xx validation failures
authentication failures
authorization failures
business-rule conflicts
expired quote/payment states
```

unless the specific endpoint contract requires another behavior.

---

# 10. AUTHENTICATION

Implement the backend's authentication model exactly.

Requirements:

* secure token storage
* access-token refresh
* refresh-token rotation where supported
* session expiry handling
* logout
* forced logout on invalid credentials/session
* biometric/device security where appropriate
* no tokens in logs
* no tokens in analytics
* no tokens in crash reports
* secure WebSocket authentication

Handle:

```text
API token expired
WebSocket token expired
refresh failure
account disabled
session revoked
```

without leaving the UI in a broken state.

---

# 11. SECURITY

Apply OWASP mobile and web security practices.

Never store secrets in:

```text
source code
Git
plain SharedPreferences
localStorage
logs
analytics payloads
screenshots
crash breadcrumbs
```

For Android use platform secure storage.

For web use secure server-managed authentication where supported by the architecture.

Implement:

* input validation
* output escaping
* XSS protection
* CSRF protection where applicable
* secure headers
* CSP
* authorization-aware routing
* role-based UI permissions
* sensitive-data redaction
* secure file handling
* certificate/security configuration according to deployment requirements

The UI must not be treated as an authorization boundary.

---

# 12. SHIPPER AND DRIVER ANDROID ARCHITECTURE

Use:

```text
Kotlin
Jetpack Compose
Material 3
Clean Architecture
MVVM
Coroutines
Flow
Hilt
Retrofit
Room
WorkManager
Foreground Service where required
Navigation Compose
```

Recommended structure:

```text
:app

:core:common
:core:ui
:core:designsystem
:core:network
:core:database
:core:auth
:core:location
:core:notifications
:core:analytics
:core:logging
:core:permissions

:data:api
:data:database
:data:repository

:domain:model
:domain:repository
:domain:usecase

:feature:auth
:feature:home
:feature:booking
:feature:tracking
:feature:documents
:feature:payments
:feature:notifications
:feature:profile
```

Keep business logic outside Composables.

---

# 13. SHIPPER APP

## 13.1 Core screens

Implement:

```text
Splash
Onboarding
Login
Registration
OTP
Home
Create Shipment
Container Details
Pickup/Delivery Details
Quote
Booking Review
Payment
Payment Processing
Booking Confirmation
Bookings
Booking Detail
Live Tracking
Status Timeline
Documents
Invoices
Payment History
Notifications
Profile
Settings
Support
Account Deletion
```

---

# 14. SHIPPER BOOKING FLOW

The complete flow must be:

```text
Create shipment
→ validate fields
→ request quote
→ display quote expiry
→ review
→ create booking
→ payment
→ await authoritative payment state
→ confirmation
```

Quote must display:

```text
price
currency
expiry
last refreshed
```

When expired:

```text
QUOTE_EXPIRED
```

must trigger re-quote or explicit user confirmation.

Never charge against a stale quote.

Prevent duplicate bookings using backend-supported idempotency.

---

# 15. PAYMENT

Use the payment provider's official Android SDK.

Never implement payment through an insecure custom WebView unless explicitly required by the provider.

Model:

```text
INITIATED
PENDING
PROCESSING
SUCCESS
FAILED
CANCELLED
EXPIRED
UNKNOWN
```

Payment success must come from the authoritative backend/payment confirmation.

Never infer:

```text
SDK callback = final business success
```

Instead:

```text
Gateway
→ webhook
→ backend
→ authoritative API/event
→ client
```

If payment is pending, show:

> Payment processing

not:

> Payment successful

---

# 16. SHIPPER LIVE TRACKING

Implement:

```text
WebSocket
↓
event validation
↓
state reconciliation
↓
UI state
```

Track:

```text
trip status
driver position
last update
connection state
ETA if backend provides it
```

Never infer:

```text
arrived
picked up
delivered
completed
```

from GPS coordinates.

The status timeline must use backend status events.

---

# 17. DOCUMENT MANAGEMENT

Support:

```text
Bill of Lading
Delivery Order
Gate Pass
Other backend-supported documents
```

Requirements:

* MIME validation
* file-size validation
* image compression
* preview
* upload progress
* cancellation
* retry
* resumability where supported
* local temporary persistence
* duplicate prevention
* upload failure recovery

If upload fails:

```text
Retry
```

must reuse the captured/selected file.

Never force the user to select it again unnecessarily.

---

# 18. PUSH NOTIFICATIONS

Use FCM.

Support:

```text
booking confirmation
driver assigned
trip started
delay
arrival
delivery
payment issue
document issue
system announcement
```

Push is a notification mechanism, not the source of truth.

On app launch:

```text
fetch authoritative state
```

even if no push was received.

Deep links must open the correct entity:

```text
notification
→ booking/trip/document/payment
```

with authentication and authorization checks.

---

# 19. DRIVER/CARRIER APP

This is the highest-risk client.

Optimize for:

```text
one-handed use
large touch targets
minimal interaction
high contrast
glanceability
poor connectivity
background operation
battery efficiency
driving safety
```

---

# 20. DRIVER CORE SCREENS

Implement:

```text
Login
Driver Home
Availability
Job Offers
Job Detail
Accept/Reject
Active Trip
Navigation Handoff
Pickup
Container Verification
Loading
Transit
Delivery
POD Capture
Signature
Trip Completion
Earnings
Trip History
Notifications
Profile
Vehicle
Documents
Support
Settings
```

---

# 21. BACKGROUND LOCATION

Implement Android-compliant background location tracking.

Use a foreground service where required.

Requirements:

* persistent notification
* explicit permission flow
* graceful permission denial
* tracking state visibility
* battery-aware location sampling
* process restart recovery
* service recovery
* trip-bound tracking
* stop tracking when no longer required

Do not continuously run maximum-frequency GPS.

---

# 22. ADAPTIVE LOCATION STRATEGY

Create a configurable location policy.

Example:

```text
ACTIVE + MOVING
    high-frequency updates

ACTIVE + STATIONARY
    reduced frequency

BACKGROUND + ACTIVE TRIP
    backend-approved interval

NO ACTIVE TRIP
    tracking disabled unless required
```

Use:

```text
GPS accuracy
speed
movement state
battery level
network state
trip state
```

to select appropriate behavior.

Do not hardcode one interval for every situation.

---

# 23. DRIVER OFFLINE-FIRST ARCHITECTURE

Offline behavior is mandatory.

Persist locally:

```text
active trip
pending commands
POD photos
signatures
status updates
location batches where required
documents
sync metadata
```

Every queued operation must have:

```text
operation ID
created timestamp
entity ID
payload
attempt count
last attempt
state
error
idempotency key
```

Example:

```text
PENDING
↓
SYNCING
↓
SUCCESS
```

or:

```text
PENDING
↓
SYNCING
↓
FAILED
↓
RETRY
```

Never silently discard operations.

---

# 24. OFFLINE COMMAND QUEUE

Operations such as:

```text
START_TRIP
ARRIVE_PICKUP
CONFIRM_PICKUP
START_TRANSIT
ARRIVE_DELIVERY
CONFIRM_DELIVERY
COMPLETE_TRIP
UPLOAD_POD
```

must follow the backend's valid state transitions.

When connectivity returns:

```text
sync queue
→ preserve ordering where required
→ use idempotency
→ process server responses
→ reconcile authoritative state
```

If the server rejects an operation because the state has changed:

```text
do not endlessly retry
```

Instead show the authoritative current state.

---

# 25. POD CAPTURE

POD is a critical offline operation.

When the driver captures:

```text
photo
signature
recipient information
```

save immediately to durable local storage.

Then:

```text
capture
→ persist
→ mark pending upload
→ upload asynchronously
→ retry
→ backend confirms
→ mark synced
```

Never require network connectivity merely to save the evidence.

---

# 26. JOB ACCEPTANCE RACE

Multiple drivers may receive the same job.

The backend is authoritative.

Possible result:

```text
ACCEPTED
```

or:

```text
ALREADY_ASSIGNED
CONFLICT
EXPIRED
REJECTED
```

If the driver's acceptance loses:

```text
Job no longer available
```

must replace any optimistic:

```text
You got the job
```

state.

Never leave stale optimistic state visible.

---

# 27. DRIVER EARNINGS

Earnings must come from:

```text
backend ledger
```

Never calculate earnings from:

```text
trip distance
trip price
local formulas
```

Client may display:

```text
gross
deductions
net
pending
paid
```

exactly as supplied by the backend.

---

# 28. OPS/ADMIN WEB DASHBOARD

Stack:

```text
React
TypeScript
Next.js
component system such as shadcn/ui
Mapbox GL JS or Google Maps JS API
Recharts-class visualization library
virtualized data tables
TanStack Query or equivalent
GraphQL client according to backend architecture
WebSocket client
```

Do not choose libraries merely for popularity.

Choose based on:

```text
performance
maintainability
bundle size
accessibility
security
ecosystem maturity
team productivity
```

---

# 29. ADMIN INFORMATION ARCHITECTURE

Create:

```text
Dashboard
Operations
Live Map
Trips
Containers
Shipments
Bookings
Drivers
Carriers
Customers
Ports
Payments
Invoices
Documents
Exceptions
Analytics
Reports
Audit Log
Settings
```

Use role-aware navigation.

---

# 30. LIVE OPERATIONS MAP

The map must support:

```text
hundreds/thousands of active trips
```

depending on backend scale.

Requirements:

* clustering
* viewport-based rendering
* marker virtualization where appropriate
* incremental position updates
* no full map rerender for one moved vehicle
* debounced viewport queries
* efficient WebSocket event handling
* filtering
* search
* status-based visualization
* selected-trip details
* stale marker state
* connection indicator

Each vehicle must expose:

```text
driver
trip
container
status
position
last updated
freshness
```

when authorized.

---

# 31. ADMIN TABLES

Implement virtualized/filterable tables.

Support:

```text
search
sort
filter
pagination
column visibility
saved views
bulk selection
export where authorized
```

Filters may include:

```text
port
status
customer
driver
carrier
date
container
booking
exception
payment state
```

Do not load thousands of records into the browser unnecessarily.

Prefer server-side:

```text
filtering
sorting
pagination
aggregation
```

where appropriate.

---

# 32. ADMIN MANUAL OVERRIDES

High-risk actions:

```text
reassign driver
modify operational state
modify pricing configuration
cancel trip
override exception
```

must require:

```text
confirmation
reason
authorization
audit log
```

Example:

```text
Action:
Reassign Trip

Current:
Driver A

New:
Driver B

Reason:
________________

[Cancel] [Confirm Reassignment]
```

No silent mutation.

---

# 33. AUDIT LOG

Every privileged mutation must capture backend-authoritative audit information including, where supplied:

```text
admin user
timestamp
action
entity
old value
new value
reason
request ID
IP/device context if backend provides it
```

Frontend must display the audit information without modifying it.

---

# 34. REAL-TIME ARCHITECTURE

Implement one consistent event-processing architecture.

Conceptually:

```text
WebSocket
   ↓
Connection Manager
   ↓
Event Validator
   ↓
Event Router
   ↓
Entity Store / Query Cache
   ↓
UI
```

Events must be:

```text
validated
deduplicated
ordered where required
reconciled
observable
```

Handle:

```text
duplicate events
out-of-order events
missing events
reconnection
subscription failure
authentication expiry
server restart
```

After reconnect:

```text
REST/GraphQL resynchronization
```

must establish authoritative state rather than assuming no events were missed.

---

# 35. REAL-TIME CONNECTION STATES

Display appropriate states:

```text
Connected
Connecting
Reconnecting
Disconnected
Authentication expired
Subscription failed
Data stale
```

Do not hide connection problems.

---

# 36. STATE MANAGEMENT

Do not put every piece of state into one global store.

Separate:

```text
server state
UI state
session state
form state
offline state
real-time state
```

Use appropriate tools for each.

Server state should support:

```text
cache
deduplication
background refresh
invalidations
stale time
retry
pagination
optimistic mutation rollback
```

---

# 37. CACHING

Define cache policies per entity.

Example:

```text
User profile
    medium-lived

Trip details
    short-lived + event-driven

Live location
    extremely short-lived

Invoices
    backend-authoritative + refreshable

Static port data
    long-lived

Permissions
    session-scoped
```

Never allow stale cache to masquerade as live state.

---

# 38. ACCESSIBILITY

Build accessibility into the design system.

Requirements:

* screen reader labels
* semantic controls
* keyboard navigation on web
* focus management
* sufficient contrast
* scalable text
* touch targets
* error announcements
* form validation messages
* reduced-motion support
* accessible tables
* accessible dialogs

WCAG-oriented web implementation.

---

# 39. LOCALIZATION

Initial languages:

```text
English
Hindi
```

All user-visible strings must come from localization resources.

Never hardcode strings inside business logic.

Support:

```text
pluralization
dates
times
currency
numbers
RTL readiness where practical
```

Do not concatenate translated fragments incorrectly.

---

# 40. DESIGN SYSTEM

Create a shared visual language across all three applications.

Define:

```text
colors
typography
spacing
radius
elevation
icons
buttons
inputs
cards
dialogs
bottom sheets
status badges
tables
empty states
loading states
error states
network banners
```

Status semantics must be consistent.

Example:

```text
SUCCESS
WARNING
ERROR
INFO
NEUTRAL
STALE
OFFLINE
```

---

# 41. UX STATE COMPLETENESS

Every important screen must implement:

```text
Loading
Loaded
Empty
Error
Offline
Stale
Permission denied
Unauthorized
Refreshing
Partial data
```

Do not implement only the happy path.

---

# 42. FORM ENGINEERING

Forms must support:

```text
validation
server validation
field-level errors
submission state
duplicate submission prevention
keyboard management
accessibility
draft persistence where useful
```

Do not clear user-entered information after an unrelated server error.

---

# 43. FILE UPLOAD ENGINEERING

All upload flows must support:

```text
pre-validation
compression
progress
cancellation
retry
resume where possible
duplicate protection
offline queue where appropriate
```

Never trust client MIME/type validation alone.

Backend remains authoritative.

---

# 44. ANALYTICS

Implement privacy-conscious product analytics.

Track meaningful events such as:

```text
signup_started
signup_completed
quote_created
booking_started
booking_completed
payment_started
payment_pending
payment_completed
payment_failed
tracking_opened
document_uploaded
pod_captured
trip_started
trip_completed
```

Do not send:

```text
passwords
tokens
payment credentials
raw sensitive documents
unnecessary PII
```

Create a standardized analytics schema.

---

# 45. OBSERVABILITY

Android:

```text
Firebase Crashlytics
```

Web:

```text
Sentry or equivalent
```

Track:

```text
crashes
ANRs
network failures
WebSocket failures
sync failures
upload failures
payment-flow errors
navigation errors
```

Add correlation/request IDs when available.

Never log secrets.

---

# 46. PERFORMANCE REQUIREMENTS

## Android

Optimize:

```text
startup
memory
battery
database queries
image loading
recomposition
network usage
background work
```

Avoid unnecessary Compose recompositions.

Use profiling before release.

## Web

Optimize:

```text
LCP
INP
CLS
bundle size
JavaScript execution
map performance
table performance
memory
WebSocket throughput
```

The dashboard must remain usable while receiving large volumes of live updates.

---

# 47. RESILIENCE TESTING

Explicitly test:

```text
Wi-Fi → LTE
LTE → offline
offline → LTE
offline → Wi-Fi
WebSocket disconnect
WebSocket reconnect
API timeout
API 500
API 401
API 403
API 409
API rate limiting
duplicate requests
delayed responses
out-of-order events
Android process death
app restart
phone reboot
low battery
GPS unavailable
location permission revoked
notification permission denied
storage pressure
large upload
payment timeout
payment pending
backend restart
```

---

# 48. ANDROID LIFECYCLE TESTING

Test:

```text
background app
foreground app
screen rotation where applicable
process death
force stop
OS reclaim
battery saver
Doze
network switching
permission changes
location service disabled
notification disabled
```

The active trip must recover safely after lifecycle interruptions.

---

# 49. WEB FAILURE TESTING

Test:

```text
tab suspension
network disconnect
WebSocket reconnect
stale GraphQL cache
expired session
multiple tabs
slow API
large dataset
rapid filters
rapid map movement
browser resize
permission differences
```

---

# 50. TESTING STRATEGY

## Android

Implement:

```text
unit tests
ViewModel tests
UseCase tests
Repository tests
Room tests
network tests
Compose UI tests
integration tests
offline sync tests
location-service tests
```

## Web

Implement:

```text
unit tests
component tests
query/cache tests
WebSocket tests
integration tests
accessibility tests
visual regression where valuable
```

## E2E

Use:

```text
Maestro
Playwright
```

for critical flows.

---

# 51. CRITICAL E2E FLOW

Automate:

```text
SHIPPER

login
→ create shipment
→ receive quote
→ book
→ payment
→ payment confirmation
→ track
→ delivery confirmation
→ invoice
```

and:

```text
DRIVER

login
→ receive job
→ accept
→ start trip
→ disable network
→ update trip
→ capture POD
→ restore network
→ synchronize
→ verify backend state
```

and:

```text
OPS

login
→ view live operations
→ filter trips
→ open trip
→ observe live update
→ perform authorized override
→ confirm audit entry
```

---

# 52. CONTRACT TESTING

CI must detect backend/frontend contract drift.

Every build should validate:

```text
generated API models
OpenAPI/schema compatibility
enum changes
required fields
breaking response changes
WebSocket event schemas
```

If contract generation changes:

```text
CI should fail or explicitly surface the change.
```

---

# 53. MOCK/SANDBOX ENVIRONMENT

Create deterministic development environments for:

```text
successful payment
failed payment
pending payment
expired quote
duplicate booking
assignment race
offline trip
WebSocket disconnect
late event
out-of-order event
POD upload failure
server error
```

Do not rely exclusively on manually manipulating production-like systems.

---

# 54. FEATURE FLAGS

Implement centralized feature flags for:

```text
new booking flow
new tracking UI
new payment method
new POD experience
new driver workflow
new map implementation
experimental analytics
```

Feature flags must support:

```text
global enable/disable
environment
role
cohort
app version
```

where the backend/remote-config system supports it.

---

# 55. REMOTE KILL SWITCH

Implement a remote mechanism to disable critical client functionality.

Examples:

```text
disable new payment flow
disable experimental tracking
disable problematic upload path
disable specific feature
force minimum supported app version
```

The kill switch must fail safely if remote configuration cannot be reached.

---

# 56. RELEASE ENGINEERING

Android:

```text
debug
internal
staging
production
```

Use:

```text
5%
→ 25%
→ 100%
```

staged rollout.

Web:

```text
preview
staging
production
```

Every production deployment must be traceable to:

```text
commit
build
version
environment
deployment
```

---

# 57. CI/CD

CI must run:

```text
formatting
lint
static analysis
unit tests
integration tests
build
dependency checks
security checks
API contract validation
E2E tests where configured
```

Production deployment should require passing quality gates.

Do not allow:

```text
known failing tests
compile warnings hiding errors
contract mismatch
critical security vulnerabilities
```

to silently ship.

---

# 58. DEPENDENCY MANAGEMENT

Pin or constrain dependency versions appropriately.

Automate:

```text
dependency vulnerability scanning
outdated dependency detection
license checks
```

Do not blindly upgrade critical libraries without regression testing.

---

# 59. ENVIRONMENT CONFIGURATION

Support:

```text
development
test
staging
production
```

Never hardcode:

```text
API URLs
WebSocket URLs
keys
secrets
feature flags
environment-specific identifiers
```

Use secure environment configuration.

---

# 60. DATABASE MIGRATIONS

Room database migrations must be tested.

Never use destructive migrations in production without an explicit migration strategy.

Test:

```text
old version
→ new version
```

for every supported migration.

---

# 61. OFFLINE DATA RETENTION

Define explicit policies for:

```text
trip cache
location batches
POD files
documents
failed operations
temporary files
```

Do not retain sensitive data indefinitely.

Expired sensitive data must be cleaned safely.

---

# 62. PRIVACY

Minimize collection of personal information.

For location data:

```text
request only necessary permissions
explain why
track only when necessary
stop tracking when no longer required
```

Do not collect background location merely because it is technically available.

---

# 63. ACCOUNT DELETION

Implement an accessible account deletion flow.

The UI must communicate:

```text
what will be deleted
what may be retained for legal/accounting reasons
what happens next
```

Follow the backend's deletion/deactivation contract.

---

# 64. PERMISSIONS

Build proper permission UX for:

```text
location
notifications
camera
photos/files
```

Never assume permission was granted.

Handle:

```text
denied
temporarily denied
permanently denied
revoked later
```

Provide appropriate recovery instructions.

---

# 65. SECURITY OF DOCUMENTS

Documents may contain sensitive commercial information.

Requirements:

* secure local storage
* temporary-file cleanup
* authorization checks
* no unnecessary analytics
* no document content in logs
* protected previews where appropriate
* backend-controlled access

---

# 66. DESIGN FOR DRIVERS

Driver UI must prioritize:

```text
large primary action
minimal text
clear current state
high visibility
few steps
voice/navigation handoff where appropriate
safe interaction
```

Avoid complex interactions while the vehicle is moving.

Where appropriate, require the driver to perform sensitive actions only when stationary.

---

# 67. DRIVER TRIP STATE MACHINE

The frontend must model the backend state machine.

Example:

```text
ASSIGNED
 ↓
ACCEPTED
 ↓
EN_ROUTE_PICKUP
 ↓
AT_PICKUP
 ↓
LOADING
 ↓
PICKED_UP
 ↓
IN_TRANSIT
 ↓
AT_DELIVERY
 ↓
DELIVERING
 ↓
POD_CAPTURED
 ↓
DELIVERED
 ↓
COMPLETED
```

Do not permit UI actions that violate backend-defined transitions.

---

# 68. IDEMPOTENCY

All mutation flows must respect backend idempotency contracts.

Especially:

```text
booking creation
payment initiation
job acceptance
trip start
trip completion
POD submission
document upload
manual admin actions
```

The client must safely handle:

```text
request sent
response lost
retry
server already processed request
```

without creating duplicate business actions.

---

# 69. CONCURRENCY

Explicitly account for:

```text
multiple devices
multiple admins
multiple drivers
simultaneous edits
stale tabs
delayed responses
duplicate events
```

Use server version/revision fields if available.

Where conflicts occur:

```text
authoritative server state wins
```

and UI clearly communicates the conflict.

---

# 70. EMPTY STATES

Every list must have meaningful empty states.

Examples:

```text
No active trips
No bookings
No available jobs
No documents
No invoices
No notifications
No exceptions
```

Empty state must not look like an error.

---

# 71. SKELETONS AND LOADING UX

Use skeletons when layout stability benefits.

Use progress indicators for operations with measurable progress.

Use explicit status for long-running operations:

```text
Processing...
Uploading 62%
Syncing 3 pending actions
Waiting for payment confirmation
Reconnecting...
```

Never use indefinite generic spinners for long operations.

---

# 72. DESIGN FOR SLOW NETWORK

Test at:

```text
Fast
4G
3G
High latency
Packet loss
Offline
```

UI should remain usable during slow requests.

Cache what is appropriate.

Do not block the entire application because one endpoint is unavailable.

---

# 73. ADMIN PERFORMANCE TARGET

The dashboard must remain responsive while:

```text
hundreds of active trips
thousands of table rows
frequent location updates
multiple operators
```

are active.

Measure rather than assume performance.

Establish performance budgets and fail CI/review when severe regressions occur.

---

# 74. ACCESSIBILITY AND QUALITY GATES

Before release:

```text
critical flows accessible
keyboard navigable
screen-reader compatible
no critical console errors
no critical accessibility violations
no known P0/P1 UX defects
```

---

# 75. ERROR REPORTING UX

Every major error must produce:

```text
human-readable explanation
recovery action
support/debug identifier where appropriate
```

Example:

```text
We couldn't update the trip.

Your previous information is still saved.

Try again.

Reference: ABC-123
```

Do not show meaningless:

```text
Something went wrong.
```

without recovery information.

---

# 76. DEEP LINKING

Support deep links for:

```text
booking
trip
invoice
document
notification
payment
```

Behavior:

```text
not authenticated
→ authenticate
→ redirect to intended destination

authenticated + unauthorized
→ access denied

authenticated + authorized
→ open entity
```

---

# 77. PUSH + REAL-TIME + REST RECONCILIATION

Use each mechanism for its correct purpose:

```text
REST/GraphQL
    authoritative snapshots

WebSocket
    near-real-time updates

Push
    user notification

Room/browser cache
    resilience and performance

Optimistic state
    UX responsiveness only
```

Never treat one mechanism as a permanent replacement for the others.

---

# 78. DOCUMENTATION

Produce:

```text
README
Architecture Decision Records
Frontend architecture diagram
State-management documentation
Offline-sync documentation
WebSocket documentation
Authentication documentation
Release documentation
Environment setup
Testing guide
Troubleshooting guide
Feature-flag guide
```

Document every non-obvious production decision.

---

# 79. DEVELOPER EXPERIENCE

A new engineer should be able to:

```text
clone repository
install dependencies
configure environment
run Android app
run web dashboard
run tests
run local mocks
```

with documented commands.

No undocumented tribal knowledge.

---

# 80. CODE QUALITY

Require:

```text
strict TypeScript
Kotlin static analysis
consistent formatting
small focused modules
dependency inversion
testable business logic
no duplicated API models
no dead code
no commented-out production code
no TODOs for critical functionality
```

Avoid overengineering abstractions without actual reuse.

---

# 81. WHAT NOT TO DO

Do not:

* build only static screens
* fake backend responses in production code
* hardcode business logic from assumptions
* calculate financial truth locally
* infer delivery from GPS
* trust push notifications as state
* silently hide offline state
* silently retry forever
* store credentials insecurely
* use custom payment WebViews unnecessarily
* require network for POD capture
* make GPS run at maximum frequency permanently
* render thousands of map markers inefficiently
* reload an entire table after every event
* duplicate API types manually
* ignore Android lifecycle behavior
* ignore app permissions
* ship without crash reporting
* ship without tests
* ship without CI/CD
* treat the happy path as production readiness

---

# 82. REQUIRED DELIVERABLES

Deliver working source code for:

```text
1. Shipper Android application
2. Driver/Carrier Android application
3. Ops/Admin web dashboard
```

Also deliver:

```text
4. Shared API generation pipeline
5. Shared design/token definitions where appropriate
6. Automated test suites
7. E2E test suites
8. CI/CD pipelines
9. Environment configuration
10. Crash/observability integration
11. Feature flag infrastructure
12. Documentation
13. Deployment instructions
14. Production-readiness checklist
15. Security checklist
16. Accessibility checklist
17. Performance test results
18. Offline synchronization test results
```

---

# 83. DEFINITION OF DONE

The implementation is NOT considered complete because:

```text
screens render
buttons work
API calls return
```

It is complete only when:

### SHIPPER

```text
User can register/login
User can create shipment
User can obtain valid quote
Expired quotes are handled
User can book
Payment states are authoritative
User can track shipment
Stale connection is visible
Documents upload reliably
Notifications work
Invoices match backend
Account deletion exists
```

### DRIVER

```text
Driver can authenticate
Driver receives jobs
Job acceptance race is handled
Driver can start trip
Location tracking works correctly
Background tracking is compliant
Battery usage is controlled
Trip works offline
Operations queue locally
POD persists immediately
POD uploads asynchronously
Reconnect synchronizes state
Conflicts reconcile correctly
Earnings exactly match backend ledger
```

### OPS

```text
Admin authentication works
Role permissions work
Live map scales
Tables scale
Filters work
Real-time updates work
Stale state is visible
Manual overrides require confirmation
Reasons are captured
Audit records are visible
Analytics work
Session controls work
```

---

# 84. PRODUCTION ACCEPTANCE TESTS

Before declaring production ready, demonstrate all of the following.

## Test 1 — Driver Offline Trip

```text
Start trip online
↓
Disable network
↓
Continue trip
↓
Update required statuses
↓
Capture POD
↓
Kill application
↓
Restart application
↓
Restore network
↓
Synchronize
↓
Verify backend state
```

Expected:

```text
No lost critical actions.
No duplicate actions.
No corrupted POD.
Final state matches backend.
```

---

## Test 2 — Tracking Disconnect

```text
Open tracking
↓
Receive live location
↓
Disconnect WebSocket
↓
Wait beyond freshness threshold
```

Expected:

```text
UI explicitly shows stale/reconnecting state.
```

It must never appear continuously live.

---

## Test 3 — Payment Pending

```text
Initiate payment
↓
Gateway remains pending
↓
Backend has not confirmed success
```

Expected:

```text
Payment processing/pending
```

not:

```text
Payment successful
```

---

## Test 4 — Job Race

```text
Driver A accepts
Driver B accepts simultaneously
```

Expected:

```text
One authoritative winner.
Loser sees authoritative conflict.
```

No phantom assignment.

---

## Test 5 — Duplicate Request

```text
Send mutation
↓
Drop response
↓
Retry same operation
```

Expected:

```text
One backend operation.
```

---

## Test 6 — WebSocket Recovery

```text
Live dashboard
↓
Disconnect WebSocket
↓
Generate events
↓
Reconnect
```

Expected:

```text
Client resynchronizes authoritative state.
```

No permanently missing events.

---

## Test 7 — Android Process Death

```text
Active trip
↓
OS kills application process
↓
Restart application
```

Expected:

```text
Trip state restored.
Pending work preserved.
Tracking/sync recovers according to backend requirements.
```

---

## Test 8 — Large Ops Load

Test with:

```text
1,000+ active trips
10,000+ table records
high-frequency location events
```

Expected:

```text
No unacceptable UI stutter.
No uncontrolled memory growth.
No full-map rerender per location event.
```

Adjust scale targets to the backend's actual expected production volume.

---

# 85. FINANCIAL INTEGRITY TEST

Every displayed financial value must trace back to backend-authoritative data.

Automate assertions for:

```text
quote
booking price
payment amount
invoice
driver earnings
deductions
balance
```

Expected:

```text
Frontend displayed value === backend authoritative value
```

No client-side financial formula should independently become the source of truth.

---

# 86. OBSERVABILITY ACCEPTANCE

Before first external user:

```text
Crash reporting active
ANR monitoring active
Web error monitoring active
Network failure visibility active
Core funnel analytics active
Performance monitoring active
```

Verify events actually arrive in the production observability systems.

Do not merely install SDKs and assume they work.

---

# 87. RELEASE ACCEPTANCE

Execute at least one complete release through the real pipeline:

```text
commit
↓
CI
↓
tests
↓
build
↓
artifact
↓
staging
↓
E2E
↓
production approval
↓
Android staged rollout
↓
web deployment
↓
observability verification
```

A local build does not satisfy production readiness.

---

# 88. FINAL OUTPUT REQUIRED FROM THE ENGINEERING AGENT

At completion, provide:

## A. Implementation summary

What was actually built.

## B. Architecture

Explain:

```text
modules
data flow
state management
offline architecture
real-time architecture
authentication
```

## C. Repository tree

Show the actual resulting structure.

## D. API integration

List generated clients and backend contracts consumed.

## E. Feature matrix

```text
Feature | Shipper | Driver | Ops | Tested
```

## F. Testing matrix

```text
Test | Unit | Integration | E2E | Result
```

## G. Production-hardening matrix

```text
Concern | Implemented | Verified | Evidence
```

## H. Known limitations

Explicitly identify anything not completed.

Never claim something is production-ready if it was not actually implemented and tested.

## I. Deployment instructions

Provide exact commands/processes for:

```text
Android build
Android release
Web build
Web deployment
environment configuration
database/cache requirements if applicable
```

## J. Runbook

Document recovery procedures for:

```text
API outage
WebSocket outage
payment outage
push notification failure
location failure
sync backlog
crash spike
bad mobile release
bad web deployment
```

---

# 89. EXECUTION RULE

Work in the following order:

```text
PHASE 1
Inspect backend contracts and repository

PHASE 2
Validate API/event/auth assumptions

PHASE 3
Establish shared models and generated clients

PHASE 4
Establish design system

PHASE 5
Build authentication/session infrastructure

PHASE 6
Build shipper core flow

PHASE 7
Build driver core/offline/location flow

PHASE 8
Build ops dashboard

PHASE 9
Implement real-time synchronization

PHASE 10
Implement offline synchronization

PHASE 11
Implement payments/documents/POD

PHASE 12
Implement observability

PHASE 13
Implement security/privacy/accessibility

PHASE 14
Implement automated tests

PHASE 15
Implement E2E tests

PHASE 16
Implement CI/CD

PHASE 17
Run failure/resilience testing

PHASE 18
Performance test

PHASE 19
Security review

PHASE 20
Production acceptance

PHASE 21
Release through real pipeline
```

At each phase:

```text
implement
→ test
→ verify
→ fix
→ document
→ continue
```

Do not build the entire UI first and postpone production engineering.

---

# 90. ABSOLUTE SUCCESS CRITERIA

The final system must behave like a real logistics product rather than a collection of screens.

The most important invariant is:

> **The frontend must always communicate what the backend knows, what the client knows, and how fresh that knowledge is — without pretending uncertainty is certainty.**

For operational state:

```text
Backend truth
    ↓
Validated client state
    ↓
Visible freshness
    ↓
User action
```

For money:

```text
Backend ledger
    ↓
API response
    ↓
Typed model
    ↓
UI
```

For live tracking:

```text
Backend event
    ↓
WebSocket
    ↓
validated event
    ↓
state reconciliation
    ↓
freshness calculation
    ↓
map/timeline
```

For offline driver actions:

```text
Driver action
    ↓
durable local transaction
    ↓
pending queue
    ↓
network recovery
    ↓
idempotent server mutation
    ↓
authoritative response
    ↓
reconciliation
```

The system is production-ready only when these flows remain correct under **normal conditions, poor network conditions, lifecycle interruptions, concurrency, retries, delayed responses, and partial system failures.**

Do not optimize for a visually impressive demo.

Optimize for:

**correctness + resilience + observability + security + performance + accessibility + maintainability + operational trust.**
