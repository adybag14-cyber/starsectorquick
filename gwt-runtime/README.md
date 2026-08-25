# Starsector 0.98a-RC8 GWT runtime

This directory contains the validated optimized GWT JavaScript and the two
deterministic asset allow-lists used by `ci/build-gwt-packs.py`. The deployment
workflow builds the binary packs from the official 0.98a-RC8 Linux payload; the
100 MB graphics pack is not stored on the source branch.

Build provenance:

- GWT: 2.12.2, optimized `OBF`, two permutations
- Java source set: 3,585 compiled units
- source-tree aggregate SHA-256: `0a89897aef931dfb4806d19360d5c587d46d46b692913a13c19844600ae5c8b1`
- `65EBC5E87CA8B85D1E0BF9C668B5C8B0.cache.js`: `ed71246f34f5481aa0b96f8b88dc33ec127c9750a73c5ac94288451c6e49660d`
- `E3F89E823494D846C13F41F04CAD0510.cache.js`: `6231ee9e180d99f3f3b21097e8419d954e1075e3d64f4622d089a40b9189210c`
- data allow-list: 1,400 files
- graphics allow-list: 3,383 files

Validated browser behavior includes the title, the full normal new-game and
developer quick-start flows, Galatia tutorial campaign rendering, hyperspace,
keyboard/pointer input, WebGL framebuffer/stencil composites, and 1024x768 to
1280x720 resolution persistence.

The canvas uses the largest aspect-correct fit inside the current browser
viewport and recomputes that fit when the viewport or internal resolution
changes. Presentation is paced directly by `requestAnimationFrame`, with no
artificial 60 FPS ceiling; 120 Hz and 144 Hz displays may therefore present
more than 60 frames per second while simulation continues to use timestamp
deltas.

`ci/verify-gwt-pages-runtime.js` drives the deployed normal-new-game tutorial
path into Galatia and rejects a mostly-white framebuffer. Its `software-ci`
profile is a frame-progress gate for GitHub-hosted SwiftShader; the `hardware`
profile retains the 58 FPS, 22 ms p95, 30 ms p99, and 3 ms jitter ceilings used
for GPU-backed browser validation.

The current browser XStream bridge retains campaign save objects for the life
of the page. Cross-reload save persistence is not yet equivalent to desktop
XStream; the retained CheerpJ launcher remains the persistence-compatible
fallback while that serializer boundary is completed.
