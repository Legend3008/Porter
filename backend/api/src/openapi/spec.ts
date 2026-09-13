export const openApiSpec = {
  openapi: '3.0.3',
  info: {
    title: 'Porter Container Logistics Platform API',
    version: '1.0.0',
    description: `
Mission-critical, production-grade REST and Realtime WebSocket API for container logistics, port haulage, and freight operations across India.

### Architectural Guarantees:
- **Integer Currency**: All monetary values are strictly in **integer paise** (₹1.00 = 100 paise). Floating-point currency is strictly forbidden.
- **Transactional Idempotency**: Mutating requests accept \`Idempotency-Key\` header to guarantee exactly-once processing.
- **Optimistic Concurrency**: Driver and trip mutations are guarded against double-booking race conditions with \`SELECT FOR UPDATE\` and atomic versioning.
- **Append-Only Ledger**: Immutable double-entry bookkeeping (\`SUM(debit) == SUM(credit)\`) for financial settlement.
- **High-Throughput Telemetry**: Partitioned monthly GPS location pings with sub-5ms latest-position reads via Redis projections.
    `,
    contact: {
      name: 'Porter Engineering Team',
      url: 'https://porter-logistics.in'
    }
  },
  servers: [
    { url: 'http://localhost:8080', description: 'Local Development Server' },
    { url: 'https://api.porter-logistics.in', description: 'Production Gateway' }
  ],
  components: {
    securitySchemes: {
      BearerAuth: {
        type: 'http',
        scheme: 'bearer',
        bearerFormat: 'JWT',
        description: 'Pass JWT access token obtained from /api/v1/auth/otp/verify'
      },
      IdempotencyHeader: {
        type: 'apiKey',
        in: 'header',
        name: 'Idempotency-Key',
        description: 'Unique client transaction UUID for double-click and network retry deduplication'
      }
    },
    schemas: {
      ApiResponse: {
        type: 'object',
        properties: {
          data: { type: 'object' },
          meta: {
            type: 'object',
            properties: {
              request_id: { type: 'string', format: 'uuid' },
              timestamp: { type: 'string', format: 'date-time' }
            }
          }
        }
      },
      ApiError: {
        type: 'object',
        properties: {
          error: {
            type: 'object',
            properties: {
              code: { type: 'string', example: 'VALIDATION_ERROR' },
              message: { type: 'string', example: 'Invalid input payload' },
              request_id: { type: 'string', format: 'uuid' },
              status: { type: 'integer', example: 400 },
              details: { type: 'object' }
            },
            required: ['code', 'message', 'request_id', 'status']
          }
        }
      },
      QuoteRequest: {
        type: 'object',
        required: ['origin_port_code', 'destination_city', 'container_type', 'container_count'],
        properties: {
          origin_port_code: { type: 'string', example: 'INNSA' },
          destination_city: { type: 'string', example: 'Pune' },
          container_type: { type: 'string', enum: ['STANDARD_20FT', 'STANDARD_40FT', 'HIGH_CUBE_40FT', 'REEFER_20FT', 'REEFER_40FT', 'FLAT_RACK_20FT', 'FLAT_RACK_40FT', 'OPEN_TOP_20FT', 'OPEN_TOP_40FT'], example: 'STANDARD_20FT' },
          container_count: { type: 'integer', minimum: 1, maximum: 50, example: 2 },
          is_hazardous: { type: 'boolean', default: false },
          is_overweight: { type: 'boolean', default: false },
          cargo_weight_kg: { type: 'integer', example: 22000 }
        }
      },
      QuoteResponse: {
        type: 'object',
        properties: {
          quote_id: { type: 'string', format: 'uuid' },
          origin_port_code: { type: 'string', example: 'INNSA' },
          destination_city: { type: 'string', example: 'Pune' },
          distance_km: { type: 'number', example: 148.5 },
          base_rate_minor: { type: 'integer', description: 'Paise', example: 2800000 },
          surcharges_minor: { type: 'integer', description: 'Paise', example: 350000 },
          subtotal_minor: { type: 'integer', description: 'Paise', example: 3150000 },
          gst_minor: { type: 'integer', description: 'Paise (18%)', example: 567000 },
          total_amount_minor: { type: 'integer', description: 'Total in paise', example: 3717000 },
          expires_at: { type: 'string', format: 'date-time' },
          guaranteed_minutes: { type: 'integer', example: 15 }
        }
      },
      BookingConfirmRequest: {
        type: 'object',
        required: ['quote_id'],
        properties: {
          quote_id: { type: 'string', format: 'uuid' },
          pickup_location_id: { type: 'string', format: 'uuid' },
          delivery_location_id: { type: 'string', format: 'uuid' },
          cargo_description: { type: 'string', example: 'Automotive Parts & Components' },
          cargo_weight_kg: { type: 'integer', example: 24000 },
          customs_doc_number: { type: 'string', example: 'BE-2026-889104' },
          reference_number: { type: 'string', example: 'PO-2026-00441' }
        }
      },
      LocationPingRequest: {
        type: 'object',
        required: ['trip_id', 'latitude', 'longitude'],
        properties: {
          trip_id: { type: 'string', format: 'uuid' },
          latitude: { type: 'number', minimum: -90, maximum: 90, example: 18.9498 },
          longitude: { type: 'number', minimum: -180, maximum: 180, example: 72.9510 },
          accuracy_meters: { type: 'number', example: 4.5 },
          speed_mps: { type: 'number', example: 16.2 },
          heading_degrees: { type: 'number', example: 88.5 },
          battery_percent: { type: 'integer', example: 85 }
        }
      }
    }
  },
  paths: {
    '/health/live': {
      get: {
        summary: 'Liveness probe',
        responses: {
          '200': { description: 'Process is alive' }
        }
      }
    },
    '/health/ready': {
      get: {
        summary: 'Readiness probe',
        description: 'Checks connectivity to PostgreSQL and Redis pool',
        responses: {
          '200': { description: 'System is healthy and ready to serve traffic' },
          '503': { description: 'Database or critical dependency is down' }
        }
      }
    },
    '/api/v1/auth/otp/send': {
      post: {
        summary: 'Send OTP for mobile login',
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: {
                type: 'object',
                required: ['phone'],
                properties: {
                  phone: { type: 'string', example: '+919876543210' }
                }
              }
            }
          }
        },
        responses: {
          '200': { description: 'OTP dispatched via SMS provider' }
        }
      }
    },
    '/api/v1/auth/otp/verify': {
      post: {
        summary: 'Verify OTP and return JWT access and refresh tokens',
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: {
                type: 'object',
                required: ['phone', 'otp'],
                properties: {
                  phone: { type: 'string', example: '+919876543210' },
                  otp: { type: 'string', example: '123456' }
                }
              }
            }
          }
        },
        responses: {
          '200': { description: 'Authenticated successfully' }
        }
      }
    },
    '/api/v1/pricing/quotes': {
      post: {
        summary: 'Generate guaranteed freight price quote (locked for 15 minutes)',
        security: [{ BearerAuth: [] }],
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: { $ref: '#/components/schemas/QuoteRequest' }
            }
          }
        },
        responses: {
          '201': { description: 'Quote generated in paise', content: { 'application/json': { schema: { $ref: '#/components/schemas/QuoteResponse' } } } }
        }
      }
    },
    '/api/v1/bookings': {
      get: {
        summary: 'List bookings for authenticated organization',
        security: [{ BearerAuth: [] }],
        responses: { '200': { description: 'List of bookings' } }
      }
    },
    '/api/v1/bookings/confirm': {
      post: {
        summary: 'Idempotent booking confirmation',
        security: [{ BearerAuth: [] }, { IdempotencyHeader: [] }],
        requestBody: {
          required: true,
          content: {
            'application/json': { schema: { $ref: '#/components/schemas/BookingConfirmRequest' } }
          }
        },
        responses: {
          '201': { description: 'Booking confirmed and operational trip dispatched' }
        }
      }
    },
    '/api/v1/trips/{id}': {
      get: {
        summary: 'Get operational trip details, containers, route stops, and telemetry',
        security: [{ BearerAuth: [] }],
        parameters: [{ in: 'path', name: 'id', required: true, schema: { type: 'string', format: 'uuid' } }],
        responses: { '200': { description: 'Trip details' } }
      }
    },
    '/api/v1/trips/{id}/accept': {
      post: {
        summary: 'Accept trip assignment (concurrency-protected via FOR UPDATE and version lock)',
        security: [{ BearerAuth: [] }],
        parameters: [{ in: 'path', name: 'id', required: true, schema: { type: 'string', format: 'uuid' } }],
        responses: {
          '200': { description: 'Trip accepted' },
          '409': { description: 'Trip already accepted by another driver' }
        }
      }
    },
    '/api/v1/driver/location': {
      post: {
        summary: 'Ingest high-throughput GPS telemetry ping',
        security: [{ BearerAuth: [] }],
        requestBody: {
          required: true,
          content: { 'application/json': { schema: { $ref: '#/components/schemas/LocationPingRequest' } } }
        },
        responses: { '201': { description: 'Ping recorded in partitioned table and cached in Redis' } }
      }
    },
    '/api/v1/tracking/{tripId}/latest': {
      get: {
        summary: 'Sub-5ms latest location lookup (Redis projection cache)',
        security: [{ BearerAuth: [] }],
        parameters: [{ in: 'path', name: 'tripId', required: true, schema: { type: 'string', format: 'uuid' } }],
        responses: { '200': { description: 'Latest location coordinates and speed' } }
      }
    },
    '/api/v1/payments/orders': {
      post: {
        summary: 'Create commercial payment order for booking',
        security: [{ BearerAuth: [] }],
        responses: { '201': { description: 'Payment order created with gateway reference' } }
      }
    },
    '/api/v1/payments/webhook/razorpay': {
      post: {
        summary: 'Payment gateway webhook (deduplicated, generates GST invoice & double-entry ledger)',
        responses: { '200': { description: 'Webhook acknowledged and processed' } }
      }
    },
    '/api/v1/invoices/{id}': {
      get: {
        summary: 'Get GST tax invoice with line items in paise',
        security: [{ BearerAuth: [] }],
        parameters: [{ in: 'path', name: 'id', required: true, schema: { type: 'string', format: 'uuid' } }],
        responses: { '200': { description: 'Tax invoice data' } }
      }
    },
    '/api/v1/carrier/settlements': {
      get: {
        summary: 'Carrier freight earnings settlement records',
        security: [{ BearerAuth: [] }],
        responses: { '200': { description: 'List of carrier settlements' } }
      }
    }
  }
};
