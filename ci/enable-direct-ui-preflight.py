#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

fixer_path = ROOT / 'jars' / 'Fixer.java'
fixer = fixer_path.read_text(encoding='utf-8-sig')
fixer_old = '''            boolean skipDirectUiPreflight =
                    Boolean.parseBoolean(
                            System.getProperty(
                                    "starsector.autoCampaignSkipDirectUiPreflight", "true"));
'''
fixer_new = '''            boolean skipDirectUiPreflight =
                    Boolean.parseBoolean(
                            System.getProperty(
                                    "starsector.autoCampaignSkipDirectUiPreflight", "false"));
'''
count = fixer.count(fixer_old)
if count != 1:
    raise RuntimeError(f'Fixer direct UI preflight default: expected exactly one match, found {count}')
fixer_path.write_text(fixer.replace(fixer_old, fixer_new, 1), encoding='utf-8', newline='\n')

# launch.html explicitly passes the Java property and previously defaulted it to
# true, overriding Fixer's corrected false default. Change the browser-side
# default too so CI exercises the same behavior that a deployed Pages launch
# will use without any query/window override.
launch_path = ROOT / 'launch.html'
launch = launch_path.read_text(encoding='utf-8')
launch_old = '''                const autoCampaignSkipDirectUiPreflight =
                    window.__STARSECTOR_AUTO_CAMPAIGN_SKIP_DIRECT_UI_PREFLIGHT__ !== false;
'''
launch_new = '''                const autoCampaignSkipDirectUiPreflight =
                    window.__STARSECTOR_AUTO_CAMPAIGN_SKIP_DIRECT_UI_PREFLIGHT__ === true;
'''
count = launch.count(launch_old)
if count != 1:
    raise RuntimeError(f'launch direct UI preflight default: expected exactly one match, found {count}')
launch_path.write_text(launch.replace(launch_old, launch_new, 1), encoding='utf-8', newline='\n')

print('Enabled direct-new-game UI/font/texture preflight in Fixer and browser launch defaults')
