#!/usr/bin/env bash
# Execute the regression suite.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn test
