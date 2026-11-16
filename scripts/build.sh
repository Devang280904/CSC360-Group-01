#!/usr/bin/env bash
# Test and package the executable shaded JAR.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q clean verify
mkdir -p build
cp target/wall-crack-detector-1.0.0.jar build/wall-crack-detector.jar
# Remove Maven intermediates; only the runnable JAR is needed locally.
rm -rf target dependency-reduced-pom.xml
echo "Built build/wall-crack-detector.jar"
