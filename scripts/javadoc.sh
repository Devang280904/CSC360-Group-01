#!/usr/bin/env bash
# Generate API documentation with strict Javadoc validation.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build
find src/main/java -name '*.java' -print | sort > build/main-sources.txt
javadoc --release 17 -encoding UTF-8 -quiet -Xdoclint:all -Werror -d build/docs @build/main-sources.txt
echo "Javadoc generated at build/docs/index.html"
