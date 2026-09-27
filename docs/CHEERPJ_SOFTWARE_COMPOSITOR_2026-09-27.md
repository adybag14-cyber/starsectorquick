# Software compositor profile: separate the runner from the renderer

The narrow depth-state experiment did not solve GitHub-runner stalls. In the
completed same-campaign run 36340640216, median window FPS changed from 10.4503
to 10.4780 (+0.27%); p95 changed from 135.05 to 134.0ms and p99 from 147.55 to
145.15ms, while jitter slightly worsened. The separate default gate failed on a
282.5ms p99. Local results had +13% median FPS but substantially worse tails.
The depth mirror remains available for explicit testing, but is returned to
OFF by default. No performance threshold is changed.

## The larger observed runner cost

In run 36339294869, a 12.19-second trace recorded 105 `GLES2::ReadPixels` events
with 6.27 seconds of inclusive duration, nested around command-buffer waits.
No corresponding page-level readPixels calls were recorded by the active game
context wrapper. This is different from the local hardware-backed trace, where
repeated depth queries themselves were the dominant observed synchronous calls.
It points to a browser readback path; the trace alone does not identify every
caller or prove a complete root cause.

Chrome documents two distinct SwiftShader configurations: using it only as a
WebGL fallback, and using it as the OpenGL ES driver. The first can leave the
rest of the browser on its software compositor. The second exercises the GPU
compositing code path while the rendering device is still CPU-based SwiftShader.
Headless shell also documents `--enable-gpu` to stop forcing software compositing.

This CI-only experiment uses:

```text
--enable-unsafe-swiftshader
--enable-gpu
--use-gl=angle
--use-angle=swiftshader
```

The existing `default` profile remains exactly `--enable-unsafe-swiftshader`.
`STARSECTOR_BROWSER_RENDERER=swiftshader-driver` explicitly selects the new
profile; unsupported strings fail rather than becoming arbitrary command flags.
The test records Chromium version, actual ANGLE device identity, selected flags
and GPU feature status. Requested driver mode fails if the actual device is not
SwiftShader or GPU compositing is not enabled. This is not a hardware-GPU upgrade,
not a change to end-user browser settings, and not a browser-security workaround.
No additional sandbox-disable or blocklist-disable flags are added.

## Local contract result

Both profiles rendered the expected `[17,34,51,255]` pixel and advanced actual
animation frames. Both reported the same CPU SwiftShader device. The default
profile reported `gpu_compositing: disabled_software`; the explicit driver profile
reported `gpu_compositing: enabled`. This verifies the intended distinction but
is not a campaign FPS benchmark or proof of Linux-runner compatibility.

## Hosted qualification

The candidate workflow explicitly selects driver mode for the real campaign and
Full-HD tests. The worker/title-control lane keeps its original default profile.
The unchanged game, framebuffer sizes, image checks, interactions and timing
limits remain in force. Three normal 240-interval windows are collected, then a
separate bounded four-second trace. Tracing happens outside measured windows.

A successful comparison must consider actual submitted/presented frames and
readback costs, not merely the name of a command-line flag. These are distinct
browser processes/runs with generated campaign worlds, so an absolute comparison
with older default runs is not a counterbalanced same-world speedup estimate.
Nothing is published or merged while the experiment is being qualified.

Official references consulted 2026-09-27:
- https://chromium.googlesource.com/chromium/src/+/main/docs/gpu/swiftshader.md
- https://chromium.googlesource.com/chromium/src/+/HEAD/docs/gpu/using-gpu-hardware-in-headless-chrome.md
- https://pptr.dev/next/troubleshooting#chrome-headless-shell-disables-gpu-compositing
