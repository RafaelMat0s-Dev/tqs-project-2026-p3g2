# Mar Azul

Mar Azul is a managed light-mobility fleet service for the fictional Alvina Mar resort. The repository contains the Spring Boot API, React web client, telemetry simulator, quality documentation, and deployment skeleton for the TQS project.

## Run locally

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend health: http://localhost:8080/actuator/health
- Telemetry simulator: http://localhost:8090

See [docs/architecture.md](docs/architecture.md) for the initial boundaries and the reservation contract.
