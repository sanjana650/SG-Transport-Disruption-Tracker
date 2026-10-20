# Interview cheat sheet: SG Transport Disruption Tracker

Read this twice. Then open the code and trace one request from start to finish.

## Before the interview (do this first, about 30 minutes)

1. `cd backend && mvn test` — this must pass. **I could not run the Java code where I built it**
   (the workspace blocked Maven Central). If anything fails, paste the error to me and we fix it.
2. Get your free LTA DataMall AccountKey (see README), put it in `.env`, then
   `docker compose up --build` and open http://localhost:3000.
   Most of the time the real API says "all normal", so the dashboard will look empty. That is correct.
   For a demo with visible disruptions, run once with `LTA_MOCK_ENABLED=true`.
3. `cd e2e && npm install && npx playwright install chromium && npx playwright test`.
   7 tests should pass.
4. Push to GitHub so the CI badge and workflow exist. Check the first CI run goes green.

## The 30-second pitch

"It's a full-stack app that tracks MRT and LRT disruptions in Singapore. A Spring Boot backend
polls the LTA DataMall API every minute, saves each disruption, and marks it resolved when it
disappears. A React dashboard shows what's disrupted now, with a filter by line. I added
Playwright tests, Docker, and a GitHub Actions pipeline."

## Be honest about the timeline

If they ask when or how long: say you built it recently, outside of coursework, to practise
the Spring Boot + React + testing + CI/CD stack. That is true, and it is a fine answer.
Do not invent a number of weeks or a team. Do not say you used Selenium. The tests use Playwright.

## The flow, in plain English

1. App starts. `PollingScheduler` calls `DisruptionService.refresh()` once, then every 60 seconds.
2. `refresh()` asks an `LtaClient` for the alerts. In real mode that is an HTTP call to LTA.
   In mock mode it is fake data in the same shape.
3. The response is turned into `Disruption` objects. Each gets a **fingerprint**: `line|direction|stations`.
4. For each one, look for an active row with the same fingerprint.
   Found: update it. Not found: save a new row.
5. Any active row whose fingerprint was *not* in this response is marked **resolved**.
6. The React page calls `/api/status` and `/api/disruptions` every 30 seconds and draws cards.

## Questions they will probably ask

**Why a fingerprint?**
LTA does not give a stable ID. Without one, every poll would create duplicate rows. The fingerprint says "this is the same disruption as last time".

**Why an interface for the LTA client?**
So I can swap the real API for fake data. The app runs with no API key, and tests do not depend on the internet. This is basic dependency injection. Spring picks the real or mock class from one config property.

**What happens if LTA is down?**
`refresh()` catches the error and keeps the old data. Otherwise one failed call would mark every disruption as resolved, which would be wrong.

**How do you decide severity?**
By keywords in the text: "no train service" is MAJOR, "delay" is MINOR. LTA sends one text for all lines, so I first cut out the sentence for the line I'm looking at. It is a simple rule. Its weakness: it depends on LTA's wording. A better version could use an LLM or a bigger rule set.

**Why H2 and not Postgres?**
It keeps setup simple for a demo. Spring Data JPA means switching to Postgres is a config change plus a driver dependency. The history is lost on restart, and I would fix that first.

**How did you test it?**
Three layers. Service tests use a mocked client and a real in-memory database to check create, dedupe, resolve and failure handling. A MockMvc test checks the REST endpoints and JSON. Playwright tests drive the real UI in a browser with the API mocked, so they are fast and not flaky. There is also a small live suite for the full stack.

**Why mock the API in Playwright?**
Speed and reliability. The UI logic is what I'm testing there. The live suite (`E2E_LIVE=1`) covers the real backend.

**What does CI do?**
On every push: backend tests, frontend lint and build, Playwright tests, then a Docker build. Docker only builds if the rest passes.

**What would you improve?**
Postgres for history. Server-sent events instead of polling. Bus disruptions from other DataMall endpoints. Alerts for lines you care about. Retry with backoff when LTA fails.

## Know where things are

| If they ask about... | Open this file |
|---|---|
| The main logic | `backend/.../service/DisruptionService.java` |
| Real vs mock data | `backend/.../client/HttpLtaClient.java`, `MockLtaClient.java` |
| REST endpoints | `backend/.../web/DisruptionController.java` |
| The scheduler | `backend/.../service/PollingScheduler.java` |
| Backend tests | `backend/src/test/java/sg/tracker/` |
| The UI | `frontend/src/App.jsx` |
| E2E tests | `e2e/tests/dashboard.spec.js` |
| Pipeline | `.github/workflows/ci.yml` |

## Tips

- If you don't know something, say "I haven't done that, but I would approach it by..." and give a real plan. Interviewers prefer that to bluffing.
- Be ready to change one small thing live, like adding a severity keyword. Practise it once: edit `classify()`, rerun `mvn test`.
- Know the one weakness (severity by keywords) before they find it. Naming it first looks strong.
