# Mar Azul base architecture

The initial skeleton is a modular monolith with a React web client and two independently deployable backend processes: the Mar Azul API and the fleet telemetry simulator.

The backend is organized around a dependency direction:

```text
API adapters -> application services -> domain
                    |                 |
                    v                 v
              infrastructure      integration ports
```

The first reservation slice uses in-memory adapters so the domain can be tested without Spring or a database. PostgreSQL, MQTT, payment, and accommodation imports will replace those adapters behind the same ports in later iterations.

## Initial boundaries

- `api`: HTTP DTOs, status codes, and error mapping.
- `application`: use-case orchestration and ports.
- `domain`: vehicle, reservation, and business rules.
- `infrastructure`: replaceable in-memory implementations for the skeleton.
- `frontend`: responsive web shell for end-user and operational views.
- `simulator`: separate process reserved for telemetry publishing.

## First reservation contract

`POST /api/bookings` accepts a vehicle ID, guest ID, start instant, and end instant. It returns `201 Created` for a valid reservation. A business-rule failure returns `422 Unprocessable Entity` with `{ "rule": "BRx", "message": "..." }`; conflict handling will become `409 Conflict` when the persistence-backed concurrency adapter is introduced.

The current implementation covers BR1, BR7, and BR8 in the application service and unit tests.
