# CheerpJ Java 8 GL4ES/WASM rebuild guide

This guide documents the current best-known steps to rebuild GL4ES for the Java 8 Starsector runtime using Emscripten, then integrate the output into this repo.

## Prerequisites

- Emscripten SDK installed and configured (`emsdk_env.sh`).
- GL4ES source checked out (e.g. `https://github.com/ptitSeb/gl4es.git`).
- Java 8 Starsector assets available locally (see `starsector_linux-0.97a-RC11.zip`).

To bootstrap dependencies (game assets, GL4ES, Emscripten SDK, CFR, Java 8 JDK), you can run:

```
ROOT_DIR=/workspace \
TOOLS_DIR=/workspace/tools \
GAME_DIR=/workspace/starsector_java8 \
GL4ES_DIR=/workspace/gl4es \
EMSDK_DIR=/workspace/emsdk \
./scripts/bootstrap_java8_env.sh
```

## Build GL4ES (Emscripten)

```
export GL4ES_DIR=/workspace/gl4es
export EMSDK_ENV=/opt/emsdk/emsdk_env.sh
export OUTPUT_DIR=/workspace/starsectorquick/build/final/wasm-modules
./scripts/build_gl4es_wasm.sh
```

Output:
- `build/final/wasm-modules/gl4es.a`

You can then link the static archive into a runtime module during CheerpJ native integration.

If you need to tune flags for JNI/GL4ES integration, pass extra CMake/emmake flags:

```
EXTRA_CMAKE_FLAGS="-DNOGLX=ON" \
EXTRA_EMCMAKE_FLAGS="-sUSE_PTHREADS=0" \
EXTRA_EMMAKE_FLAGS="VERBOSE=1" \
./scripts/build_gl4es_wasm.sh
```

## Extract JNI native method signatures

Use `javap` to list `native` methods in the Starsector jars. This helps when wiring JNI stubs for Emscripten/GL4ES:

```
./scripts/extract_jni_native_methods.sh /path/to/starfarer_obf.jar
```

The output is stored in `test_output/native_methods.txt` by default.

## Generate JNI export names for Emscripten

If you need to drive Emscripten exports during native builds, generate JNI export names from the same jar:

```
./scripts/generate_jni_exports.sh /path/to/starfarer_obf.jar
```

The output is stored in `test_output/jni_exports.txt` by default and can be fed into `-sEXPORTED_FUNCTIONS=@jni_exports.txt` during Emscripten link steps.

## Run Playwright launch matrix

To probe multiple launch URLs and Java versions with the Node server:

```
JAR_ROOT=/path/to/starsector \
ASSET_ROOT=/path/to/starsector \
GAME_URLS="http://localhost:8888/STARSECTOR_V6J_FINAL_WORKING.html,http://localhost:8888/launch.html" \
GAME_JAVA_VERSIONS="8" \
./scripts/run_playwright_matrix.sh
```

Logs and screenshots are written to `test_output/`.

## Integration notes

- The launcher mounts `/app` as an overlay filesystem and expects `native/linux` and `native` folders to exist for LWJGL native lookups.
- `java.library.path` and `org.lwjgl.librarypath` are set to `/app/native/linux:/app/native` in the launcher.
- Place rebuilt GL4ES artifacts alongside existing CheerpJ WASM modules under `build/final/wasm-modules`.

## Known gaps

- The current CheerpJ runtime still hits `ModManager` `NullPointerException` and `Unsafe` JNI errors when launching the Java 8 build, so additional JNI glue work is expected.
- A full JNI map of Starsector-specific natives is required before a proper GL4ES build can be confirmed.
