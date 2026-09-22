#!/usr/bin/env bash
set -euo pipefail

CASE_ID="${1:-all}"
TIMEOUT_MS="${PROJECTX_WEBWALKER_TIMEOUT_MS:-1800000}"
LEG_TIMEOUT_MS="${PROJECTX_WEBWALKER_LEG_TIMEOUT_MS:-240000}"
OUTPUT_DIR="${PROJECTX_WEBWALKER_OUTPUT_DIR:-$HOME/.runelite/test-results/f2p-webwalker}"
USE_TELEPORTATION_SPELLS="${PROJECTX_WEBWALKER_USE_TELEPORTATION_SPELLS:-}"

cd "$(dirname "$0")/.."

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

./gradlew :client:compileJava

CMD=(
  ./gradlew :client:runTest
  -Dprojectx.test.mode=true
  "-Dprojectx.test.script=F2P Web Walker Harness"
  "-Dprojectx.test.timeout=$TIMEOUT_MS"
  "-Dprojectx.test.output=$OUTPUT_DIR"
  -Dprojectx.test.webwalker.stopOnFailure=true
  "-Dprojectx.test.webwalker.walkTimeoutMs=$LEG_TIMEOUT_MS"
)

if [[ "$CASE_ID" != "all" ]]; then
  CMD+=("-Dprojectx.test.webwalker.case=$CASE_ID")
fi

if [[ -n "$USE_TELEPORTATION_SPELLS" ]]; then
  CMD+=("-Dprojectx.test.webwalker.useTeleportationSpells=$USE_TELEPORTATION_SPELLS")
fi

set +e
"${CMD[@]}"
STATUS=$?
set -e

RESULT_FILE="$OUTPUT_DIR/result.json"
if [[ -f "$RESULT_FILE" ]]; then
  echo "Result written to $RESULT_FILE"
else
  echo "No result file was written at $RESULT_FILE" >&2
fi

exit "$STATUS"
