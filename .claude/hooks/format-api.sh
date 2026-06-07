#!/usr/bin/env bash
# Stop hook: if any api/ Java files changed this turn, run Spotless once.
# Spotless is gradle-driven, so we format once per turn (not per edit) to avoid
# paying JVM startup repeatedly.
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel 2>/dev/null || echo "")"
[ -n "$repo_root" ] || exit 0

changed="$(git -C "$repo_root" status --porcelain -- 'api/**/*.java' 2>/dev/null || true)"
[ -n "$changed" ] || exit 0

( cd "$repo_root/api" && ./gradlew spotlessApply -q >/dev/null 2>&1 ) || exit 0
exit 0
