#!/usr/bin/env bash
set -euo pipefail

NAME=${1:?experiment name required}
SWAP_MODE=${2:?swap mode required}
PATCH_SLEEP=${3:?patch sleep flag required}
EXPECT_STATE=${4:-campaign}
WINDOW_CONFIG=${5:-'{}'}
OUT="test_output/${NAME}"
mkdir -p "$OUT" .ci-build/fixer .ci-cache

cleanup() {
  cp /tmp/starsector-http.log "$OUT/http.log" 2>/dev/null || true
  git diff -- jars/Fixer.java jars/index.list launch.html build/final/wasm-modules/lwjgl.js starsector/starsector/data/scripts/world/SectorGen.java > "$OUT/candidate.patch" || true
  git diff --stat -- starsector/starsector > "$OUT/runtime-assets.stat" || true
  sha256sum jars/fixer_patch.jar jars/starfarer.api.jar jars/starfarer_obf.jar > "$OUT/runtime-sha256.txt" 2>/dev/null || true
}
trap cleanup EXIT

git lfs pull --include='jars/resources.jar' --exclude=''
test "$(wc -c < jars/resources.jar)" -gt 100000

OFFICIAL_URL=${STARSECTOR_OFFICIAL_ARCHIVE_URL:-https://f005.backblazeb2.com/file/fractalsoftworks/release/starsector_linux-0.98a-RC8.zip}
OFFICIAL_ZIP=${STARSECTOR_OFFICIAL_ARCHIVE:-.ci-cache/starsector_linux-0.98a-RC8.zip}
if [[ ! -s "$OFFICIAL_ZIP" ]] || ! unzip -tq "$OFFICIAL_ZIP" >/dev/null 2>&1; then
  rm -f "$OFFICIAL_ZIP"
  curl -fL --retry 3 --retry-delay 3 --connect-timeout 30 --max-time 900 \
    -o "$OFFICIAL_ZIP" "$OFFICIAL_URL"
fi
test "$(wc -c < "$OFFICIAL_ZIP")" -gt 200000000
python3 ci/restore-official-runtime-assets.py \
  --archive "$OFFICIAL_ZIP" \
  --root starsector/starsector \
  --section graphics \
  | tee "$OUT/official-asset-restore.log"

python3 ci/sanitize-runtime-assets.py | tee "$OUT/asset-sanitation.log"
if grep -RIl $'\xEF\xBB\xBF' starsector/starsector --include='index.list' > "$OUT/index-bom-files.txt"; then
  echo 'UTF-8 BOM remains in runtime index files:' >&2
  cat "$OUT/index-bom-files.txt" >&2
  exit 1
fi

test -s starsector/starsector/graphics/particlealpha32sq.png

# Keep browser bootstrap bounded. Full desktop world generation monopolizes the
# CheerpJ VM before CampaignState can ever render; the campaign can start in
# hyperspace and later gain richer world-generation compatibility separately.
python3 ci/patch-browser-sector-gen.py

python3 ci/patch-lwjgl-matrix-stack.py
python3 ci/patch-lwjgl-display-lists.py
grep -q 'LWJGL_MATRIX_STACK_GUARD_V1' build/final/wasm-modules/lwjgl.js
grep -q 'LWJGL_DISPLAY_LIST_NONFATAL_V1' build/final/wasm-modules/lwjgl.js

SWAP_YIELD_MODE="$SWAP_MODE" KEEP_UNSAFE_FORCE_ACTIVATION=0 \
  python3 ci/apply-campaign-runtime-fix.py
python3 ci/require-owned-title-state.py
python3 ci/enable-direct-ui-preflight.py
python3 ci/reject-partial-campaign-create.py
python3 ci/harden-settings-api-proxy.py
python3 ci/set-cheerpj-version.py

CP=$(find jars -maxdepth 1 -type f -name '*.jar' -printf '%p:' | sed 's/:$//')
mapfile -t COMPAT_SOURCES < <(find ci/java17-xstream -type f -name '*.java' -print | sort)
javac -encoding UTF-8 -source 8 -target 8 -cp "$CP" -d .ci-build/fixer \
  jars/Fixer.java "${COMPAT_SOURCES[@]}"
jar cf jars/fixer_patch.jar -C .ci-build/fixer .
javap -verbose -classpath jars/fixer_patch.jar Fixer | grep 'major version: 52'
javap -classpath jars/fixer_patch.jar com.thoughtworks.xstream.core.util.Fields \
  | grep 'public class com.thoughtworks.xstream.core.util.Fields'
javap -classpath jars/fixer_patch.jar com.fs.starfarer.MainThreadTransitionBridge \
  | grep 'public static void drain(java.lang.Object)'
python3 - <<'PY'
from pathlib import Path
p = Path('jars/index.list')
lines = [line for line in p.read_text(encoding='utf-8').splitlines() if line and not line.startswith('fixer_patch.jar')]
size = Path('jars/fixer_patch.jar').stat().st_size
lines.insert(0, f'fixer_patch.jar\t{size}')
p.write_text('\n'.join(lines) + '\n', encoding='utf-8')
PY

mkdir -p .ci-build/asm .ci-build/transform
if [[ ! -s .ci-build/asm/asm.jar ]]; then
  curl -fsSL -o .ci-build/asm/asm.jar \
    https://repo1.maven.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar
fi
javac -cp .ci-build/asm/asm.jar -d .ci-build/transform \
  ci/PatchCampaignOrbitalJunk.java ci/PatchCoreLifecycleBrowserWorld.java ci/PatchCampaignProcGen.java
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignOrbitalJunk jars/starfarer.api.jar .ci-build/starfarer-api-no-junk.jar
mv .ci-build/starfarer-api-no-junk.jar jars/starfarer.api.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCoreLifecycleBrowserWorld jars/starfarer.api.jar .ci-build/starfarer-api-browser-world.jar
mv .ci-build/starfarer-api-browser-world.jar jars/starfarer.api.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignProcGen jars/starfarer_obf.jar .ci-build/starfarer-no-procgen.jar
mv .ci-build/starfarer-no-procgen.jar jars/starfarer_obf.jar

if [[ "$PATCH_SLEEP" == "true" ]]; then
  javac -cp .ci-build/asm/asm.jar -d .ci-build/transform ci/PatchBaseGameState.java
  java -cp .ci-build/asm/asm.jar:.ci-build/transform \
    PatchBaseGameState jars/starfarer_obf.jar .ci-build/starfarer-no-sleep.jar
  mv .ci-build/starfarer-no-sleep.jar jars/starfarer_obf.jar
fi

javac -cp .ci-build/asm/asm.jar -d .ci-build/transform ci/PatchBaseGameStateTransition.java
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchBaseGameStateTransition jars/starfarer_obf.jar .ci-build/starfarer-transition.jar
mv .ci-build/starfarer-transition.jar jars/starfarer_obf.jar

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

# The stock ResourceLoaderState can legitimately take several minutes under
# headless CheerpJ while Java source/rules and restored graphics are decoded.
# Do not terminate the run before the lightweight campaign bootstrap patches
# have had a chance to execute.
STARSECTOR_TEST_URL=http://127.0.0.1:8000/launch.html \
STARSECTOR_TEST_TIMEOUT_MS=720000 \
STARSECTOR_FRAME_SETTLE_MS=30000 \
STARSECTOR_EXPECT_STATE="$EXPECT_STATE" \
STARSECTOR_WINDOW_CONFIG="$WINDOW_CONFIG" \
STARSECTOR_TEST_OUTPUT_DIR="$OUT" \
  node ci/campaign-render-test.js
