#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
fixer_path = ROOT / 'jars' / 'Fixer.java'
text = fixer_path.read_text(encoding='utf-8-sig')

old = '''            if (isCampaignGameManagerNpe(t) && isSyntheticPlayerFleetFallbackEnabled()) {
                String recoveryReadiness = checkDirectNewGameRuntimeReadiness();
                if (recoveryReadiness == null) {
                    directNewGameLastNullMarker = "campaign-newgame-npe-synthetic-recovered";
                    System.out.println(
                            "Fixer: direct new-game campaign-manager NPE recovered via synthetic partial runtime readiness.");
                    return null;
                }
                System.out.println(
                        "Fixer: direct new-game campaign-manager NPE synthetic recovery readiness="
                                + String.valueOf(recoveryReadiness));
            }
'''
new = '''            if (isCampaignGameManagerNpe(t) && isSyntheticPlayerFleetFallbackEnabled()) {
                String recoveryReadiness = checkDirectNewGameRuntimeReadiness();
                System.out.println(
                        "Fixer: direct new-game campaign-manager NPE is not accepted as successful creation; "
                                + "synthetic readiness="
                                + String.valueOf(recoveryReadiness)
                                + ". Logging the original create() failure instead of entering a partial CampaignState.");
            }
'''
count = text.count(old)
if count != 1:
    raise RuntimeError(f'campaign-manager synthetic recovery block: expected exactly one match, found {count}')
fixer_path.write_text(text.replace(old, new, 1), encoding='utf-8', newline='\n')
print('Disabled synthetic-success recovery for CampaignGameManager.create() NPEs')
