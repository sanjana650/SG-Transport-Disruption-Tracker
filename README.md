# SG Transport Disruption Tracker

A full-stack app that tracks Singapore MRT/LRT train service alerts from the
[LTA DataMall](https://datamall.lta.gov.sg) `TrainServiceAlerts` API.

The Spring Boot backend polls LTA every minute, stores each disruption, works out how
severe it is, and marks it resolved when it disappears from the feed. A React dashboard
shows what is disrupted right now, with filters by line and a resolved-history view.

## Architecture

```
LTA DataMall API  ->  Spring Boot backend  ->  H2 database
                           |
                       REST API (/api/*)
                           |
                      React dashboard (Vite)
```

| Part | Tech |
|------|------|
| Backend | Java 21, Spring Boot 3.3, Spring Data JPA, H2, Spring Scheduling |
| Frontend | React 19, Vite |
| Tests | JUnit 5, Mockito, MockMvc (backend); Playwright (end-to-end) |
| Delivery | Docker, docker compose, GitHub Actions CI |

## Run it

**With Docker (easiest):**

```bash
docker compose up --build
# dashboard: http://localhost:3000     API: http://localhost:8080/api/status
```

By default the app uses built-in sample data that cycles through
"one disruption", "two disruptions", "all normal", so you can watch the dashboard change.

**With real LTA data:** get a free AccountKey from LTA DataMall, then:

```bash
LTA_MOCK_ENABLED=false LTA_ACCOUNT_KEY=your-key docker compose up --build
```

**Without Docker (development):**

```bash
cd backend  && mvn spring-boot:run      # http://localhost:8080
cd frontend && npm install && npm run dev   # http://localhost:5173 (proxies /api to :8080)
```

## API

| Method | Path | What it does |
|--------|------|--------------|
| GET | `/api/disruptions?line=EWL&activeOnly=true` | List disruptions. `activeOnly=false` includes resolved history. |
| GET | `/api/status` | Overall status (`NORMAL` / `DISRUPTED`) and active count per line. |
| POST | `/api/refresh` | Poll LTA immediately instead of waiting for the next scheduled poll. |
| GET | `/actuator/health` | Health check (used by Docker). |

## How it works

- **Polling:** `PollingScheduler` runs `DisruptionService.refresh()` at startup and then every
  `lta.poll-interval-ms` (60 s).
- **Deduplication:** each affected segment gets a fingerprint (`line|direction|stations`).
  The same fingerprint seen again updates the existing row instead of creating a new one.
- **Resolving:** an active disruption missing from the latest poll is marked resolved with a timestamp.
- **Severity:** read from the text for that line. "No train service" is `MAJOR`, "delay" is `MINOR`, otherwise `INFO`.
  LTA sends one text blob covering several lines, so the code first extracts the sentence for the line in question.
- **Failure handling:** if LTA is down, the previous data is kept. A failed call does not wrongly mark everything resolved.
- **Mock mode:** `LtaClient` is an interface with a real HTTP implementation and a mock one,
  chosen by the `lta.mock-enabled` property. This lets the app and tests run with no API key.

## Tests

```bash
cd backend && mvn test                    # service logic, controller, JSON parsing
cd e2e && npm install && npx playwright install chromium && npx playwright test
```

- **Backend:** 14 tests covering the disruption lifecycle (create, dedupe, resolve, API failure),
  per-line severity, REST endpoints and parsing of the real LTA response shape.
- **End-to-end:** 7 Playwright tests drive the dashboard in a real browser with the API mocked
  (banner states, line filter, resolved toggle, refresh button, error handling).
  A 2-test full-stack suite runs against the real backend with `E2E_LIVE=1`.

## CI/CD

`.github/workflows/ci.yml` runs on every push and pull request: backend tests, frontend lint and build,
Playwright tests, then a `docker compose build` once everything else passes.

## Possible next steps

- Persistent database (PostgreSQL) instead of in-memory H2
- Bus service disruptions and traffic incidents from other DataMall endpoints
- Push notifications for chosen lines
- Server-sent events instead of 30 s polling in the dashboard
