import http from 'http';
import { createApp } from './app';
import { env } from './config/env';
import { pool } from './config/database';
import { redis } from './config/redis';
import { TrackingWebSocketServer } from './websocket/tracking-server';

async function bootstrap() {
  const app = createApp();
  const server = http.createServer(app);

  // Attach Real-Time Telemetry WebSocket Server
  const wsServer = new TrackingWebSocketServer(server);

  server.listen(env.PORT, () => {
    console.log(JSON.stringify({
      level: 'INFO',
      message: `Porter Container Logistics API running on port ${env.PORT}`,
      environment: env.NODE_ENV,
      endpoints: {
        http: `http://localhost:${env.PORT}`,
        docs: `http://localhost:${env.PORT}/api/docs`,
        websocket: `ws://localhost:${env.PORT}/ws/tracking`,
        live: `http://localhost:${env.PORT}/health/live`,
        ready: `http://localhost:${env.PORT}/health/ready`
      }
    }));
  });

  // Graceful shutdown
  const shutdown = async (signal: string) => {
    console.log(JSON.stringify({ level: 'INFO', message: `Received ${signal}, initiating graceful drain...` }));

    // Stop accepting new connections
    server.close(async () => {
      console.log(JSON.stringify({ level: 'INFO', message: 'HTTP server closed' }));

      try {
        wsServer.close();
        console.log(JSON.stringify({ level: 'INFO', message: 'WebSocket server closed' }));

        await pool.end();
        console.log(JSON.stringify({ level: 'INFO', message: 'Database connection pool drained' }));

        if (redis) {
          await redis.quit();
          console.log(JSON.stringify({ level: 'INFO', message: 'Redis connection closed' }));
        }

        process.exit(0);
      } catch (err) {
        console.error(JSON.stringify({ level: 'ERROR', message: 'Error during graceful shutdown', error: String(err) }));
        process.exit(1);
      }
    });

    // Force exit after 10s if connections fail to close
    setTimeout(() => {
      console.error(JSON.stringify({ level: 'ERROR', message: 'Shutdown timeout exceeded, forcing process exit' }));
      process.exit(1);
    }, 10000);
  };

  process.on('SIGTERM', () => shutdown('SIGTERM'));
  process.on('SIGINT', () => shutdown('SIGINT'));
}

bootstrap().catch((err) => {
  console.error(JSON.stringify({ level: 'FATAL', message: 'Bootstrap failed', error: String(err) }));
  process.exit(1);
});
