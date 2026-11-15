#!/usr/bin/env bash
# Execute the regression suite and enforce coverage.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn verify
