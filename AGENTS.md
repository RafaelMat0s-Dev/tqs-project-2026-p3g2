# Mar Azul agent instructions

## Project shape

- `backend/`: Java 21 and Spring Boot API.
- `frontend/`: React and TypeScript web UI.
- `simulator/`: standalone fleet telemetry simulator.
- `docker-compose.yml`: local service composition.

## Development rules

- Keep business rules in framework-independent Java under `domain/`.
- Use ports for persistence, payments, stays, and telemetry integrations.
- Use `java.time.Clock` for time-dependent behavior; never call `now()` inside domain rules.
- API errors must preserve the rule identifier when a business rule is violated.
- Use English identifiers in code and Portuguese user-facing text/features when applicable.
- Do not commit secrets or personal data.

## Definition of done

- Acceptance criteria are represented by automated tests where applicable.
- Unit and integration tests pass locally.
- A second team member reviews the change before merge.
- The relevant Jira key appears in the branch, commit, and pull request.

## Commands

```bash
cd backend && mvn -B test
cd frontend && npm ci && npm run build
docker compose up --build
```
