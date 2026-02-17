#!/usr/bin/env bash
set -euo pipefail

JAR_PATH=${1:-}
OUTPUT_FILE=${OUTPUT_FILE:-/workspace/starsectorquick/test_output/jni_exports.txt}

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

scratch_file=$(mktemp)
trap 'rm -f "$scratch_file"' EXIT

while IFS= read -r cls; do
  javap -classpath "$JAR_PATH" -s "$cls" | awk -v class="$cls" '
    /native/ && $0 ~ /\(/ {
      line=$0
      sub(/\(.*/, "", line)
      n=split(line, parts, /[[:space:]]+/)
      pending=parts[n]
      next
    }
    /Descriptor:/ && pending != "" {
      print class "\t" pending "\t" $2
      pending=""
    }
  ' >> "$scratch_file"
done <<< "$classes"

{
  echo "# JNI exports derived from $JAR_PATH"
  echo "# Generated on $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo
} > "$OUTPUT_FILE"

awk '
  function mangle(str,  out,i,c) {
    out=""
    for (i = 1; i <= length(str); i++) {
      c = substr(str, i, 1)
      if (c == "_") {
        out = out "_1"
      } else if (c == ";") {
        out = out "_2"
      } else if (c == "[") {
        out = out "_3"
      } else if (c == "/") {
        out = out "_"
      } else {
        out = out c
      }
    }
    return out
  }
  {
    class=$1
    method=$2
    descriptor=$3
    gsub(/\./, "/", class)
    base="Java_" mangle(class) "_" mangle(method)
    print base
    params=descriptor
    sub(/^\(/, "", params)
    sub(/\).*/, "", params)
    if (params != "") {
      print base "__" mangle(params)
    }
  }
' "$scratch_file" | sort -u >> "$OUTPUT_FILE"

echo "JNI exports written to $OUTPUT_FILE"
