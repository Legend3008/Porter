import pg from "pg";
import { env } from "./env.js";

const { Pool } = pg;

export const pool = new Pool({
  connectionString: env.DATABASE_URL,
  max: 25,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
  statement_timeout: 30000, // 30s statement timeout
});

export async function query<T extends pg.QueryResultRow = any>(
  text: string,
  params?: any[]
): Promise<pg.QueryResult<T>> {
  const start = Date.now();
  const res = await pool.query<T>(text, params);
  const duration = Date.now() - start;
  if (env.LOG_LEVEL === "debug") {
    console.log(JSON.stringify({ type: "db_query", duration_ms: duration, rows: res.rowCount }));
  }
  return res;
}

export async function withTransaction<T>(
  callback: (client: pg.PoolClient) => Promise<T>
): Promise<T> {
  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    const result = await callback(client);
    await client.query("COMMIT");
    return result;
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}

export async function checkDatabaseHealth(): Promise<{ status: "healthy" | "unhealthy"; latency_ms: number }> {
  const start = Date.now();
  try {
    await pool.query("SELECT 1");
    return { status: "healthy", latency_ms: Date.now() - start };
  } catch {
    return { status: "unhealthy", latency_ms: Date.now() - start };
  }
}
