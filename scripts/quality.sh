#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
TARGET="${1:-all}"
SONAR_PORT="${SONAR_PORT:-9000}"
SONAR_URL="http://localhost:${SONAR_PORT}"

echo "==> SonarQube local (compose profile quality)"
mkdir -p .sonar
docker compose --profile quality up -d sonar-db sonarqube >/dev/null
docker compose --profile quality run --rm sonar-setup
TOKEN="$(cat .sonar/token)"

if [[ "$TARGET" == "all" || "$TARGET" == "backend" ]]; then
  echo "==> Backend: unit, integration and Cucumber tests, JaCoCo gates, SonarQube"
  if java -version 2>&1 | grep -q 'version "21'; then
    (cd backend && mvn -B -ntp verify sonar:sonar -Dsonar.host.url="$SONAR_URL" -Dsonar.token="$TOKEN")
  else
    docker run --rm \
      -v "$ROOT":/repo -w /repo/backend \
      -v "$HOME/.m2":/root/.m2 \
      -v /var/run/docker.sock:/var/run/docker.sock \
      -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
      --add-host=host.docker.internal:host-gateway \
      maven:3.9-eclipse-temurin-21 \
      mvn -B -ntp verify sonar:sonar \
        -Dsonar.host.url="http://host.docker.internal:${SONAR_PORT}" -Dsonar.token="$TOKEN"
  fi
fi

if [[ "$TARGET" == "all" || "$TARGET" == "frontend" ]]; then
  echo "==> Frontend: lint, types, Vitest with coverage, SonarQube"
  (
    cd frontend
    npm ci --no-audit --no-fund
    npm run lint
    npm run typecheck
    npm run test:coverage
    npm run sonar -- -Dsonar.host.url="$SONAR_URL" -Dsonar.token="$TOKEN"
  )
fi

echo
echo "Quality gate passed. Dashboards:"
echo "  $SONAR_URL/dashboard?id=marazul-backend"
echo "  $SONAR_URL/dashboard?id=marazul-frontend"
echo "  (login: admin / SONAR_ADMIN_PASSWORD, default in .env.example)"
