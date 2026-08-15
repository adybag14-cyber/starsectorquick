#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIXER = ROOT / "jars" / "Fixer.java"

text = FIXER.read_text(encoding="utf-8-sig")
target = '            requiredFonts.addAll(collectFontPathsFromIndex("graphics/fonts"));\n'
count = text.count(target)
if count != 1:
    raise RuntimeError(
        f"Fixer broad font-index preflight: expected exactly one target, found {count}"
    )
replacement = (
    '            // Browser quick-start: keep the default/explicit/settings-derived font warmup,\n'
    '            // but do not force every font in graphics/fonts/index.list through direct-new-game.\n'
)
FIXER.write_text(text.replace(target, replacement, 1), encoding="utf-8", newline="\n")

patched = FIXER.read_text(encoding="utf-8")
if 'requiredFonts.addAll(collectFontPathsFromIndex("graphics/fonts"))' in patched:
    raise RuntimeError("broad font-index preflight still present after patch")
for required in (
    'requiredFonts.add(defaultFontPath);',
    'requiredFonts.add("graphics/fonts/orbitron24aabold.fnt");',
    'requiredFonts.add("graphics/fonts/orbitron20aabold.fnt");',
    'requiredFonts.add("graphics/fonts/insignia21LTaa.fnt");',
    'String uiTexturePreflight = ensureUiPanelTextureObjectsReadyForDirectNewGame();',
    'String uiSpriteIssue = ensureUiBorderSpriteMappingsReady();',
):
    if required not in patched:
        raise RuntimeError(f"required direct UI safeguard missing after font narrowing: {required}")

print("Narrowed direct-new-game font preflight to default/explicit/settings-derived fonts")
