#!/usr/bin/env bash
set -euo pipefail

JAR_PATH=${1:-}
OUTPUT_FILE=${OUTPUT_FILE:-/workspace/starsectorquick/test_output/native_methods.txt}

if [[ -z "$JAR_PATH" ]]; then
  echo "Usage: $0 /path/to/starfarer_obf.jar" >&2
  exit 1
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Jar not found: $JAR_PATH" >&2
  exit 1
fi

if ! command -v javap >/dev/null 2>&1; then
  echo "javap not found. Install a JDK (e.g., apt-get install openjdk-17-jdk)" >&2
  exit 1
fi

mkdir -p "$(dirname "$OUTPUT_FILE")"

classes=$(jar tf "$JAR_PATH" | grep '\.class$' | sed 's#/#.#g' | sed 's/\.class$//')

{
  echo "# Native methods from $JAR_PATH"
  echo "# Generated on $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo
} > "$OUTPUT_FILE"

while IFS= read -r cls; do
  if javap -classpath "$JAR_PATH" -s "$cls" 2>/dev/null | grep -q "native"; then
    echo "## $cls" >> "$OUTPUT_FILE"
    javap -classpath "$JAR_PATH" -s "$cls" | grep -E "native|Descriptor" >> "$OUTPUT_FILE"
    echo >> "$OUTPUT_FILE"
  fi
done <<< "$classes"

echo "Native method list written to $OUTPUT_FILE"
