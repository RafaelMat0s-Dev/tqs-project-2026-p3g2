# Mar Azul architecture

## Current shape

Mar Azul is a modular monolith with three local processes:

```text
React web client  --->  Spring Boot API  --->  in-memory adapters
                              ^                         |
                              |                         v
                       JWT staff security       fleet telemetry simulator
```

The API and simulator are separate containers. The frontend is served by
Nginx and calls the API over HTTP. The current adapters are deliberately
in-memory so the domain and HTTP contracts can be tested before PostgreSQL,
MQTT, payment, and accommodation integrations are introduced.

## Backend boundaries

The backend follows a simple dependency direction:

```text
API/controllers -> application services -> domain rules
                         |
                         v
                    ports/repositories -> infrastructure adapters
```

- `api`: HTTP controllers, request validation, and response DTOs.
- `application`: use-case orchestration and repository ports.
- `domain`: framework-independent vehicles, reservations, staff accounts,
  roles, and business-rule exceptions.
- `identity`: staff account creation, login, password hashing, JWT sessions,
  and role authorization.
- `infrastructure`: replaceable in-memory repository implementations.
- `simulator`: independent telemetry service reserved for the telemetry port.

Business rules stay in plain Java. External persistence and integrations must
be added behind ports instead of being called directly by domain classes.

## Implemented API slices

### Identity and authorization

- `POST /api/auth/login` authenticates an active staff account.
- `POST /api/auth/staff` creates a staff account and requires the
  `ADMINISTRATOR` role.
- Supported roles are `ADMINISTRATOR`, `COORDINATOR`, `TECHNICIAN`, and
  `CLIENT_MANAGER`.
- Passwords use Argon2id. Sessions use a signed JWT in an `HttpOnly`,
  `Secure`, `SameSite=Strict` cookie.
- Duplicate emails return `409`; disabled or invalid credentials return `401`.
- A valid user without the required role receives `403`.

### Fleet

- `POST /api/vehicles` registers a vehicle for an administrator or coordinator.
- `PATCH /api/vehicles/{id}/status` changes its lifecycle status for an
  administrator or coordinator.
- `GET /api/vehicles` exposes active vehicles to authenticated staff roles.
- A retired vehicle cannot be reactivated; the API returns `422` with
  `rule: BR-E4-01`.

### Reservations

`POST /api/bookings` accepts a vehicle ID, guest ID, start instant, and end
instant. The current service covers:

- `BR1`: vehicle existence, availability, and minimum battery.
- `BR7`: valid periods and overlapping reservations.
- `BR8`: one active reservation per guest.

## Error contract

All mapped API errors use RFC 9457 Problem Details with:

```json
{
  "type": "urn:marazul:erro:regra-de-negocio",
  "title": "Regra de negócio violada",
  "status": 422,
  "detail": "...",
  "instance": "/api/vehicles/...",
  "rule": "BR-E4-01"
}
```

The `rule` property is included for business-rule violations. Authorization
errors use dedicated `type` values for unauthenticated and forbidden access.

## Quality status

The backend currently has unit and Spring integration tests for identity,
authorization, fleet transitions, and reservation rules. The current test
command is:

```bash
cd backend && mvn -B test
```

The account repository and fleet/reservation repositories are temporary
in-memory adapters. The next persistence step should replace those adapters
without changing the domain or controller contracts.

## Next boundaries

The following work remains outside this skeleton:

- PostgreSQL persistence and migrations.
- Ticket assignment and technician-only ticket views.
- MQTT telemetry adapter and simulator integration.
- Frontend login, staff-account, and fleet screens.
- Cucumber scenarios, Xray evidence, and the user manual.
