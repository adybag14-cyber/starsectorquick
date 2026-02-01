#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=${ROOT_DIR:-/workspace}
TOOLS_DIR=${TOOLS_DIR:-${ROOT_DIR}/tools}
GAME_DIR=${GAME_DIR:-${ROOT_DIR}/starsector_java8}
GL4ES_DIR=${GL4ES_DIR:-${ROOT_DIR}/gl4es}
EMSDK_DIR=${EMSDK_DIR:-${ROOT_DIR}/emsdk}
CFR_VERSION=${CFR_VERSION:-0.152}

GAME_ZIP_URL=${GAME_ZIP_URL:-"https://f005.backblazeb2.com/file/fractalsoftworks/release/starsector_linux-0.97a-RC11.zip"}
GL4ES_REPO=${GL4ES_REPO:-"https://github.com/ptitSeb/gl4es.git"}
EMSDK_REPO=${EMSDK_REPO:-"https://github.com/emscripten-core/emsdk.git"}
CFR_URL=${CFR_URL:-"https://www.benf.org/other/cfr/cfr-${CFR_VERSION}.jar"}
JAVA8_JDK_URL=${JAVA8_JDK_URL:-"https://developers.redhat.com/content-gateway/file/openjdk/1.8.0.482/java-1.8.0-openjdk-portable-1.8.0.482.b08-1.portable.jdk.el.x86_64.tar.xz"}

mkdir -p "$TOOLS_DIR" "$GAME_DIR"

fetch() {
  local url="$1"
  local out="$2"
  if [[ -f "$out" ]]; then
    echo "Already present: $out"
    return 0
  fi
  echo "Downloading $url -> $out"
  curl -L --fail "$url" -o "$out"
}

if [[ ! -d "$GL4ES_DIR/.git" ]]; then
  echo "Cloning GL4ES into $GL4ES_DIR..."
  git clone "$GL4ES_REPO" "$GL4ES_DIR"
else
  echo "GL4ES already present: $GL4ES_DIR"
fi

if [[ ! -d "$EMSDK_DIR/.git" ]]; then
  echo "Cloning Emscripten SDK into $EMSDK_DIR..."
  git clone "$EMSDK_REPO" "$EMSDK_DIR"
else
  echo "Emscripten SDK already present: $EMSDK_DIR"
fi

fetch "$CFR_URL" "$TOOLS_DIR/cfr.jar"

fetch "$JAVA8_JDK_URL" "$TOOLS_DIR/java8-jdk.tar.xz"

if [[ ! -d "$GAME_DIR/starsector" ]]; then
  fetch "$GAME_ZIP_URL" "$TOOLS_DIR/starsector_java8.zip"
  echo "Extracting game assets into $GAME_DIR..."
  unzip -q "$TOOLS_DIR/starsector_java8.zip" -d "$GAME_DIR"
else
  echo "Game assets already present: $GAME_DIR/starsector"
fi

echo "Bootstrap complete."
