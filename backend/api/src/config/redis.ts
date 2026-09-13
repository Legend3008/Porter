import { Redis } from "ioredis";
import { env } from "./env.js";

let redisClient: Redis | null = null;
let isRedisAvailable = false;

try {
  redisClient = new Redis(env.REDIS_URL, {
    maxRetriesPerRequest: 1,
    retryStrategy(times) {
      if (times > 3) return null; // Stop retrying quickly to avoid blocking
      return Math.min(times * 100, 1000);
    },
    lazyConnect: true,
  });

  redisClient.on("connect", () => {
    isRedisAvailable = true;
    console.log(JSON.stringify({ level: "INFO", message: "Redis connected" }));
  });

  redisClient.on("error", (err) => {
    isRedisAvailable = false;
    // Log once, avoid log flooding
  });

  redisClient.connect().catch(() => {
    isRedisAvailable = false;
  });
} catch {
  isRedisAvailable = false;
}

export const redis = redisClient;

export function getRedisStatus(): { available: boolean } {
  return { available: isRedisAvailable && redisClient?.status === "ready" };
}

export async function cacheGet(key: string): Promise<string | null> {
  if (!isRedisAvailable || !redisClient) return null;
  try {
    return await redisClient.get(key);
  } catch {
    return null;
  }
}

export async function cacheSet(key: string, value: string, ttlSeconds?: number): Promise<void> {
  if (!isRedisAvailable || !redisClient) return;
  try {
    if (ttlSeconds) {
      await redisClient.set(key, value, "EX", ttlSeconds);
    } else {
      await redisClient.set(key, value);
    }
  } catch {
    // Ignore cache set failure
  }
}

export async function cachePublish(channel: string, message: string): Promise<void> {
  if (!isRedisAvailable || !redisClient) return;
  try {
    await redisClient.publish(channel, message);
  } catch {
    // Ignore pub failure
  }
}
