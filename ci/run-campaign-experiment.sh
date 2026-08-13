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
  git diff -- jars/Fixer.java jars/index.list launch.html build/final/wasm-modules/lwjgl.js data/scripts/world/SectorGen.java starsector/starsector/data/scripts/world/SectorGen.java > "$OUT/candidate.patch" || true
  git diff --stat -- starsector/starsector > "$OUT/runtime-assets.stat" || true
  sha256sum jars/fixer_patch.jar jars/starfarer.api.jar jars/starfarer_obf.jar jars/scripts-precompiled.jar jars/txw2-2.3.1.jar > "$OUT/runtime-sha256.txt" 2>/dev/null || true
}
trap cleanup EXIT

git lfs pull --include='jars/resources.jar' --exclude=''
test "$(wc -c < jars/resources.jar)" -gt 100000

OFFICIAL_URL=${STARSECTOR_OFFICIAL_ARCHIVE_URL:-https://f005.backblazeb2.com/file/fractalsoftworks/release/starsector_linux-0.98a-RC8.zip}
OFFICIAL_ZIP=${STARSECTOR_OFFICIAL_ARCHIVE:-.ci-cache/starsector_linux-0.98a-RC8.zip}
if [[ ! -s "$OFFICIAL_ZIP" ]] || ! unzip -tq "$OFFICIAL_ZIP" >/dev/null 2>&1; then
  rm -f "$OFFICIAL_ZIP"
  curl -fL --retry 8 --retry-all-errors --retry-delay 3 --retry-max-time 1200 \
    --connect-timeout 30 --max-time 900 --continue-at - \
    -o "$OFFICIAL_ZIP" "$OFFICIAL_URL"
fi
test "$(wc -c < "$OFFICIAL_ZIP")" -gt 200000000
python3 ci/restore-official-runtime-assets.py \
  --archive "$OFFICIAL_ZIP" \
  --root starsector/starsector \
  --section graphics \
  | tee "$OUT/official-asset-restore.log"

python3 ci/sanitize-runtime-assets.py | tee "$OUT/asset-sanitation.log"
if [[ "${STARSECTOR_MINIMAL_ASHARU_ECONOMY:-false}" == "true" ]]; then
  python3 ci/prepare-browser-minimal-economy.py | tee "$OUT/browser-economy.log"
fi
# Collapse the stock ship/weapon JSON-like spec files into one small browser cache.
# LoadingUtils still parses and registers every spec normally; this removes the
# per-file HTTP filesystem round trip for the stock no-mod quick-start path.
python3 ci/build-browser-spec-cache.py --root starsector/starsector
test -s starsector/starsector/data/browser-spec-cache-v1.json
if grep -RIl $'\xEF\xBB\xBF' starsector/starsector --include='index.list' > "$OUT/index-bom-files.txt"; then
  echo 'UTF-8 BOM remains in runtime index files:' >&2
  cat "$OUT/index-bom-files.txt" >&2
  exit 1
fi

test -s starsector/starsector/graphics/particlealpha32sq.png

# Keep the loose browser source copies lightweight as a Janino fallback. The
# class actually used by the current launcher is also patched directly in
# scripts-precompiled.jar below.
python3 ci/patch-browser-sector-gen.py

python3 ci/patch-lwjgl-matrix-stack.py
python3 ci/patch-lwjgl-display-lists.py
grep -q 'LWJGL_MATRIX_STACK_GUARD_V1' build/final/wasm-modules/lwjgl.js
grep -q 'LWJGL_DISPLAY_LIST_NONFATAL_V1' build/final/wasm-modules/lwjgl.js
grep -q 'LWJGL_CLIENT_ARRAY_COMPAT_V1' build/final/wasm-modules/lwjgl.js
grep -q 'LWJGL_ALPHA_TEST_COMPAT_V1' build/final/wasm-modules/lwjgl.js
grep -q 'LWJGL_ATTRIB_STACK_COMPAT_V1' build/final/wasm-modules/lwjgl.js
python3 ci/verify-lwjgl-fixed-function.py build/final/wasm-modules/lwjgl.js
python3 ci/verify-lwjgl-no-sync-validation.py
python3 ci/verify-fatal-console-classification.py
# The browser quick-start keeps the stock 45s fallback available via override,
# but defaults the successful create-settle gate to 15s. Guard both the default
# and Java property propagation so this latency win cannot silently regress.
grep -q '__STARSECTOR_AUTO_CAMPAIGN_DIRECT_CREATE_SETTLE_MS__ ?? 15000' launch.html
grep -q 'Number.isFinite(parsedAutoCampaignDirectCreateSettleMs)' launch.html
grep -q 'starsector.autoCampaignDirectCreateSettleMs=${autoCampaignDirectCreateSettleMs}' launch.html
grep -q '__STARSECTOR_BROWSER_BULK_SPEC_CACHE__' launch.html
grep -q 'const browserSpecCachePath = `${contentRoot}data/browser-spec-cache-v1.json`' launch.html
grep -q 'starsector.browserSpecCachePath=${browserSpecCachePath}' launch.html
node ci/verify-service-worker-negative-cache.js
node ci/verify-campaign-center-subject.js

# Rebuild the browser-facing LWJGL bridge classes. GL11 owns the fixed-function
# compatibility/fast paths; Display and the input classes own live DOM-backed
# keyboard/mouse polling. Keeping them in one compile step ensures the runtime
# never falls back to the old always-false input stubs in bridge.jar.
rm -rf .ci-build/bridge-runtime
mkdir -p .ci-build/bridge-runtime
javac -encoding UTF-8 -source 8 -target 8 \
  -cp "jars/bridge.jar:jars/lwjgl.jar" \
  -d .ci-build/bridge-runtime \
  bridge_src/org/lwjgl/opengl/GL11.java \
  bridge_src/org/lwjgl/opengl/Display.java \
  bridge_src/org/lwjgl/input/Keyboard.java \
  bridge_src/org/lwjgl/input/Mouse.java
jar uf jars/bridge.jar \
  -C .ci-build/bridge-runtime org/lwjgl/opengl/GL11.class \
  -C .ci-build/bridge-runtime org/lwjgl/opengl/Display.class \
  -C .ci-build/bridge-runtime org/lwjgl/input/Keyboard.class \
  -C .ci-build/bridge-runtime org/lwjgl/input/Mouse.class
javap -classpath jars/bridge.jar -c org.lwjgl.opengl.GL11 > .ci-build/bridge-runtime-gl11.javap
javap -classpath jars/bridge.jar -c org.lwjgl.opengl.Display > .ci-build/bridge-runtime-display.javap
javap -classpath jars/bridge.jar -c org.lwjgl.input.Keyboard > .ci-build/bridge-runtime-keyboard.javap
javap -classpath jars/bridge.jar -c org.lwjgl.input.Mouse > .ci-build/bridge-runtime-mouse.javap
python3 ci/verify-bridge-client-arrays.py .ci-build/bridge-runtime-gl11.javap
python3 ci/verify-browser-input-bridge.py \
  .ci-build/bridge-runtime-display.javap \
  .ci-build/bridge-runtime-keyboard.javap \
  .ci-build/bridge-runtime-mouse.javap \
  build/final/wasm-modules/lwjgl.js

SWAP_YIELD_MODE="$SWAP_MODE" KEEP_UNSAFE_FORCE_ACTIVATION=0 \
  python3 ci/apply-campaign-runtime-fix.py
python3 ci/require-owned-title-state.py
python3 ci/enable-direct-ui-preflight.py
python3 ci/reject-partial-campaign-create.py
python3 ci/harden-settings-api-proxy.py
python3 ci/set-cheerpj-version.py

CP=$(find jars -maxdepth 1 -type f -name '*.jar' -printf '%p:' | sed 's/:$//')
javap -classpath "$CP" com.sun.xml.txw2.output.IndentingXMLStreamWriter >/dev/null
rm -rf .ci-build/spec-cache-helper .ci-build/verify-bulk-spec-cache
mkdir -p .ci-build/spec-cache-helper .ci-build/verify-bulk-spec-cache
javac -encoding UTF-8 --release 8 -cp "$CP" \
  -d .ci-build/spec-cache-helper ci/BrowserSpecCache.java
javac -encoding UTF-8 --release 8 -cp ".ci-build/spec-cache-helper:$CP" \
  -d .ci-build/verify-bulk-spec-cache ci/VerifyBulkSpecCache.java
java -cp ".ci-build/verify-bulk-spec-cache:.ci-build/spec-cache-helper:$CP" \
  VerifyBulkSpecCache "$PWD/starsector/starsector/data/browser-spec-cache-v1.json"
mapfile -t COMPAT_SOURCES < <(find ci/java17-xstream -type f -name '*.java' -print | sort)
javac -encoding UTF-8 -source 8 -target 8 -cp "$CP" -d .ci-build/fixer \
  jars/Fixer.java "${COMPAT_SOURCES[@]}"
jar cf jars/fixer_patch.jar -C .ci-build/fixer .
javap -verbose -classpath jars/fixer_patch.jar Fixer | grep 'major version: 52'
javap -classpath jars/fixer_patch.jar -c -p Fixer \
  | grep -q 'MainThreadTransitionBridge.disableTitleHandoff'
javap -classpath jars/fixer_patch.jar com.thoughtworks.xstream.core.util.Fields \
  | grep 'public class com.thoughtworks.xstream.core.util.Fields'
javap -classpath jars/fixer_patch.jar com.thoughtworks.xstream.core.util.SerializationMembers \
  | grep 'public class com.thoughtworks.xstream.core.util.SerializationMembers'
javap -classpath jars/fixer_patch.jar com.fs.starfarer.MainThreadTransitionBridge \
  | grep 'public static void drain(java.lang.Object)'
javap -classpath jars/fixer_patch.jar com.fs.starfarer.MainThreadTransitionBridge \
  | grep 'public static boolean isTitleHandoffActive()'
javap -classpath jars/fixer_patch.jar com.fs.starfarer.MainThreadTransitionBridge \
  | grep 'public static void disableTitleHandoff()'
mkdir -p .ci-build/verify-starting-supplies
javac -encoding UTF-8 -source 8 -target 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-starting-supplies ci/VerifyStartingSupplies.java
java -cp ".ci-build/verify-starting-supplies:jars/fixer_patch.jar:$CP" VerifyStartingSupplies
mkdir -p .ci-build/verify-texture-upload
javac -encoding UTF-8 -source 8 -target 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-texture-upload ci/VerifyTextureUploadCompat.java
java -cp ".ci-build/verify-texture-upload:jars/fixer_patch.jar:$CP" VerifyTextureUploadCompat
mkdir -p .ci-build/verify-texture-assets
javac -encoding UTF-8 -source 8 -target 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-texture-assets ci/VerifyTextureAssets.java
java -Xmx3g -cp ".ci-build/verify-texture-assets:jars/fixer_patch.jar:$CP" \
  VerifyTextureAssets starsector/starsector/graphics
mkdir -p .ci-build/verify-xstream
javac -encoding UTF-8 -source 8 -target 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-xstream ci/VerifyJava17XStreamCompat.java
java -cp ".ci-build/verify-xstream:jars/fixer_patch.jar:$CP" VerifyJava17XStreamCompat
python3 - <<'PY'
from pathlib import Path
p = Path('jars/index.list')
lines = [line for line in p.read_text(encoding='utf-8').splitlines() if line and not line.startswith('fixer_patch.jar')]
size = Path('jars/fixer_patch.jar').stat().st_size
lines.insert(0, f'fixer_patch.jar\t{size}')
p.write_text('\n'.join(lines) + '\n', encoding='utf-8')
PY

mkdir -p .ci-build/asm .ci-build/transform
ASM_JAR=.ci-build/asm/asm.jar
ASM_SHA256=8cadd43ac5eb6d09de05faecca38b917a040bb9139c7edeb4cc81c740b713281
verify_asm_jar() {
  [[ -s "$ASM_JAR" ]] && printf '%s  %s\n' "$ASM_SHA256" "$ASM_JAR" | sha256sum -c --status
}
if ! verify_asm_jar; then
  rm -f "$ASM_JAR" "$ASM_JAR.tmp"
  asm_downloaded=false
  for asm_url in \
    'https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar' \
    'https://repo1.maven.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar'; do
    echo "Downloading ASM 9.7.1 from $asm_url"
    if curl -fL --retry 8 --retry-all-errors --retry-delay 3 --retry-max-time 300 \
         --connect-timeout 20 --max-time 180 -o "$ASM_JAR.tmp" "$asm_url"; then
      if printf '%s  %s\n' "$ASM_SHA256" "$ASM_JAR.tmp" | sha256sum -c --status; then
        mv "$ASM_JAR.tmp" "$ASM_JAR"
        asm_downloaded=true
        break
      fi
      echo 'Downloaded ASM JAR failed SHA-256 verification; trying fallback.' >&2
    fi
    rm -f "$ASM_JAR.tmp"
  done
  [[ "$asm_downloaded" == "true" ]] || { echo 'Unable to download verified ASM 9.7.1.' >&2; exit 1; }
fi
verify_asm_jar || { echo 'ASM 9.7.1 SHA-256 verification failed.' >&2; exit 1; }
rm -rf .ci-build/script-plugin-helper
mkdir -p .ci-build/script-plugin-helper
javac -encoding UTF-8 -source 8 -target 8 -cp "$CP" \
  -d .ci-build/script-plugin-helper ci/BrowserScriptPluginResolver.java

javac -cp .ci-build/asm/asm.jar -d .ci-build/transform \
  ci/PatchCampaignOrbitalJunk.java \
  ci/PatchCoreLifecycleBrowserWorld.java \
  ci/PatchCoreLifecycleDiagnostics.java \
  ci/PatchMiscAcademyFleetCreator.java \
  ci/PatchTextureUploadRaster.java \
  ci/PatchSlipstreamBrowserAdvance.java \
  ci/PatchCampaignProcGen.java \
  ci/PatchCampaignCreateDiagnostics.java \
  ci/PatchPrecompiledSectorGen.java \
  ci/PatchTitleScreenCampaignCreateGuard.java \
  ci/PatchScriptStorePluginFallback.java \
  ci/PatchResourceLoaderQuickStart.java \
  ci/PatchSpecStoreDiagnostics.java \
  ci/PatchLoadingUtilsBulkSpecCache.java
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignOrbitalJunk jars/starfarer.api.jar .ci-build/starfarer-api-no-junk.jar
mv .ci-build/starfarer-api-no-junk.jar jars/starfarer.api.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCoreLifecycleBrowserWorld jars/starfarer.api.jar .ci-build/starfarer-api-browser-world.jar
mv .ci-build/starfarer-api-browser-world.jar jars/starfarer.api.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCoreLifecycleDiagnostics jars/starfarer.api.jar .ci-build/starfarer-api-lifecycle-diag.jar
mv .ci-build/starfarer-api-lifecycle-diag.jar jars/starfarer.api.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchMiscAcademyFleetCreator jars/starfarer.api.jar .ci-build/starfarer-api-academy-null.jar
mv .ci-build/starfarer-api-academy-null.jar jars/starfarer.api.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchSlipstreamBrowserAdvance jars/starfarer.api.jar .ci-build/starfarer-api-slipstream-guard.jar
mv .ci-build/starfarer-api-slipstream-guard.jar jars/starfarer.api.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignProcGen jars/starfarer_obf.jar .ci-build/starfarer-no-procgen.jar
mv .ci-build/starfarer-no-procgen.jar jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignCreateDiagnostics jars/starfarer_obf.jar .ci-build/starfarer-create-diag.jar
mv .ci-build/starfarer-create-diag.jar jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchTextureUploadRaster jars/starfarer_obf.jar .ci-build/starfarer-texture-rgba.jar
mv .ci-build/starfarer-texture-rgba.jar jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform:.ci-build/script-plugin-helper \
  PatchScriptStorePluginFallback jars/starfarer_obf.jar .ci-build/starfarer-script-plugin-fix.jar
mv .ci-build/starfarer-script-plugin-fix.jar jars/starfarer_obf.jar
jar uf jars/starfarer_obf.jar \
  -C .ci-build/script-plugin-helper com/fs/starfarer/loading/scripts/BrowserScriptPluginResolver.class
javap -classpath jars/starfarer_obf.jar -c com.fs.starfarer.loading.scripts.ScriptStore \
  | grep -q 'BrowserScriptPluginResolver.resolve'
javap -verbose -classpath jars/starfarer_obf.jar com.fs.starfarer.loading.scripts.BrowserScriptPluginResolver \
  | grep -q 'major version: 52'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchPrecompiledSectorGen jars/scripts-precompiled.jar .ci-build/scripts-precompiled-browser-world.jar
mv .ci-build/scripts-precompiled-browser-world.jar jars/scripts-precompiled.jar
javap -classpath jars/scripts-precompiled.jar -c data.scripts.world.SectorGen \
  | grep -q 'BrowserSectorGenDiag: executing patched scripts-precompiled SectorGen.generate'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchTitleScreenCampaignCreateGuard jars/starfarer.api.jar .ci-build/starfarer-title-create-guard.jar
mv .ci-build/starfarer-title-create-guard.jar jars/starfarer.api.jar

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
javap -classpath jars/starfarer_obf.jar -c -p com.fs.starfarer.BaseGameState \
  | grep -q 'MainThreadTransitionBridge.isTitleHandoffActive'

java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchResourceLoaderQuickStart jars/starfarer_obf.jar .ci-build/starfarer-resource-quick.jar
mv .ci-build/starfarer-resource-quick.jar jars/starfarer_obf.jar
javap -classpath jars/starfarer_obf.jar -p -c com.fs.starfarer.loading.ResourceLoaderState \
  | grep -q 'starsector.browserQuickResourceLoad'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchSpecStoreDiagnostics jars/starfarer_obf.jar .ci-build/starfarer-specstore-diag.jar
mv .ci-build/starfarer-specstore-diag.jar jars/starfarer_obf.jar
javap -classpath jars/starfarer_obf.jar -c -p com.fs.starfarer.loading.SpecStore \
  | grep -q 'BrowserSpecStoreStage:'
# Ship the Java-8 helper in the same JAR/package as the obfuscated loader, then
# insert a cache hit before LoadingUtils performs its normal resource-manager read.
jar uf jars/starfarer_obf.jar \
  -C .ci-build/spec-cache-helper com/fs/starfarer/loading/BrowserSpecCache.class
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchLoadingUtilsBulkSpecCache jars/starfarer_obf.jar .ci-build/starfarer-bulk-spec-cache.jar
mv .ci-build/starfarer-bulk-spec-cache.jar jars/starfarer_obf.jar
javap -verbose -classpath jars/starfarer_obf.jar com.fs.starfarer.loading.BrowserSpecCache \
  | grep -q 'major version: 52'
mkdir -p .ci-build/verify-bulk-spec-patch
javac -cp .ci-build/asm/asm.jar -d .ci-build/verify-bulk-spec-patch \
  ci/VerifyLoadingUtilsBulkSpecPatch.java
java -cp .ci-build/asm/asm.jar:.ci-build/verify-bulk-spec-patch \
  VerifyLoadingUtilsBulkSpecPatch jars/starfarer_obf.jar
# Prove the transformer is idempotent so repeated local/CI preparation cannot
# accumulate a second fast path in the obfuscated LoadingUtils method.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchLoadingUtilsBulkSpecCache jars/starfarer_obf.jar .ci-build/starfarer-bulk-spec-cache-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/verify-bulk-spec-patch \
  VerifyLoadingUtilsBulkSpecPatch .ci-build/starfarer-bulk-spec-cache-repeat.jar
mkdir -p .ci-build/verify-resource-loader
javac -cp .ci-build/asm/asm.jar -d .ci-build/verify-resource-loader \
  ci/VerifyResourceLoaderQuickStart.java
java -cp .ci-build/asm/asm.jar:.ci-build/verify-resource-loader \
  VerifyResourceLoaderQuickStart jars/starfarer_obf.jar

mkdir -p .ci-build/verify-script-plugin
javac -encoding UTF-8 -source 8 -target 8 -cp "jars/starfarer_obf.jar:$CP" \
  -d .ci-build/verify-script-plugin ci/VerifyScriptPluginFallback.java
java -Xverify:all -cp ".ci-build/verify-script-plugin:jars/starfarer_obf.jar:$CP" \
  VerifyScriptPluginFallback

# Every transformed JAR must advertise its post-transform size to the HTTP mount.
# This avoids stale directory metadata causing CheerpJ range/read mismatches.
python3 ci/refresh-runtime-jar-index.py

# Verify that the exact post-transform classpath can be reconstructed from one
# contiguous Pages payload before the expensive browser launch.
rm -rf .ci-build/jar-pack-test
python3 ci/build-pages-jar-pack.py --root . --output-dir .ci-build/jar-pack-test
test -s .ci-build/jar-pack-test/starsector-jar-pack-v1.bin
test -s .ci-build/jar-pack-test/starsector-jar-pack-v1.json
rm -rf .ci-build/jar-pack-test

# Fail before the expensive browser launch if ASM produced bytecode rejected by
# the stock JVM verifier. Static initialization remains disabled so native/GL
# startup does not run here.
mkdir -p .ci-build/verify
javac -encoding UTF-8 -d .ci-build/verify ci/VerifyPatchedRuntimeClasses.java
java -Xverify:all -cp ".ci-build/verify:jars/fixer_patch.jar:$CP" \
  VerifyPatchedRuntimeClasses \
  data.scripts.world.SectorGen \
  com.fs.starfarer.api.impl.campaign.CoreLifecyclePluginImpl \
  com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2 \
  com.fs.starfarer.api.impl.campaign.fleets.misc.MiscAcademyFleetCreator \
  com.fs.starfarer.util.O \
  com.fs.starfarer.campaign.save.CampaignGameManager \
  com.fs.starfarer.BaseGameState

if [[ "${STARSECTOR_PREPARE_ONLY:-false}" == "true" ]]; then
  echo "Prepared verified campaign runtime candidate; browser execution skipped by STARSECTOR_PREPARE_ONLY=true."
  exit 0
fi

npm ci
# GitHub-hosted Ubuntu already carries Chromium runtime libraries. Avoid apt here:
# external Microsoft package feeds have intermittently returned 403 and should not
# make a browser-runtime diagnostic fail before Playwright launches.
npx playwright install chromium
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
# Do not terminate the run before the campaign bootstrap has had a chance to run.
STARSECTOR_TEST_URL=http://127.0.0.1:8000/launch.html \
STARSECTOR_TEST_TIMEOUT_MS=720000 \
STARSECTOR_FRAME_SETTLE_MS=30000 \
STARSECTOR_EXPECT_STATE="$EXPECT_STATE" \
STARSECTOR_WINDOW_CONFIG="$WINDOW_CONFIG" \
STARSECTOR_TEST_OUTPUT_DIR="$OUT" \
  node ci/campaign-render-test.js
# The optimized run is only valid if the real game loaded and used the bulk spec
# cache. This prevents a transparent fallback from being mistaken for a speedup.
grep -q 'BrowserSpecCache: ready' "$OUT/browser.log"
grep -q 'BrowserSpecCache: first-hit' "$OUT/browser.log"
