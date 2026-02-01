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
JAVA8_JRE_URL=${JAVA8_JRE_URL:-"https://developers.redhat.com/content-gateway/file/openjdk/1.8.0.482/java-1.8.0-openjdk-portable-1.8.0.482.b08-1.portable.jre.el.x86_64.tar.xz"}
ALLOW_MISSING=${ALLOW_MISSING:-0}

mkdir -p "$TOOLS_DIR" "$GAME_DIR"

fetch() {
  local url="$1"
  local out="$2"
  if [[ -f "$out" ]]; then
    echo "Already present: $out"
    return 0
  fi
  echo "Downloading $url -> $out"
  curl -L --fail -A "Mozilla/5.0" "$url" -o "$out"
}

fetch_any() {
  local out="$1"
  shift
  local url
  for url in "$@"; do
    if fetch "$url" "$out"; then
      return 0
    fi
  done
  return 1
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

if ! fetch_any "$TOOLS_DIR/java8-jdk.tar.xz" "$JAVA8_JDK_URL"; then
  echo "Warning: failed to download Java 8 JDK from configured URL." >&2
  if [[ "$ALLOW_MISSING" != "1" ]]; then
    echo "Set ALLOW_MISSING=1 to continue without the Java 8 JDK." >&2
    exit 1
  fi
fi

if ! fetch_any "$TOOLS_DIR/java8-jre.tar.xz" "$JAVA8_JRE_URL"; then
  echo "Warning: failed to download Java 8 JRE from configured URL." >&2
fi

if [[ ! -d "$GAME_DIR/starsector" ]]; then
  fetch "$GAME_ZIP_URL" "$TOOLS_DIR/starsector_java8.zip"
  echo "Extracting game assets into $GAME_DIR..."
  unzip -q "$TOOLS_DIR/starsector_java8.zip" -d "$GAME_DIR"
else
  echo "Game assets already present: $GAME_DIR/starsector"
fi

echo "Bootstrap complete."
