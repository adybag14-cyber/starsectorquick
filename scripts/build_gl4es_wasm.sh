#!/usr/bin/env bash
set -euo pipefail

GL4ES_DIR=${GL4ES_DIR:-/workspace/gl4es}
BUILD_DIR=${BUILD_DIR:-$GL4ES_DIR/build-emscripten}
EMSDK_ENV=${EMSDK_ENV:-/opt/emsdk/emsdk_env.sh}
OUTPUT_DIR=${OUTPUT_DIR:-/workspace/starsectorquick/build/final/wasm-modules}
EXTRA_CMAKE_FLAGS=${EXTRA_CMAKE_FLAGS:-}
EXTRA_EMCMAKE_FLAGS=${EXTRA_EMCMAKE_FLAGS:-}
EXTRA_EMMAKE_FLAGS=${EXTRA_EMMAKE_FLAGS:-}

if [[ ! -d "$GL4ES_DIR" ]]; then
  echo "GL4ES_DIR not found: $GL4ES_DIR" >&2
  exit 1
fi

if [[ ! -f "$EMSDK_ENV" ]]; then
  echo "EMSDK_ENV not found: $EMSDK_ENV" >&2
  echo "Install/initialize Emscripten and set EMSDK_ENV to emsdk_env.sh" >&2
  exit 1
fi

source "$EMSDK_ENV"

cmake_flags=(
  -DCMAKE_BUILD_TYPE=RelWithDebInfo
  -DNOX11=ON
  -DNOEGL=ON
  -DSTATICLIB=ON
)

extra_cmake_flags=()
extra_emcmake_flags=()
extra_emmake_flags=()

if [[ -n "$EXTRA_CMAKE_FLAGS" ]]; then
  read -r -a extra_cmake_flags <<< "$EXTRA_CMAKE_FLAGS"
fi

if [[ -n "$EXTRA_EMCMAKE_FLAGS" ]]; then
  read -r -a extra_emcmake_flags <<< "$EXTRA_EMCMAKE_FLAGS"
fi

if [[ -n "$EXTRA_EMMAKE_FLAGS" ]]; then
  read -r -a extra_emmake_flags <<< "$EXTRA_EMMAKE_FLAGS"
fi

mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

emcmake "${extra_emcmake_flags[@]}" cmake "$GL4ES_DIR" "${cmake_flags[@]}" "${extra_cmake_flags[@]}"
emmake make -j"$(nproc)" "${extra_emmake_flags[@]}"

mkdir -p "$OUTPUT_DIR"

build_archive="$BUILD_DIR/libGL.a"
lib_archive="$GL4ES_DIR/lib/libGL.a"

if [[ -f "$build_archive" ]]; then
  cp -v "$build_archive" "$OUTPUT_DIR/gl4es.a"
elif [[ -f "$lib_archive" ]]; then
  cp -v "$lib_archive" "$OUTPUT_DIR/gl4es.a"
else
  echo "Expected libGL.a in $BUILD_DIR or $GL4ES_DIR/lib" >&2
  exit 1
fi

echo "GL4ES static lib copied to $OUTPUT_DIR/gl4es.a"
