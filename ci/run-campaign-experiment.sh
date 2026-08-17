#!/usr/bin/env bash
set -euo pipefail

NAME=${1:?experiment name required}
SWAP_MODE=${2:?swap mode required}
PATCH_SLEEP=${3:?patch sleep flag required}
EXPECT_STATE=${4:-campaign}
WINDOW_CONFIG=${5:-'{}'}
OUT="test_output/${NAME}"
mkdir -p "$OUT" .ci-build/fixer .ci-cache

# A deep gameplay run must use the real stock campaign world. The old minimal
# Corvus/Asharu knobs remain available for explicit bootstrap diagnostics only.
if [[ "${STARSECTOR_DEEP_GAMEPLAY:-false}" == "true" ]]; then
  if [[ "${STARSECTOR_MINIMAL_ASHARU_ECONOMY:-false}" == "true" || "${STARSECTOR_MINIMAL_CORVUS_ASHARU_ANCHOR:-false}" == "true" ]]; then
    echo 'Deep gameplay refuses minimal Corvus/Asharu world preparation; use the full stock sector/economy.' >&2
    exit 1
  fi
fi

cleanup() {
  cp /tmp/starsector-http.log "$OUT/http.log" 2>/dev/null || true
  git diff -- jars/Fixer.java jars/index.list launch.html build/final/wasm-modules/lwjgl.js data/scripts/world/SectorGen.java starsector/starsector/data/scripts/world/SectorGen.java > "$OUT/candidate.patch" || true
  git diff --stat -- starsector/starsector > "$OUT/runtime-assets.stat" || true
  sha256sum jars/fixer_patch.jar jars/fs.common_obf.jar jars/starfarer.api.jar jars/starfarer_obf.jar jars/scripts-precompiled.jar jars/xstream-1.4.10.jar jars/txw2-2.3.1.jar > "$OUT/runtime-sha256.txt" 2>/dev/null || true
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
test "$(grep -c 'runSectorStep("' data/scripts/world/SectorGen.java)" -eq 24

python3 ci/patch-lwjgl-matrix-stack.py
python3 ci/patch-lwjgl-display-lists.py
grep -q 'LWJGL_MATRIX_STACK_GUARD_V1' build/final/wasm-modules/lwjgl.js
grep -q 'LWJGL_DISPLAY_LIST_NONFATAL_V1' build/final/wasm-modules/lwjgl.js
grep -q 'LWJGL_CLIENT_ARRAY_COMPAT_V1' build/final/wasm-modules/lwjgl.js
grep -q 'LWJGL_ALPHA_TEST_COMPAT_V1' build/final/wasm-modules/lwjgl.js
grep -q 'LWJGL_ATTRIB_STACK_COMPAT_V1' build/final/wasm-modules/lwjgl.js
grep -q '__lwjglGraphicsInfo' build/final/wasm-modules/lwjgl.js
python3 ci/verify-lwjgl-fixed-function.py build/final/wasm-modules/lwjgl.js
node ci/verify-lwjgl-quad-batching.js build/final/wasm-modules/lwjgl.js
python3 ci/verify-lwjgl-no-sync-validation.py
python3 ci/verify-fatal-console-classification.py
# The browser quick-start keeps the stock 45s fallback available via override,
# but defaults the successful create-settle gate to 15s. Guard both the default
# and Java property propagation so this latency win cannot silently regress.
grep -q '__STARSECTOR_AUTO_CAMPAIGN_DIRECT_CREATE_SETTLE_MS__ ?? 15000' launch.html
grep -q 'Number.isFinite(parsedAutoCampaignDirectCreateSettleMs)' launch.html
grep -q 'starsector.autoCampaignDirectCreateSettleMs=${autoCampaignDirectCreateSettleMs}' launch.html
grep -q '__STARSECTOR_BROWSER_BULK_SPEC_CACHE__' launch.html
grep -q '__STARSECTOR_AUTO_CAMPAIGN_STARTING_LOCATION__' launch.html
grep -q 'starsector.autoCampaignStartingLocation=${autoCampaignStartingLocation}' launch.html
grep -q '__STARSECTOR_BROWSER_FAST_CSV_PARSER__' launch.html
grep -q 'starsector.browserFastCsvParser=${browserFastCsvParser}' launch.html
grep -q '__STARSECTOR_BROWSER_FAST_TEXT_PREPROCESS__' launch.html
grep -q 'starsector.browserFastTextPreprocess=${browserFastTextPreprocess}' launch.html
grep -q '__STARSECTOR_BROWSER_JANINO_NEGATIVE_CACHE__' launch.html
grep -q 'starsector.browserJaninoNegativeCache=${browserJaninoNegativeCache}' launch.html
grep -q '__STARSECTOR_BROWSER_RULE_DUPLICATE_INDEX__' launch.html
grep -q 'starsector.browserRuleDuplicateIndex=${browserRuleDuplicateIndex}' launch.html
grep -q '__STARSECTOR_BROWSER_DEFERRED_TEXTURES__' launch.html
grep -q '__STARSECTOR_BROWSER_CONTINUE_RENDER_GUARD__' launch.html
grep -q 'starsector.browserContinueRenderGuard=${browserContinueRenderGuard}' launch.html
grep -q '__STARSECTOR_BROWSER_XSTREAM_UNSAFE_READ_FAST_PATH__' launch.html
grep -q 'starsector.browserXstreamUnsafeReadFastPath=${browserXstreamUnsafeReadFastPath}' launch.html
grep -q '__STARSECTOR_BROWSER_XSTREAM_LOAD_DIAG__' launch.html
grep -q 'starsector.browserXstreamLoadDiag=${browserXstreamLoadDiag}' launch.html
grep -q '__STARSECTOR_BROWSER_GAMEPLAY_PROBE__' launch.html
grep -q 'starsector.browserGameplayProbe=${browserGameplayProbe}' launch.html
grep -q '__STARSECTOR_BROWSER_GAMEPLAY_SPEEDUP_MULT__' launch.html
grep -q 'starsector.browserGameplaySpeedupMult=${browserGameplaySpeedupMult}' launch.html
grep -q '__STARSECTOR_BROWSER_GAMEPLAY_PREWARM__' launch.html
grep -q 'starsector.browserGameplayPrewarm=${browserGameplayPrewarm}' launch.html
grep -q '__STARSECTOR_BROWSER_SKIP_OUTER_SECTOR_PROCGEN__ === true' launch.html
grep -q 'starsector.browserSkipOuterSectorProcGen=${browserSkipOuterSectorProcGen}' launch.html
grep -q '__STARSECTOR_BROWSER_LIGHTWEIGHT_SECTOR_COMPAT__ === true' launch.html
grep -q 'starsector.compatibilityFastPath=${browserLightweightSectorCompat}' launch.html
grep -q 'const browserSpecCachePath = `${contentRoot}data/browser-spec-cache-v1.json`' launch.html
grep -q 'starsector.browserSpecCachePath=${browserSpecCachePath}' launch.html
grep -q 'starsector.browserDeferredTextures=${browserDeferredTextures}' launch.html
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

# Keep Keyboard.getKeyName/getKeyIndex byte-for-byte behavior aligned with the
# stock LWJGL 2 table. Starsector renders these names directly in campaign HUD
# shortcut labels; placeholder names such as unknown_33 are therefore visible UI
# regressions even when keyboard events themselves still work.
rm -rf .ci-build/keyboard-keyname-probe
mkdir -p .ci-build/keyboard-keyname-probe
javac -encoding UTF-8 -source 8 -target 8 \
  -cp "jars/lwjgl.jar" \
  -d .ci-build/keyboard-keyname-probe \
  ci/ProbeKeyboardKeyNames.java
java -cp ".ci-build/keyboard-keyname-probe:jars/lwjgl.jar" \
  ProbeKeyboardKeyNames > .ci-build/keyboard-keynames-stock.tsv
java -cp ".ci-build/keyboard-keyname-probe:jars/bridge.jar:jars/lwjgl.jar" \
  ProbeKeyboardKeyNames > .ci-build/keyboard-keynames-bridge.tsv
diff -u .ci-build/keyboard-keynames-stock.tsv .ci-build/keyboard-keynames-bridge.tsv

SWAP_YIELD_MODE="$SWAP_MODE" KEEP_UNSAFE_FORCE_ACTIVATION=0 \
  python3 ci/apply-campaign-runtime-fix.py
python3 ci/require-owned-title-state.py
python3 ci/enable-direct-ui-preflight.py
python3 ci/narrow-direct-font-preflight.py
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
# Prove the browser CSV parser is byte-for-byte semantic-equivalent at the data
# model level to Starsector's real parser for every restored stock CSV before
# allowing the runtime hook to be compiled or applied.
rm -rf .ci-build/fast-csv-helper .ci-build/verify-fast-csv
mkdir -p .ci-build/fast-csv-helper .ci-build/verify-fast-csv
javac -encoding UTF-8 --release 8 -cp "$CP" \
  -d .ci-build/fast-csv-helper \
  ci/java17-xstream/com/fs/starfarer/loading/BrowserFastCsvParser.java
javac -encoding UTF-8 --release 8 -cp ".ci-build/fast-csv-helper:$CP" \
  -d .ci-build/verify-fast-csv ci/VerifyBrowserFastCsvParser.java
java -Xverify:all -cp ".ci-build/verify-fast-csv:.ci-build/fast-csv-helper:$CP" \
  VerifyBrowserFastCsvParser "$PWD/starsector/starsector"
# Exact one-pass replacement for SpecStore's two whole-string smart-quote regexes.
# Verify every restored stock CSV plus deterministic edge/fuzz cases first.
rm -rf .ci-build/fast-text-helper .ci-build/verify-fast-text
mkdir -p .ci-build/fast-text-helper .ci-build/verify-fast-text
javac -encoding UTF-8 --release 8 -cp "$CP" \
  -d .ci-build/fast-text-helper \
  ci/java17-xstream/com/fs/starfarer/loading/BrowserTextPreprocessor.java
javac -encoding UTF-8 --release 8 -cp ".ci-build/fast-text-helper:$CP" \
  -d .ci-build/verify-fast-text ci/VerifyBrowserTextPreprocessor.java
java -Xverify:all -cp ".ci-build/verify-fast-text:.ci-build/fast-text-helper:$CP" \
  VerifyBrowserTextPreprocessor "$PWD/starsector/starsector"
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
javap -verbose -classpath jars/fixer_patch.jar com.fs.starfarer.BrowserDeferredTextureQueue \
  | grep -q 'major version: 52'
javap -verbose -classpath jars/fixer_patch.jar com.fs.starfarer.BrowserGameplayProbe \
  | grep -q 'major version: 52'
rm -rf .ci-build/verify-gameplay-probe-readiness
mkdir -p .ci-build/verify-gameplay-probe-readiness
javac -encoding UTF-8 --release 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-gameplay-probe-readiness ci/VerifyGameplayProbeReadiness.java
java -Xverify:all -cp ".ci-build/verify-gameplay-probe-readiness:jars/fixer_patch.jar:$CP" \
  VerifyGameplayProbeReadiness
javap -verbose -classpath jars/fixer_patch.jar com.fs.starfarer.loading.BrowserFastCsvParser \
  | grep -q 'major version: 52'
javap -verbose -classpath jars/fixer_patch.jar com.fs.starfarer.loading.BrowserTextPreprocessor \
  | grep -q 'major version: 52'
mkdir -p .ci-build/verify-deferred-texture-policy
javac -encoding UTF-8 --release 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-deferred-texture-policy ci/VerifyDeferredTexturePolicy.java
java -cp ".ci-build/verify-deferred-texture-policy:jars/fixer_patch.jar:$CP" VerifyDeferredTexturePolicy
rm -rf .ci-build/verify-deferred-texture-behavior
mkdir -p .ci-build/verify-deferred-texture-behavior
javac -encoding UTF-8 --release 8 -d .ci-build/verify-deferred-texture-behavior \
  ci/deferred-texture-test/com/fs/graphics/oOoO.java \
  ci/deferred-texture-test/com/fs/graphics/L.java
javac -encoding UTF-8 --release 8 \
  -cp ".ci-build/verify-deferred-texture-behavior:jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-deferred-texture-behavior ci/VerifyDeferredTextureBehavior.java
java -cp ".ci-build/verify-deferred-texture-behavior:jars/fixer_patch.jar:$CP" \
  VerifyDeferredTextureBehavior
mkdir -p .ci-build/verify-starting-supplies
javac -encoding UTF-8 -source 8 -target 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-starting-supplies \
  ci/VerifyStartingSupplies.java \
  ci/VerifyPlayableStartingResources.java \
  ci/VerifyStartingAbilities.java \
  ci/VerifyCampaignWorldReadiness.java \
  ci/VerifyCampaignProcGenCompat.java \
  ci/VerifyAutoCampaignContinueMode.java
java -cp ".ci-build/verify-starting-supplies:jars/fixer_patch.jar:$CP" VerifyStartingSupplies
java -Xverify:all -cp ".ci-build/verify-starting-supplies:jars/fixer_patch.jar:$CP" VerifyPlayableStartingResources
java -Xverify:all -cp ".ci-build/verify-starting-supplies:jars/fixer_patch.jar:$CP" VerifyStartingAbilities
java -Xverify:all -cp ".ci-build/verify-starting-supplies:jars/fixer_patch.jar:$CP" VerifyCampaignWorldReadiness
java -Xverify:all -cp ".ci-build/verify-starting-supplies:jars/fixer_patch.jar:$CP" VerifyCampaignProcGenCompat
java -Xverify:all -cp ".ci-build/verify-starting-supplies:jars/fixer_patch.jar:$CP" VerifyAutoCampaignContinueMode
rm -rf .ci-build/verify-browser-tiled-terrain
mkdir -p .ci-build/verify-browser-tiled-terrain
javac -encoding UTF-8 --release 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-browser-tiled-terrain ci/VerifyBrowserTiledTerrainCompat.java
java -Xverify:all -cp ".ci-build/verify-browser-tiled-terrain:jars/fixer_patch.jar:$CP" \
  VerifyBrowserTiledTerrainCompat
mkdir -p .ci-build/verify-texture-upload
javac -encoding UTF-8 -source 8 -target 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-texture-upload ci/VerifyTextureUploadCompat.java
java -cp ".ci-build/verify-texture-upload:jars/fixer_patch.jar:$CP" VerifyTextureUploadCompat
mkdir -p .ci-build/verify-texture-assets
javac -encoding UTF-8 -source 8 -target 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-texture-assets ci/VerifyTextureAssets.java
java -Xmx3g -cp ".ci-build/verify-texture-assets:jars/fixer_patch.jar:$CP" \
  VerifyTextureAssets starsector/starsector/graphics
mkdir -p .ci-build/verify-texture-prepared-assets
javac -encoding UTF-8 -source 8 -target 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-texture-prepared-assets ci/VerifyTexturePreparedAssets.java
java -Xmx2g -cp ".ci-build/verify-texture-prepared-assets:jars/fixer_patch.jar:$CP" \
  VerifyTexturePreparedAssets starsector/starsector/graphics
mkdir -p .ci-build/verify-xstream
javac -encoding UTF-8 -source 8 -target 8 -cp "jars/fixer_patch.jar:$CP" \
  -d .ci-build/verify-xstream ci/VerifyJava17XStreamCompat.java
java -Dstarsector.browserXstreamUnsafeReadFastPath=false \
  -cp ".ci-build/verify-xstream:jars/fixer_patch.jar:$CP" VerifyJava17XStreamCompat \
  > .ci-build/verify-xstream/reflection.txt
java -Dstarsector.browserXstreamUnsafeReadFastPath=true \
  -cp ".ci-build/verify-xstream:jars/fixer_patch.jar:$CP" VerifyJava17XStreamCompat \
  > .ci-build/verify-xstream/unsafe.txt
cmp .ci-build/verify-xstream/reflection.txt .ci-build/verify-xstream/unsafe.txt
cat .ci-build/verify-xstream/unsafe.txt
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
rm -rf .ci-build/rules-duplicate-helper .ci-build/verify-rules-duplicate-helper
mkdir -p .ci-build/rules-duplicate-helper .ci-build/verify-rules-duplicate-helper
javac -encoding UTF-8 --release 8 \
  -d .ci-build/rules-duplicate-helper ci/BrowserRuleDuplicateIndex.java
javac -encoding UTF-8 --release 8 -cp .ci-build/rules-duplicate-helper \
  -d .ci-build/verify-rules-duplicate-helper ci/TestBrowserRuleDuplicateIndex.java
java -Xverify:all -cp ".ci-build/verify-rules-duplicate-helper:.ci-build/rules-duplicate-helper" \
  TestBrowserRuleDuplicateIndex

javac -cp .ci-build/asm/asm.jar -d .ci-build/transform \
  ci/PatchCampaignOrbitalJunk.java \
  ci/PatchCoreLifecycleBrowserWorld.java \
  ci/PatchCoreLifecycleDiagnostics.java \
  ci/PatchMiscAcademyFleetCreator.java \
  ci/PatchBaseTiledTerrainBrowserCodec.java \
  ci/PatchTextureUploadRaster.java \
  ci/PatchTextureLoaderBulkUpload.java \
  ci/VerifyTextureLoaderBulkUploadPatch.java \
  ci/PatchTextureRegistryDeferredLookup.java \
  ci/PatchResourceLoaderDeferredTextures.java \
  ci/PatchResourceLoaderDeferredPredecode.java \
  ci/VerifyDeferredTexturePatches.java \
  ci/PatchAbilityGameplayProbe.java \
  ci/VerifyAbilityGameplayProbePatch.java \
  ci/PatchCampaignGameplayProbe.java \
  ci/VerifyCampaignGameplayProbePatch.java \
  ci/PatchAbilityUiGameplayProbe.java \
  ci/VerifyAbilityUiGameplayProbePatch.java \
  ci/PatchControlMatcherGameplayProbe.java \
  ci/VerifyControlMatcherGameplayProbePatch.java \
  ci/PatchCampaignPauseGameplayProbe.java \
  ci/VerifyCampaignPauseGameplayProbePatch.java \
  ci/PatchBrowserFastCsvParser.java \
  ci/VerifyBrowserFastCsvParserPatch.java \
  ci/PatchBrowserTextPreprocessor.java \
  ci/VerifyBrowserTextPreprocessorPatch.java \
  ci/PatchSlipstreamBrowserAdvance.java \
  ci/PatchCampaignProcGen.java \
  ci/PatchTitleContinueRenderGuard.java \
  ci/VerifyTitleContinueRenderGuardPatch.java \
  ci/PatchCampaignCreateDiagnostics.java \
  ci/PatchInitialSavePerfDiagnostics.java \
  ci/PatchCampaignXStreamLoadDiag.java \
  ci/VerifyCampaignXStreamLoadDiagPatch.java \
  ci/PatchCampaignXStreamProviderDiag.java \
  ci/VerifyCampaignXStreamProviderDiagPatch.java \
  ci/PatchXStreamReferenceEntry.java \
  ci/VerifyXStreamReferenceEntryPatch.java \
  ci/PatchPrecompiledSectorGen.java \
  ci/PatchTitleScreenCampaignCreateGuard.java \
  ci/PatchScriptStorePluginFallback.java \
  ci/PatchJaninoNegativeSourceCache.java \
  ci/VerifyJaninoNegativeSourceCachePatch.java \
  ci/PatchResourceLoaderQuickStart.java \
  ci/PatchSpecStoreDiagnostics.java \
  ci/PatchRulesVariableDiagnostics.java \
  ci/VerifyRulesVariableDiagnosticsPatch.java \
  ci/PatchRulesDuplicateIndex.java \
  ci/VerifyRulesDuplicateIndexPatch.java \
  ci/PatchRulesLiteralStringCleanup.java \
  ci/VerifyRulesLiteralStringCleanupPatch.java \
  ci/PatchRulesDeadVariableWrites.java \
  ci/VerifyRulesDeadVariableWritesPatch.java \
  ci/PatchRulesDeadVariableTraversal.java \
  ci/VerifyRulesDeadVariableTraversalPatch.java \
  ci/PatchSpecStoreVariantDiscovery.java \
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
  PatchBaseTiledTerrainBrowserCodec jars/starfarer.api.jar .ci-build/starfarer-api-fast-tiled-terrain.jar
mv .ci-build/starfarer-api-fast-tiled-terrain.jar jars/starfarer.api.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchSlipstreamBrowserAdvance jars/starfarer.api.jar .ci-build/starfarer-api-slipstream-guard.jar
mv .ci-build/starfarer-api-slipstream-guard.jar jars/starfarer.api.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignProcGen jars/starfarer_obf.jar .ci-build/starfarer-no-procgen.jar
mv .ci-build/starfarer-no-procgen.jar jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignCreateDiagnostics jars/starfarer_obf.jar .ci-build/starfarer-create-diag.jar
mv .ci-build/starfarer-create-diag.jar jars/starfarer_obf.jar
jar tf jars/fixer_patch.jar | grep -qx 'com/fs/starfarer/BrowserInitialSavePerfDiag.class'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchInitialSavePerfDiagnostics jars/starfarer_obf.jar .ci-build/starfarer-save-perf-diag.jar
mv .ci-build/starfarer-save-perf-diag.jar jars/starfarer_obf.jar
javap -classpath jars/starfarer_obf.jar -c -p com.fs.starfarer.campaign.save.CampaignGameManager \
  | grep -q 'BrowserInitialSavePerfDiag'
jar tf jars/fixer_patch.jar | grep -qx 'com/fs/starfarer/BrowserXStreamLoadDiag.class'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignXStreamLoadDiag jars/starfarer_obf.jar .ci-build/starfarer-xstream-load-diag.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyCampaignXStreamLoadDiagPatch .ci-build/starfarer-xstream-load-diag.jar
mv .ci-build/starfarer-xstream-load-diag.jar jars/starfarer_obf.jar
javap -classpath "jars/fixer_patch.jar:jars/starfarer_obf.jar:$CP" -c -p \
  'com.fs.starfarer.campaign.save.CampaignGameManager$5' | grep -q 'BrowserXStreamLoadDiag.wrap'
javap -classpath "jars/fixer_patch.jar:jars/starfarer_obf.jar:$CP" -c -p \
  'com.fs.starfarer.campaign.save.CampaignGameManager$6' | grep -q 'BrowserXStreamLoadDiag.logProvider'
jar tf jars/fixer_patch.jar | grep -qx 'com/fs/starfarer/BrowserLoggingSunUnsafeReflectionProvider.class'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignXStreamProviderDiag jars/starfarer_obf.jar .ci-build/starfarer-xstream-provider-diag.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyCampaignXStreamProviderDiagPatch .ci-build/starfarer-xstream-provider-diag.jar
mv .ci-build/starfarer-xstream-provider-diag.jar jars/starfarer_obf.jar
javap -classpath "jars/fixer_patch.jar:jars/starfarer_obf.jar:$CP" -c -p \
  'com.fs.starfarer.campaign.save.CampaignGameManager$6' | grep -q 'BrowserLoggingSunUnsafeReflectionProvider'
jar tf jars/fixer_patch.jar | grep -qx 'com/fs/starfarer/BrowserXStreamReferenceDiag.class'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchXStreamReferenceEntry jars/xstream-1.4.10.jar .ci-build/xstream-reference-entry.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyXStreamReferenceEntryPatch .ci-build/xstream-reference-entry.jar
mv .ci-build/xstream-reference-entry.jar jars/xstream-1.4.10.jar
javap -classpath "jars/fixer_patch.jar:jars/xstream-1.4.10.jar:$CP" -c -p \
  com.thoughtworks.xstream.core.AbstractReferenceUnmarshaller | grep -q 'BrowserXStreamReferenceDiag.enter'
jar tf jars/fixer_patch.jar | grep -qx 'com/fs/starfarer/BrowserTitleContinueCompat.class'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchTitleContinueRenderGuard jars/starfarer_obf.jar .ci-build/starfarer-title-continue.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyTitleContinueRenderGuardPatch .ci-build/starfarer-title-continue.jar
mv .ci-build/starfarer-title-continue.jar jars/starfarer_obf.jar
javap -classpath "jars/fixer_patch.jar:jars/starfarer_obf.jar:$CP" -c -p \
  com.fs.starfarer.title.TitleScreenState | grep -q 'BrowserTitleContinueCompat.renderBeforeContinue'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchTextureUploadRaster jars/starfarer_obf.jar .ci-build/starfarer-texture-rgba.jar
mv .ci-build/starfarer-texture-rgba.jar jars/starfarer_obf.jar
# Replace fs.common TextureLoader's per-pixel Raster.getPixel()/indexed-ByteBuffer loop
# with the verified Java-8 bulk converter in fixer_patch.jar. All textures, padding,
# vertical flip, texture-object dimensions, reusable scratch state and derived colors remain intact.
jar tf jars/fixer_patch.jar | grep -qx 'com/fs/starfarer/TextureUploadCompat.class'
jar tf jars/fixer_patch.jar | grep -qx 'com/fs/starfarer/TextureUploadCompat$PreparedTexture.class'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchTextureLoaderBulkUpload jars/fs.common_obf.jar .ci-build/fs-common-texture-bulk.jar
# Verify the candidate before replacing the runtime JAR, so a bad transform never
# becomes the input to later packaging or browser checks.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyTextureLoaderBulkUploadPatch .ci-build/fs-common-texture-bulk.jar
javap -classpath .ci-build/fs-common-texture-bulk.jar -c -p com.fs.graphics.TextureLoader \
  | grep -q 'TextureUploadCompat.prepareTexture'
mv .ci-build/fs-common-texture-bulk.jar jars/fs.common_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchTextureRegistryDeferredLookup jars/fs.common_obf.jar .ci-build/fs-common-deferred.jar
mv .ci-build/fs-common-deferred.jar jars/fs.common_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform:.ci-build/script-plugin-helper \
  PatchScriptStorePluginFallback jars/starfarer_obf.jar .ci-build/starfarer-script-plugin-fix.jar
mv .ci-build/starfarer-script-plugin-fix.jar jars/starfarer_obf.jar
jar uf jars/starfarer_obf.jar \
  -C .ci-build/script-plugin-helper com/fs/starfarer/loading/scripts/BrowserScriptPluginResolver.class
javap -classpath jars/starfarer_obf.jar -c com.fs.starfarer.loading.scripts.ScriptStore \
  | grep -q 'BrowserScriptPluginResolver.resolve'
javap -verbose -classpath jars/starfarer_obf.jar com.fs.starfarer.loading.scripts.BrowserScriptPluginResolver \
  | grep -q 'major version: 52'
# Janino repeatedly asks its source ResourceFinder for the same missing Java source.
# Cache only failures within one finder instance, and only when the browser property
# is enabled. Successful source reads and the stock disabled-property path are unchanged.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchJaninoNegativeSourceCache jars/starfarer_obf.jar .ci-build/starfarer-janino-negative-cache.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyJaninoNegativeSourceCachePatch .ci-build/starfarer-janino-negative-cache.jar
mv .ci-build/starfarer-janino-negative-cache.jar jars/starfarer_obf.jar
# The transform must be byte-for-byte idempotent on a prepared candidate.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchJaninoNegativeSourceCache jars/starfarer_obf.jar .ci-build/starfarer-janino-negative-cache-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyJaninoNegativeSourceCachePatch .ci-build/starfarer-janino-negative-cache-repeat.jar
cmp -s jars/starfarer_obf.jar .ci-build/starfarer-janino-negative-cache-repeat.jar
# Directly prove the property gate: disabled mode retries every miss; enabled mode
# memoizes only failed names on that finder; successful source reads always repeat.
rm -rf .ci-build/verify-janino-negative-behavior
mkdir -p .ci-build/verify-janino-negative-behavior
javac -encoding UTF-8 --release 8 -cp "jars/starfarer_obf.jar:$CP" \
  -d .ci-build/verify-janino-negative-behavior \
  ci/janino-negative-test/com/fs/starfarer/loading/LoadingUtils.java \
  ci/TestJaninoNegativeSourceCache.java
java -Xverify:all -cp ".ci-build/verify-janino-negative-behavior:jars/starfarer_obf.jar:$CP" \
  TestJaninoNegativeSourceCache
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchPrecompiledSectorGen jars/scripts-precompiled.jar .ci-build/scripts-precompiled-browser-world.jar \
  | tee "$OUT/precompiled-sector-gen.log"
grep -q 'encountered=24 skipped=0' "$OUT/precompiled-sector-gen.log"
mv .ci-build/scripts-precompiled-browser-world.jar jars/scripts-precompiled.jar
javap -classpath jars/scripts-precompiled.jar -c data.scripts.world.SectorGen \
  | grep -q 'BrowserSectorGenDiag: executing patched scripts-precompiled SectorGen.generate'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchTitleScreenCampaignCreateGuard jars/starfarer.api.jar .ci-build/starfarer-title-create-guard.jar
mv .ci-build/starfarer-title-create-guard.jar jars/starfarer.api.jar
# Deep gameplay mode records real Starsector ability button/activation events without
# changing ability semantics. The helper itself is property-gated and silent otherwise.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchAbilityGameplayProbe jars/starfarer.api.jar .ci-build/starfarer-api-gameplay-probe.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyAbilityGameplayProbePatch .ci-build/starfarer-api-gameplay-probe.jar
mv .ci-build/starfarer-api-gameplay-probe.jar jars/starfarer.api.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchAbilityGameplayProbe jars/starfarer.api.jar .ci-build/starfarer-api-gameplay-probe-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyAbilityGameplayProbePatch .ci-build/starfarer-api-gameplay-probe-repeat.jar
cmp -s jars/starfarer.api.jar .ci-build/starfarer-api-gameplay-probe-repeat.jar

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
  PatchResourceLoaderDeferredTextures jars/starfarer_obf.jar .ci-build/starfarer-resource-deferred.jar
mv .ci-build/starfarer-resource-deferred.jar jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchResourceLoaderDeferredPredecode jars/starfarer_obf.jar .ci-build/starfarer-resource-deferred-predecode.jar
mv .ci-build/starfarer-resource-deferred-predecode.jar jars/starfarer_obf.jar
# Use the exact-equivalent browser CSV parser only when its property/no-mod gate
# succeeds. The full stock oOoO parser stays immediately behind the hook.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchBrowserFastCsvParser jars/starfarer_obf.jar .ci-build/starfarer-fast-csv.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyBrowserFastCsvParserPatch .ci-build/starfarer-fast-csv.jar
mv .ci-build/starfarer-fast-csv.jar jars/starfarer_obf.jar
# Reapplying the hook must be byte-for-byte idempotent.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchBrowserFastCsvParser jars/starfarer_obf.jar .ci-build/starfarer-fast-csv-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyBrowserFastCsvParserPatch .ci-build/starfarer-fast-csv-repeat.jar
cmp -s jars/starfarer_obf.jar .ci-build/starfarer-fast-csv-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchSpecStoreDiagnostics jars/starfarer_obf.jar .ci-build/starfarer-specstore-diag.jar
mv .ci-build/starfarer-specstore-diag.jar jars/starfarer_obf.jar
# Deep gameplay telemetry around actual CampaignState core-tab open/dismiss paths.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignGameplayProbe jars/starfarer_obf.jar .ci-build/starfarer-campaign-gameplay-probe.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyCampaignGameplayProbePatch .ci-build/starfarer-campaign-gameplay-probe.jar
mv .ci-build/starfarer-campaign-gameplay-probe.jar jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignGameplayProbe jars/starfarer_obf.jar .ci-build/starfarer-campaign-gameplay-probe-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyCampaignGameplayProbePatch .ci-build/starfarer-campaign-gameplay-probe-repeat.jar
cmp -s jars/starfarer_obf.jar .ci-build/starfarer-campaign-gameplay-probe-repeat.jar
# Deep gameplay telemetry at the actual campaign ability-button UI layer. This
# distinguishes plugin readiness from whether the rendered button has refreshed
# to enabled and proves numeric-key dispatch reached H.actionPerformed().
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchAbilityUiGameplayProbe jars/starfarer_obf.jar .ci-build/starfarer-ability-ui-gameplay-probe.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyAbilityUiGameplayProbePatch .ci-build/starfarer-ability-ui-gameplay-probe.jar
mv .ci-build/starfarer-ability-ui-gameplay-probe.jar jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchAbilityUiGameplayProbe jars/starfarer_obf.jar .ci-build/starfarer-ability-ui-gameplay-probe-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyAbilityUiGameplayProbePatch .ci-build/starfarer-ability-ui-gameplay-probe-repeat.jar
cmp -s jars/starfarer_obf.jar .ci-build/starfarer-ability-ui-gameplay-probe-repeat.jar
# Observe the final named-control shortcut matcher for ability slots 6-8. This
# preserves the original boolean result at all three returns; telemetry only.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchControlMatcherGameplayProbe jars/starfarer_obf.jar .ci-build/starfarer-control-matcher-gameplay-probe.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyControlMatcherGameplayProbePatch .ci-build/starfarer-control-matcher-gameplay-probe.jar
mv .ci-build/starfarer-control-matcher-gameplay-probe.jar jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchControlMatcherGameplayProbe jars/starfarer_obf.jar .ci-build/starfarer-control-matcher-gameplay-probe-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyControlMatcherGameplayProbePatch .ci-build/starfarer-control-matcher-gameplay-probe-repeat.jar
cmp -s jars/starfarer_obf.jar .ci-build/starfarer-control-matcher-gameplay-probe-repeat.jar
# Observe actual CampaignEngine pause transitions without changing pause semantics.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignPauseGameplayProbe jars/starfarer_obf.jar .ci-build/starfarer-campaign-pause-gameplay-probe.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyCampaignPauseGameplayProbePatch .ci-build/starfarer-campaign-pause-gameplay-probe.jar
mv .ci-build/starfarer-campaign-pause-gameplay-probe.jar jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchCampaignPauseGameplayProbe jars/starfarer_obf.jar .ci-build/starfarer-campaign-pause-gameplay-probe-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyCampaignPauseGameplayProbePatch .ci-build/starfarer-campaign-pause-gameplay-probe-repeat.jar
cmp -s jars/starfarer_obf.jar .ci-build/starfarer-campaign-pause-gameplay-probe-repeat.jar
# Fast exact smart-quote normalization before SpecStore's original two regex passes.
# Null from the property-gated helper falls through to the untouched stock body.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchBrowserTextPreprocessor jars/starfarer_obf.jar .ci-build/starfarer-fast-text-preprocess.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyBrowserTextPreprocessorPatch .ci-build/starfarer-fast-text-preprocess.jar
mv .ci-build/starfarer-fast-text-preprocess.jar jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchBrowserTextPreprocessor jars/starfarer_obf.jar .ci-build/starfarer-fast-text-preprocess-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyBrowserTextPreprocessorPatch .ci-build/starfarer-fast-text-preprocess-repeat.jar
cmp -s jars/starfarer_obf.jar .ci-build/starfarer-fast-text-preprocess-repeat.jar
javap -classpath jars/starfarer_obf.jar -c -p com.fs.starfarer.loading.SpecStore \
  | grep -q 'BrowserSpecStoreStage:'
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchRulesVariableDiagnostics jars/starfarer_obf.jar .ci-build/starfarer-rules-no-variable-diag.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesVariableDiagnosticsPatch .ci-build/starfarer-rules-no-variable-diag.jar
mv .ci-build/starfarer-rules-no-variable-diag.jar jars/starfarer_obf.jar
# Preserve Rules' duplicate-id exception while replacing the O(n^2) per-trigger scan
# with a per-load O(1) trigger/id index in browser mode. The original scan remains
# in bytecode and executes unchanged when the browser property is disabled.
jar uf jars/starfarer_obf.jar \
  -C .ci-build/rules-duplicate-helper com/fs/starfarer/campaign/rules/BrowserRuleDuplicateIndex.class
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchRulesDuplicateIndex jars/starfarer_obf.jar .ci-build/starfarer-rules-duplicate-index.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesDuplicateIndexPatch .ci-build/starfarer-rules-duplicate-index.jar
mv .ci-build/starfarer-rules-duplicate-index.jar jars/starfarer_obf.jar
javap -verbose -classpath jars/starfarer_obf.jar com.fs.starfarer.campaign.rules.BrowserRuleDuplicateIndex \
  | grep -q 'major version: 52'
# Replace only fixed CR/LF regex cleanup with equivalent literal String.replace calls.
# The genuine trailing-whitespace regex remains replaceAll(), and a direct Java
# semantics test proves the transformed CR/LF behavior matches the original.
rm -rf .ci-build/rules-literal-cleanup-test
mkdir -p .ci-build/rules-literal-cleanup-test
javac -encoding UTF-8 --release 8 -d .ci-build/rules-literal-cleanup-test \
  ci/TestRulesLiteralStringCleanup.java
java -cp .ci-build/rules-literal-cleanup-test TestRulesLiteralStringCleanup
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchRulesLiteralStringCleanup jars/starfarer_obf.jar .ci-build/starfarer-rules-literal-cleanup.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesLiteralStringCleanupPatch .ci-build/starfarer-rules-literal-cleanup.jar
mv .ci-build/starfarer-rules-literal-cleanup.jar jars/starfarer_obf.jar
# The warning tail is already gone, so its six CountingMap writes and six tracking
# Map.put writes have no reader. Remove only those writes in this benchmark; token
# traversal/parsing remains intact for a deliberately conservative first step.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchRulesDeadVariableWrites jars/starfarer_obf.jar .ci-build/starfarer-rules-no-variable-writes.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesDeadVariableWritesPatch .ci-build/starfarer-rules-no-variable-writes.jar
mv .ci-build/starfarer-rules-no-variable-writes.jar jars/starfarer_obf.jar
# The dead variable-usage analysis now has no reader. In browser mode, bypass its
# six token/list traversal blocks via one cached per-load browser-state flag. The
# original IFNULL analysis path remains reachable when browser mode is disabled.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchRulesDeadVariableTraversal jars/starfarer_obf.jar .ci-build/starfarer-rules-no-variable-traversal.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesDeadVariableTraversalPatch .ci-build/starfarer-rules-no-variable-traversal.jar
mv .ci-build/starfarer-rules-no-variable-traversal.jar jars/starfarer_obf.jar
# All Rules transforms must remain idempotent and compatible in their final order.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchRulesDuplicateIndex jars/starfarer_obf.jar .ci-build/starfarer-rules-duplicate-index-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesDuplicateIndexPatch .ci-build/starfarer-rules-duplicate-index-repeat.jar
cmp -s jars/starfarer_obf.jar .ci-build/starfarer-rules-duplicate-index-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesVariableDiagnosticsPatch jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesLiteralStringCleanupPatch jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesDeadVariableWritesPatch jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesDeadVariableTraversalPatch jars/starfarer_obf.jar
# Reapplying the literal cleanup must also be byte-for-byte idempotent.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchRulesLiteralStringCleanup jars/starfarer_obf.jar .ci-build/starfarer-rules-literal-cleanup-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesLiteralStringCleanupPatch .ci-build/starfarer-rules-literal-cleanup-repeat.jar
cmp -s jars/starfarer_obf.jar .ci-build/starfarer-rules-literal-cleanup-repeat.jar
# The dead-write transform is independently idempotent too.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchRulesDeadVariableWrites jars/starfarer_obf.jar .ci-build/starfarer-rules-no-variable-writes-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesDeadVariableWritesPatch .ci-build/starfarer-rules-no-variable-writes-repeat.jar
cmp -s jars/starfarer_obf.jar .ci-build/starfarer-rules-no-variable-writes-repeat.jar
# The guarded traversal transform is independently idempotent and must preserve
# the original fallback bodies plus all earlier Rules patches.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchRulesDeadVariableTraversal jars/starfarer_obf.jar .ci-build/starfarer-rules-no-variable-traversal-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesDeadVariableTraversalPatch .ci-build/starfarer-rules-no-variable-traversal-repeat.jar
cmp -s jars/starfarer_obf.jar .ci-build/starfarer-rules-no-variable-traversal-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyRulesDuplicateIndexPatch jars/starfarer_obf.jar
# Ship the Java-8 helper in the same JAR/package as the obfuscated loader, then
# insert a cache hit before LoadingUtils performs its normal resource-manager read.
jar uf jars/starfarer_obf.jar \
  -C .ci-build/spec-cache-helper com/fs/starfarer/loading/BrowserSpecCache.class
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchLoadingUtilsBulkSpecCache jars/starfarer_obf.jar .ci-build/starfarer-bulk-spec-cache.jar
mv .ci-build/starfarer-bulk-spec-cache.jar jars/starfarer_obf.jar
# SpecStore's stock variant loader discovers 645 files by synchronously walking
# data/variants and every child directory. The generated BrowserSpecCache already
# contains those exact paths and contents, so expand the root result from that
# manifest and suppress only the now-redundant child-directory loop.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchSpecStoreVariantDiscovery jars/starfarer_obf.jar .ci-build/starfarer-variant-discovery.jar
mv .ci-build/starfarer-variant-discovery.jar jars/starfarer_obf.jar
javap -verbose -classpath jars/starfarer_obf.jar com.fs.starfarer.loading.BrowserSpecCache \
  | grep -q 'major version: 52'
mkdir -p .ci-build/verify-bulk-spec-patch
javac -cp .ci-build/asm/asm.jar -d .ci-build/verify-bulk-spec-patch \
  ci/VerifyLoadingUtilsBulkSpecPatch.java \
  ci/VerifySpecStoreVariantDiscoveryPatch.java
java -cp .ci-build/asm/asm.jar:.ci-build/verify-bulk-spec-patch \
  VerifyLoadingUtilsBulkSpecPatch jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/verify-bulk-spec-patch \
  VerifySpecStoreVariantDiscoveryPatch jars/starfarer_obf.jar
# Prove the transformer is idempotent so repeated local/CI preparation cannot
# accumulate a second fast path in the obfuscated LoadingUtils method.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchLoadingUtilsBulkSpecCache jars/starfarer_obf.jar .ci-build/starfarer-bulk-spec-cache-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/verify-bulk-spec-patch \
  VerifyLoadingUtilsBulkSpecPatch .ci-build/starfarer-bulk-spec-cache-repeat.jar
# The SpecStore transform is independently idempotent as well. Reapplying it must
# preserve exactly one manifest expansion and one directory filter.
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  PatchSpecStoreVariantDiscovery jars/starfarer_obf.jar .ci-build/starfarer-variant-discovery-repeat.jar
java -cp .ci-build/asm/asm.jar:.ci-build/verify-bulk-spec-patch \
  VerifySpecStoreVariantDiscoveryPatch .ci-build/starfarer-variant-discovery-repeat.jar
mkdir -p .ci-build/verify-resource-loader
javac -cp .ci-build/asm/asm.jar -d .ci-build/verify-resource-loader \
  ci/VerifyResourceLoaderQuickStart.java
java -cp .ci-build/asm/asm.jar:.ci-build/verify-resource-loader \
  VerifyResourceLoaderQuickStart jars/starfarer_obf.jar
java -cp .ci-build/asm/asm.jar:.ci-build/transform \
  VerifyDeferredTexturePatches jars/starfarer_obf.jar jars/fs.common_obf.jar

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
  com.fs.graphics.TextureLoader \
  com.fs.starfarer.BrowserDeferredTextureQueue \
  com.fs.starfarer.BrowserGameplayProbe \
  com.fs.starfarer.api.impl.campaign.abilities.BaseAbilityPlugin \
  com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility \
  com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility \
  com.fs.starfarer.api.impl.campaign.abilities.TransponderAbility \
  com.fs.starfarer.campaign.save.CampaignGameManager \
  com.fs.starfarer.loading.ooOo \
  com.fs.starfarer.loading.oOoO \
  com.fs.starfarer.loading.BrowserFastCsvParser \
  com.fs.starfarer.loading.BrowserTextPreprocessor \
  com.fs.starfarer.campaign.rules.BrowserRuleDuplicateIndex \
  com.fs.starfarer.BaseGameState

# Rules itself is valid HotSpot bytecode, but its method descriptor references
# stock ResourceLoaderState, whose obfuscated method name `if.new` HotSpot rejects
# before Rules method resolution completes. Verify Rules against a descriptor-only
# stub on a separate classpath. The stub never enters any runtime JAR/package.
rm -rf .ci-build/verify-rules-hotspot-stub
mkdir -p .ci-build/verify-rules-hotspot-stub
javac -encoding UTF-8 --release 8 \
  -d .ci-build/verify-rules-hotspot-stub \
  ci/hotspot-verify-stubs/com/fs/starfarer/loading/ResourceLoaderState.java
java -Xverify:all -cp \
  ".ci-build/verify:.ci-build/verify-rules-hotspot-stub:jars/fixer_patch.jar:$CP" \
  VerifyPatchedRuntimeClasses com.fs.starfarer.campaign.rules.Rules

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
if [[ "${STARSECTOR_PUBLIC_TUTORIAL_SMOKE:-false}" == "true" ]]; then
  TUTORIAL_OUT="${OUT}-tutorial"
  rm -rf "$TUTORIAL_OUT"
  STARSECTOR_TEST_URL=http://127.0.0.1:8000/launch.html \
  STARSECTOR_TEST_TIMEOUT_MS=720000 \
  STARSECTOR_FRAME_SETTLE_MS=5000 \
  STARSECTOR_EXPECT_STATE=campaign \
  STARSECTOR_DEEP_GAMEPLAY=false \
  STARSECTOR_SAVE_LOAD_SMOKE=false \
  STARSECTOR_WINDOW_CONFIG='{"__STARSECTOR_AUTO_CAMPAIGN_SECTOR_SIZE__":"normal","__STARSECTOR_AUTO_CAMPAIGN_STARTING_LOCATION__":"Galatia","__STARSECTOR_BROWSER_TUTORIAL__":true,"__STARSECTOR_BROWSER_GAMEPLAY_PROBE__":false}' \
  STARSECTOR_TEST_OUTPUT_DIR="$TUTORIAL_OUT" \
    node ci/campaign-render-test.js
  python3 ci/verify-full-campaign-map.py "$TUTORIAL_OUT/browser.log" \
    --expected-sector-size normal \
    --expected-start-location Galatia \
    --expected-seed SEK968276040 \
    --require-tutorial
fi
# The optimized run is only valid if the real game loaded and used the bulk spec
# cache. This prevents a transparent fallback from being mistaken for a speedup.
grep -q 'BrowserSpecCache: ready' "$OUT/browser.log"
grep -q 'BrowserSpecCache: first-hit' "$OUT/browser.log"
grep -q 'BrowserFastCsvParser: enabled stock fast path' "$OUT/browser.log"
grep -q 'BrowserTextPreprocessor: enabled exact linear smart-quote normalization' "$OUT/browser.log"
grep -q 'BrowserJaninoNegativeCache: remember path=' "$OUT/browser.log"
grep -q 'BrowserRuleDuplicateIndex: rules=' "$OUT/browser.log"
grep -q 'BrowserDeferredTexture: first-deferred' "$OUT/browser.log"
if [[ "${STARSECTOR_EXPECT_GAMEPLAY_PREWARM:-false}" == "true" ]]; then
  grep -q 'BrowserDeferredTexturePrewarm: scheduled' "$OUT/browser.log"
fi
if [[ "${STARSECTOR_DEEP_GAMEPLAY:-false}" == "true" ]]; then
  python3 ci/verify-full-campaign-map.py "$OUT/browser.log" \
    --expected-sector-size normal \
    --expected-start-location Corvus \
    --expected-seed SEK968276040
  grep -q 'Fixer: auto campaign deep escape ability id=fracture_jump ready=true' "$OUT/browser.log"
  grep -q 'BrowserGameplayProbe: .*event=gameplay-speedup mult=8.0' "$OUT/browser.log"
  grep -q 'BrowserGameplayProbe: .*event=ability-ui-ready' "$OUT/browser.log"
  grep -q 'BrowserGameplayProbe: .*event=ability-ui-action' "$OUT/browser.log"
  grep -q 'BrowserGameplayProbe: .*event=control-match control=CORE_ABILITY_7' "$OUT/browser.log"
  grep -q 'BrowserGameplayProbe: .*event=ability-press' "$OUT/browser.log"
  grep -q 'BrowserGameplayProbe: .*event=core-tab-ready' "$OUT/browser.log"
fi
