# CheerpJ Java 8 GL4ES/WASM rebuild guide

This guide documents the current best-known steps to rebuild GL4ES for the Java 8 Starsector runtime using Emscripten, then integrate the output into this repo.

## Prerequisites

- Emscripten SDK installed and configured (`emsdk_env.sh`).
- GL4ES source checked out (e.g. `https://github.com/ptitSeb/gl4es.git`).
- Java 8 Starsector assets available locally (see `starsector_linux-0.97a-RC11.zip`).

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

## Extract JNI native method signatures

Use `javap` to list `native` methods in the Starsector jars. This helps when wiring JNI stubs for Emscripten/GL4ES:

```
./scripts/extract_jni_native_methods.sh /path/to/starfarer_obf.jar
```

The output is stored in `test_output/native_methods.txt` by default.

## Integration notes

- The launcher mounts `/app` as an overlay filesystem and expects `native/linux` and `native` folders to exist for LWJGL native lookups.
- `java.library.path` and `org.lwjgl.librarypath` are set to `/app/native/linux:/app/native` in the launcher.
- Place rebuilt GL4ES artifacts alongside existing CheerpJ WASM modules under `build/final/wasm-modules`.

## Known gaps

- The current CheerpJ runtime still hits `ModManager` `NullPointerException` and `Unsafe` JNI errors when launching the Java 8 build, so additional JNI glue work is expected.
- A full JNI map of Starsector-specific natives is required before a proper GL4ES build can be confirmed.
