# Student Placement Preparation Portal

Spring Boot 3 + Spring Security (JWT) + Spring Data JPA + MySQL, with a React (Vite) + Bootstrap frontend.

| Folder | What it is |
|---|---|
| `backend/` | Spring Boot REST API (Java 17+, Maven) |
| `frontend/` | React 18 + Vite + Bootstrap 5 single-page app |
| `db/schema.sql` | Hand-written MySQL 8 DDL (same tables the entities create) |
| `docs/GUIDE.md` | Architecture, ER design, API reference, workflows, interview preparation |
| `scripts/smoke-test.sh` | End-to-end curl check of a running backend |
| `docker-compose.yml` | MySQL 8 for local development |

## Prerequisites
Java 17+, Maven 3.9+, Node 18+, and either Docker or a local MySQL 8.

## Run it (4 commands)

```bash
# 1. database
docker compose up -d                       # or: mysql -u root -p < db/schema.sql

# 2. backend  (http://localhost:8080)
cd backend && mvn spring-boot:run

# 3. frontend (http://localhost:5173)  - in a second terminal
cd frontend && npm ci && npm run dev

# 4. (optional) verify the running API end-to-end
bash scripts/smoke-test.sh
```

Default database login is `root` / `root` (matches `docker-compose.yml`). Override with environment variables:
`DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (32+ chars), `CORS_ORIGINS`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`.

On first start the app creates the roles, an **admin account** (`admin@placement.local` / `Admin@123`) and sample
topics, questions, two timed tests and three company roadmaps. Change the admin password before any real deployment.

## Run the tests
```bash
cd backend && mvn test        # unit tests (JUnit 5 + Mockito) and Spring Boot integration tests on in-memory H2; no MySQL needed
```

## Production notes
* `DDL_AUTO=none` and run `db/schema.sql` yourself; Hibernate then never alters the schema.
* Set a strong `JWT_SECRET`, change the admin password, set `CORS_ORIGINS` to your real frontend origin, serve over HTTPS.
* `cd frontend && npm run build` produces static files in `frontend/dist` (set `VITE_API_URL` first).
