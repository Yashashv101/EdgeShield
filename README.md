# 🛡️ EdgeShield API Gateway

> Drop-in JWT Authentication · Distributed Rate Limiting · Async Threat Logging

EdgeShield is a Spring Boot API Gateway that acts as a reverse proxy in front of your existing backend. It handles JWT authentication, rate limiting, and threat logging — **without requiring any changes to your existing application or database.**

---

## How It Works

```
  Client (Postman / Browser)
           │
           ▼  :8080
  ┌─────────────────────┐
  │     EdgeShield      │  ← JWT validation, rate limiting, threat logging
  └────────┬────────────┘
           │  host.docker.internal:<your-port>
           ▼
  ┌─────────────────────┐
  │    Your Backend     │  ← Runs locally, completely untouched
  └─────────────────────┘
```

EdgeShield's own infrastructure (Postgres, Redis, RabbitMQ) runs in Docker and is **completely isolated** from your application's database.

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed (includes Docker Compose)
- Your backend app running locally (e.g. on port `9090`)
- `openssl` available in your terminal (comes with Git Bash on Windows)

---

## Setup

### Step 1 — Generate a JWT Secret

```bash
openssl rand -base64 32
```

Copy the output — you'll use it as `JWT_SECRET` below.

> **Windows users:** Run this in Git Bash or WSL.

---

### Step 2 — Create `docker-compose.yml`

Create a `docker-compose.yml` file (anywhere on your machine — it does **not** need to be inside your project):

```yaml
services:

  # ─── EDGESHIELD POSTGRES ──────────────────────────────────────
  postgres:
    image: postgres:16
    container_name: shieldgate-postgres
    environment:
      POSTGRES_DB: shieldgate
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: changeme
    volumes:
      - shieldgate-pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

  # ─── EDGESHIELD REDIS ─────────────────────────────────────────
  redis:
    image: redis:7-alpine
    container_name: shieldgate-redis
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5

  # ─── EDGESHIELD RABBITMQ ──────────────────────────────────────
  rabbitmq:
    image: rabbitmq:3-management
    container_name: shieldgate-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 10s
      retries: 5

  # ─── EDGESHIELD GATEWAY ───────────────────────────────────────
  shieldgate:
    image: yashashv101/edgeshield:latest
    container_name: shieldgate-app
    environment:
      SPRING_PROFILES_ACTIVE: docker

      # ─── CONFIGURE THESE ──────────────────────────────────────
      TARGET_URL: http://host.docker.internal:9090       # Your backend port
      LOGIN_URL: http://host.docker.internal:9090/login
      REGISTER_URL: http://host.docker.internal:9090/register
      JWT_SECRET: <paste-your-generated-secret-here>     # From Step 1
      RATE_LIMIT: 100                                    # Max requests per IP per minute
      # ──────────────────────────────────────────────────────────

      POSTGRES_PASSWORD: changeme
      RABBITMQ_USER: guest
      RABBITMQ_PASS: guest
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

volumes:
  shieldgate-pgdata:
```

> ⚠️ **Important:** The service names `postgres`, `redis`, and `rabbitmq` must stay exactly as shown. EdgeShield's internal config is hardcoded to these hostnames. See the [FAQ](#faq) if you need to rename them.

---

### Step 3 — Start Your Backend

Make sure your backend is running locally before starting Docker:

```bash
# Spring Boot
./mvnw spring-boot:run

# Node.js
node server.js
```

---

### Step 4 — Start EdgeShield

```bash
docker compose up -d
```

EdgeShield will wait for Postgres, Redis, and RabbitMQ to pass their health checks before starting.

---

### Step 5 — Open the Dashboard

Open your browser and go to:

```
http://localhost:8080/index.html
```

You'll be greeted with a login screen. Use your backend credentials to sign in — EdgeShield proxies the login request to your backend's `LOGIN_URL`, and on success issues a JWT and stores it in your browser session.

---

### Step 6 — Register & Get a JWT Token (Postman)

EdgeShield exposes two auth endpoints that proxy to your backend and return a signed JWT:

**Register a new user:**
- **Method:** `POST`
- **URL:** `http://localhost:8080/auth/register`
- **Headers:** `Content-Type: application/json`
- **Body (raw JSON):**
```json
{
  "username": "testuser",
  "password": "testpassword"
}
```

**Login with an existing user:**
- **Method:** `POST`
- **URL:** `http://localhost:8080/auth/login`
- **Headers:** `Content-Type: application/json`
- **Body (raw JSON):**
```json
{
  "username": "testuser",
  "password": "testpassword"
}
```

Both endpoints return a JWT on success:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Copy the token — you'll need it for all protected API requests. You can also use these credentials to log into the dashboard at `http://localhost:8080/index.html`.

> The exact request body fields depend on your backend's register/login endpoint. Adjust accordingly.

---

### Step 7 — Send Requests Through EdgeShield

Point all your API calls at port `8080` instead of your backend directly:

```
GET http://localhost:8080/api/jobs
Authorization: Bearer <your-token-here>
```

EdgeShield validates the token, checks the rate limit, and forwards the request to your backend.

---

## Dashboard

All dashboard functionality is built into a single page at:

```
http://localhost:8080/index.html
```

The dashboard requires authentication — log in with your backend credentials (same username/password you registered with). EdgeShield issues a JWT on login and stores it in your browser session.

**Features:**
- Live threat log table with filtering by type (Missing JWT, Invalid JWT, Rate Limited) and IP address
- Stats cards showing total threats broken down by category
- Sortable columns
- Auto-refresh toggle (every 5 seconds)
- Auto-logout on token expiry

> The `/admin/threats` API used by the dashboard is JWT-protected. The dashboard automatically attaches your session token to every request.

---

## Debugging

### Check container status

```bash
docker ps
```

You should see four containers running: `shieldgate-app`, `shieldgate-postgres`, `shieldgate-redis`, `shieldgate-rabbitmq`.

### View logs

```bash
docker logs shieldgate-app
docker logs shieldgate-postgres
docker logs shieldgate-rabbitmq
docker logs shieldgate-redis
```

### Restart

```bash
docker compose down
docker compose up -d
```

### Full reset (wipes EdgeShield data)

```bash
docker compose down -v
```

> ⚠️ The `-v` flag deletes the `shieldgate-pgdata` volume — all EdgeShield config and threat logs will be erased. Your own app's database is never affected.

---

## FAQ

### ❌ `ECONNREFUSED 127.0.0.1:8080` — Can't connect to EdgeShield

The `shieldgate-app` container isn't running. Check with:

```bash
docker ps
docker logs shieldgate-app
```

Most likely cause: one of the infrastructure services (Postgres, Redis, RabbitMQ) failed its health check and EdgeShield never started. Fix that service first, then restart.

---

### ❌ `UnknownHostException: postgres` — Database connection failed

EdgeShield's internal config looks for a host named exactly `postgres`. If you renamed the service, either rename it back, or add this override to the `shieldgate` environment block:

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://<your-service-name>:5432/shieldgate
```

---

### ❌ `UnknownHostException: rabbitmq` — RabbitMQ connection failed

Same issue — service name mismatch. Either keep the service named `rabbitmq`, or override:

```yaml
SPRING_RABBITMQ_HOST: <your-rabbitmq-service-name>
```

---

### ❌ `UnknownHostException: redis` — Redis connection failed

Override with:

```yaml
SPRING_DATA_REDIS_HOST: <your-redis-service-name>
```

> 💡 **Tip:** If you hit all three hostname errors, the easiest fix is to keep the service names as `postgres`, `redis`, and `rabbitmq`. The `container_name` field can be anything — Docker DNS resolves **service names**, not container names.

---

### ❌ `TARGET_URL` uses `localhost` — Requests not reaching backend

Inside Docker, `localhost` refers to the container itself, not your machine. Always use:

```yaml
TARGET_URL: http://host.docker.internal:<your-port>
```

**Linux users:** `host.docker.internal` doesn't resolve automatically. Add this to the `shieldgate` service:

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

---

### ❌ `service depends on undefined service` — Compose validation error

Your `depends_on` block references a service name that doesn't exist. Make sure every name in `depends_on` exactly matches a service key defined at the top level of your `docker-compose.yml`. `container_name` and service names are different things — `depends_on` uses service names only.

---

### ❌ Dashboard login says "Cannot reach ShieldGate"

The `shieldgate-app` container isn't running or hasn't finished starting. Check:

```bash
docker ps
docker logs shieldgate-app
```

Wait for the startup logs to show `Started ShieldGateApplication` before trying to log in.

---

### ❌ Dashboard login says "Invalid credentials"

EdgeShield proxies your login to your backend's `LOGIN_URL`. This means:
- Your backend app must be running locally
- The credentials must be valid in your backend's own user database
- The `LOGIN_URL` in your `docker-compose.yml` must point to the correct endpoint

---

### ❌ `401 Unauthorized` on all requests

You need a valid JWT token in every protected request. Get one via Postman:

```
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "testpassword"
}
```

Then attach the returned token to all requests:

```
Authorization: Bearer <token>
```

If you changed `JWT_SECRET` after generating a token, the old token is invalid — login again to get a new one.

---

### ❌ `429 Too Many Requests`

You've hit the rate limit for your IP. Either wait for the window to reset, or increase `RATE_LIMIT` in your `docker-compose.yml` and restart — or change it live via the config dashboard at `/dashboard/config.html`.

---

### ❌ My backend app's data disappeared after running Docker

EdgeShield never touches your backend's database. Your app's data lives in your local Postgres instance (`localhost:5432`). EdgeShield only creates its own database inside the `shieldgate-postgres` container. They are completely separate.

---

### ❌ Port `8080` is already in use

Change EdgeShield's exposed port in `docker-compose.yml`:

```yaml
ports:
  - "9000:8080"   # Clients now hit port 9000
```

Then access EdgeShield at `http://localhost:9000`.

---

## Admin API

EdgeShield exposes REST endpoints for querying threat logs programmatically. These endpoints are **JWT-protected** — include your token in the `Authorization` header.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/admin/threats` | All threat logs, newest first |
| `GET` | `/admin/threats/type?type=INVALID_JWT` | Filter logs by threat type |
| `GET` | `/admin/threats/ip?ip=<ip-address>` | Filter logs by source IP |

**Threat types:** `INVALID_JWT`, `MISSING_JWT`, `RATE_LIMIT_EXCEEDED`

**Example (Postman):**
```
GET http://localhost:8080/admin/threats/type?type=MISSING_JWT
Authorization: Bearer <your-token>
```

---

## Quick Reference

| Action | Command / URL |
|--------|---------------|
| Start | `docker compose up -d` |
| Stop | `docker compose down` |
| Full reset | `docker compose down -v` |
| View container logs | `docker logs shieldgate-app` |
| Dashboard | `http://localhost:8080/index.html` |
| Register (Postman) | `POST http://localhost:8080/auth/register` |
| Login (Postman) | `POST http://localhost:8080/auth/login` |
| All threat logs | `GET http://localhost:8080/admin/threats` |
| Threats by type | `GET http://localhost:8080/admin/threats/type?type=INVALID_JWT` |
| Threats by IP | `GET http://localhost:8080/admin/threats/ip?ip=192.168.1.1` |

---

*Built with Spring Boot 3 · PostgreSQL · Redis · RabbitMQ*