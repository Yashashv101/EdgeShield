# EdgeShield — API Gateway

> A drop-in, containerized API Gateway that adds JWT authentication, login/register delegation, distributed rate limiting, and real-time threat monitoring to any backend service — with zero code changes required.

---

## What is EdgeShield?

Most backend services are built without authentication or traffic control. EdgeShield sits in front of your API as a **reverse proxy**, handling security so your backend doesn't have to.

```
Client Request
      │
      ▼
┌─────────────────────────────────────┐
│             EdgeShield              │
│                                     │
│  • Delegates login/register         │
│  • Issues & validates JWT tokens    │
│  • Enforces rate limits via Redis   │
│  • Logs threats via RabbitMQ        │
└─────────────────────────────────────┘
      │  (only clean, authenticated traffic passes)
      ▼
┌─────────────────────┐
│   Your Backend API  │  ← untouched, no code changes needed
└─────────────────────┘
```

---

## Features

- **Login & Register Delegation** — EdgeShield forwards credentials to your backend's login/register endpoints. If your backend returns `200`, EdgeShield issues a JWT. Your backend never needs to know about tokens.
- **JWT Authentication** — validates `Bearer` tokens on every protected request. Missing or invalid tokens are rejected with `401` before reaching your backend.
- **Distributed Rate Limiting** — per-IP request throttling backed by Redis. Works across multiple EdgeShield instances.
- **Async Threat Logging** — security violations are published to RabbitMQ and persisted to PostgreSQL without blocking the request thread.
- **Live Threat Dashboard** — real-time analytics, top offending IPs, and paginated threat logs via a built-in web UI.
- **Zero Backend Changes** — point EdgeShield at your existing API. No code modifications needed.

---

## Quick Start

### Prerequisites
- Docker & Docker Compose

### 1. Add this `docker-compose.yml` to your project

```yaml
services:
  yourapp:
    build: .
    container_name: yourapp
    depends_on:
      - your-postgres
    # No ports exposed — only reachable through EdgeShield

  your-postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: yourdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: changeme
    volumes:
      - your-pgdata:/var/lib/postgresql/data

  edgeshield:
    image: yashashv101/edgeshield:latest
    container_name: edgeshield
    environment:
      SPRING_PROFILES_ACTIVE: docker
      TARGET_URL: http://yourapp:8080       # your backend service
      JWT_SECRET: your-secret-min-32-chars-here
      RATE_LIMIT: 100                        # requests per IP per minute
      LOGIN_URL: http://yourapp:8080/login   # your backend's login endpoint
      REGISTER_URL: http://yourapp:8080/register
    ports:
      - "8080:8080"                          # only EdgeShield is publicly exposed
    depends_on:
      - edgeshield-postgres
      - edgeshield-redis
      - edgeshield-rabbitmq
      - yourapp

  edgeshield-postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: shieldgate
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: changeme
    volumes:
      - edgeshield-pgdata:/var/lib/postgresql/data

  edgeshield-redis:
    image: redis:7-alpine

  edgeshield-rabbitmq:
    image: rabbitmq:3-management
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

volumes:
  your-pgdata:
  edgeshield-pgdata:
```

### 2. Start everything

```bash
docker-compose up -d
```

### 3. Register a user

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "secret123", "email": "john@example.com"}'

# Response: {"token": "eyJhbGci...", "message": "Registration successful"}
```

### 4. Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "secret123"}'

# Response: {"token": "eyJhbGci..."}
```

### 5. Make authenticated requests

```bash
curl http://localhost:8080/api/your-endpoint \
  -H "Authorization: Bearer <token>"
```

---

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `TARGET_URL` | Your backend service URL | `http://localhost:9090` |
| `JWT_SECRET` | Secret key for signing JWTs (min 32 chars) | required in production |
| `RATE_LIMIT` | Max requests per IP per minute | `100` |
| `LOGIN_URL` | Your backend's login endpoint | `http://localhost:9090/login` |
| `REGISTER_URL` | Your backend's register endpoint | `http://localhost:9090/register` |

### Generating a JWT Secret

```bash
openssl rand -base64 32
```

---

## How It Works

### Request Lifecycle

```
Incoming Request
      │
      ├─ POST /auth/login or /auth/register
      │         │
      │         ▼
      │   Forward to LOGIN_URL / REGISTER_URL on your backend
      │         │
      │   Backend returns 200? → EdgeShield generates JWT → returns token
      │   Backend returns 4xx? → EdgeShield returns 401 to client
      │
      └─ Any other request (e.g. GET /api/jobs)
                │
                ▼
        JwtAuthenticationFilter (Order 1)
          ├─ Public path (/auth/**, /health)? → pass through
          ├─ Missing token? → log MISSING_JWT → 401
          ├─ Invalid token? → log INVALID_JWT → 401
          └─ Valid token → continue
                │
                ▼
        RateLimitFilter (Order 2)
          ├─ Exceeds limit? → log RATE_LIMIT_EXCEEDED → 429
          └─ Within limit → continue
                │
                ▼
        ProxyController
          └─ Forward to TARGET_URL → return response to client
```

### Async Threat Logging

When a request is blocked, EdgeShield **immediately returns the error** and fires a lightweight event to RabbitMQ. A decoupled consumer writes it to PostgreSQL off the HTTP thread — keeping the gateway responsive even during sustained attacks.

```
Blocked Request → ThreatEventPublisher → RabbitMQ
                                              │
                                              ▼ (async)
                                   ThreatEventConsumer → PostgreSQL
```

---

## Integrating with Your Frontend

Point your frontend at EdgeShield (`localhost:8080`) instead of your backend directly. Use an axios interceptor to attach the JWT automatically:

```js
// axiosInstance.js
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default api;
```

Login and store the token:

```js
const res = await axios.post("http://localhost:8080/auth/login", { username, password });
localStorage.setItem("token", res.data.token);
```

All subsequent API calls via `api.get(...)`, `api.post(...)` etc. will automatically carry the JWT.

---

## Dashboard

Visit `http://localhost:8080/dashboard/index.html` after startup.

| Page | URL |
|---|---|
| Threat Stats | `/dashboard/stats.html` |
| Threat Logs | `/dashboard/threats.html` |
| Configuration | `/dashboard/config.html` |

---

## Public Endpoints

These paths bypass JWT validation by design:

| Path | Purpose |
|---|---|
| `POST /auth/login` | Delegate login to backend, receive JWT |
| `POST /auth/register` | Delegate registration to backend, receive JWT |
| `/health` | Health check |

All other paths require a valid `Authorization: Bearer <token>` header.

---

## Architecture

| Component | Technology | Purpose |
|---|---|---|
| Gateway Core | Spring Boot 3 / Java 17 | Reverse proxy, filter chain, REST APIs |
| Auth Delegation | RestTemplate | Forwards login/register to your backend |
| Token Signing | JJWT | JWT generation and validation |
| Rate Limiting | Redis | Distributed per-IP counters |
| Threat Logging | RabbitMQ + PostgreSQL | Async audit log |
| Dashboard | HTML / CSS / Vanilla JS | Monitoring UI |
| Deployment | Docker Compose | Single-command setup |

---

## Project Structure

```
src/main/java/com/shieldgate/
├── filter/
│   ├── JwtAuthenticationFilter.java   # Auth gate (Order 1)
│   └── RateLimitFilter.java           # Rate limit gate (Order 2)
├── service/
│   ├── JwtService.java                # Token generation & validation
│   ├── RateLimiterService.java        # Redis fixed-window logic
│   ├── ThreatEventPublisher.java      # Pushes events to RabbitMQ
│   └── ThreatEventConsumer.java       # Persists events to PostgreSQL
├── controller/
│   ├── AuthController.java            # Login/register delegation + JWT issuance
│   ├── ProxyController.java           # Reverse proxy engine
│   └── AdminController.java          # Threat data API for dashboard
└── model/
    └── ThreatLog.java                 # Threat audit log entity
```

---

## Limitations

- **Fixed-window rate limiting** — susceptible to burst traffic at window boundaries compared to sliding-log algorithms.
- **RestTemplate proxying** — buffers full request/response bodies in memory. Not suited for streaming large files or Server-Sent Events. A reactive WebClient migration would be needed for those use cases.
- **Credential delegation model** — EdgeShield trusts your backend's `200` response as proof of valid credentials. Ensure your backend properly validates passwords before returning `200`.