# MarAzul agent instructions

## Project shape

- `backend/`: Java 21 and Spring Boot 4.1 API, a modular monolith with ports and adapters.
  - One package per epic under `pt.marazul`: `identity`, `contract`, `network`, `fleet`, `telemetry`, `booking`, `autonomy`, `trip`, `payment`, `maintenance`, `metrics`, `gbfs`; cross-cutting code in `shared`.
  - Inside each module: `api` (REST, DTOs, public facade), `application` (use cases, outbound ports), `domain` (plain Java), `infrastructure` (JPA, external adapters).
- `frontend/`: React 19 and TypeScript web UI (Vite), served by nginx, which proxies `/api` to the backend.
- `simulator/`: standalone fleet telemetry simulator (MQTT through Mosquitto).
- `perf/`: k6 load tests with SLO thresholds. `infra/`: Mosquitto and SonarQube configuration.
- `docker-compose.yml`: the whole application with `docker compose up`; SonarQube with `--profile quality`.

## Development rules

- Keep business rules in framework-independent Java under `domain/`. ArchUnit (`ArchitectureTest`) fails the build if `domain` imports Spring, Jakarta, Hibernate or Jackson, or calls `now()`.
- A module uses another module only through its `api` package or its events; `shared` depends on no module.
- Use ports for persistence, payments, stays, and telemetry integrations.
- Use `java.time.Clock` for time-dependent behavior; never call `now()` inside domain rules. Tests use `Clock.fixed(...)`.
- Money is an integer number of cents, never `double`. Instants are stored in UTC; the operating zone is Europe/Lisbon.
- A violated business rule throws `BusinessRuleViolation` (422) or `BusinessRuleConflict` (409) from `shared.domain` with the rule id (`BR1`…`BR16`); `ApiExceptionHandler` returns Problem Details with the `rule` field.
- Rule values come from the contract (specification section 3.2), never invented and never hard-coded.
- Use English identifiers in code and Portuguese user-facing text and `.feature` files.
- Do not commit secrets or personal data; test data is synthetic and uses the names of the specification scenarios.

## Tests

- Unit tests are `*Test` (Surefire); integration tests are `*IT` (Failsafe) and use `@Import(TestcontainersConfiguration.class)` for a real PostgreSQL. Never H2.
- Mockito only for ports. Use Awaitility for asynchronous code, never `Thread.sleep`.
- Every business rule has parameterized tests on its boundaries (for example 24 % / 25 % for BR1).
- Acceptance criteria are Gherkin scenarios in `backend/src/test/resources/features/E<n>/US-E<n>-<yy>_*.feature`, tagged `@REQ_SCRUM-<n> @US-E<n>-<yy> @E<n> @BR<x>`. `@wip` and `@quarentena` do not run. Reuse `ApiSteps`.
- Whoever implements a story writes its `.feature` from the Jira acceptance criteria, with `@wip`; QA reviews the scenarios in the pull request before `@wip` is removed.
- TDD with a `test(...)` commit before the `feat(...)` commit for the autonomy engine and pricing.
- Coverage gates: 80 % overall and 90 % per `domain` package (JaCoCo), and the SonarQube "MarAzul" gate.

## Definition of done

- Acceptance criteria are represented by automated tests where applicable and the `.feature` runs without `@wip`.
- Unit and integration tests pass locally (`mvn verify`, `npm run test:coverage`) and the CI pipeline is green, including `quality-gate` and `dependencias`.
- A second team member reviews the change before merge. Agents never approve or merge.
- The relevant Jira key (`SCRUM-<n>`) appears in the branch, commit, and pull request, and the PR has one `ia:*` label.

## Commands

```bash
docker compose up --build                       # whole application on http://localhost:5173
cd backend && mvn -B verify                     # unit, integration, Cucumber, ArchUnit, JaCoCo gates
cd frontend && npm ci && npm run lint && npm run typecheck && npm run test:coverage && npm run build
cd simulator && npm ci && npm test
./scripts/quality.sh                            # local SonarQube analysis with the quality gate
```
