#!/usr/bin/env bash
# PostToolUse(Edit|Write) hook: format edited web/ files with Prettier (fast).
# Reads the hook payload from stdin and formats only the edited file.
set -euo pipefail

payload="$(cat)"
file="$(printf '%s' "$payload" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))' 2>/dev/null || echo "")"

[ -n "$file" ] || exit 0
case "$file" in
  */web/*) ;;
  *) exit 0 ;;
esac
case "$file" in
  *.ts|*.tsx|*.js|*.jsx|*.mjs|*.cjs|*.json|*.css) ;;
  *) exit 0 ;;
esac
[ -f "$file" ] || exit 0

repo_root="$(git -C "$(dirname "$file")" rev-parse --show-toplevel 2>/dev/null || echo "")"
[ -n "$repo_root" ] || exit 0

( cd "$repo_root/web" && pnpm exec prettier --write "$file" >/dev/null 2>&1 ) || exit 0
exit 0
