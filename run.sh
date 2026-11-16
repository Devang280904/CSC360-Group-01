#!/usr/bin/env bash
# Linux launcher for the Wall Crack Detector desktop app.
#
# On Linux, Java Swing only supports WHOLE-NUMBER UI scales (1, 2, 3) and ignores
# GNOME's fractional scaling, so on high-resolution laptop screens the app looks tiny.
# This script picks scale 2 automatically when the screen is 2560 px wide or more.
# Override it with:  UI_SCALE=1 ./run.sh   or   UI_SCALE=2 ./run.sh
# Extra arguments go to the app, e.g. ./run.sh --input photo.png --output out
set -euo pipefail
cd "$(dirname "$0")"

JAR="build/wall-crack-detector.jar"
if [[ ! -f "$JAR" ]]; then
    echo "[ERROR] $JAR not found. Build it first with: bash scripts/build.sh" >&2
    exit 1
fi

if [[ -z "${UI_SCALE:-}" ]]; then
    UI_SCALE=1
    width=$(xwininfo -root 2>/dev/null | awk '/Width:/ {print $2}') || true
    if [[ -n "${width:-}" && "$width" -ge 2560 ]]; then
        UI_SCALE=2
    fi
fi

exec java -Dsun.java2d.uiScale="$UI_SCALE" -jar "$JAR" "$@"
