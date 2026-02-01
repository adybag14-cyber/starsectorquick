#!/usr/bin/env bash
set -euo pipefail

GAME_URLS=${GAME_URLS:-"http://localhost:8888/STARSECTOR_V6J_FINAL_WORKING.html,http://localhost:8888/launch.html,http://localhost:8888/index.html"}
GAME_JAVA_VERSIONS=${GAME_JAVA_VERSIONS:-"8"}
PW_ATTEMPTS=${PW_ATTEMPTS:-"2"}
START_SERVER=${START_SERVER:-"1"}
SERVER_PORT=${SERVER_PORT:-"8888"}

JAR_ROOT=${JAR_ROOT:-""}
ASSET_ROOT=${ASSET_ROOT:-""}

server_pid=""

cleanup() {
  if [[ -n "$server_pid" ]]; then
    echo "Stopping server (pid $server_pid)..."
    kill "$server_pid" 2>/dev/null || true
    wait "$server_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT

mkdir -p test_output

if [[ "$START_SERVER" == "1" ]]; then
  echo "Starting node server on port ${SERVER_PORT}..."
  if [[ -n "$JAR_ROOT" ]]; then
    export JAR_ROOT
  fi
  if [[ -n "$ASSET_ROOT" ]]; then
    export ASSET_ROOT
  fi
  PORT="$SERVER_PORT" node server.js > test_output/server.log 2>&1 &
  server_pid=$!
  sleep 2
fi

IFS=',' read -r -a url_list <<< "$GAME_URLS"
IFS=',' read -r -a java_versions <<< "$GAME_JAVA_VERSIONS"

for url in "${url_list[@]}"; do
  for java_version in "${java_versions[@]}"; do
    echo "Running Playwright against ${url} (java=${java_version})..."
    GAME_URL="$url" GAME_JAVA_VERSION="$java_version" PW_ATTEMPTS="$PW_ATTEMPTS" node run_playwright_game.js
  done
done
