#!/bin/sh
set -eu

SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"
SONAR_ADMIN_PASSWORD="${SONAR_ADMIN_PASSWORD:-MarAzul-local-2026!}"
SONAR_TOKEN_FILE="${SONAR_TOKEN_FILE:-}"
GATE="MarAzul"

log() { printf '[sonar-setup] %s\n' "$*" >&2; }

log "waiting for $SONAR_HOST_URL"
attempt=0
until curl -fsS "$SONAR_HOST_URL/api/system/status" 2>/dev/null | grep -q '"status":"UP"'; do
  attempt=$((attempt + 1))
  if [ "$attempt" -gt 120 ]; then
    log "SonarQube is not UP after 10 minutes"
    exit 1
  fi
  sleep 5
done

if curl -fsS -u "admin:$SONAR_ADMIN_PASSWORD" "$SONAR_HOST_URL/api/authentication/validate" | grep -q '"valid":true'; then
  log "admin password already set"
else
  log "replacing the default admin password"
  curl -fsS -u admin:admin -X POST "$SONAR_HOST_URL/api/users/change_password" \
    --data-urlencode login=admin --data-urlencode previousPassword=admin \
    --data-urlencode "password=$SONAR_ADMIN_PASSWORD"
fi

api() {
  path="$1"
  shift
  curl -fsS -u "admin:$SONAR_ADMIN_PASSWORD" -X POST "$SONAR_HOST_URL/api/$path" "$@"
}

api qualitygates/set_as_default --data-urlencode "name=Sonar way" >/dev/null
api qualitygates/destroy --data-urlencode "name=$GATE" >/dev/null 2>&1 || true
api qualitygates/create --data-urlencode "name=$GATE" >/dev/null

condition() {
  existing=$(curl -fsS -u "admin:$SONAR_ADMIN_PASSWORD" "$SONAR_HOST_URL/api/qualitygates/show?name=$GATE")
  case "$existing" in
    *"\"metric\":\"$1\""*)
      log "  $1 (already in the gate)"
      return 0
      ;;
  esac
  answer=$(curl -sS -u "admin:$SONAR_ADMIN_PASSWORD" -X POST "$SONAR_HOST_URL/api/qualitygates/create_condition" \
    --data-urlencode "gateName=$GATE" --data-urlencode "metric=$1" --data-urlencode "op=$2" --data-urlencode "error=$3")
  case "$answer" in
    *'"errors"'*)
      log "  $1: $answer"
      exit 1
      ;;
  esac
  log "  $1 $2 $3"
}

log "quality gate $GATE"
condition new_coverage LT 80
condition new_duplicated_lines_density GT 3
condition new_security_hotspots_reviewed LT 100
condition new_violations GT 0
condition coverage LT 80
condition duplicated_lines_density GT 3
condition software_quality_reliability_rating GT 1
condition software_quality_security_rating GT 1
condition software_quality_maintainability_rating GT 1
api qualitygates/set_as_default --data-urlencode "name=$GATE" >/dev/null

api user_tokens/revoke --data-urlencode name=marazul-analysis >/dev/null 2>&1 || true
TOKEN=$(api user_tokens/generate --data-urlencode name=marazul-analysis --data-urlencode type=GLOBAL_ANALYSIS_TOKEN \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
  log "could not generate the analysis token"
  exit 1
fi

if [ -n "$SONAR_TOKEN_FILE" ]; then
  printf '%s' "$TOKEN" > "$SONAR_TOKEN_FILE"
  log "analysis token written to $SONAR_TOKEN_FILE"
else
  printf '%s\n' "$TOKEN"
fi
log "ready: $SONAR_HOST_URL"
