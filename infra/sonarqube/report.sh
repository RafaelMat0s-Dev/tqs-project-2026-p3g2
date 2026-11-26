#!/usr/bin/env bash
set -euo pipefail

SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"
: "${SONAR_TOKEN:?SONAR_TOKEN is required}"

get() { curl -fsS -u "$SONAR_TOKEN:" "$SONAR_HOST_URL/api/$1"; }

for project in "$@"; do
  status=$(get "qualitygates/project_status?projectKey=$project")
  echo "### $project: $(jq -r '.projectStatus.status' <<<"$status")"
  echo
  echo "| Métrica | Valor | Limite | Estado |"
  echo "| :- | -: | -: | :- |"
  jq -r '.projectStatus.conditions[] | "| \(.metricKey) | \(.actualValue // "-") | \(.comparator) \(.errorThreshold) | \(.status) |"' <<<"$status"
  echo
  issues=$(get "issues/search?componentKeys=$project&resolved=false&ps=10&s=SEVERITY&asc=false")
  total=$(jq -r '.total' <<<"$issues")
  echo "Problemas abertos: **$total** (os 10 mais graves)"
  echo
  if [ "$total" != "0" ]; then
    echo "| Severidade | Regra | Ficheiro | Mensagem |"
    echo "| :- | :- | :- | :- |"
    jq -r '.issues[] | "| \(.severity) | \(.rule) | \(.component | sub("^[^:]*:"; "")):\(.line // "-") | \(.message | gsub("\\|"; "/")) |"' <<<"$issues"
    echo
  fi
done
