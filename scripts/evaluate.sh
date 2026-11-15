#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q test-compile exec:java \
  -Dexec.mainClass=org.wallcrack.FieldEvaluation \
  -Dexec.classpathScope=test \
  -Dexec.args="evaluation/labels.csv evaluation/images"
