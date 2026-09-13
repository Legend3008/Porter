import { Server as HttpServer } from 'http';
import { WebSocketServer, WebSocket } from 'ws';
import jwt from 'jsonwebtoken';
import { env } from '../config/env';
import { redis } from '../config/redis';

interface AuthenticatedClient {
  ws: WebSocket;
  userId?: string;
  isAlive: boolean;
  subscriptions: Set<string>; // set of tripIds
}

export class TrackingWebSocketServer {
  private wss: WebSocketServer;
  private clients: Map<WebSocket, AuthenticatedClient> = new Map();
  private redisSubClient: any = null;

  constructor(server: HttpServer) {
    this.wss = new WebSocketServer({ server, path: '/ws/tracking' });

    this.initWebSocket();
    this.initRedisSubscription();
    this.startHeartbeat();
  }

  private initWebSocket() {
    this.wss.on('connection', (ws: WebSocket, req) => {
      // Parse token from query string if present
      let userId: string | undefined;
      const url = new URL(req.url || '', `http://${req.headers.host || 'localhost'}`);
      const token = url.searchParams.get('token');

      if (token) {
        try {
          const decoded = jwt.verify(token, env.JWT_ACCESS_SECRET) as any;
          userId = decoded.userId;
        } catch {
          // Token invalid, can still authenticate via message
        }
      }

      const clientState: AuthenticatedClient = {
        ws,
        userId,
        isAlive: true,
        subscriptions: new Set()
      };

      this.clients.set(ws, clientState);

      ws.send(JSON.stringify({
        type: 'connected',
        authenticated: !!userId,
        server_time: new Date().toISOString()
      }));

      ws.on('pong', () => {
        const c = this.clients.get(ws);
        if (c) c.isAlive = true;
      });

      ws.on('message', (message: string) => {
        try {
          const data = JSON.parse(message.toString());
          this.handleClientMessage(ws, data);
        } catch {
          ws.send(JSON.stringify({ type: 'error', message: 'Invalid JSON message' }));
        }
      });

      ws.on('close', () => {
        this.clients.delete(ws);
      });

      ws.on('error', () => {
        this.clients.delete(ws);
      });
    });
  }

  private handleClientMessage(ws: WebSocket, data: any) {
    const client = this.clients.get(ws);
    if (!client) return;

    switch (data.type) {
      case 'auth':
        if (data.token) {
          try {
            const decoded = jwt.verify(data.token, env.JWT_ACCESS_SECRET) as any;
            client.userId = decoded.userId;
            ws.send(JSON.stringify({ type: 'auth_success', userId: client.userId }));
          } catch {
            ws.send(JSON.stringify({ type: 'auth_error', message: 'Invalid or expired JWT' }));
          }
        }
        break;

      case 'subscribe':
        if (data.trip_id) {
          client.subscriptions.add(data.trip_id);
          ws.send(JSON.stringify({ type: 'subscribed', trip_id: data.trip_id }));
        }
        break;

      case 'unsubscribe':
        if (data.trip_id) {
          client.subscriptions.delete(data.trip_id);
          ws.send(JSON.stringify({ type: 'unsubscribed', trip_id: data.trip_id }));
        }
        break;

      case 'ping':
        ws.send(JSON.stringify({ type: 'pong', timestamp: Date.now() }));
        break;

      default:
        ws.send(JSON.stringify({ type: 'unknown_type', supported: ['auth', 'subscribe', 'unsubscribe', 'ping'] }));
    }
  }

  // Subscribe to Redis Pub/Sub for multi-instance horizontal scaling
  private initRedisSubscription() {
    if (redis) {
      try {
        this.redisSubClient = redis.duplicate();
        this.redisSubClient.psubscribe('tracking:trip:*', (err: any) => {
          if (!err) {
            console.log(JSON.stringify({ level: 'INFO', message: 'WebSocket server subscribed to tracking:trip:* Redis channel' }));
          }
        });

        this.redisSubClient.on('pmessage', (_pattern: string, channel: string, message: string) => {
          // Extract trip_id from tracking:trip:<tripId>
          const parts = channel.split(':');
          const tripId = parts[2];
          if (tripId) {
            try {
              const pingData = JSON.parse(message);
              this.broadcastTripLocation(tripId, pingData);
            } catch {
              // ignore
            }
          }
        });
      } catch {
        // Fallback without Redis Pub/Sub
      }
    }
  }

  public broadcastTripLocation(tripId: string, locationData: any) {
    const payload = JSON.stringify({
      type: 'location_update',
      trip_id: tripId,
      data: locationData,
      broadcast_at: new Date().toISOString()
    });

    for (const [ws, client] of this.clients.entries()) {
      if (ws.readyState === WebSocket.OPEN && client.subscriptions.has(tripId)) {
        ws.send(payload);
      }
    }
  }

  private startHeartbeat() {
    setInterval(() => {
      for (const [ws, client] of this.clients.entries()) {
        if (!client.isAlive) {
          ws.terminate();
          this.clients.delete(ws);
          continue;
        }
        client.isAlive = false;
        ws.ping();
      }
    }, 30000);
  }

  public close() {
    if (this.redisSubClient) {
      try {
        this.redisSubClient.quit();
      } catch {}
    }
    this.wss.close();
  }
}
