#!/usr/bin/env bash
# Compile and package without downloading dependencies.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/classes
find src/main/java -name '*.java' -print | sort > build/main-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes @build/main-sources.txt
jar --create --file build/wall-crack-detector.jar --main-class org.wallcrack.app.Main -C build/classes .
echo "Built build/wall-crack-detector.jar"
