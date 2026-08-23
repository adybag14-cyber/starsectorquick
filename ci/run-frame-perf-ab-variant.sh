#!/usr/bin/env bash
set -uo pipefail

NAME=${1:?variant name required}
REF=${2:?git ref required}
ROOT=${GITHUB_WORKSPACE:-$(pwd)}
WORKTREE="${RUNNER_TEMP:-/tmp}/starsector-frame-perf-${NAME}"
ARCHIVE="$ROOT/.ci-cache/starsector_linux-0.98a-RC8.zip"
OUT_ROOT="$ROOT/test_output/frame-perf-ab"
STATUS_FILE="$OUT_ROOT/status.tsv"

mkdir -p "$OUT_ROOT"
rm -rf "$WORKTREE"
if ! GIT_LFS_SKIP_SMUDGE=1 git worktree add --detach "$WORKTREE" "$REF"; then
  printf '%s\t%s\t%s\t%s\n' "$NAME" "$REF" "125" "125" >> "$STATUS_FILE"
  exit 0
fi
mkdir -p "$WORKTREE/.ci-cache"
cp "$ARCHIVE" "$WORKTREE/.ci-cache/starsector_linux-0.98a-RC8.zip"
cp "$ROOT/ci/patch-frame-perf-fixed-seed.py" "$WORKTREE/ci/patch-frame-perf-fixed-seed.py"
cp "$ROOT/ci/patch-frame-tail-telemetry.py" "$WORKTREE/ci/patch-frame-tail-telemetry.py"
cp "$ROOT/ci/patch-playtesting-mode-telemetry.py" "$WORKTREE/ci/patch-playtesting-mode-telemetry.py"
cp "$ROOT/ci/verify-lwjgl-frame-timing.js" "$WORKTREE/ci/verify-lwjgl-frame-timing.js"
if ! (
  set -euo pipefail
  cd "$WORKTREE"
  python3 ci/patch-frame-perf-fixed-seed.py
  python3 ci/patch-frame-tail-telemetry.py
  python3 ci/patch-playtesting-mode-telemetry.py
  node ci/verify-lwjgl-frame-timing.js build/final/wasm-modules/lwjgl.js
  grep -q 'FRAME_TAIL_TELEMETRY_BENCH_V1' build/final/wasm-modules/lwjgl.js
  grep -q 'frameP95Ms: Number(perfAfter.frameP95Ms' ci/campaign-render-test.js
); then
  ACTUAL_REF=$(git -C "$WORKTREE" rev-parse HEAD 2>/dev/null || printf '%s' "$REF")
  printf '%s	%s	%s	%s
' "$NAME" "$ACTUAL_REF" "124" "124" >> "$STATUS_FILE"
  git worktree remove --force "$WORKTREE" || true
  exit 0
fi

cleanup_server() {
  if [[ -s /tmp/starsector-http.pid ]]; then
    kill "$(cat /tmp/starsector-http.pid)" 2>/dev/null || true
    rm -f /tmp/starsector-http.pid
  fi
}
cleanup_server

set +e
(
  cd "$WORKTREE"
  CHEERPJ_JAVA_VERSION=17 \
  STARSECTOR_DEEP_GAMEPLAY=true \
  STARSECTOR_PUBLIC_TUTORIAL_SMOKE=false \
  STARSECTOR_SAVE_LOAD_SMOKE=false \
  STARSECTOR_SCREENSHOT_TIMEOUT_MS=30000 \
  STARSECTOR_TEST_TIMEOUT_MS=720000 \
    bash ci/run-campaign-experiment.sh "$NAME" sync false campaign '{}'
)
RC=$?
set -e
cleanup_server

if [[ -d "$WORKTREE/test_output/$NAME" ]]; then
  rm -rf "$OUT_ROOT/$NAME"
  cp -a "$WORKTREE/test_output/$NAME" "$OUT_ROOT/$NAME"
fi

VERIFY_RC=0
if [[ "$RC" -eq 0 && -s "$OUT_ROOT/$NAME/browser.log" ]]; then
  python3 "$WORKTREE/ci/verify-full-campaign-map.py" "$OUT_ROOT/$NAME/browser.log" \
    --expected-sector-size normal \
    --expected-start-location Galatia || VERIFY_RC=$?
fi

ACTUAL_REF=$(git -C "$WORKTREE" rev-parse HEAD 2>/dev/null || printf '%s' "$REF")
printf '%s\t%s\t%s\t%s\n' "$NAME" "$ACTUAL_REF" "$RC" "$VERIFY_RC" >> "$STATUS_FILE"
git worktree remove --force "$WORKTREE" || true
exit 0
