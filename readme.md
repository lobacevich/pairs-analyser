# Pairs Analyzer

A Spring Boot service that polls the average price of a trading pair from the
[Binance API](https://developers.binance.com/docs/binance-spot-api-docs/rest-api/market-data-endpoints#current-average-price)
on a schedule, stores it in PostgreSQL, and exposes the data through a REST API.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Requirements](#requirements)
- [Quick Start with Docker](#quick-start-with-docker)
- [Running Locally](#running-locally)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Rate Limits and Error Handling](#rate-limits-and-error-handling)

---

## Features

- Polls `GET /api/v3/avgPrice?symbol=BNBUSDT` from Binance on a configurable schedule
- Persists every observation as a `(symbol, price, fetchedAt)` record in PostgreSQL
- Respects Binance `Retry-After` header on `429` / `418` responses and blocks subsequent polls until the retry window expires
- Blocks further polls for 60 seconds on `5xx` responses and network errors, so the scheduler never hammered a struggling upstream
- Detects price trend (`UPWARD`, `DOWNWARD`, `FLAT`) using linear regression over all records in a range
- Database schema managed by Liquibase
- Full OpenAPI / Swagger documentation

---

## Tech Stack

| Layer | Technology                                 |
|---|--------------------------------------------|
| Language | Java 21                                    |
| Framework | Spring Boot 3.5.x                          |
| Persistence | Spring Data JPA, Hibernate 6               |
| Database | PostgreSQL 16+                             |
| Migrations | Liquibase                                  |
| Build | Gradle                                     |
| Documentation | springdoc-openapi (Swagger UI)             |
| Testing | JUnit 5, Mockito, Testcontainers, WireMock |
| Containerization | Docker, Docker Compose                     |

---

## Requirements

- **Docker** and **Docker Compose** (for the containerized run), or
- **JDK 21+** and a running **PostgreSQL 16+** instance (for the local run)

---

## Quick Start with Docker

The simplest way to start the whole stack (application + PostgreSQL):

```bash
docker compose up --build
```

The application will be available after the startup (typically 15–30 seconds, since Spring Boot, Liquibase and the JPA schema validation all run during boot):

- REST API — `http://localhost:8082/api/v1/prices`
- Swagger UI — `http://localhost:8082/swagger-ui/index.html`

To check that the service is up, call any endpoint:

```bash
curl http://localhost:8082/api/v1/prices/latest
```

To stop:

```bash
docker compose down
```

To stop and remove the database volume:

```bash
docker compose down -v
```

---

## Running Locally

1. Start a PostgreSQL instance, for example:

   ```bash
   docker run -d --name postgres \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -e POSTGRES_DB=pairs_analyzer \
     -p 5432:5432 \
     postgres:17.6
   ```

2. Export the environment variables (see [Environment Variables](#environment-variables)):

   ```bash
   export DB_URL=jdbc:postgresql://localhost:5432/pairs_analyzer
   export DB_USERNAME=postgres
   export DB_PASSWORD=postgres
   ```

3. Build and run the application:

   ```bash
   ./gradlew bootRun
   ```

4. The service starts on `http://localhost:8080`. Liquibase applies the schema
   automatically on startup.

---

## Environment Variables

All variables have sensible defaults and can be omitted in the Docker Compose setup.

| Variable | Description | Default | Required |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` | No |
| `DB_URL` | JDBC URL of the PostgreSQL database | `jdbc:postgresql://localhost:5432/pairs_analyzer` | **Yes** |
| `DB_USERNAME` | Database user | `postgres` | **Yes** |
| `DB_PASSWORD` | Database password | `postgres` | **Yes** |
| `DB_DRIVER` | JDBC driver class | `org.postgresql.Driver` | No |
| `BINANCE_API_URL` | Base URL of the Binance API | `https://api.binance.com` | No |
| `BINANCE_SYMBOL` | Trading pair to poll | `BNBUSDT` | No |
| `BINANCE_POLL_INTERVAL` | Polling interval in **ISO-8601 duration** format | `PT30S` | No |

### `BINANCE_POLL_INTERVAL` — duration format

The value **must** be an ISO-8601 duration, otherwise the application fails on start.

| Value | Meaning |
|---|---|
| `PT30S` | 30 seconds (default) |
| `PT1M` | 1 minute |
| `PT5M` | 5 minutes |
| `PT1H` | 1 hour |
| `PT30M` | 30 minutes |

Invalid examples that **will not work**:

- `30` — interpreted as **30 milliseconds**
- `30s` — not ISO-8601

Example of a custom interval in `docker-compose.yml`:

```yaml
environment:
  BINANCE_POLL_INTERVAL: PT1M
```

---

## API Reference

Base path: `/api/v1/prices`

All timestamps are in **UTC** and use ISO-8601 format (`YYYY-MM-DDTHH:mm:ssZ`).
All date parameters are in **`YYYY-MM-DD`** format.

### 1. Get latest saved record

```http
GET /api/v1/prices/latest
```

**Example**

```bash
curl http://localhost:8080/api/v1/prices/latest
```

**Response `200 OK`**

```json
{
  "symbol": "BNBUSDT",
  "price": 672.82896388,
  "fetchedAt": "2026-11-13T19:25:00Z"
}
```

**Response `404 Not Found`** — no records have been saved yet.

---

### 2. Get average price for a period

```http
GET /api/v1/prices/average?from=YYYY-MM-DD&to=YYYY-MM-DD
```

**Query parameters**

| Name | Type | Required | Description |
|---|---|---|---|
| `from` | `LocalDate` | Yes | Start date (inclusive) |
| `to` | `LocalDate` | Yes | End date (inclusive) |

**Example**

```bash
curl "http://localhost:8080/api/v1/prices/average?from=2026-11-01&to=2026-11-13"
```

**Response `200 OK`**

```json
{
  "averagePrice": 671.53
}
```

**Response `400 Bad Request`** — invalid date format or `from > to`.

---

### 3. Get paginated records for a period

```http
GET /api/v1/prices?from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=20
```

**Query parameters**

| Name | Type | Required | Default | Description |
|---|---|---|---|---|
| `from` | `LocalDate` | Yes | — | Start date (inclusive) |
| `to` | `LocalDate` | Yes | — | End date (inclusive) |
| `page` | `int` | No | `0` | Zero-based page index |
| `size` | `int` | No | `20` | Page size (1..100) |

**Example**

```bash
curl "http://localhost:8080/api/v1/prices?from=2026-11-01&to=2026-11-13&page=0&size=20"
```

**Response `200 OK`**

```json
{
  "items": [
    {
      "symbol": "BNBUSDT",
      "price": 672.82896388,
      "fetchedAt": "2026-11-13T19:25:00Z"
    },
    {
      "symbol": "BNBUSDT",
      "price": 671.95000000,
      "fetchedAt": "2026-11-13T19:24:30Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 135,
  "hasNext": true
}
```

**Response `400 Bad Request`** — invalid parameters or `from > to`.

---

### 4. Determine price trend for a period

```http
GET /api/v1/prices/trend?from=YYYY-MM-DD&to=YYYY-MM-DD
```

**Query parameters**

| Name | Type | Required | Description |
|---|---|---|---|
| `from` | `LocalDate` | Yes | Start date (inclusive) |
| `to` | `LocalDate` | Yes | End date (inclusive) |

**Example**

```bash
curl "http://localhost:8080/api/v1/prices/trend?from=2026-11-01&to=2026-11-13"
```

**Response `200 OK`**

```json
{
  "symbol": "BNBUSDT",
  "start_date": "2026-11-01",
  "end_date": "2026-11-13",
  "trend": "UPWARD"
}
```

`trend` values:

- `UPWARD` — the price is growing on average
- `DOWNWARD` — the price is falling on average
- `FLAT` — no statistically significant trend

The trend is computed with linear regression over **all records** in the range,
not just the first and last values.

---

## Testing

### Run all tests

```bash
./gradlew test
```

### Run only unit tests

```bash
./gradlew test --tests "by.lobacevich.analyzer.service.*"
```

### Run only integration tests

```bash
./gradlew test --tests "by.lobacevich.analyzer.it.*"
```

> Integration tests require **Docker** to be running, because they use
> **Testcontainers** (PostgreSQL) and **WireMock** (Binance API).

### Test coverage

```bash
./gradlew jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

### Test layers

- **Unit tests** — business logic in `PriceRecordServiceImpl` and `TrendAnalyzer`
- **Integration tests** — `BinanceClient` with WireMock, full application context with Testcontainers + PostgreSQL

---

## Project Structure

```
src/main/java/by/lobacevich/analyzer/
├── client/              Binance HTTP client
├── config/              Spring configuration (RestClient, OpenAPI, Clock)
├── controller/          REST endpoints
├── dto/                 Request/response DTOs (Java records)
├── entity/              JPA entities
├── exception/           Custom exceptions and global handler
├── exceptionhandler/    Global @RestControllerAdvice that maps exceptions to HTTP responses
├── mapper/              Entity ↔ DTO mapping
├── repository/          Spring Data JPA repositories
├── scheduler/           Scheduled polling
└── service/             Business logic

src/main/resources/
├── application.yml
├── application-docker.yml
└── db/changelog/        Liquibase changelogs


src/test/java/by/lobacevich/analyzer/
├── service/             Unit tests
└── it/                  Integration tests
```

---

## Rate Limits and Error Handling

The service respects Binance rate limits and never stops the scheduler on failure.

### Behaviour

| Response | Handling |
|---|---|
| `200 OK` | Persist the record |
| `429 Too Many Requests` | Read `Retry-After`, block further polls for that many seconds |
| `418 I'm a teapot` | Binance auto-ban; same as `429` |
| `5xx` server errors | Block further polls for 60 seconds |
| Network errors / timeouts | Block further polls for 60 seconds |
| `4xx` client errors (400, 404, …) | Log an error; do **not** block |
| Empty response body | Throw `BinanceApiException`; log and continue |

### Idempotency

A unique index on `(symbol, fetched_at)` guarantees that duplicate inserts
(from retries or overlapping polls) are rejected at the database level and
logged as a warning, never as an error.

---