#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / 'starsector' / 'starsector' / 'data' / 'scripts' / 'world' / 'SectorGen.java'
text = path.read_text(encoding='utf-8-sig')

signature = 'public void generate(SectorAPI sector) {'
start = text.find(signature)
if start < 0:
    raise RuntimeError('SectorGen.generate(SectorAPI) not found')
brace = text.find('{', start)
if brace < 0:
    raise RuntimeError('SectorGen.generate opening brace not found')

depth = 0
end = None
for i in range(brace, len(text)):
    ch = text[i]
    if ch == '{':
        depth += 1
    elif ch == '}':
        depth -= 1
        if depth == 0:
            end = i + 1
            break
if end is None:
    raise RuntimeError('SectorGen.generate closing brace not found')

replacement = '''public void generate(SectorAPI sector) {
        // Browser compatibility sector. The desktop SectorGen eagerly constructs
        // two dozen star systems, planets, terrain maps and hyperspace nebulae.
        // Those constructors are both extremely expensive under CheerpJ and some
        // rely on desktop-only GL/resource state. CampaignGameManager already
        // creates Hyperspace and the player fleet; keeping this entry point small
        // lets the actual CampaignState own the render loop instead of freezing
        // the browser inside new-game world generation.
        try {
            initFactionRelationships(sector);
        } catch (Throwable t) {
            try {
                Global.getLogger(SectorGen.class).warn(
                        "SectorGen: browser relationship initialization was partial", t);
            } catch (Throwable ignored) {
            }
        }

        try {
            sector.registerPlugin(new CoreCampaignPluginImpl());
        } catch (Throwable t) {
            try {
                Global.getLogger(SectorGen.class).warn(
                        "SectorGen: browser core campaign plugin registration skipped", t);
            } catch (Throwable ignored) {
            }
        }
        try {
            sector.addScript(new CoreScript());
            sector.addScript(new CoreEventProbabilityManager());
        } catch (Throwable t) {
            try {
                Global.getLogger(SectorGen.class).warn(
                        "SectorGen: browser core script registration was partial", t);
            } catch (Throwable ignored) {
            }
        }

        try {
            Global.getLogger(SectorGen.class).warn(
                    "SectorGen: using lightweight CheerpJ browser sector; desktop core-system generation deferred.");
        } catch (Throwable ignored) {
        }
    }'''

text = text[:start] + replacement + text[end:]
path.write_text(text, encoding='utf-8', newline='\n')
print('Patched SectorGen.generate for lightweight browser campaign bootstrap')
