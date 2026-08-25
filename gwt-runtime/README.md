# Starsector 0.98a-RC8 GWT runtime

This directory contains the validated optimized GWT JavaScript and the two
deterministic asset allow-lists used by `ci/build-gwt-packs.py`. The deployment
workflow builds the binary packs from the official 0.98a-RC8 Linux payload; the
100 MB graphics pack is not stored on the source branch.

Build provenance:

- GWT: 2.12.2, optimized `OBF`, two permutations
- Java source set: 3,585 compiled units
- canonical source-tree digest: `135aca32aac0cb9037e20824dfc424ef03fc56ecb55b56fb9e7bd03f1f3bd19a`
- `4A498B62021A25CF6620E01AC1957F24.cache.js`: `d0684f12d79daa09025fac4ef00041c59ba0c7201af7f211531a41ddb4881c22`
- `B63DFE0D3C9E9F10A408C4BA057B87D6.cache.js`: `5ca36621feb13625bf34ef97cf4b9f450fb8471a05739a884e4ca8ea5f326734`
- data allow-list: 1,400 files
- graphics allow-list: 3,383 files

Validated browser behavior includes the title, the full normal new-game and
developer quick-start flows, Galatia tutorial campaign rendering, hyperspace,
keyboard/pointer input, WebGL framebuffer/stencil composites, and 1024x768 to
1280x720 resolution persistence.

`ci/verify-gwt-pages-runtime.js` drives the deployed normal-new-game tutorial
path into Galatia and rejects a mostly-white framebuffer. Its `software-ci`
profile is a frame-progress gate for GitHub-hosted SwiftShader; the `hardware`
profile retains the 58 FPS, 22 ms p95, 30 ms p99, and 3 ms jitter ceilings used
for GPU-backed browser validation.

The current browser XStream bridge retains campaign save objects for the life
of the page. Cross-reload save persistence is not yet equivalent to desktop
XStream; the retained CheerpJ launcher remains the persistence-compatible
fallback while that serializer boundary is completed.
