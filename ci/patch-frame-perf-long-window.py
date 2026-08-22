#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
p=ROOT/'ci/campaign-render-test.js'
s=p.read_text(encoding='utf-8')
old="""    await page.keyboard.down('w');
    await sleep(2200);
    await page.keyboard.up('w');
    await sleep(5800);"""
new="""    await page.keyboard.down('w');
    await sleep(5000);
    await page.keyboard.up('w');
    // FINALIST_FRAME_WINDOW_V1: keep measuring long enough to replace the
    // entire 240-interval frame-tail ring even near the current 6 FPS floor.
    await sleep(35000);"""
if old not in s: raise SystemExit('performance window anchor missing')
s=s.replace(old,new,1)
p.write_text(s,encoding='utf-8',newline='\n')
print('patched finalist frame window to 40 seconds')
