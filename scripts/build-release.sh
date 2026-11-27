#!/usr/bin/env bash
# Builds ONE runnable JAR with the React app inside: backend/target/healthdekho.jar
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STATIC="$ROOT/backend/src/main/resources/static"

echo "==> Building the React app"
cd "$ROOT/frontend"
npm install
npm run build

echo "==> Copying the React build into the backend"
rm -rf "$STATIC"
mkdir -p "$STATIC"
cp -R "$ROOT/frontend/dist/." "$STATIC/"

echo "==> Building the Spring Boot JAR"
cd "$ROOT/backend"
if [ -x ./mvnw ]; then MVN=./mvnw; else MVN=mvn; fi
"$MVN" -B -DskipTests package

echo "Release JAR: $ROOT/backend/target/healthdekho.jar"
