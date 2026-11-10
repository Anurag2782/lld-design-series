#!/usr/bin/env bash
# Runs every example in every language you have installed. Missing toolchains are skipped.
set -uo pipefail
cd "$(dirname "$0")"
failed=0

run() {  # name, command...
  local name="$1"; shift
  echo "################ $name"
  if ! "$@"; then echo "!!! $name FAILED"; failed=1; fi
}

if command -v javac >/dev/null; then run "Java"       ./java/run-all.sh;               else echo "skip Java (no JDK 17+)"; fi
if command -v dotnet >/dev/null; then run ".NET"      bash -c 'cd dotnet && dotnet run'; else echo "skip .NET (no SDK 8+)"; fi
if command -v python3 >/dev/null; then run "Python"   bash -c 'cd python && python3 run_all.py'; else echo "skip Python"; fi
if command -v node >/dev/null; then run "TypeScript"  bash -c 'cd typescript && node src/run_all.ts'; else echo "skip TypeScript (no Node 22.18+)"; fi

[ $failed -eq 0 ] && echo && echo "EVERYTHING PASSED" || { echo; echo "SOMETHING FAILED"; exit 1; }
