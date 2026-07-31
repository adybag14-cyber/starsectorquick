#!/usr/bin/env bash
set -euo pipefail

NAME=${1:?experiment name required}
SWAP_MODE=${2:?swap mode required}
PATCH_SLEEP=${3:?patch sleep flag required}
EXPECT_STATE=${4:-campaign}
WINDOW_CONFIG=${5:-'{}'}
OUT="test_output/${NAME}"
mkdir -p "$OUT" .ci-build/fixer

cleanup() {
  cp /tmp/starsector-http.log "$OUT/http.log" 2>/dev/null || true
  git diff -- jars/Fixer.java jars/index.list launch.html build/final/wasm-modules/lwjgl.js > "$OUT/candidate.patch" || true
  git diff --stat -- starsector/starsector > "$OUT/runtime-assets.stat" || true
  sha256sum jars/fixer_patch.jar jars/starfarer_obf.jar > "$OUT/runtime-sha256.txt" 2>/dev/null || true
}
trap cleanup EXIT

git lfs pull --include='jars/resources.jar' --exclude=''
test "$(wc -c < jars/resources.jar)" -gt 100000

python3 ci/sanitize-runtime-assets.py | tee "$OUT/asset-sanitation.log"
if grep -RIl $'\xEF\xBB\xBF' starsector/starsector --include='index.list' > "$OUT/index-bom-files.txt"; then
  echo 'UTF-8 BOM remains in runtime index files:' >&2
  cat "$OUT/index-bom-files.txt" >&2
  exit 1
fi

test -s starsector/starsector/graphics/particlealpha32sq.png
cmp starsector/starsector/graphics/fx/particlealpha32sq.png \
    starsector/starsector/graphics/particlealpha32sq.png

SWAP_YIELD_MODE="$SWAP_MODE" KEEP_UNSAFE_FORCE_ACTIVATION=0 \
  python3 ci/apply-campaign-runtime-fix.py

CP=$(find jars -maxdepth 1 -type f -name '*.jar' -printf '%p:' | sed 's/:$//')
javac -encoding UTF-8 -source 8 -target 8 -cp "$CP" -d .ci-build/fixer jars/Fixer.java
jar cf jars/fixer_patch.jar -C .ci-build/fixer .
javap -verbose -classpath jars/fixer_patch.jar Fixer | grep 'major version: 52'
python3 - <<'PY'
from pathlib import Path
p = Path('jars/index.list')
lines = [line for line in p.read_text(encoding='utf-8').splitlines() if line and not line.startswith('fixer_patch.jar')]
size = Path('jars/fixer_patch.jar').stat().st_size
lines.insert(0, f'fixer_patch.jar\t{size}')
p.write_text('\n'.join(lines) + '\n', encoding='utf-8')
PY

if [[ "$PATCH_SLEEP" == "true" ]]; then
  mkdir -p .ci-build/asm .ci-build/transform
  curl -fsSL -o .ci-build/asm/asm.jar \
    https://repo1.maven.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar
  javac -cp .ci-build/asm/asm.jar -d .ci-build/transform ci/PatchBaseGameState.java
  java -cp .ci-build/asm/asm.jar:.ci-build/transform \
    PatchBaseGameState jars/starfarer_obf.jar .ci-build/starfarer_obf.jar
  mv .ci-build/starfarer_obf.jar jars/starfarer_obf.jar
fi

npm ci
npx playwright install --with-deps chromium
STATIC_ROOT="$PWD" STATIC_HOST=127.0.0.1 STATIC_PORT=8000 \
  node ci/range-server.js > /tmp/starsector-http.log 2>&1 &
echo $! > /tmp/starsector-http.pid
for _ in {1..30}; do
  if curl -fsS -H 'Range: bytes=0-0' http://127.0.0.1:8000/launch.html >/dev/null; then
    break
  fi
  sleep 1
done
curl -fsS -H 'Range: bytes=0-0' http://127.0.0.1:8000/launch.html >/dev/null

STARSECTOR_TEST_URL=http://127.0.0.1:8000/launch.html \
STARSECTOR_TEST_TIMEOUT_MS=240000 \
STARSECTOR_FRAME_SETTLE_MS=15000 \
STARSECTOR_EXPECT_STATE="$EXPECT_STATE" \
STARSECTOR_WINDOW_CONFIG="$WINDOW_CONFIG" \
STARSECTOR_TEST_OUTPUT_DIR="$OUT" \
  node ci/campaign-render-test.js
