# URL Shortener (Spring Boot + PostgreSQL + Redis)

A system design learning project implementing a production-style URL
shortener using real-world backend patterns including **database source
of truth**, **cache-aside with Redis**, **Liquibase migrations**, **rate
limiting**, and **cache stampede mitigation**.

------------------------------------------------------------------------

## Tech Stack

-   Java + Spring Boot
-   Spring Web (REST APIs)
-   Spring Data JPA + Hibernate
-   PostgreSQL (Primary persistence / source of truth)
-   Liquibase (Database migrations)
-   Redis (Caching + Rate Limiting)
-   Lombok
-   Docker Compose (Local infrastructure)

------------------------------------------------------------------------

## High-Level Architecture

### Write Path (`POST /api/shorten`)

1.  Generate short code
2.  Optional best-effort deduplication check
3.  Insert into PostgreSQL (with retry + constraints)
4.  Return short code

### Read Path (`GET /{code}`)

1.  Check Redis cache (cache-aside)
2.  If HIT → return redirect immediately
3.  If MISS → query PostgreSQL → populate cache → return redirect
4.  Negative caching prevents repeated DB hits for missing codes
5.  Single-flight prevents duplicate DB queries during cache misses

------------------------------------------------------------------------

### ASCII Diagram (Fallback)

                +------------------+
                |      Client      |
                +--------+---------+
                         |
                         v
                +------------------+
                |   Spring Boot    |
                |       App        |
                +----+--------+----+
                     |        |
             Cache Read      DB Query
                     |        |
                     v        v
                +---------+ +-----------+
                |  Redis  | | Postgres  |
                +---------+ +-----------+

------------------------------------------------------------------------

## API Endpoints

### POST `/api/shorten`

Create a short URL.

**Request**

``` json
{
  "url": "https://example.com"
}
```

**Response**

``` json
{
  "code": "AbC123x",
  "shortUrl": "http://localhost:8080/AbC123x",
  "longUrl": "https://example.com"
}
```

------------------------------------------------------------------------

### GET `/{code}`

Redirects to original URL.

Example:

    GET /AbC123x → 302 Redirect → https://example.com

------------------------------------------------------------------------

## Local Development (Docker)

### Start Infrastructure

``` bash
docker compose up -d
```

### Stop Infrastructure

``` bash
docker compose down
```

### Reset Data (Dev Only)

``` bash
docker compose down -v
docker compose up -d
```

------------------------------------------------------------------------

## Configuration Overview

Application uses:

-   PostgreSQL → localhost:5432
-   Redis → localhost:6379
-   Liquibase → Schema migration management
-   Hibernate → ddl-auto: validate

------------------------------------------------------------------------

## Database Schema (Liquibase)

Includes:

-   short_url table
-   UNIQUE(code) constraint
-   Index on long_url for fast lookup

------------------------------------------------------------------------

## Caching Strategy (Redis)

### Cache-Aside Pattern

Application controls cache population.

Flow: Check Cache → If MISS → Query DB → Populate Cache → Return

------------------------------------------------------------------------

### TTL + Jitter

-   TTL controls cache lifetime
-   Jitter prevents synchronized expiration
-   Reduces cache stampede risk

------------------------------------------------------------------------

### Negative Caching

Missing codes are cached briefly to prevent repeated DB queries for
non-existent resources.

------------------------------------------------------------------------

## Concurrency Protection (Single Flight)

Local in-memory request coalescing:

-   Only one request loads data from DB during cache miss
-   Other concurrent requests wait for the same result

Note: Local only --- distributed version could use Redis locks.

------------------------------------------------------------------------

## Rate Limiting

Redis-based rate limiting using:

-   Atomic INCR
-   TTL-based fixed window
-   Returns 429 Too Many Requests
-   Includes Retry-After header

------------------------------------------------------------------------

## Running The Application

1️⃣ Start infrastructure:

``` bash
docker compose up -d
```

2️⃣ Run Spring Boot app from IntelliJ

3️⃣ Test shorten endpoint:

``` bash
curl -X POST http://localhost:8080/api/shorten   -H "Content-Type: application/json"   -d '{"url":"https://example.com"}'
```

4️⃣ Test redirect:

``` bash
curl -i http://localhost:8080/<CODE>
```

------------------------------------------------------------------------

## System Design Decisions

### Source of Truth

PostgreSQL is authoritative. Redis is optimization layer.

### Consistency Model

Eventual consistency acceptable due to caching layer.

### Constraint Strategy

Database constraints enforce correctness under concurrency.

### Performance Strategy

Caching + Single Flight + Rate Limiting protect database and improve
latency.

------------------------------------------------------------------------

## Future Improvements

-   API Keys + Multi-tenant rate limiting
-   Distributed single-flight (Redis locks)
-   Observability (metrics + tracing)
-   URL expiration + cleanup jobs
-   Click analytics pipeline
-   API Gateway integration

------------------------------------------------------------------------
