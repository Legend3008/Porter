#!/usr/bin/env bash
# ==============================================================================
# Script: run_api_tests.sh
# Description: Starts isolated PostgreSQL cluster, loads schema & seeds,
#              and executes the complete Porter API integration test suite.
# ==============================================================================

set -eo pipefail

API_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_DIR="${API_DIR}/../database"
TEST_PORT=54329
SCRATCH_DIR="/Users/ritesh/.gemini/antigravity-ide/brain/f76b2346-1a25-4143-a886-470dcc2afd10/scratch/api_pg_test"
PG_DATA="${SCRATCH_DIR}/data"
LOG_FILE="${SCRATCH_DIR}/postgres.log"

cleanup() {
    echo ""
    echo "── Stopping temporary PostgreSQL instance..."
    pg_ctl -D "${PG_DATA}" stop -m immediate >/dev/null 2>&1 || true
    rm -rf "${SCRATCH_DIR}"
    echo "── Test cleanup completed."
}

trap cleanup EXIT

echo "=================================================================="
echo "      PORTER API PLATFORM — INTEGRATION & CONTRACT SUITE         "
echo "=================================================================="

# 1. Initialize temporary test cluster
rm -rf "${SCRATCH_DIR}"
mkdir -p "${PG_DATA}"

echo "1. Initializing isolated PostgreSQL cluster on 127.0.0.1:${TEST_PORT}..."
initdb -D "${PG_DATA}" --no-locale -E UTF8 >/dev/null 2>&1
pg_ctl -D "${PG_DATA}" -o "-p ${TEST_PORT} -h 127.0.0.1" -l "${LOG_FILE}" start >/dev/null

until pg_isready -p "${TEST_PORT}" -h 127.0.0.1 >/dev/null 2>&1; do
    sleep 0.2
done

createdb -p "${TEST_PORT}" -h 127.0.0.1 porter_db

# 2. Run Migrations
echo "2. Applying database migrations (V01 - V16)..."
for migration in "${DB_DIR}"/migrations/V*.sql; do
    psql -p "${TEST_PORT}" -h 127.0.0.1 -d porter_db -v ON_ERROR_STOP=1 -f "${migration}" >/dev/null
done

# 3. Run Seeds
echo "3. Seeding reference and development data..."
for seed in "${DB_DIR}"/seeds/*.sql; do
    psql -p "${TEST_PORT}" -h 127.0.0.1 -d porter_db -v ON_ERROR_STOP=1 -f "${seed}" >/dev/null
done

# 4. Run TypeScript Test Suite
echo "4. Launching API Test Suite via tsx..."
export DATABASE_URL="postgresql://$(whoami)@127.0.0.1:${TEST_PORT}/porter_db"
export REDIS_URL="redis://127.0.0.1:6379"
export PORT=0
export NODE_ENV="test"
export JWT_ACCESS_SECRET="porter_test_jwt_access_secret_super_secure_12345"
export JWT_REFRESH_SECRET="porter_test_jwt_refresh_secret_super_secure_67890"

cd "${API_DIR}"
npx tsx src/tests/run-all-tests.ts
