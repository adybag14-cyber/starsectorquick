#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
fixer_path = ROOT / 'jars' / 'Fixer.java'
launch_path = ROOT / 'launch.html'


def replace_exact(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


fixer = fixer_path.read_text(encoding='utf-8')

# The watcher had a second, independent false-title fallback even after
# resolveDriverContext() was fixed: if currState was null but states already
# contained a constructed Title Screen object, it labelled that as the active
# title. Never infer state ownership from the registration map.
fixer = replace_exact(
    fixer,
    '''                if (stateId == null && titleStateFromMap != null) {
                    stateId = TITLE_STATE_ID;
                }
''',
    '''                // Do not synthesize Title Screen State from states[] while AppDriver
                // still reports no current state. State registration is not state ownership.
''',
    'watcher state-id map fallback',
)

fixer = replace_exact(
    fixer,
    '''                Object titleState =
                        isTitleState(stateId, currentState) ? currentState : titleStateFromMap;
''',
    '''                Object titleState =
                        isTitleState(stateId, currentState) ? currentState : null;
''',
    'watcher title object map fallback',
)

fixer_path.write_text(fixer, encoding='utf-8', newline='\n')

launch = launch_path.read_text(encoding='utf-8')
# Once AppDriver really owns Title Screen State, ResourceLoaderState has already
# returned. A 25-second Thread.sleep here is actively harmful under CheerpJ's
# cooperative scheduler: the synchronous title render loop can starve the watcher
# after its first title observation. Start the direct campaign in that same watcher
# timeslice instead of sleeping and hoping the watcher gets scheduled again.
launch = replace_exact(
    launch,
    '''                const autoCampaignTitleSettleMs = Math.max(
                    0,
                    Number(window.__STARSECTOR_AUTO_CAMPAIGN_TITLE_SETTLE_MS__ ?? 25000) || 25000
                );''',
    '''                const autoCampaignTitleSettleMs = Math.max(
                    0,
                    Number(window.__STARSECTOR_AUTO_CAMPAIGN_TITLE_SETTLE_MS__ ?? 0) || 0
                );''',
    'owned-title settle delay',
)
launch_path.write_text(launch, encoding='utf-8', newline='\n')

print('Required AppDriver-owned title state and removed post-title watcher sleep')
