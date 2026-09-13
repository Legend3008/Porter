.PHONY: up down psql test help

help:
	@echo "Porter Database Management Commands:"
	@echo "  make up      - Start PostgreSQL 16 & Redis 7 containers"
	@echo "  make down    - Stop PostgreSQL & Redis containers"
	@echo "  make psql    - Connect to local running database"
	@echo "  make test    - Run automated end-to-end migration & invariant tests"

up:
	docker compose up -d

down:
	docker compose down

psql:
	PGPASSWORD=porter_secure_password_2026 psql -h localhost -p 5432 -U porter_admin -d porter_db

test:
	./tests/run_verification.sh
