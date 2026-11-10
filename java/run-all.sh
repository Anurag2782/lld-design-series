#!/usr/bin/env bash
# Compiles every example and runs each Demo. Needs JDK 17+.
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out && mkdir -p out
javac --release 17 -d out $(find src -name '*.java')
pass=0
for f in $(find src -name Demo.java | sort); do
  cls=$(echo "${f#src/}" | sed 's#/#.#g; s#\.java$##')
  echo "=== $cls"
  java -cp out "$cls"
  pass=$((pass+1))
done
echo
echo "ALL $pass JAVA EXAMPLES PASSED"
