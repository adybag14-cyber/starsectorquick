#!/usr/bin/env python3
from pathlib import Path

s = Path("ci/campaign-render-test.js").read_text(encoding="utf-8")

required = {
    "bounded polling interval": "STARSECTOR_PLAYABLE_POLL_MS",
    "bounded polling timeout": "STARSECTOR_PLAYABLE_POLL_TIMEOUT_MS",
    "poll within second-frame window": "Math.min(\n    secondFrameDueAt",
    "successful probe artifact": "frame-first-playable.png",
    "playable campaign hard gate": "reachedExpected && campaignPlayable && rendered",
    "second-frame due time preserved": "secondFrameDueAt - Date.now()",
    "probe count evidence": "playableProbeCount",
}
missing = [name for name, needle in required.items() if needle not in s]
if "firstFramePlayable ? firstFrameCapturedAt" in s:
    missing.append("legacy coarse two-shot first-playable chooser still present")
if s.index("const secondFrameDueAt") > s.index("const playablePollDeadline"):
    missing.append("second-frame due time must be established before polling deadline")
if missing:
    raise SystemExit("Playable-frame contract failed: " + "; ".join(missing))
print("PlayableFrameContract: OK bounded polling, hard playable gate, second-frame evidence preserved")
