#!/usr/bin/env bash
# Execute the same dependency-free regression suite used by Maven.
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/build.sh
mkdir -p build/test-classes
find src/test/java -name '*.java' -print | sort > build/test-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -cp build/classes -d build/test-classes @build/test-sources.txt
java -Djava.awt.headless=true -cp build/classes:build/test-classes org.wallcrack.AnalysisTestSuite
