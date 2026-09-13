import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { requestIdMiddleware } from './common/middleware/request-id';
import { requestLogger } from './common/middleware/logger';
import { errorHandler } from './common/errors/error-handler';
import { checkDatabaseHealth } from './config/database';
import { getRedisStatus } from './config/redis';
import { AppError } from './common/errors/app-error';
import { ErrorCode } from './common/errors/error-codes';

// Routers
import { authRouter } from './modules/auth/auth.router';
import { pricingRouter } from './modules/pricing/pricing.router';
import { bookingRouter } from './modules/booking/booking.router';
import { tripsRouter } from './modules/operations/trips.router';
import { driverRouter } from './modules/driver/driver.router';
import { trackingRouter } from './modules/tracking/tracking.router';
import { documentsRouter } from './modules/documents/documents.router';
import { paymentsRouter } from './modules/payments/payments.router';
import { invoicesRouter } from './modules/billing/invoices.router';
import { settlementsRouter } from './modules/settlement/settlements.router';
import { adminRouter } from './modules/admin/admin.router';
import { swaggerRouter } from './openapi/swagger';

export function createApp(): express.Application {
  const app = express();

  // Security and core middleware
  app.use(helmet({
    contentSecurityPolicy: false // Allows Swagger UI inline assets
  }));
  app.use(cors({
    origin: true,
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Request-ID', 'Idempotency-Key']
  }));
  app.use(express.json({ limit: '10mb' }));
  app.use(express.urlencoded({ extended: true, limit: '10mb' }));

  // Correlation and Structured Observability
  app.use(requestIdMiddleware);
  app.use(requestLogger);

  // Health and Readiness probes (Kubernetes / ECS ready)
  app.get('/health/live', (_req, res) => {
    res.status(200).json({ status: 'ok', time: new Date().toISOString() });
  });

  app.get('/health/ready', async (_req, res) => {
    const dbHealthy = await checkDatabaseHealth();
    const redisStatus = getRedisStatus();

    if (!dbHealthy) {
      return res.status(503).json({
        status: 'degraded',
        database: 'unreachable',
        redis: redisStatus.available ? 'healthy' : 'disconnected',
        time: new Date().toISOString()
      });
    }

    return res.status(200).json({
      status: 'ready',
      database: 'healthy',
      redis: redisStatus.available ? 'healthy' : 'fallback-memory',
      time: new Date().toISOString()
    });
  });

  // Swagger UI Documentation
  app.use('/api/docs', swaggerRouter);

  // API v1 Domain Mounts
  app.use('/api/v1/auth', authRouter);
  app.use('/api/v1/pricing', pricingRouter);
  app.use('/api/v1/bookings', bookingRouter);
  app.use('/api/v1/trips', tripsRouter);
  app.use('/api/v1/driver', driverRouter);
  app.use('/api/v1/tracking', trackingRouter);
  app.use('/api/v1/documents', documentsRouter);
  app.use('/api/v1/payments', paymentsRouter);
  app.use('/api/v1/invoices', invoicesRouter);
  app.use('/api/v1/settlements', settlementsRouter);
  app.use('/api/v1/admin', adminRouter);

  // Fallback 404 for undefined routes
  app.use((req, _res, next) => {
    next(new AppError(`Endpoint ${req.method} ${req.path} not found`, 404, ErrorCode.NOT_FOUND));
  });

  // Centralized Error Handling Envelope
  app.use(errorHandler);

  return app;
}
