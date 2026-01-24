# StarSector 2 WebAssembly - Project Structure Analysis

## 📊 Complete Project Overview

This document provides a comprehensive analysis of the entire project structure, including all directories, files, and their purposes.

---

## 🗂️ Root Directory Structure

```
starsectorquick/
├── 📄 Configuration & Documentation
│   ├── README.md                          # Main project documentation
│   ├── LICENSE                            # MIT license + third-party attributions
│   ├── .gitignore                         # Git ignore rules
│   └── .gitlab-ci.yml                     # GitLab CI configuration
│
├── 🎮 Game Launchers
│   ├── STARSECTOR_V6J_FINAL_WORKING.html  # Main production launcher
│   ├── index.html                         # Project homepage
│   └── test_starsector_*.html            # Various test launchers
│
├── 🧪 Testing & Scripts
│   ├── test_frontend.js                   # Playwright test suite
│   ├── run_playwright_test.js             # Test runner
│   ├── button_check.js                    # Button functionality test
│   ├── click_test.js                      # Click interaction test
│   ├── quick_test.js                      # Quick validation test
│   ├── simple_test.js                     # Simple test suite
│   ├── final_test.js                      # Final integration test
│   └── verify_no_errors.js                # Error verification script
│
├── 🐍 Python Build Scripts
│   ├── add_format_to_misc.py              # Add format to misc files
│   ├── add_quote_to_misc.py               # Add quotes to misc files
│   ├── cleanup_src.py                     # Clean up source files
│   ├── create_framework.py                # Create build framework
│   ├── create_server.py                   # Create test server
│   ├── fix_botched_clones.py              # Fix cloning issues
│   ├── fix_clones_final.py                # Final clone fixes
│   ├── fix_references.py                  # Fix file references
│   ├── fix_reserved_names.py              # Fix reserved names
│   ├── fix_string_format.py               # Fix string formatting
│   ├── mega_fix.py                        # Major fixes
│   ├── move_stubs.py                      # Move stub files
│   ├── patch_misc.py                      # Patch misc files
│   ├── refine_src.py                      # Refine source code
│   ├── replace_imports.py                 # Replace imports
│   └── install_jars*.py                   # JAR installation scripts
│
├── 📝 Documentation Files
│   ├── FINAL_SUMMARY.md                   # LWJGL compilation summary
│   ├── GWT_POLYFILL_REPORT.md             # GWT polyfill report
│   ├── FRONTEND_TEST_REPORT.md            # Test results
│   ├── EMSDK_INSTALLATION_REPORT.md       # Emscripten setup report
│   ├── COMPILATION_REPORT.md              # Compilation details
│   ├── COMPILE_SUMMARY.md                 # Compile summary
│   ├── PHASE_1_REPORT.md                  # Phase 1 report
│   └── PROJECT_STRUCTURE_ANALYSIS.md      # This file
│
├── 🔧 C/C++ Source Files
│   ├── memory_mapping_utils.c             # Buffer mapping utilities
│   ├── memory_mapping_utils.h             # Header file
│   └── test_memory_mapping.c              # Test file
│
├── 🌐 Server Files
│   ├── server.js                          # Node.js server
│   ├── server_gwt.js                      # GWT server
│   └── server_gwt_err.log                 # Error log
│
├── 📦 JAR Files (14 total)
│   └── jars/                              # Game JARs directory
│
├── 🔨 Build Artifacts
│   └── build/                             # Build output directory
│
├── ☕ Java Source (GWT)
│   └── starsector-gwt/                    # GWT compilation project
│
├── 🎮 Original Game Files
│   └── starsector/                        # Original game directory
│
├── 🧪 Test Infrastructure
│   ├── tests/                             # Test files
│   ├── test_output/                       # Test output
│   └── test_results/                      # Test results
│
├── 📚 Logs
│   └── logs/                              # Build and test logs
│
├── 🛠️ Tools
│   └── tools/                             # Build tools (excluded from git)
│
└── 📂 Excluded Directories (.gitignore)
    ├── decompiled_src/                    # Decompiled Java source
    ├── jadx_src/                          # JADX decompiled source
    ├── emsdk/                             # Emscripten SDK
    └── sources/                           # Source code for dependencies
```

---

## 📦 JAR Files Directory (`jars/`)

Contains 14 JAR files required for the game:

| JAR File | Purpose | Size |
|----------|---------|------|
| `starfarer_obf.jar` | Main game engine | ~50MB |
| `starfarer.api.jar` | Game API | ~5MB |
| `lwjgl.jar` | LWJGL library | ~1MB |
| `lwjgl_util.jar` | LWJGL utilities | ~500KB |
| `fs.common_obf.jar` | Common game code | ~10MB |
| `fs.sound_obf.jar` | Sound system | ~5MB |
| `commons-compiler.jar` | Compiler utilities | ~200KB |
| `janino.jar` | Java compiler | ~500KB |
| `jinput.jar` | Input handling | ~100KB |
| `jogg-0.0.7.jar` | OGG audio codec | ~50KB |
| `jorbis-0.0.15.jar` | Vorbis audio codec | ~100KB |
| `json.jar` | JSON parsing | ~50KB |
| `log4j-1.2.9.jar` | Logging framework | ~300KB |
| `xstream-1.4.10.jar` | XML serialization | ~500KB |

---

## 🔨 Build Directory (`build/`)

### Root Level Files
- `all_exports_merged.txt` - Merged export symbols
- `all_gl_functions.txt` - All OpenGL functions
- `all_native_methods.txt` - Native method signatures
- `emscipten_exports.txt` - Emscripten exports
- `exports_final.txt` - Final export list
- `generate_jni_wrappers.py` - JNI wrapper generator
- `jni_exports_clean.txt` - Clean JNI exports
- `opengl_functions_from_templates.txt` - OpenGL templates
- `raw_gl11_funcs.txt` - Raw GL11 functions

### Subdirectories

#### `build/final/` - Final Build Artifacts

**Documentation Files (50+ MD files):**
- `FINAL_SUMMARY.md` - Final compilation summary
- `FINAL_REPORT.md` - Detailed final report
- `CHEERPJ_4_2_FINAL_FIX.md` - CheerpJ fixes
- `GAME_LAUNCH_ANALYSIS.md` - Launch analysis
- `GWT_MIGRATION_GUIDE.md` - GWT migration guide
- `STARSECTOR_LAUNCH_GUIDE.md` - Launch instructions
- `ULTIMATE_GUIDE.md` - Ultimate guide
- And 40+ more documentation files

**LWJGL Compilation Artifacts:**
- `lwjgl_40.js` + `lwjgl_40.wasm` - Working version (37 functions)
- `lwjgl_100.js` + `lwjgl_100.wasm` - 100 functions
- `lwjgl_117.js` + `lwjgl_117.wasm` - 117 functions
- `lwjgl_294.js` + `lwjgl_294.wasm` - 294 functions
- `lwjgl_691.js` + `lwjgl_691.wasm` - 691 functions (all)
- Multiple `.c` source files for each version

**Test HTML Files (30+ versions):**
- `starsector-v2.html` through `starsector-v6l.html`
- `STARSECTOR_V6J_FINAL_CHEERPJRUNJAR.html`
- `workstream_01_clearing.html` through `workstream_06_viewport.html`
- `TEST_DASHBOARD.html`
- `PARALLEL_TEST_RUNNER.html`

**Build Scripts:**
- `generate_all_294_fixed.py` - Generate 294 functions
- `generate_all_691.py` - Generate all 691 functions
- `generate_full_jni.py` - Full JNI generator
- `jni_analyzer.py` - JNI analyzer
- `extract_jni.py` - JNI extractor

**Configuration:**
- `package.json` - Node.js dependencies
- `playwright_config.js` - Playwright configuration
- `pom-gwt.xml` - Maven GWT configuration
- `Starsector.gwt.xml` - GWT module configuration

#### `build/final/wasm-modules/` - WebAssembly Modules

**Core Modules:**
- `gl4es.wasm` - OpenGL 1.x/2.x to WebGL 2.0 translation (2.6MB)
- `lwjgl.js` - LWJGL JavaScript bindings (123B)
- `unsafe.wasm` - Unsafe operations (4.5KB)
- `cheerpj-awt.jar` - AWT polyfill (107KB)
- `liblwjgl.so` - LWJGL shared library stub

**Extracted:**
- `cheerpj-awt-extracted/` - Extracted AWT classes

#### `build/final/polyfills/` - Java Polyfills

**AWT Polyfills:**
- `HashtableEntrySet.js` - Hashtable entry set
- Additional AWT class polyfills

#### `build/final/starsector-gwt/` - GWT Compiled Output

- Compiled Java classes
- GWT JavaScript output
- Module definitions

#### `build/final/parallel-workstreams/` - Parallel Build Workspace

- Workstream configurations
- Parallel build artifacts
- Build logs

#### `build/final/chunks_compiled/` - Compiled Chunks

- Compiled object files
- Chunk artifacts

#### `build/final/deobfuscation-tools/` - Deobfuscation Tools

- Deobfuscation scripts
- Analysis tools

#### `build/final/phase3-unsafe/` - Phase 3 Unsafe Implementation

- Unsafe operation implementations
- Memory management

---

## ☕ GWT Source Directory (`starsector-gwt/`)

### Structure
```
starsector-gwt/
├── pom.xml                              # Maven POM file
├── libs/                                # Library dependencies
├── src/
│   └── main/
│       ├── java/                        # Java source files
│       │   ├── com/                     # Game-specific code
│       │   │   └── fs/
│       │   │       └── Starfarer.gwt.xml
│       │   ├── java/                    # Java standard library polyfills
│       │   │   ├── awt/                 # AWT classes
│       │   │   │   ├── Color.java
│       │   │   │   └── image/           # Image classes
│       │   │   │       ├── BufferedImage.java
│       │   │   │       ├── ColorModel.java
│       │   │   │       ├── DirectColorModel.java
│       │   │   │       ├── WritableRaster.java
│       │   │   │       ├── Raster.java
│       │   │   │       ├── SampleModel.java
│       │   │   │       └── DataBuffer.java
│       │   │   └── util/                # Utility classes
│       │   │       ├── zip/             # ZIP utilities
│       │   │       │   ├── DataFormatException.java
│       │   │       │   ├── Inflater.java
│       │   │       │   └── Deflater.java
│       │   │       ├── regex/           # Regex support
│       │   │       │   ├── Pattern.java
│       │   │       │   └── Matcher.java
│       │   │       ├── GregorianCalendar.java
│       │   │       └── StringFormat.java
│       │   └── org/                     # Third-party libraries
│       │       ├── lwjgl/               # LWJGL polyfills
│       │       │   └── util/vector/
│       │       │       └── Vector2f.java
│       │       ├── apache/              # Apache Commons
│       │       └── json/                # JSON library
│       └── resources/                   # Resource files
└── target/                              # Maven build output
```

### Key Polyfills Implemented

**AWT (Abstract Window Toolkit):**
- `Color` - Color representation
- `BufferedImage` - Image handling
- `ColorModel` - Color model abstraction
- `DirectColorModel` - Direct color model
- `WritableRaster` - Writable raster
- `Raster` - Raster operations
- `SampleModel` - Sample model
- `DataBuffer` - Data buffer

**Utilities:**
- `Inflater` / `Deflater` - Compression
- `Pattern` / `Matcher` - Regular expressions
- `GregorianCalendar` - Calendar
- `StringFormat` - String formatting

**LWJGL:**
- `Vector2f` - 2D vector operations

---

## 🎮 Original Game Directory (`starsector/`)

### Structure
```
starsector/
└── starsector/
    ├── LICENSE.txt                       # Game license
    ├── starsector.bat                    # Windows launcher
    ├── starsector.sh                     # Linux/Mac launcher
    ├── data/                             # Game data
    │   ├── campaign/                     # Campaign data
    │   │   ├── abilities.csv             # Ship abilities
    │   │   ├── commodities.csv           # Trade commodities
    │   │   ├── industries.csv           # Industries
    │   │   ├── rules.csv                # Game rules
    │   │   ├── starmap.json             # Star map
    │   │   ├── econ/                    # Economy
    │   │   ├── procgen/                 # Procedural generation
    │   │   └── terrain/                 # Terrain types
    │   ├── characters/                   # Character data
    │   │   ├── person_names.csv         # Name database
    │   │   ├── personalities.csv        # NPC personalities
    │   │   └── skills/                  # Skill definitions
    │   ├── config/                       # Configuration
    │   │   ├── settings.json            # Game settings
    │   │   ├── sounds.json              # Sound config
    │   │   └── planets.json             # Planet types
    │   ├── hullmods/                     # Hull modifications
    │   │   └── *.java                   # Hull mod scripts
    │   ├── hulls/                        # Ship hulls
    │   ├── missions/                     # Mission scripts
    │   ├── scripts/                      # Game scripts
    │   ├── shipsystems/                  # Ship systems
    │   ├── strings/                      # String resources
    │   ├── variants/                     # Ship variants
    │   ├── weapons/                      # Weapon definitions
    │   └── world/                        # World generation
    ├── graphics/                         # Graphics assets
    │   ├── asteroids/                    # Asteroid sprites
    │   ├── backgrounds/                  # Background images
    │   ├── cursors/                      # Mouse cursors
    │   ├── damage/                       # Damage effects
    │   ├── debris/                       # Debris sprites
    │   ├── factions/                     # Faction icons
    │   ├── fonts/                        # Font files
    │   ├── fx/                           # Visual effects
    │   ├── hud/                          # HUD elements
    │   ├── icons/                        # UI icons
    │   ├── illustrations/                # Illustrations
    │   ├── misc/                         # Miscellaneous graphics
    │   ├── missiles/                     # Missile sprites
    │   ├── planets/                      # Planet textures
    │   ├── portraits/                    # Character portraits
    │   ├── ships/                        # Ship sprites
    │   ├── starscape/                    # Star backgrounds
    │   ├── stations/                     # Station sprites
    │   ├── terrain/                      # Terrain graphics
    │   ├── ui/                           # UI elements
    │   ├── warroom/                      # War room graphics
    │   └── weapons/                      # Weapon sprites
    ├── sounds/                           # Audio assets
    │   ├── music/                        # Background music
    │   ├── sfx_abilities/                # Ability sounds
    │   ├── sfx_bar/                      # Bar sounds
    │   ├── sfx_cargo/                    # Cargo sounds
    │   ├── sfx_engines/                  # Engine sounds
    │   ├── sfx_flux/                     # Flux sounds
    │   ├── sfx_impacts/                  # Impact sounds
    │   ├── sfx_interface/                # UI sounds
    │   ├── sfx_misc/                     # Miscellaneous sounds
    │   ├── sfx_refit/                    # Refit sounds
    │   ├── sfx_shields/                  # Shield sounds
    │   ├── sfx_systems/                  # System sounds
    │   ├── sfx_terrain/                  # Terrain sounds
    │   ├── sfx_wpn_energy/               # Energy weapon sounds
    │   ├── sfx_wpn_guns/                 # Gun sounds
    │   ├── sfx_wpn_missiles/             # Missile sounds
    │   └── soe/                          # Sound effects
    ├── jre_linux/                        # Linux JRE
    │   ├── bin/                          # Java binaries
    │   ├── lib/                          # Java libraries
    │   └── man/                          # Manual pages
    ├── native/                           # Native libraries
    │   └── linux/                        # Linux native libs
    ├── mods/                             # Mods directory
    ├── saves/                            # Save games
    └── screenshots/                      # Screenshots
```

---

## 🧪 Test Infrastructure

### Test Files
```
tests/
├── integration/                          # Integration tests
└── unit/                                 # Unit tests
    └── unsafe/                           # Unsafe operation tests
```

### Test Output
```
test_output/
├── step1_loaded.png                     # Screenshot: loaded
├── step3_initialize_clicked.png         # Screenshot: initialize
├── step4_after_init.png                 # Screenshot: after init
├── step5_launch_clicked.png             # Screenshot: launch
├── step6_final.png                      # Screenshot: final
└── test_2026-01-23T22-52-36.log        # Test log
```

### Test Results
```
test_results/                             # Test result files
```

---

## 📚 Logs Directory

```
logs/
├── ws-02a-emsdk-clone.log               # EMSDK clone log
├── ws-02c-emscripten-activate.log       # Emscripten activation
├── ws-02d-emscripten-test.log           # Emscripten test
├── ws-03-gl4es.log                      # GL4ES build
├── ws-04-lwjgl-generate.log             # LWJGL generation
├── ws-04-lwjgl.log                      # LWJGL build
├── ws-05-starsector.log                 # Starsector build
├── ws-06-browsercraft.log               # Browsercraft test
├── ws-07-cheerpj.log                    # CheerpJ test
└── ws-09-ant-generate.log               # Ant generation
```

---

## 🛠️ Tools Directory (Excluded from Git)

```
tools/
├── apache-maven-3.9.6/                  # Maven build tool
└── jdk-11.0.21+9/                       # Java Development Kit
```

---

## 📂 Excluded Directories (.gitignore)

### `decompiled_src/`
- Decompiled Java source from JAR files
- Used for analysis and reference
- Too large for Git (100MB+)

### `jadx_src/`
- JADX decompiled source
- Alternative decompilation
- Too large for Git

### `emsdk/`
- Emscripten SDK installation
- 500MB+ of tools and libraries
- Can be installed separately

### `sources/`
- Source code for dependencies
- GL4ES, Browsercraft, etc.
- Can be cloned separately

---

## 📊 File Statistics

### By Type
- **HTML Files**: 35+ (launchers, tests, dashboards)
- **JavaScript Files**: 20+ (tests, servers, utilities)
- **Python Scripts**: 30+ (build, fix, install scripts)
- **C/C++ Files**: 5+ (memory mapping, LWJGL wrappers)
- **Java Files**: 20+ (GWT polyfills)
- **Markdown Files**: 60+ (documentation)
- **JAR Files**: 14 (game libraries)
- **WASM Files**: 10+ (compiled modules)
- **Configuration Files**: 10+ (XML, JSON, YAML)

### By Size
- **JAR Files**: ~70MB total
- **WASM Modules**: ~3MB total
- **Source Code**: ~50,000+ lines
- **Documentation**: ~100,000+ words
- **Test Files**: ~5,000+ lines

---

## 🔑 Key Files Summary

### Essential for Running the Game
1. `STARSECTOR_V6J_FINAL_WORKING.html` - Main launcher
2. `jars/` - All 14 JAR files
3. `build/final/wasm-modules/` - WASM modules
4. `build/final/polyfills/` - Java polyfills

### Essential for Development
1. `README.md` - Main documentation
2. `FINAL_SUMMARY.md` - Compilation summary
3. `starsector-gwt/` - GWT source
4. `build/final/` - Build artifacts

### Essential for Testing
1. `test_frontend.js` - Playwright tests
2. `tests/` - Test infrastructure
3. `test_output/` - Test results

---

## 🎯 Project Architecture

### Technology Stack
```
┌─────────────────────────────────────────┐
│         Browser (Chrome/Firefox)        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         HTML/JavaScript Layer           │
│  - STARSECTOR_V6J_FINAL_WORKING.html    │
│  - CheerpJ 4.2 Loader                   │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         CheerpJ Runtime                 │
│  - Java to WebAssembly compiler         │
│  - Class loading                        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         WebAssembly Layer               │
│  - gl4es.wasm (OpenGL → WebGL)          │
│  - lwjgl.js (LWJGL bindings)            │
│  - unsafe.wasm (Unsafe operations)      │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         WebGL 2.0                       │
│  - Hardware-accelerated graphics        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         Game JARs                       │
│  - starfarer_obf.jar (main game)       │
│  - lwjgl.jar (graphics library)         │
│  - +12 other JARs                       │
└─────────────────────────────────────────┘
```

---

## 📝 Notes

### Git Repository
- **Remote**: https://github.com/adybag14-cyber/starsectorquick.git
- **Branch**: feature/starsector-webgl-integration
- **Size**: ~100MB (excluding large directories)

### Build System
- **Primary**: Emscripten (C/C++ → WASM)
- **Secondary**: Maven (Java → GWT)
- **Package Manager**: npm (JavaScript dependencies)

### Testing
- **Framework**: Playwright
- **Coverage**: 11 test cases
- **Timeout**: 20 seconds
- **Node Version**: node.exe (not node)

---

## 🚀 Quick Reference

### Run the Game
```bash
# Start server
python3 -m http.server 8000

# Open in browser
http://localhost:8000/STARSECTOR_V6J_FINAL_WORKING.html
```

### Run Tests
```bash
# Install dependencies
npm install

# Run tests
node test_frontend.js
```

### Build LWJGL
```bash
cd build/final
emcc lwjgl_clean.c -o lwjgl_40.js \
    -s WASM=1 \
    -s EXPORTED_FUNCTIONS="['_Java_org_lwjgl_*']"
```

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-24  
**Author**: StarSector 2 WebAssembly Team