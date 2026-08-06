#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CANDIDATES = [
    ROOT / 'data' / 'scripts' / 'world' / 'SectorGen.java',
    ROOT / 'starsector' / 'starsector' / 'data' / 'scripts' / 'world' / 'SectorGen.java',
]

signature = 'public void generate(SectorAPI sector) {'
replacement = '''public void generate(SectorAPI sector) {
        // Browser compatibility sector. Match the authoritative precompiled
        // diagnostic path: preserve the stock Corvus shell/current/respawn
        // skeleton, faction relationships and core plugin/scripts, while
        // deferring heavyweight system population.
        System.out.println("BrowserSectorGenDiag: begin lightweight SectorGen.generate");
        try {
            System.out.println("BrowserSectorGenDiag: before Corvus shell bootstrap");
            StarSystemAPI system = sector.createStarSystem("Corvus");
            system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
            sector.setCurrentLocation(system);
            sector.setRespawnLocation(system);
            sector.getRespawnCoordinates().set(-2500, -3500);
            System.out.println("BrowserSectorGenDiag: after Corvus shell bootstrap");
        } catch (Throwable t) {
            System.out.println("BrowserSectorGenDiag: Corvus shell bootstrap partial: " + t);
            try {
                Global.getLogger(SectorGen.class).warn(
                        "SectorGen: browser Corvus shell bootstrap was partial", t);
            } catch (Throwable ignored) {
            }
        }

        try {
            System.out.println("BrowserSectorGenDiag: before initFactionRelationships");
            initFactionRelationships(sector);
            System.out.println("BrowserSectorGenDiag: after initFactionRelationships");
        } catch (Throwable t) {
            System.out.println("BrowserSectorGenDiag: initFactionRelationships partial: " + t);
            try {
                Global.getLogger(SectorGen.class).warn(
                        "SectorGen: browser relationship initialization was partial", t);
            } catch (Throwable ignored) {
            }
        }

        try {
            System.out.println("BrowserSectorGenDiag: before CoreCampaignPluginImpl registration");
            sector.registerPlugin(new CoreCampaignPluginImpl());
            System.out.println("BrowserSectorGenDiag: after CoreCampaignPluginImpl registration");
        } catch (Throwable t) {
            System.out.println("BrowserSectorGenDiag: CoreCampaignPluginImpl registration skipped: " + t);
            try {
                Global.getLogger(SectorGen.class).warn(
                        "SectorGen: browser core campaign plugin registration skipped", t);
            } catch (Throwable ignored) {
            }
        }
        try {
            System.out.println("BrowserSectorGenDiag: before core script registration");
            sector.addScript(new CoreScript());
            sector.addScript(new CoreEventProbabilityManager());
            System.out.println("BrowserSectorGenDiag: after core script registration");
        } catch (Throwable t) {
            System.out.println("BrowserSectorGenDiag: core script registration partial: " + t);
            try {
                Global.getLogger(SectorGen.class).warn(
                        "SectorGen: browser core script registration was partial", t);
            } catch (Throwable ignored) {
            }
        }

        System.out.println("BrowserSectorGenDiag: end lightweight SectorGen.generate");
        try {
            Global.getLogger(SectorGen.class).warn(
                    "SectorGen: using lightweight CheerpJ browser sector; desktop core-system generation deferred.");
        } catch (Throwable ignored) {
        }
    }'''

patched = []
for path in CANDIDATES:
    if not path.is_file():
        continue
    text = path.read_text(encoding='utf-8-sig')
    start = text.find(signature)
    if start < 0:
        raise RuntimeError(f'SectorGen.generate(SectorAPI) not found in {path.relative_to(ROOT)}')
    brace = text.find('{', start)
    if brace < 0:
        raise RuntimeError(f'SectorGen.generate opening brace not found in {path.relative_to(ROOT)}')

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
        raise RuntimeError(f'SectorGen.generate closing brace not found in {path.relative_to(ROOT)}')

    text = text[:start] + replacement + text[end:]
    path.write_text(text, encoding='utf-8', newline='\n')
    patched.append(str(path.relative_to(ROOT)))

if not patched:
    raise RuntimeError('No SectorGen.java source copy was found')

print('Patched SectorGen.generate source copies for lightweight browser campaign bootstrap:')
for item in patched:
    print(f'  {item}')
