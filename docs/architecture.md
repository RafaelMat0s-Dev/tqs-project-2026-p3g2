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

The `fleet` module owns the vehicle (UUID, unique `code` such as `B-17`, type,
base hub, status, last known battery). It persists in PostgreSQL
(`V3__fleet_vehicle.sql`); other modules use it only through `fleet.api.FleetApi`.

- `POST /api/vehicles` registers a vehicle (it starts `DISABLED`, "Inativa").
- `PATCH /api/vehicles/{id}/activate|deactivate|retire` move it through the
  lifecycle; any other pair of states is invalid (Figure 2, 13 valid pairs).
- `GET /api/vehicles?status=&hub=` lists vehicles.
- Roles: `COORDINATOR` and `TECHNICIAN`; other roles receive `403`, no session `401`.
- An invalid transition returns `422` with `rule: VEHICLE_LIFECYCLE` and the
  transition in `detail`; a duplicate code returns `409` (`VEHICLE_CODE`); a
  state changed by a concurrent request returns `409` (`VEHICLE_STATE_CHANGED`).
- Battery and `lastReadingAt` arrive from the telemetry event
  `TelemetryReadingReceived`; only newer readings are applied.
- The `booking` module reads and updates vehicles through `FleetVehicleRepository`
  (an adapter over `FleetApi`); an unknown battery counts as 0 %, so `BR1` refuses it.

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
  "instance": "/api/vehicles/.../activate",
  "rule": "VEHICLE_LIFECYCLE"
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

The account, reservation and trip repositories are temporary in-memory
adapters (the vehicle repository is already PostgreSQL, in `fleet`). The next persistence step should replace those adapters
without changing the domain or controller contracts.

## Next boundaries

The following work remains outside this skeleton:

- PostgreSQL persistence for identity, reservations and trips.
- Ticket assignment and technician-only ticket views.
- Frontend login, staff-account, and fleet screens.
- Cucumber scenarios, Xray evidence, and the user manual.
