#!/usr/bin/env bash
# Compile and package executable shaded JAR.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q clean package -DskipTests
mkdir -p build
cp target/wall-crack-detector-1.0.0.jar build/wall-crack-detector.jar
echo "Built build/wall-crack-detector.jar"
