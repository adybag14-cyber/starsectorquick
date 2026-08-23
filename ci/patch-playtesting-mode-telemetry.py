#!/usr/bin/env python3
from pathlib import Path

p = Path("ci/campaign-render-test.js")
s = p.read_text(encoding="utf-8")
marker = "servedPlaytestingMode"
if marker not in s:
    anchor = "  const ok = reachedExpected && rendered && campaignVisualQuality && progressing\n"
    probe = """  const servedPlaytestingMode = await page.evaluate(async () => {
    const response = await fetch(new URL('data/config/settings.json', location.href), { cache: 'no-store' });
    const text = await response.text();
    return /\"playtestingMode\"\\s*:\\s*true/.test(text);
  });

"""
    if s.count(anchor) != 1:
        raise SystemExit("result gate anchor mismatch")
    s = s.replace(anchor, probe + anchor, 1)
    result_anchor = "    ok,\n"
    if s.count(result_anchor) != 1:
        raise SystemExit("result object anchor mismatch")
    s = s.replace(result_anchor, result_anchor + "    servedPlaytestingMode,\n", 1)
p.write_text(s, encoding="utf-8", newline="\n")
print("Applied served playtesting-mode telemetry")
