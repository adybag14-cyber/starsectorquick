#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / 'jars' / 'Fixer.java'
text = path.read_text(encoding='utf-8-sig')
old = '''            boolean skipDirectUiPreflight =
                    Boolean.parseBoolean(
                            System.getProperty(
                                    "starsector.autoCampaignSkipDirectUiPreflight", "true"));
'''
new = '''            boolean skipDirectUiPreflight =
                    Boolean.parseBoolean(
                            System.getProperty(
                                    "starsector.autoCampaignSkipDirectUiPreflight", "false"));
'''
count = text.count(old)
if count != 1:
    raise RuntimeError(f'direct UI preflight default: expected exactly one match, found {count}')
path.write_text(text.replace(old, new, 1), encoding='utf-8', newline='\n')
print('Enabled direct-new-game UI/font/texture preflight by default')
