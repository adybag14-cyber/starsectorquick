#!/usr/bin/env python3
import os
import runpy
from pathlib import Path

version = os.environ.get("CHEERPJ_JAVA_VERSION", "8").strip()
if version not in {"8", "11", "17"}:
    raise RuntimeError(f"Unsupported CheerpJ Java version: {version}")

root = Path(__file__).resolve().parents[1]
path = root / "launch.html"
text = path.read_text(encoding="utf-8")
old = "                    version: 17,"
new = f"                    version: {version},"
count = text.count(old)
if count != 1:
    raise RuntimeError(f"CheerpJ version field: expected one Java 17 match, found {count}")
text = text.replace(old, new, 1)
text = text.replace(
    "Initializing CheerpJ 4.3 Runtime (Java 17 compatibility mode)...",
    f"Initializing CheerpJ 4.3 Runtime (Java {version} compatibility mode)...",
    1,
)

# Public browser quick-start must use the normal stock sector scale and begin in
# an actual star system. The old small+hyperspace defaults were useful while the
# port only built a lightweight test world, but now make a correct full campaign
# look empty/stranded even when sector generation succeeded. Explicit window
# overrides remain available for diagnostics.
def replace_exact(source: str, old_text: str, new_text: str, label: str) -> str:
    matches = source.count(old_text)
    if matches != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {matches}")
    return source.replace(old_text, new_text, 1)

text = replace_exact(
    text,
    "window.__STARSECTOR_AUTO_CAMPAIGN_SECTOR_SIZE__ || 'small'",
    "window.__STARSECTOR_AUTO_CAMPAIGN_SECTOR_SIZE__ || 'normal'",
    "normal campaign sector size default",
)
text = replace_exact(
    text,
    "window.__STARSECTOR_AUTO_CAMPAIGN_STARTING_LOCATION__ || 'hyperspace'",
    "window.__STARSECTOR_AUTO_CAMPAIGN_STARTING_LOCATION__ || 'Galatia'",
    "real campaign starting location default",
)

# Galatia.java has a CheerpJ auto-detection path that returns early after Tetra,
# skipping tutorial derelicts plus the late system content (Derinkuyu, gate, jump
# points and tutorial debris). The source already catches individual derelict
# failures, so public full-stock mode should request the complete Galatia layout.
# Keep the old lightweight behavior available as an explicit browser override.
# Public launches also request the stock tutorial; the deep gameplay probe keeps
# it disabled so mature ability-slot regression coverage remains meaningful.
compat_anchor = """                    `-Dstarsector.compatibilityFastPath=${browserLightweightSectorCompat}`,
                    `starsector.compatibilityFastPath=${browserLightweightSectorCompat}`"""
full_galatia = """                    `-Dstarsector.compatibilityFastPath=${browserLightweightSectorCompat}`,
                    `starsector.compatibilityFastPath=${browserLightweightSectorCompat}`,
                    `-Dstarsector.skipGalatiaDerelicts=${window.__STARSECTOR_SKIP_GALATIA_DERELICTS__ === true}`,
                    `starsector.skipGalatiaDerelicts=${window.__STARSECTOR_SKIP_GALATIA_DERELICTS__ === true}`,
                    `-Dstarsector.browserTutorial=${window.__STARSECTOR_BROWSER_TUTORIAL__ !== false && !browserGameplayProbe}`,
                    `starsector.browserTutorial=${window.__STARSECTOR_BROWSER_TUTORIAL__ !== false && !browserGameplayProbe}`"""
text = replace_exact(
    text,
    compat_anchor,
    full_galatia,
    "full Galatia tutorial/system default",
)

path.write_text(text, encoding="utf-8", newline="\n")

# The tutorial script clears mature abilities and grants them through progression.
# Patch Fixer's post-create resource repair so it does not immediately overwrite
# that tutorial-managed state. set-cheerpj-version.py already runs before Fixer is
# compiled in every production-like campaign preparation path.
runpy.run_path(str(root / "ci" / "enable-browser-tutorial.py"), run_name="__main__")

print(
    f"Configured CheerpJ Java runtime version={version}; "
    "campaign defaults sectorSize=normal startingLocation=Galatia "
    "fullGalatia=true publicTutorial=true"
)
