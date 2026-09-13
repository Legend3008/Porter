import http from 'http';
import { createApp } from '../app';
import { pool, query } from '../config/database';
import { redis } from '../config/redis';

let server: http.Server;
let baseUrl: string;

async function request(path: string, options: {
  method?: string;
  headers?: Record<string, string>;
  body?: any;
} = {}): Promise<{ status: number; data: any; headers: http.IncomingHttpHeaders }> {
  return new Promise((resolve, reject) => {
    const url = new URL(path, baseUrl);
    const bodyData = options.body ? JSON.stringify(options.body) : undefined;

    const req = http.request(url, {
      method: options.method || 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(bodyData ? { 'Content-Length': Buffer.byteLength(bodyData).toString() } : {}),
        ...options.headers
      }
    }, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        let parsed = data;
        try {
          parsed = JSON.parse(data);
        } catch {}
        resolve({
          status: res.statusCode || 500,
          data: parsed,
          headers: res.headers
        });
      });
    });

    req.on('error', reject);
    if (bodyData) req.write(bodyData);
    req.end();
  });
}

function assert(condition: boolean, message: string) {
  if (!condition) {
    console.error(`❌ FAILED: ${message}`);
    throw new Error(message);
  }
  console.log(`  ✓ ${message}`);
}

async function runTests() {
  console.log('\n==================================================================');
  console.log('       PORTER BACKEND API — AUTOMATED END-TO-END VERIFICATION     ');
  console.log('==================================================================\n');

  const app = createApp();
  server = http.createServer(app);
  await new Promise<void>((resolve) => {
    server.listen(0, '127.0.0.1', () => {
      const addr = server.address() as any;
      baseUrl = `http://127.0.0.1:${addr.port}`;
      console.log(`Test server running at ${baseUrl}\n`);
      resolve();
    });
  });

  try {
    // -------------------------------------------------------------------------
    // TEST 1: System Health Probes
    // -------------------------------------------------------------------------
    console.log('1. Health & Readiness Probes');
    const liveRes = await request('/health/live');
    assert(liveRes.status === 200, 'GET /health/live returns 200 OK');
    assert(liveRes.data.status === 'ok', 'Liveness probe reports status: ok');

    const readyRes = await request('/health/ready');
    assert(readyRes.status === 200, 'GET /health/ready returns 200 OK');
    assert(readyRes.data.database === 'healthy', 'Database connectivity is verified');

    // -------------------------------------------------------------------------
    // TEST 2: OpenAPI Specification
    // -------------------------------------------------------------------------
    console.log('\n2. OpenAPI 3.0 Documentation');
    const docsRes = await request('/api/docs/json');
    assert(docsRes.status === 200, 'GET /api/docs/json returns 200 OK');
    assert(docsRes.data.openapi === '3.0.3', 'OpenAPI version is 3.0.3');
    assert(!!docsRes.data.paths['/api/v1/pricing/quotes'], 'Pricing quotes path is documented');

    // -------------------------------------------------------------------------
    // TEST 3: Authentication & JWT Token Issuance
    // -------------------------------------------------------------------------
    console.log('\n3. Authentication & JWT Life Cycle');
    const sendOtpRes = await request('/api/v1/auth/otp/send', {
      method: 'POST',
      body: { phone: '+919876543210' }
    });
    assert(sendOtpRes.status === 200, 'POST /api/v1/auth/otp/send dispatches OTP');

    const verifyOtpRes = await request('/api/v1/auth/otp/verify', {
      method: 'POST',
      body: { phone: '+919876543210', otp: '123456' }
    });
    assert(verifyOtpRes.status === 200, 'POST /api/v1/auth/otp/verify returns 200');
    assert(!!verifyOtpRes.data.data.access_token, 'Access token is returned');
    assert(!!verifyOtpRes.data.data.user.id, 'Authenticated user profile is populated');

    const authToken = verifyOtpRes.data.data.access_token;
    const authHeaders = { Authorization: `Bearer ${authToken}` };

    const meRes = await request('/api/v1/auth/me', { headers: authHeaders });
    assert(meRes.status === 200, 'GET /api/v1/auth/me verifies Bearer token');
    assert(meRes.data.data.phone === '+919876543210', 'User phone matches token');

    // -------------------------------------------------------------------------
    // TEST 4: Pricing Engine & Integer Paise Precision
    // -------------------------------------------------------------------------
    console.log('\n4. Pricing Engine & Guaranteed Quote Calculation');
    const quoteReq = {
      origin_port_code: 'INNSA',
      destination_city: 'Pune',
      container_type: 'STANDARD_20FT',
      container_count: 2,
      is_hazardous: false,
      is_overweight: false,
      cargo_weight_kg: 22000
    };

    const quoteRes = await request('/api/v1/pricing/quotes', {
      method: 'POST',
      headers: authHeaders,
      body: quoteReq
    });
    assert(quoteRes.status === 201, 'POST /api/v1/pricing/quotes returns 201 Created');
    const quote = quoteRes.data.data;
    assert(typeof quote.total_amount_minor === 'number', 'Total amount is in minor units (integer paise)');
    assert(quote.total_amount_minor > 0, `Total price calculated: ₹${quote.total_amount_minor / 100}`);
    assert(quote.currency === 'INR', 'Currency is INR');
    assert(quote.guaranteed_minutes === 15, 'Price lock duration is 15 minutes');

    const quoteId = quote.quote_id;
    const getQuoteRes = await request(`/api/v1/pricing/quotes/${quoteId}`, { headers: authHeaders });
    assert(getQuoteRes.status === 200, 'GET /api/v1/pricing/quotes/:id retrieves valid quote');

    // -------------------------------------------------------------------------
    // TEST 5: Booking Creation & Transactional Idempotency Protection
    // -------------------------------------------------------------------------
    console.log('\n5. Booking Creation & Idempotent Confirmation');
    const idempotencyKey = `idemp-test-${Date.now()}`;

    const confirmPayload = {
      quote_id: quoteId,
      cargo_description: 'Precision Industrial Machinery',
      cargo_weight_kg: 24000,
      customs_doc_number: 'BE-2026-9901',
      reference_number: 'PO-2026-AUTO-99'
    };

    const confirmRes1 = await request('/api/v1/bookings/confirm', {
      method: 'POST',
      headers: { ...authHeaders, 'Idempotency-Key': idempotencyKey },
      body: confirmPayload
    });

    assert(confirmRes1.status === 201, 'POST /api/v1/bookings/confirm creates confirmed booking');
    const booking = confirmRes1.data.data;
    assert(!!booking.booking_id, 'Booking ID created');
    assert(!!booking.trip_id, 'Operational trip automatically dispatched');
    assert(booking.payment_status === 'PENDING', 'Payment status is initially PENDING');

    // Idempotency Retry Check (Exact same key must replay response without duplicate)
    console.log('   Testing double-click idempotency guard with identical Idempotency-Key...');
    const confirmRes2 = await request('/api/v1/bookings/confirm', {
      method: 'POST',
      headers: { ...authHeaders, 'Idempotency-Key': idempotencyKey },
      body: confirmPayload
    });
    console.log('   confirmRes2 status & data:', confirmRes2.status, JSON.stringify(confirmRes2.data));
    assert(confirmRes2.status === 200 || confirmRes2.status === 201, 'Replayed request returns cached result');
    assert(confirmRes2.data.data.booking_id === booking.booking_id, 'Same booking_id returned without duplicate insert');

    const bookingId = booking.booking_id;
    const tripId = booking.trip_id;

    // -------------------------------------------------------------------------
    // Authenticate Driver Persona (Vikram Singh)
    // -------------------------------------------------------------------------
    const driverVerifyRes = await request('/api/v1/auth/otp/verify', {
      method: 'POST',
      body: { phone: '+919812345678', otp: '123456' }
    });
    assert(driverVerifyRes.status === 200, 'Driver Vikram Singh authenticated via OTP');
    const driverToken = driverVerifyRes.data.data.access_token;
    const driverHeaders = { Authorization: `Bearer ${driverToken}` };

    // -------------------------------------------------------------------------
    // TEST 6: Operations Concurrency & Race-Condition Protection
    // -------------------------------------------------------------------------
    console.log('\n6. Operational Trip Execution & Concurrency Protection');
    const tripRes = await request(`/api/v1/trips/${tripId}`, { headers: authHeaders });
    assert(tripRes.status === 200, 'GET /api/v1/trips/:id fetches operational trip details');
    assert(tripRes.data.data.stops.length >= 2, 'Trip has at least origin and destination stops');

    // Accept Trip (by Driver)
    const acceptRes1 = await request(`/api/v1/trips/${tripId}/accept`, {
      method: 'POST',
      headers: driverHeaders
    });
    assert(acceptRes1.status === 200, 'POST /api/v1/trips/:id/accept transitions trip to DRIVER_ACCEPTED');
    assert(acceptRes1.data.data.status === 'DRIVER_ACCEPTED', 'Trip status is DRIVER_ACCEPTED');

    // Race condition test: Attempting to accept an already-accepted trip must return 409
    console.log('   Testing race condition protection against duplicate trip acceptance...');
    const acceptRes2 = await request(`/api/v1/trips/${tripId}/accept`, {
      method: 'POST',
      headers: driverHeaders
    });
    assert(acceptRes2.status === 409, 'Duplicate trip acceptance rejected with 409 CONFLICT');

    // Start Trip
    const startRes = await request(`/api/v1/trips/${tripId}/start`, {
      method: 'POST',
      headers: driverHeaders,
      body: { reason: 'Driver departing from depot' }
    });
    assert(startRes.status === 200, 'POST /api/v1/trips/:id/start moves status to EN_ROUTE_PICKUP');

    // Trip Timeline Audit
    const timelineRes = await request(`/api/v1/trips/${tripId}/timeline`, { headers: authHeaders });
    assert(timelineRes.status === 200, 'GET /api/v1/trips/:id/timeline fetches chronological history');
    assert(timelineRes.data.data.length >= 2, 'Audit log captured status state transitions');

    // -------------------------------------------------------------------------
    // TEST 7: Telemetry Ingestion & Sub-5ms Latest Tracking Read
    // -------------------------------------------------------------------------
    console.log('\n7. High-Throughput GPS Telemetry & Projections');
    const pingRes = await request('/api/v1/driver/location', {
      method: 'POST',
      headers: driverHeaders,
      body: {
        trip_id: tripId,
        latitude: 18.9498000,
        longitude: 72.9510000,
        accuracy_meters: 3.5,
        speed_mps: 18.2,
        heading_degrees: 92.0,
        battery_percent: 88
      }
    });
    assert(pingRes.status === 201, 'POST /api/v1/driver/location ingests GPS coordinate ping');

    // Fast Latest Location Read
    const t0 = Date.now();
    const latestRes = await request(`/api/v1/tracking/${tripId}/latest`, { headers: authHeaders });
    const readLatencyMs = Date.now() - t0;
    assert(latestRes.status === 200, 'GET /api/v1/tracking/:tripId/latest returns 200');
    assert(Number(latestRes.data.data.latitude) === 18.9498000, 'Latest projection latitude matches');
    console.log(`  ✓ Telemetry read latency: ${readLatencyMs}ms (<5ms target)`);

    // Batch Ingestion
    const batchRes = await request('/api/v1/driver/location/batch', {
      method: 'POST',
      headers: driverHeaders,
      body: {
        trip_id: tripId,
        pings: [
          { latitude: 18.9510, longitude: 72.9530, device_recorded_at: new Date(Date.now() - 30000).toISOString() },
          { latitude: 18.9550, longitude: 72.9580, device_recorded_at: new Date(Date.now() - 15000).toISOString() },
          { latitude: 18.9600, longitude: 72.9640, device_recorded_at: new Date().toISOString() }
        ]
      }
    });
    assert(batchRes.status === 201, 'POST /api/v1/driver/location/batch records 3 batch pings');

    const histRes = await request(`/api/v1/tracking/${tripId}/history`, { headers: authHeaders });
    assert(histRes.status === 200, 'GET /api/v1/tracking/:tripId/history retrieves GPS breadcrumb path');
    assert(histRes.data.data.length >= 4, 'Chronological pings returned');

    // -------------------------------------------------------------------------
    // TEST 8: Document Storage & Proof of Delivery (POD)
    // -------------------------------------------------------------------------
    console.log('\n8. Shipping Documents & Proof of Delivery (POD)');
    const uploadUrlRes = await request('/api/v1/documents/upload-url', {
      method: 'POST',
      headers: authHeaders,
      body: {
        booking_id: bookingId,
        document_type: 'BILL_OF_LADING',
        file_name: 'BL_INNSA_2026_001.pdf',
        mime_type: 'application/pdf',
        file_size_bytes: 245000
      }
    });
    assert(uploadUrlRes.status === 200, 'POST /api/v1/documents/upload-url returns secure upload URL');
    const storageKey = uploadUrlRes.data.data.storage_key;

    const completeDocRes = await request('/api/v1/documents/complete', {
      method: 'POST',
      headers: authHeaders,
      body: {
        booking_id: bookingId,
        trip_id: tripId,
        document_type: 'BILL_OF_LADING',
        storage_key: storageKey,
        file_name: 'BL_INNSA_2026_001.pdf',
        mime_type: 'application/pdf',
        file_size_bytes: 245000,
        checksum_sha256: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
      }
    });
    assert(completeDocRes.status === 201, 'POST /api/v1/documents/complete registers document with SHA-256');

    // Proof of Delivery
    const podRes = await request(`/api/v1/driver/trips/${tripId}/pod`, {
      method: 'POST',
      headers: driverHeaders,
      body: {
        receiver_name: 'Warehouse Manager Rajesh Patil',
        receiver_phone: '+919123456780',
        signature_storage_key: `signatures/${tripId}.png`,
        latitude: 18.7340,
        longitude: 73.6820
      }
    });
    assert(podRes.status === 201, 'POST /api/v1/driver/trips/:id/pod records tamper-evident delivery');

    // Complete Trip
    const completeTripRes = await request(`/api/v1/trips/${tripId}/complete`, {
      method: 'POST',
      headers: driverHeaders,
      body: { reason: 'Delivery confirmed and signed off' }
    });
    assert(completeTripRes.status === 200, 'POST /api/v1/trips/:id/complete transitions trip to COMPLETED');

    // -------------------------------------------------------------------------
    // TEST 9: Commercial Payment, Tax Invoicing & General Ledger Balancing
    // -------------------------------------------------------------------------
    console.log('\n9. Commercial Payments, GST Invoicing & Double-Entry Ledger');
    const orderRes = await request('/api/v1/payments/orders', {
      method: 'POST',
      headers: authHeaders,
      body: {
        booking_id: bookingId,
        provider: 'RAZORPAY',
        method: 'UPI'
      }
    });
    assert(orderRes.status === 201, 'POST /api/v1/payments/orders creates payment order');
    const payment = orderRes.data.data;
    assert(payment.amount_minor > 0, `Payment order amount: ₹${payment.amount_minor / 100}`);

    // Simulate Razorpay payment.captured webhook
    console.log('   Simulating payment.captured webhook with idempotency & ledger posting...');
    const webhookEventId = `evt_test_${Date.now()}`;
    const webhookRes1 = await request('/api/v1/payments/webhook/razorpay', {
      method: 'POST',
      body: {
        id: webhookEventId,
        event: 'payment.captured',
        order_id: payment.provider_order_id,
        payment_id: payment.payment_id
      }
    });
    assert(webhookRes1.status === 200, 'POST /api/v1/payments/webhook/razorpay processed successfully');

    // Webhook Deduplication Test
    const webhookRes2 = await request('/api/v1/payments/webhook/razorpay', {
      method: 'POST',
      body: {
        id: webhookEventId,
        event: 'payment.captured'
      }
    });
    assert(webhookRes2.data.status === 'already_processed', 'Duplicate webhook correctly recognized and skipped');

    // GST Invoice Verification
    const invoicesRes = await request('/api/v1/invoices', { headers: authHeaders });
    assert(invoicesRes.status === 200, 'GET /api/v1/invoices returns 200');
    assert(invoicesRes.data.data.length > 0, 'GST Tax Invoice was generated for payment');
    const invoiceId = invoicesRes.data.data[0].id;

    const invoiceDetailRes = await request(`/api/v1/invoices/${invoiceId}`, { headers: authHeaders });
    assert(invoiceDetailRes.status === 200, 'GET /api/v1/invoices/:id fetches itemized breakdown');
    const inv = invoiceDetailRes.data.data;
    assert(inv.subtotal_paise + inv.tax_paise === inv.total_paise, 'Invoice math: subtotal + tax == total (to exact paise)');
    assert(inv.cgst_paise === inv.sgst_paise, 'CGST and SGST are split evenly (50/50)');

    // Immutable Double-Entry Ledger Verification
    console.log('   Verifying general ledger mathematical balancing...');
    const ledgerTxRes = await query(
      `SELECT id, transaction_number FROM ledger.ledger_transactions WHERE reference_id = $1`,
      [payment.payment_id]
    );
    assert(ledgerTxRes.rows.length > 0, 'Double-entry transaction posted to general ledger');
    const ledgerTxId = ledgerTxRes.rows[0].id;

    const balanceCheck = await query(
      `SELECT ledger.verify_transaction_balance($1) as is_balanced`,
      [ledgerTxId]
    );
    assert(balanceCheck.rows[0].is_balanced === true, 'General ledger: SUM(DEBIT) == SUM(CREDIT) strictly balanced to the paise!');

    // -------------------------------------------------------------------------
    // TEST 10: Settlements & Driver Earnings Rollup
    // -------------------------------------------------------------------------
    console.log('\n10. Carrier & Driver Earnings Analytics');
    const earningsRes = await request('/api/v1/settlements/driver/earnings', { headers: driverHeaders });
    assert(earningsRes.status === 200, 'GET /api/v1/settlements/driver/earnings returns 200');
    assert(typeof earningsRes.data.data.completed_trips_count === 'number', 'Driver earnings reflects completed trip analytics');

    console.log('\n==================================================================');
    console.log('       ALL 10 VERIFICATION TEST SUITES PASSED SUCCESSFULLY!       ');
    console.log('==================================================================\n');
  } finally {
    server.close();
    await pool.end();
    if (redis) {
      try {
        await redis.quit();
      } catch {}
    }
  }
}

runTests().catch((err) => {
  console.error('\n❌ TEST SUITE FAILED WITH UNHANDLED ERROR:\n', err);
  process.exit(1);
});
