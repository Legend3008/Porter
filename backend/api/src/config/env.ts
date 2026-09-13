import { z } from "zod";
import dotenv from "dotenv";

dotenv.config();

const envSchema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  PORT: z.coerce.number().default(8080),
  HOST: z.string().default("0.0.0.0"),
  DATABASE_URL: z.string().default("postgresql://porter_admin:porter_secure_password_2026@localhost:5432/porter_db"),
  REDIS_URL: z.string().default("redis://localhost:6379"),
  JWT_ACCESS_SECRET: z.string().default("porter_dev_access_secret_super_secure_key_2026"),
  JWT_REFRESH_SECRET: z.string().default("porter_dev_refresh_secret_super_secure_key_2026"),
  JWT_ACCESS_TTL_SECONDS: z.coerce.number().default(900), // 15 minutes
  JWT_REFRESH_TTL_SECONDS: z.coerce.number().default(2592000), // 30 days
  CORS_ORIGIN: z.string().default("*"),
  LOG_LEVEL: z.enum(["debug", "info", "warn", "error"]).default("info"),
});

export const env = envSchema.parse(process.env);
