#!/usr/bin/env bash
set -euo pipefail

APP_HOME="${APP_HOME:-/app/poker}"
PLAYER_COUNT="${PLAYER_COUNT:-4}"
MATCH_HANDS="${MATCH_HANDS:-5}"
WEB_PORT="${WEB_PORT:-8080}"
WEB_CREDENTIALS="${WEB_CREDENTIALS:-}"
ALLOW_NO_AUTH="${ALLOW_NO_AUTH:-false}"

JAVA_CMD="java -cp .:models:game:evaluation:players:ui MainClass ${PLAYER_COUNT} ${MATCH_HANDS}"

echo "Starting Console Poker web terminal..."
echo "App home: ${APP_HOME}"
echo "Player count: ${PLAYER_COUNT}"
echo "Hands per match: ${MATCH_HANDS}"
echo "Web port: ${WEB_PORT}"

cd "${APP_HOME}"

if [[ "${ALLOW_NO_AUTH}" != "true" && -z "${WEB_CREDENTIALS}" ]]; then
  echo "Refusing to start without web auth."
  echo "Set WEB_CREDENTIALS=user:password or ALLOW_NO_AUTH=true."
  exit 1
fi

if [[ -n "${WEB_CREDENTIALS}" ]]; then
  echo "Basic auth enabled for web terminal."
  exec ttyd --writable --port "${WEB_PORT}" --credential "${WEB_CREDENTIALS}" bash -lc "${JAVA_CMD}"
fi

echo "Basic auth disabled. Set WEB_CREDENTIALS=user:password to enable."
exec ttyd --writable --port "${WEB_PORT}" bash -lc "${JAVA_CMD}"
